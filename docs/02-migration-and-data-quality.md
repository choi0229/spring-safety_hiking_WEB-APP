# 02. Migration & Data Quality

이 문서는 Phase 2(DB.txt 기반 PostgreSQL Schema/Seed 재구축)에서 실제로 수행하고 검증한 내용만 기록한다.
Spring Boot datasource 전환, MyBatis Mapper 수정, Trail/TrailSegment 공간 스키마, GeoJSON Import,
Frontend 수정은 아직 진행하지 않았다(Phase 3 이후).

## 1. DB.txt를 원본 DB 기록으로 사용한 이유

로컬에 과거 프로젝트가 사용하던 MySQL DB가 더 이상 실행되고 있지 않다. 저장소 루트의 `DB.txt`에
당시 DDL과 초기 INSERT 데이터가 텍스트로 보존되어 있어, 이를 실제 MySQL 인스턴스에 접속하지 않고도
PostgreSQL 스키마/시드를 복원할 수 있는 유일한 기준 자료로 사용했다. **`DB.txt`는 이번 작업에서 전혀
수정하지 않았다.** 새로 만든 `schema.sql`/`seed.sql`이 `DB.txt`의 PostgreSQL 번역본이다.

파일 위치:
```
backend/src/main/resources/db/postgresql/
├── schema.sql
└── seed.sql
```
(향후 Phase 4의 Trail/TrailSegment PostGIS 공간 스키마는 별도 디렉터리에 분리할 예정이며, 이번 Phase의
관계형 스키마와 섞지 않는다.)

## 2. MySQL → PostgreSQL DDL 변환 목록

[코드 확인] `DB.txt` 전체(559줄)를 근거로 실제 적용한 변환:

| MySQL(DB.txt) | PostgreSQL(schema.sql) | 비고 |
|---|---|---|
| `AUTO_INCREMENT` | `GENERATED ALWAYS AS IDENTITY` | 모든 정수 PK에 적용. `DB.txt`의 INSERT 문 어디에도 identity 컬럼에 명시적 값을 넣는 곳이 없어 `OVERRIDING SYSTEM VALUE` 없이 그대로 동작 확인 |
| `DATETIME` | `TIMESTAMP` | community, community_comment, path_info |
| `double`(소문자 bare) | `DOUBLE PRECISION` | mountain.mountain_lat/lon, complaint.latitude/longitude |
| `TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | `TIMESTAMP DEFAULT CURRENT_TIMESTAMP` + `BEFORE UPDATE` 트리거(`set_updated_at()`) | PostgreSQL은 `ON UPDATE` DDL절이 없어 트리거로 대체 |
| `DELIMITER //` 기반 트리거 4개 | `CREATE FUNCTION ... $$ ... $$ LANGUAGE plpgsql;` + `CREATE OR REPLACE TRIGGER` | 로직은 그대로, 문법 wrapper만 교체(4번 섹션 참고) |
| `# 주석` | `-- 주석` | 전량 교체 |
| `create schema if not exists team04; use team04;` | 제거 | DB 자체는 Docker `POSTGRES_DB` 환경변수로 관리 |
| `create table user (...)` | `CREATE TABLE app_user (...)` | 3번 섹션 참고 |
| `values("...")`(user, mountain 테이블의 이중따옴표 문자열 리터럴) | `values('...')` | **[코드 확인, 이번 작업에서 새로 발견]** MySQL은 기본 설정에서 큰따옴표를 문자열 리터럴로도 허용하지만, PostgreSQL은 큰따옴표를 항상 식별자(identifier)로 해석한다. `user`, `mountain` 두 테이블의 INSERT만 이 문법을 썼고, 그대로 실행하면 `column "등린이" does not exist` 식의 오류가 난다. seed.sql 생성 스크립트에서 해당 두 블록만 큰따옴표를 작은따옴표로 변환했다(값 내부에 따옴표가 없음을 확인한 뒤 안전하게 처리) |
| `INSERT INTO complaint SET col=val, col2=val2, ...` | `INSERT INTO complaint (col1, col2, ...) VALUES (...)` | 83건 전체 재작성(5번 섹션 참고) |

course/course_reviews/community/mountain의 나머지 INSERT 문법(표준 `INSERT INTO t (...) VALUES (...), (...);`)은
PostgreSQL과 원래 호환되어 문법 변경 없이 그대로 옮겼다.

## 3. `user` → `app_user` 변경 이유

**[코드 확인]** `USER`는 PostgreSQL 예약어(SQL 표준 `CURRENT_USER` 계열 의사 식별자와 충돌)라 테이블명으로
그대로 쓰면 매 쿼리마다 `"user"`로 인용해야 한다. 인용 누락 시 파싱 오류가 나기 쉬워, 테이블명 자체를
`app_user`로 변경하는 방향을 택했다. `schema.sql`에서 이 테이블을 참조하는 모든 FK(`userprofile`,
`usernotification`, `userhelpcall`, `course_reviews`, `community`, `community_comment`,
`community_likes`, `path_info`)를 `REFERENCES app_user(user_id)`로 일괄 수정했다.

**이번 Phase에서는 MyBatis Mapper를 수정하지 않았다.** 매�퍼의 SQL 문자열(`mapper-user.xml` 등 8개 파일에서
`user` 테이블을 참조)을 `app_user`로 맞추는 작업은 Phase 3(Spring Boot/MyBatis PostgreSQL 전환)에서 진행한다.

## 4. Trigger 변환

**[코드 확인 + 실행 결과]** DB.txt의 트리거 4개를 PL/pgSQL 함수 + `CREATE OR REPLACE TRIGGER`(PostgreSQL 14+
문법, 이 프로젝트는 PostgreSQL 16 사용)로 변환했다. 각 트리거는 격리된 트랜잭션(`BEGIN ... ROLLBACK`)에서
실행 전/후 값을 직접 비교해 검증했고, 테스트에 사용한 데이터는 전부 롤백으로 제거해 seed에 남지 않았다.

### 4-1. `update_complaint_state_after_processing_insert`
- 기존 MySQL 동작: `processing`에 INSERT되면 같은 `complaint_no`를 가진 `complaint.complaint_state`를 `'처리완료'`로 갱신
- PostgreSQL 변환 방식: 동일 로직을 `AFTER INSERT ON processing` PL/pgSQL 트리거로 이식
- 실제 검증 방법과 결과:
  ```sql
  BEGIN;
  INSERT INTO complaint (...) VALUES (...) RETURNING complaint_no; -- complaint_no=83, state='미처리'
  INSERT INTO processing (processing_content, processor, processing_complaint_no) VALUES (..., 83);
  SELECT complaint_state FROM complaint WHERE complaint_no=83; -- '처리완료'
  ROLLBACK;
  ```
  결과: BEFORE `미처리` → AFTER `처리완료` 확인, ROLLBACK 후 `complaint` 82건/`processing` 0건으로 원복 확인.

### 4-2. `update_course_rate_after_insert`
- 기존 MySQL 동작: `course_reviews` INSERT 후 해당 `course_id`의 평균 `rating`으로 `course.course_rate` 갱신
- PostgreSQL 변환 방식: 동일 로직을 `AFTER INSERT ON course_reviews` 트리거로 이식(`DECLARE avg_rating NUMERIC(2,1)` 그대로 유지)
- 실제 검증 방법과 결과:
  ```sql
  BEGIN;
  SELECT course_rate FROM course WHERE course_id=5; -- NULL (효자길, 원래 리뷰 없음)
  INSERT INTO course_reviews (course_id, user_id, review_content, rating, difficulty) VALUES (5, 'user01', 'trigger test review', 1, '어려움');
  SELECT course_rate FROM course WHERE course_id=5; -- 1.0
  ROLLBACK;
  ```
  결과: BEFORE `NULL` → AFTER `1.0` 확인, ROLLBACK 후 다시 `NULL`, `course_reviews` 40건으로 원복 확인.
- 부수 확인: seed.sql 자체 실행 과정에서도(course_id 1~4에 대해 40건의 course_reviews가 순서대로 INSERT되며)
  이 트리거가 자동으로 반복 실행되어, 최종 `course.course_rate`가 1~4번 코스에 대해 각각 3.6/4.2/2.2/4.2로
  채워졌음을 확인했다(이는 트리거가 정상 동작한 결과이지 seed.sql이 직접 넣은 값이 아니다).

### 4-3. `update_course_level_after_insert`
- 기존 MySQL 동작: `course_reviews` INSERT 후 난이도 문자열 평균을 계산해 `course.course_level`을 쉬움/보통/어려움으로 갱신
- PostgreSQL 변환 방식: `CASE WHEN` 기반 평균 계산 로직을 그대로 이식
- 실제 검증 방법과 결과: 4-2와 같은 트랜잭션에서 함께 확인. `course_id=5`의 `course_level`이 초기값 `'보통'`
  → 리뷰 삽입 후 `'어려움'`으로 변경, ROLLBACK 후 `'보통'`으로 원복됨을 확인.
- 부수 확인: seed.sql 실행 후 course 1~4의 `course_level`이 각각 보통/어려움/쉬움/어려움으로 채워져 있음을
  확인(트리거가 40건의 리뷰 삽입 과정에서 반복 실행된 결과).

### 4-4. `update_path_info_stats`
- 기존 MySQL 동작: `tracking_path` INSERT 후 같은 `path_id`의 좌표들로 Haversine 거리 합계 /
  평균 속도 / 최대 고도를 계산해 `path_info`에 반영(CTE + `LEAD() OVER (PARTITION BY path_id ORDER BY id)` 사용)
- PostgreSQL 변환 방식: CTE와 윈도우 함수는 표준 SQL이라 로직 변경 없이 그대로 이식, `DELIMITER`/`DECLARE` wrapper만
  PL/pgSQL 함수로 재작성
- 실제 검증 방법과 결과:
  ```sql
  BEGIN;
  INSERT INTO path_info (user_id, start_time, path_img, total_time) VALUES ('user01', now(), '/images/test.jpg', '00:10:00')
    RETURNING id; -- id=1, total_distance/avg_speed/max_altitude 모두 NULL
  INSERT INTO tracking_path (path_id, latitude, longitude, altitude, speed, bearing, timelog) VALUES (1, 37.590000, 126.960000, 100.0, 1.0, 0.0, 't1');
  INSERT INTO tracking_path (path_id, latitude, longitude, altitude, speed, bearing, timelog) VALUES (1, 37.591000, 126.961000, 120.0, 2.0, 10.0, 't2');
  SELECT total_distance, avg_speed, max_altitude FROM path_info WHERE id=1;
  -- total_distance=0.14, avg_speed=1.5, max_altitude=120.0000000
  ROLLBACK;
  ```
  결과: BEFORE 전부 `NULL` → AFTER `total_distance=0.14`, `avg_speed=1.5`, `max_altitude=120.0`으로 정상 계산됨을
  확인, ROLLBACK 후 `path_info`/`tracking_path` 모두 0건으로 원복 확인.

## 5. seed.sql 익명화 원칙

**[설계 제안 → 실행 완료]** `DB.txt`의 `user` 테이블에는 실제 팀원 이름과 평문 비밀번호가 들어 있어,
새로 만드는 `seed.sql`에는 다음과 같이 익명화를 적용했다(매핑은 `DB.txt`의 INSERT 등장 순서 기준):

| DB.txt 원본 user_id | seed.sql user_id | 비고 |
|---|---|---|
| `123` | `user01` | |
| `qwe` | `user02` | |
| `wer` | `user03` | |
| `111` | `user04` | |
| `222` | `user05` | |
| `aaa` | `user06` | |
| `333` | `user07` | |

- `user_name`(실명) → `테스트유저1`~`테스트유저7`로 교체
- `user_pw`(평문 비밀번호) → 전부 고정 더미 값 `devpw1234`로 교체(각 계정을 구분해 테스트할 필요가 없어 단일 값 사용)
- `user_email` → `<seed_user_id>@example.com`으로 교체(원본은 전부 동일한 `123@naver.com`이었음)
- `user_nickname` → `테스트닉네임1`~`테스트닉네임7`로 교체
- `user_age` → 전부 `'20'`으로 교체(원본과 동일하게 숫자 문자열 형식 유지)
- `user_address` → `테스트지역1`~`테스트지역7`로 교체
- `user_gender` → `테스트성별1`~`테스트성별7`로 교체
- `user_institution`(원본에 값이 있던 2개 계정만 해당) → `테스트기관6`, `테스트기관7`로 교체
- 위 5개 컬럼 모두 **컬럼 타입(VARCHAR)과 "값이 채워지는 형식"은 유지**했고, 값의 내용만 일반화했다.
  이 컬럼들을 조건부로 분기하는 로직이 코드에 없음을 확인한 뒤(예: `user_gender` 값으로 분기하는 코드
  없음) 진행했다.
- 위 `user_id` 매핑은 `complaint.user_id`, `course_reviews.user_id`, `community.user_id`에 등장하는
  동일한 원본 값에도 **일관되게** 적용했다(FK 무결성 유지를 위해 필수).
- `complaint.institution`(예: "서대문구청 푸른도시과")은 이번 익명화 대상에 포함하지 않았다 — `app_user`
  테이블의 개인 식별 정보가 아니라 공공기관명이며, 사용자가 요청한 익명화 범위(`app_user`의 5개 컬럼)
  밖이다.
- `DB.txt` 원본은 수정하지 않았다.

## 6. 형식 오류 데이터 처리

**[데이터 확인 → 처리 완료]**

| 위치 | 원본 값(DB.txt) | 문제 | 처리 |
|---|---|---|---|
| `complaint` (초안산, "등산로 파손 조치 요청" 4번째 중복행) | `created_at='2024-11-00 10:45:42'` | 일(day)이 00으로 존재하지 않는 날짜 — PostgreSQL은 파싱 자체를 거부함 | **제외**(어떤 날짜가 정답인지 추정하지 않고, 이 행 자체를 seed.sql에서 뺐다). 결과: complaint 83건 → 82건 |
| `community` ("버섯 채취 자제" / "여기 등산로는 심하게 훼손") | `'2024-11-10.'` | 날짜 끝에 마침표가 붙은 손상된 문자열 | **수정**(마침표만 제거해 `'2024-11-10'`으로 정정 — 값 자체를 추정한 것이 아니라 명백한 오타 문자 제거이므로 기록) |

두 항목 모두 "PostgreSQL에서 실행 자체가 불가능한 형식 오류"에 해당하며, 의미(값)를 임의로 바꾼 것이 아니다.

## 7. Known Data Issues (수정하지 않고 원본 값 그대로 보존)

**[데이터 확인, 의도적으로 수정하지 않음]**

- **`complaint` 중복행**: "초안산"(같은 제목/좌표/시간대 행이 원래 4번, 형식 오류 1건 제외 후 3번),
  "홍복산"(같은 제목/좌표/시간대 행이 3번) 반복 삽입이 원본 그대로 남아 있다. 어느 것이 "진짜" 데이터인지
  DB.txt만으로 판단할 수 없어 그대로 두었다. seed.sql 실행 후 실제로도 3~4건씩 중복되어 있음을 확인했다
  (7-1 참고: `등산로 계단 보수 요청`/`등산객 벤치 정비 요청`/`산책로 말뚝 제거 요청` 각 4건, `등산로 파손 조치 요청` 3건).
- **`course_reviews`의 주석과 실제 `course_id` 불일치**: DB.txt 원본 주석은 `# 무악동`(course_id=2 사용),
  `# 부암동`(course_id=1,3 사용), `# 마루`(course_id=4 사용)로 되어 있지만, `course` 테이블의 실제 auto-increment
  순서는 1=마루, 2=부암동, 3=무악동, 4=홍제동이다. 즉 주석에 적힌 코스명과 실제로 연결된 course_id가 서로
  다르다. **주석을 근거로 course_id 값을 바꾸지 않았다** — seed.sql 실행 후 `course_reviews`에 실제로
  존재하는 `course_id` 값이 원본 그대로 1/2/3/4임을 확인했다.
- **`course.mountain_id`가 실제로는 FK가 아님**: 컬럼명은 `mountain_id`이지만 타입이 `VARCHAR(30)`이고
  값도 `'북한산_백운대'`, `'북한산_둘레길11'` 같은 자유 텍스트 라벨이다. `mountain.mountain_id`(INTEGER,
  identity)와 값 형식이 다르고, DB.txt 자체에 이 두 컬럼을 잇는 FK 제약이 없다. 이번 Phase는 DB.txt를
  있는 그대로 복원하는 것이 목적이므로 `schema.sql`에서도 FK를 추가하지 않고 VARCHAR 그대로 두었다.
  공간 데이터 모델 개선(Trail 개념 도입 시 이 관계를 어떻게 다룰지)은 Phase 4에서 별도로 다룬다.

## 8. DB.txt에는 있으나 코드에서 미사용인 테이블

**[코드 확인]** `userhelpcall` — `DB.txt`에 DDL이 있고 이번 `schema.sql`에도 그대로 복원했지만, 저장소의
어떤 MyBatis 매퍼/DAO도 이 테이블을 참조하지 않는다. 실제 SOS 위치공유 기능(`HelpCall_LocationController`)은
DB를 쓰지 않고 인메모리 `Map`으로만 동작한다.

## 9. 코드에는 있지만 DB.txt에 DDL이 없는 기능 (Known Limitation)

**[코드 확인]** `backend/src/main/resources/mapper/mapper-mentordetail.xml`이 `applymentee`, `mentorcontent`
두 테이블을 SQL에서 직접 참조하지만, `DB.txt` 어디에도 이 두 테이블의 `CREATE TABLE` 정의가 없다.

**이번 Phase에서는 두 테이블의 스키마를 추측해서 만들지 않았다.** `schema.sql`에 포함되어 있지 않으며,
`MentorContentController`/`mapper-mentordetail.xml`이 의존하는 멘토링 관련 기능은 **PostgreSQL 환경에서
동작하지 않는 Known Limitation으로 남긴다.** 이 기능을 되살리려면 원본 DDL을 별도로 확보하거나, 코드
기준으로 새 스키마를 설계하는 결정이 필요하며 이는 이번 Phase의 범위(= DB.txt 복원) 밖이다.

**이 Known Limitation 때문에 Phase 3의 완료 조건도 "MyBatis 매퍼 전체가 동작"이 아니라 "DB.txt로 복원
가능한 기존 기능이 PostgreSQL에서 정상 동작"으로 정의한다.**

## 10. schema.sql / seed.sql 실행 방법과 재실행 정책

**[설계 제안 → 실행 완료]** 이번 프로젝트 규모에서는 Flyway 등 별도 마이그레이션 도구를 추가하지 않았다.
가장 단순한 재현 전략을 택했다:

- `schema.sql`은 **파일 맨 위에서 자신이 만드는 모든 트리거/함수/테이블을 `DROP ... IF EXISTS ... CASCADE`로
  먼저 제거한 뒤 `CREATE`한다.** 즉 몇 번을 실행해도 항상 "완전히 빈 스키마"라는 동일한 결과가 된다
  (개발환경 재현 목적의 스크립트이며, 운영 마이그레이션 도구가 아니다 — 데이터 보존이 필요한 운영 환경에는
  적합하지 않다는 점을 명시).
- `seed.sql`은 `schema.sql` 실행 직후 항상 빈 테이블에 삽입되는 것을 전제로 하므로, 복잡한
  `ON CONFLICT`/UPSERT 없이 단순 `INSERT`만 사용한다. **두 파일은 항상 이 순서로 짝을 지어 실행하는 것을
  기본 시나리오로 한다.**

실행 명령(실제 비밀번호는 `.env`에서 로드하며 여기 문서에는 값 자체를 적지 않는다):
```bash
set -a; source .env; set +a
docker exec -i -e PGPASSWORD="$POSTGRES_PASSWORD" safety_hiking_postgis \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 \
  < backend/src/main/resources/db/postgresql/schema.sql

docker exec -i -e PGPASSWORD="$POSTGRES_PASSWORD" safety_hiking_postgis \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 \
  < backend/src/main/resources/db/postgresql/seed.sql
```

## 11. 실제 실행/검증 결과

**[실행 결과]** 위 두 명령을 이번 세션에서 실제로 Docker PostgreSQL/PostGIS(Phase 1에서 구축한
`safety_hiking_postgis`, PostgreSQL 16.4 / PostGIS 3.4.3)에 실행하고 다음을 확인했다.

1. `schema.sql` 실행: 오류 없이 성공(최초 실행이라 모든 `DROP ... IF EXISTS`가 `NOTICE: ... does not exist,
   skipping`로 처리됨). 14개 테이블, 5개 트리거 함수, 5개 트리거 생성 확인.
2. `seed.sql` 실행: 오류 없이 성공.
3. 테이블 개수: 아래 쿼리로 정확히 애플리케이션 테이블만 골라 확인했다(처음 썼던
   `information_schema.tables WHERE table_schema='public'` 단순 카운트는 PostGIS가 `public` 스키마에
   함께 만드는 `geometry_columns`/`geography_columns` 뷰까지 16으로 잡는 오류가 있어 폐기했다):
   ```sql
   SELECT COUNT(*)
   FROM information_schema.tables
   WHERE table_schema = 'public'
     AND table_type = 'BASE TABLE'
     AND table_name <> 'spatial_ref_sys';
   ```
   결과: **14** (DB.txt의 14개 테이블과 정확히 일치). `applymentee`/`mentorcontent`는 존재하지 않음
   (의도된 결과, 9번 참고).
4. PK: 14개 테이블 모두 `information_schema.table_constraints`에서 PRIMARY KEY 확인.
5. FK: 13개 FK 제약 확인(원본 DB.txt와 동일한 개수 — `complaint`, `course.mountain_id`는 원본대로 FK 없음).
6. Identity 컬럼: `course.course_id`가 `is_identity=YES`, `identity_generation=ALWAYS`로 확인, 실제
   삽입된 행이 1~15로 정상 채번됨을 확인.
7. Seed row count(실측):

   | 테이블 | row count |
   |---|---|
   | app_user | 7 |
   | mountain | 4 |
   | complaint | 82 (원본 83 − 형식 오류 제외 1) |
   | course | 15 |
   | course_reviews | 40 |
   | community | 59 |
   | processing / community_comment / community_likes / path_info / tracking_path / userprofile / usernotification / userhelpcall | 0 (DB.txt 원본에도 INSERT 데이터 없음) |

8. `course` 데이터 조회: 15건 전부 정상 조회, `course_rate`/`course_level`이 트리거에 의해 course_id 1~4에
   대해서만 채워지고 나머지는 리뷰가 없어 `NULL`/원본 기본값(`보통` 등)으로 남아 있음을 확인(정상 동작).
9. `app_user` FK 관계: `course_reviews JOIN app_user`로 정상 조인 확인. 존재하지 않는 `user_id`로
   `course_reviews` INSERT를 시도하면 `insert or update on table "course_reviews" violates foreign key
   constraint "course_reviews_user_id_fkey"`로 정상 거부됨을 확인.
10. 트리거 4개: 4번 섹션에 기록한 대로 각각 격리된 트랜잭션에서 BEFORE/AFTER 값 비교로 실제 동작 확인,
    테스트 데이터는 ROLLBACK으로 완전히 제거됨을 재확인(카운트 원복 확인).
11. 형식 오류 데이터 처리 확인: 6번 섹션의 두 항목이 의도한 대로 반영됨을 실제 조회로 확인(제외된 행은
    3건만 남았고, 수정된 날짜는 `2024-11-10 00:00:00`으로 정상 파싱됨).

## 12. 각 테이블의 실제 seed row count (요약)

7번 표와 동일. DB.txt 원본 INSERT 행 수 대비 변화가 있는 테이블은 `complaint`(83→82, 형식 오류 1건 제외)
뿐이며, 나머지는 원본 INSERT 건수를 그대로 유지했다.

---

# Spatial Data Import (Phase 5)

이 섹션은 Phase 5에서 실제로 수행하고 검증한 내용만 기록한다. Raw Spatial Layer(`Trail`/`TrailFeature`,
Phase 4에서 확정한 스키마)만 채우며, Derived Network Layer(`TrailNode`/`TrailSegment`)는 만들지 않았다.

## 13. Import 원본과 대상 선정 이유

**[코드 확인]** Import 대상은 `frontend/public/data/인왕산ele copy.geojson` 한 파일이다. 이전 분석
(1번째 턴)에서 프론트엔드 소스 전체를 전수 검색한 결과, 등산로류 GeoJSON 파일 중 실제로 애플리케이션
코드에서 `fetch()`되는 것은 이 파일뿐임을 확인했다 — 나머지(`인왕산.geojson`, `인왕산2.geojson`,
`도봉산.geojson`, `산.geojson`, `수리산.geojson`, `test9.geojson` 등)는 코드 어디에서도 참조되지
않는 미사용 파일이다. Point 계열(사고/위험지역/화장실/헬기장/벤치/국가지점번호)은 이번 Phase 범위 밖으로
Import하지 않았다.

## 14. `source_mountain_name`의 출처 — 파일명 기반 가정이 틀렸다는 것을 재확인함

**[데이터 확인, 이전 턴 분석을 실제 파일로 재검증한 결과 정정]** 이전 분석에서는 이 파일 전체가
"인왕산" 데이터라고 가정했다(파일명 "인왕산ele copy.geojson"에서 비롯된 가정). 이번 Phase에서 원본
GeoJSON의 실제 property를 직접 확인한 결과 이 가정은 틀렸다:

- `properties.MNTN_NM`이라는 산 이름 property가 실제로 존재하며, 파일 전체에 걸쳐 **두 가지 값이
  섞여 있다**: `북한산_백운대`(1279개 Feature) / `인왕산`(440개 Feature).
- `PMNTN_NM`(코스명)과 교차 대조한 결과:

  | PMNTN_NM | MNTN_NM | Feature 수 |
  |---|---|---|
  | 마루 | 북한산_백운대 | 1275 |
  | (공백) | 북한산_백운대 | 4 |
  | 무악동구간 | 인왕산 | 216 |
  | 홍제동구간 | 인왕산 | 109 |
  | 부암동구간 | 인왕산 | 115 |

즉 "마루" 코스는 실제로는 **북한산_백운대** 데이터이고, 나머지 세 코스만 인왕산 데이터다. 파일명은
전체 내용을 정확히 대표하지 않는다.

`source_mountain_name`은 **GeoJSON `properties.MNTN_NM`에서 직접 얻은 값**이며, 파일명이나 추정으로
만들어내지 않았다. 이 값은 `trail-import-manifest.json`에 코스별로 명시적으로 박아 두었다(아래 15번).

## 15. Import Manifest

**[설계 제안 → 실행 완료]** 새 dependency 없이 Jackson(이미 의존성 존재)으로 읽는 JSON 파일로
관리한다: `backend/src/main/resources/spatial-import/trail-import-manifest.json`.

Manifest는 두 부분으로 구성된다.

### 15-1. `trails` — 4개 대상 Trail과 기존 `course` 연결 정책

기존 `course.mountain_id`/`course.course_name`과 GeoJSON의 `MNTN_NM`/`PMNTN_NM`을 이름만으로 비교해
자동 연결하지 않고, 각 코스마다 **좌표 기반 독립 증거**까지 확인한 뒤 결정했다:

| source_course_name | source_mountain_name | course_id | mapping_status | 근거 |
|---|---|---|---|---|
| 마루 | 북한산_백운대 | **NULL** | UNVERIFIED | `mountain_id`/`course_name`은 정확히 일치하지만, `course.course_lat/lon`(37.660833, 126.993333)이 이 코스의 GeoJSON 좌표 전체(1275개) 중 가장 가까운 점과도 **약 624m** 떨어져 있어 좌표로 확증되지 않음 |
| 무악동구간 | 인왕산 | 3 | VERIFIED | `mountain_id` 일치 + `course.course_lat/lon`이 실제 GeoJSON 정점과 **약 0.107m** 이내로 일치 |
| 홍제동구간 | 인왕산 | 4 | VERIFIED | `mountain_id` 일치 + 좌표 **약 0.094m** 이내 일치 |
| 부암동구간 | 인왕산 | 2 | VERIFIED | `mountain_id` 일치 + 좌표 **약 0.093m** 이내 일치 |

**[추가 확인 필요, 정직하게 기록]** 대화 중 사용자에게 "4개 코스 모두 mountain_id 일치 + 좌표 소수점
6자리까지 일치"라는 전제로 자동 연결 여부를 질의했고 "간접 증거로 VERIFIED 처리"를 승인받았다. 이후
각 코스별로 실제 최근접 정점 거리를 개별 계산해보니 **이 전제가 "마루"에는 적용되지 않는다는 것을
발견했다** — 무악동/홍제동/부암동 3개는 실제 정점과 0.1m 이내로 일치하지만, 마루는 624m 떨어져 있다.
이름/산 일치만으로 연결하지 않는다는 원래 원칙을 일관되게 적용해, 마루는 **NULL로 유지**하고 나머지
3개만 VERIFIED로 연결했다 — 처음에 사용자에게 제시했던 전제보다 보수적인 결과다.

### 15-2. `excludedFeatures` — 명시적으로 제외한 13개 Feature

인덱스 하드코딩(`if index == 1 || ...`)이 아니라, Manifest에 각 제외 대상을 근거와 함께 선언하고
Importer가 이 목록에 없는 이상 Feature를 만나면 전체 Import를 실패시키도록 구현했다(17번 참고).

**EXCLUDED_SOURCE_FEATURE (4개, `PMNTN_NM`이 공백이라 대상 코스에 매칭되지 않음):**

| source_feature_index | DN | 비고 |
|---|---|---|
| 61 | 177 | 바로 앞뒤(60=176, 62=178) 마루 DN 연속 흐름 한가운데 위치 |
| 167 | 298 | 앞뒤(297/299) 마루 DN 연속 흐름 한가운데 |
| 189 | 320 | 앞뒤(319/321) 마루 DN 연속 흐름 한가운데 |
| 1202 | 172 | 앞뒤(173/171) 마루 DN 연속 흐름 한가운데 |

**[데이터 확인]** 이 4개는 기하학적으로 무관한 노이즈가 아니라, 마루 코스의 DN 연속 구간 한가운데
위치해 원본 데이터에서 코스명 라벨링이 누락된 것으로 보인다. 그럼에도 `(source_file, PMNTN_NM)`
그룹화 규칙상 공백은 4개 대상 코스 중 어디에도 매칭되지 않으므로 제외했다 — 규칙을 임의로 예외
처리하지 않았다.

**INVALID_GEOMETRY_SOURCE_FEATURE (9개, 대상 코스 안에 있지만 구조적으로 무효):**

| source_feature_index | PMNTN_NM | 문제 |
|---|---|---|
| 11 | 마루 | 두 sub-line이 각각 좌표 1개짜리 degenerate line |
| 371, 372, 373, 1192, 1276 | 마루 | MultiLineString 전체가 좌표 1개짜리 단일 sub-line뿐 |
| 998 | 마루 | 유효한 3점 line + 좌표 1개짜리 degenerate line 혼재 |
| 999 | 마루 | 3개 sub-line 중 2개가 degenerate |
| 1522 | 홍제동구간 | 유효한 5점 line + degenerate line 혼재 |

**[데이터 확인, 사용자 확인 후 결정]** 이 9개는 "각 LineString은 최소 2개 좌표를 가져야 한다"는
Java 레벨 구조 검증에 실패한다. 대상 코스 안에 있으므로 원칙적으로는 전체 Import를 실패시켜야 하지만,
검토 결과를 사용자에게 보고하고 "명시적 `INVALID_GEOMETRY_SOURCE_FEATURE`로 분류 후 나머지는 정상
진행" 결정을 받아 이 방식으로 처리했다. 일부(3개 sub-line 중 유효한 것만 취사선택)를 조용히 살리지
않고 **해당 Feature 전체를 제외**했다.

이 13개를 제외한 **1706개**(1719 − 4 − 9)가 실제 Import 대상이다.

## 16. Trail Grouping 기준과 `UNIQUE(source_file, source_course_name)` 안전성 재확인

**[데이터 확인]** `(source_file, PMNTN_NM)` 그룹화 결과 정확히 4개의 서로 다른 `PMNTN_NM` 값(마루,
무악동구간, 홍제동구간, 부암동구간)만 대상 코스로 남고, 파일 내 다른 조합과 충돌하지 않아
`UNIQUE(source_file, source_course_name)` 제약이 이번 Import에서 실제로 위반되지 않음을 Import 전
집계로 재확인했다.

## 17. TrailFeature 정의, `source_feature_index`, `sequence`의 정확한 의미

**[코드 확인]** 원본 GeoJSON Feature 1개 = `TrailFeature` 1개(1:1). **`TrailSegment`라는 용어는 이번
Phase 어디에서도 사용하지 않았다** — Java 클래스명, 변수명, 로그, docs 전부 `TrailFeature`로 통일했다.

- `source_feature_index`: 원본 `FeatureCollection.features[]`의 **절대 index**를 그대로 저장한다.
  노이즈/무효 Feature가 중간에 제외되어도 번호를 다시 매기지 않는다. 실제로 마루 코스의
  `source_feature_index`는 0~1278 범위에서 8개(11,371,372,373,998,999,1192,1276)가 빠진 채로
  저장되어 있음을 확인했다 — 언제든 이 인덱스로 원본 GeoJSON Feature를 역추적할 수 있다.
- `sequence`: **source-order sequence**로만 정의한다 — "동일 Trail에 포함된 정상 Feature들을 원본
  파일 등장 순서대로 필터링했을 때의 연속 순번"이며, **Network/Routing/Traversal 순서가 아니다.**
  코드 주석(`FeatureClassifier.java`, `TrailFeatureInsertParam.java`)과 이 문서 모두에서 이 표현만
  사용했다. 실제 저장 결과, 모든 Trail에서 `sequence`가 0부터 `feature_count - 1`까지 빈틈없이
  연속임을 확인했다(19번 표 참고) — `source_feature_index`에는 결번이 있어도 `sequence`에는 없다.

## 18. Java/Spring/MyBatis Import 구조

**[코드 확인]** 신규 패키지 `com.season.semiproject.spatial`(Controller/API에서 참조되지 않음):

```text
GeoJSON File (파일시스템, frontend/public/data/)
      ↓  Jackson ObjectMapper.readTree()  (JsonNode, Java GIS 객체로 변환하지 않음)
GeoJsonFeatureValidator.validate()        구조 검증(타입/좌표/DN/PMNTN_NM)
      ↓
FeatureClassifier.classify()              Manifest 기준 포함/제외/미확인(unexpected) 분류 (순수 로직, DB 없음)
      ↓  (unexpected 있으면 여기서 예외 발생, DB에 아무 것도 쓰지 않음)
TrailImportService.importTrails()  (@Transactional)
      ↓  MyBatis (mapper-trail-import.xml)
TrailImportDAO → PostgreSQL/PostGIS (trail, trail_feature)
```

- `GeoJsonFeatureValidator`/`FeatureClassifier`는 순수 로직으로 분리해 DB 없이 단위 테스트했다(22번).
- geometry는 Java 쪽에서 좌표 배열을 순회하며 JTS 등 GIS 객체로 만들지 않고, 원본 Feature의
  `geometry` JSON 서브트리를 `ObjectMapper.writeValueAsString()`으로 그대로 직렬화해 MyBatis
  파라미터(`TrailFeatureInsertParam.geometryJson`)로 전달한다.
- 새 JPA/Hibernate Spatial/JTS/GDAL 의존성을 추가하지 않았다 — 기존 MyBatis + `SqlSession` 직접 주입
  패턴(`TrailImportDAO`)을 그대로 따랐다.

### 18-1. `ST_GeomFromGeoJSON` 저장 방식과 실제 SRID 동작

**[실행 결과]** SRID 동작을 추측하지 않고 실제로 테스트했다:
```sql
SELECT ST_SRID(ST_GeomFromGeoJSON('{"type":"MultiLineString","coordinates":[...]}'));
-- 결과: 4326 (별도 ST_SetSRID 없이도 GeoJSON 입력에 대해 기본값 4326을 반환함을 확인)
```
`geometry(MultiLineString,4326)` 타입 컬럼에 `ST_SetSRID`) 없이도 그대로 삽입 성공함을 임시 테이블로
확인했다. 다만 이 동작을 암묵적으로 의존하지 않고 SQL에 `ST_SetSRID(ST_GeomFromGeoJSON(#{geometryJson}), 4326)`을
명시적으로 적어 의도를 분명히 했다(`mapper-trail-import.xml`).

### 18-2. Import 실행 방식

**[설계 제안 → 실행 완료]** `spatial-import` Spring Profile로 게이트했다:
```bash
SPRING_PROFILES_ACTIVE=spatial-import ./mvnw spring-boot:run
```
- `SpatialImportRunner`(`@Component @Profile("spatial-import")` `CommandLineRunner`)는 이 프로필이
  아니면 Spring이 Bean 자체를 만들지 않으므로, 평상시 `./mvnw spring-boot:run`에서는 절대 실행되지
  않는다 — 실제로 Phase 3부터 계속 켜져 있던 평상시 인스턴스가 이번 Import 실행 중에도 방해받지 않고
  계속 정상 응답했음을 확인했다(21-3번 참고).
- `application-spatial-import.properties`에 `spring.main.web-application-type=none`을 설정해, 이
  프로필로 실행할 때는 내장 Tomcat/포트 9000을 아예 띄우지 않는다 — 평상시 실행 중인 인스턴스와 포트
  충돌이 날 수 없는 구조다. 가장 단순한 방식이라고 판단한 이유: 별도 배치 프레임워크(Spring Batch 등)
  도입 없이 Spring Boot 표준 기능(`@Profile`, `CommandLineRunner`, `web-application-type=none`)만으로
  "평상시엔 절대 안 뜨고, 명시적으로 지정했을 때만 1회 실행 후 종료"를 완전히 만족한다.
- 실행이 끝나면 `SpringApplication.exit(context, () -> exitCode)` 후 `System.exit()`으로 JVM을
  종료한다 — 실제로 매 실행이 몇 초 안에 프로세스 자체가 끝나는 것을 확인했다(장시간 떠 있는 서버가
  되지 않음).

## 19. 실제 Import 결과

**[실행 결과]** `SPRING_PROFILES_ACTIVE=spatial-import ./mvnw spring-boot:run`을 Docker
PostgreSQL/PostGIS(`safety_hiking_postgis`)에 대해 실제로 실행했다.

### Trail (4건)

| id | course_id | source_mountain_name | source_course_name |
|---|---|---|---|
| (실행마다 identity 값은 달라짐) | NULL | 북한산_백운대 | 마루 |
| 〃 | 3 | 인왕산 | 무악동구간 |
| 〃 | 4 | 인왕산 | 홍제동구간 |
| 〃 | 2 | 인왕산 | 부암동구간 |

### TrailFeature (1706건)

| source_course_name | Feature 수 | sequence 범위 | source_feature_index 범위 | DN 범위 |
|---|---|---|---|---|
| 마루 | 1267 | 0~1266 | 0~1278 (8개 결번) | 90~655 |
| 무악동구간 | 216 | 0~215 | 1279~1494 | 84~224 |
| 홍제동구간 | 108 | 0~107 | 1495~1603 (1개 결번) | 89~227 |
| 부암동구간 | 115 | 0~114 | 1604~1718 | 99~255 |

`COUNT(DISTINCT sequence) = COUNT(*)`, `COUNT(DISTINCT source_feature_index) = COUNT(*)`를 4개 Trail
전부에서 확인했다(중복 없음).

### PostGIS 저장 값 검증 (1706건 전수)

| 검사 | 통과 |
|---|---|
| `ST_SRID(geom) = 4326` | 1706/1706 |
| `GeometryType(geom) = 'MULTILINESTRING'` | 1706/1706 |
| `NOT ST_IsEmpty(geom)` | 1706/1706 |
| `ST_IsValid(geom)` | 1706/1706 |
| `ST_NPoints(geom) >= 2` | 1706/1706 |
| `ST_IsSimple(geom)` | 1705/1706 |

**[데이터 확인] `ST_IsSimple=false` 1건 분석**: `source_feature_index=1190`(마루). 좌표를 확인한 결과
경로 끝부분에서 두 점의 좌표가 소수점 9자리 수준(`...280802`/`...280812`, 약 0.001m 차이)까지만
다르고 사실상 같은 위치로 되돌아온다 — 즉 경로가 거의 같은 지점을 다시 지나가는 헤어핀/급커브 구간이다.
LineString의 `ST_IsSimple=false`는 폴리곤과 달리 **유효성(validity)과 무관**하다(자기 자신과
스치거나 겹치는 라인도 OGC상 유효한 LineString이다) — `ST_IsValid`는 이 Feature에 대해서도 `true`를
반환했다. 데이터 오류로 판단하지 않고 "실제 지형상 경로가 되짚어가는 구간"으로 해석했다.

### GeoJSON ↔ PostGIS Parity (완전 일치)

**[실행 결과]** 원본에서 추출한 1706개 Feature(제외 대상 13개 제외)를 임시 테이블에 넣고
`source_feature_index`로 조인해 비교했다:

| 검사 | 결과 |
|---|---|
| Python 쪽 집계 건수 | 1706 |
| DB 쪽 TrailFeature 건수 | 1706 |
| `source_feature_index` 기준 조인 건수 | 1706 (1:1 완전 대응) |
| DN 값 일치(`tf.dn_value = pc.dn`) | 1706/1706 |
| geometry 일치(`ST_Equals(tf.geom, ST_SetSRID(ST_GeomFromGeoJSON(원본), 4326))`) | 1706/1706 |

정밀도/표현 차이로 `ST_Equals`가 실패한 사례는 없었다 — 별도 근사 비교 방법이 필요하지 않았다.
**원본 Feature 경계가 1:1로 완전히 보존되었음을 증명한다.**

## 20. Transaction / Idempotency 실검증

**[실행 결과]** 운영 GeoJSON 원본 파일은 수정하지 않고, 별도 fixture(`TrailA`/`TrailB`, source_file=
`test-fixture.geojson`, 저장소에 커밋하지 않음)로 검증했다.

### 20-1. Rollback (source_file replace 중 실패)

1. fixture로 정상 Import 1회 실행 → `trail` 2건, `trail_feature` 3건 생성 확인.
2. 동일 `source_file`에 대해, 두 번째 Trail의 `course_id`를 존재하지 않는 `999999`로 바꾼 Manifest로
   재실행 → `TrailA`는 정상 삭제·재삽입되었으나 `TrailB` 삽입 시점에
   `violates foreign key constraint "fk_trail_course"`로 실패.
3. 실패 직후 DB를 조회한 결과, **삭제됐던 원래 2건(TrailA/TrailB)과 3개 Feature가 정확히 그대로
   남아 있음을 확인했다** — 삭제 → 부분 삽입 → 실패가 발생했음에도 `@Transactional`이 전체를 롤백해
   "삭제 후 중간 실패로 DB가 빈 상태가 되는" 시나리오가 실제로 발생하지 않았다.
4. 이 과정에서 `SpatialImportRunner`가 `TrailImportException`/`IOException`만 잡고 있어 DB 제약
   위반 예외(`DataIntegrityViolationException` 계열)가 처리되지 않고 스택트레이스로 노출되는 것을
   발견해 `RuntimeException` catch 절을 추가, 이후 동일 시나리오에서 "롤백되어 부분 데이터가 커밋되지
   않았다"는 명확한 로그가 남도록 수정했다.
5. fixture 데이터는 검증 직후 `DELETE FROM trail WHERE source_file = 'test-fixture.geojson'`로
   정리했다(실제 인왕산 데이터에는 영향 없음, 별도 `source_file` 값 사용).

### 20-2. Idempotency (동일 원본 2회 실행)

실제 `인왕산ele copy.geojson`으로 동일 Import를 연속 2회 실행한 결과:

| | 1회차 | 2회차 |
|---|---|---|
| Trail 수 | 4 | 4 |
| TrailFeature 수 | 1706 | 1706 |
| Trail별 Feature 수 | 마루 1267 / 무악동 216 / 홍제동 108 / 부암동 115 | 동일 |
| 중복 `source_file` Trail | 없음 | 없음 |

`id`(identity 값) 자체는 매 실행마다 새로 채번되어 달라지지만(예: 1회차 6~9 → 2회차 10~13), "동일
원본을 다시 Import했을 때 논리적으로 동일한 최종 Dataset이 만들어지는가"라는 정의 기준으로
idempotent함을 확인했다.

## 21. Raw Spatial Data Quality — Phase 6 입력자료

**아직 snapping이나 geometry 수정은 하지 않았다. 아래는 전부 통계 측정 결과이며, 이 결과로 이번
Phase에서 tolerance를 확정하지 않는다.**

### 21-1. 연결성(endpoint 간격) 거리 분포

**[실행 결과]** 정의: 동일 Trail 안에서 source-order `sequence`가 연속인 두 TrailFeature에 대해,
앞 Feature의 "끝점"(마지막 sub-line의 마지막 좌표)과 뒤 Feature의 "시작점"(첫 sub-line의 첫 좌표)
사이의 거리를 `geometry`를 `::geography`로 캐스팅해 미터 단위로 계산했다(PostGIS `ST_Distance`).
이 "끝점/시작점" 정의는 MultiLineString 내부에 여러 sub-line이 있을 때 이 프로젝트에서 채택한
작업 정의이며, 다른 정의(예: 모든 sub-line 조합 중 최소 거리)가 Phase 6에서 더 적절하다고 판단되면
바뀔 수 있다.

| source_course_name | 인접 쌍 수 | 0~1m | 1~5m | 5~20m | 20m 초과 | 최소 | 최대 | 평균 |
|---|---|---|---|---|---|---|---|---|
| 마루 | 1266 | 1250 | 7 | 5 | 4 | 0.000m | 98.785m | 0.198m |
| 무악동구간 | 215 | 211 | 3 | 1 | 0 | 0.000m | 12.319m | 0.105m |
| 홍제동구간 | 107 | 105 | 2 | 0 | 0 | 0.000m | 4.440m | 0.081m |
| 부암동구간 | 114 | 114 | 0 | 0 | 0 | 0.000m | 0.620m | 0.005m |
| **전체** | **1702** | **1680(98.7%)** | **12** | **6** | **4** | | | |

### 21-2. 교차 후보(ST_Intersects) 통계

**[실행 결과]** 동일 Trail 내에서 `geom && geom`(GiST bbox 사전 필터, `EXPLAIN`으로 인덱스 사용 확인)
+ `ST_Intersects`로 교차하는 Feature 쌍을 전수 조사했다(1706개 규모에서 87ms, `O(n²)` 우려 없이
인덱스로 충분히 빠름):

- 전체 교차 쌍: **1681**
- 이 중 source-order상 바로 인접(`|seq_a-seq_b|=1`)한, 즉 "기대되는 연결": **1678**
- **인접하지 않은데도 교차하는 쌍: 3건** (전부 무악동구간)

이 3건을 개별 확인한 결과, 교차점은 모두 두 Feature 중 한쪽의 정확한 끝점과 다른 쪽의 정확한
시작점에 **정확히 0m**로 일치했다 — 즉 "라인 중간을 가로지르는" 진짜 mid-line crossing이 아니라,
**3개 이상의 Feature가 하나의 지점을 공유하는(분기/합류 후보) 지점**이다(예: `sequence` 5와 7이
사이의 6을 건너뛰고도 같은 점에서 만남 — 6번 Feature가 매우 짧은 곁가지일 가능성).

**결론: 이번 데이터셋에서 "endpoint가 아닌 진짜 line-intersection"은 0건, 대신 "3개 이상 Feature가
endpoint를 공유하는 분기/합류 후보 지점"이 무악동구간에 최소 2곳 존재**한다. 이는 "Feature 경계 =
실제 이동 구간 경계"라는 가정이 무악동구간에서는 이미 깨져 있다는 것을 데이터로 보여준다 — Phase 4의
Feature≠Segment 설계 판단을 뒷받침하는 실증 근거다.

## 22. Importer 테스트

**[실행 결과]** 새 테스트 프레임워크를 추가하지 않고 기존 JUnit 5(`spring-boot-starter-test`)로
작성했다. DB가 필요 없는 순수 로직은 단위 테스트로, DB 트랜잭션이 필요한 부분(20번)은 실제 Docker
환경에서 직접 실행해 구분했다.

- `GeoJsonFeatureValidatorTest`(9 케이스): 정상 Feature, geometry 타입 오류, 빈 coordinates,
  좌표 구조 오류, DN 누락, DN 비숫자, PMNTN_NM 누락, 좌표 범위 이상. 전부 통과.
- `FeatureClassifierTest`(6 케이스): Manifest JSON 로딩, Trail 그룹화 + `source_feature_index` 보존 +
  source-order `sequence` 생성, Manifest에 선언된 `INVALID_GEOMETRY_SOURCE_FEATURE` 제외, **선언되지
  않은 이상 Feature는 조용히 버려지지 않고 `unexpected`로 보고됨**, 선언되지 않은 미매칭 `PMNTN_NM`도
  동일하게 처리, `FeatureCollection`이 아닌 루트 거부. 전부 통과.
- 전체 테스트 스위트(`./mvnw test`, 기존 `SemiprojectApplicationTests` 포함) 재실행 결과 회귀 없음.

## 23. 기존 기능 영향 없음 확인

**[실행 결과]** Import를 실행하는 동안 Phase 3부터 계속 실행 중이던 일반 인터랙티브 인스턴스
(`./mvnw spring-boot:run`, 프로필 없음, 포트 9000)에 `GET /api/mountains`, `GET /api/course/1`을
호출해 정상 응답(200, 기존 `course_rate=3.6` 등 그대로)을 확인했다 — `spatial-import` 프로필 실행이
평상시 서버를 방해하지 않았다. 프론트엔드(`processGeoJSON`/`groupCoordinates(5)`/지도 렌더링)는
전혀 수정하지 않았고, 이번 Phase가 끝난 시점에도 프론트엔드는 여전히 `frontend/public/data/인왕산ele
copy.geojson` 정적 파일을 직접 읽는다 — DB로의 전환은 Phase 9의 책임이다.

## 24. Phase 6에서 추가로 검토해야 하는 사항

- 21-1번 결과: 20m 초과 gap이 마루에서 4건(최대 98.785m) 존재 — snapping tolerance 후보를 정할 때
  이 값들을 참고해야 한다(이번 Phase에서 tolerance를 확정하지 않음).
- 21-2번 결과: 무악동구간에 최소 2곳의 분기/합류 후보 지점(seq 5/7, seq 64~67 부근)이 있다 — Phase 6
  Node 후보 추출 시 이 위치를 우선 검토 대상으로 삼을 수 있다.
- `ST_IsSimple=false` 1건(마루, source_feature_index=1190) — 헤어핀형 경로가 Node/Segment 분할
  로직에서 어떻게 다뤄져야 하는지는 Phase 6에서 별도 검토가 필요하다.
- 마루 Trail은 `course_id=NULL` 상태로 유지된다 — Phase 6/7 이후에도 이 Trail은 서비스 `course`와
  연결되지 않은 상태로 다뤄야 한다(별도 확인 전까지).

# Accident Raw Data Import (Phase 8)

TrailSegment와의 공간 관계 질의 자체(ST_DWithin/ST_Distance 사용, threshold를 위험 반경으로 보지
않는 이유 등)는 **`docs/05-accident-spatial-query.md`**에 기록했다 — 여기서는 원본 데이터 선정과
Import에 관한 내용만 다루고 중복 서술하지 않는다.

## 25. 42건 파일을 Raw Source로 선택하고 15건 파일을 제외한 이유

원본 후보 파일은 두 개였다.

```text
frontend/public/data/2023산악사고_인왕산.geojson   (42 Feature)
frontend/public/data/2023산악사고_인왕산2.geojson  (15 Feature)
```

실제 frontend 코드 대부분(`MountainDetailView.vue`, `CompareCourseView.vue`,
`MobileMountainDetailView.vue` 등)은 15건 파일(`인왕산2.geojson`)을 사용한다. 그러나 실측 결과
15건 파일은 42건의 부분집합이 아니었다:

```text
15개 Feature 중 실제 고유 report_no(msfrtn_resc_reprt_no) = 5개
그중 하나(20231103201R00167)가 11회 반복 재사용됨
공통 report_no 5개조차 42건 파일과 geometry가 5/5 전부 불일치
type/image 필드가 별도로 추가됨(42건에는 없음)
```

또한 이 15건 중 9건은 TrailSegment로부터 정확히 0.00m(즉 트레일 선 위에 정확히 스냅된 좌표)였다 —
실제 GPS 사고 좌표가 디지타이징된 트레일 정점과 우연히 일치할 확률은 사실상 0이므로, 이는 frontend
마커 UI(코스별 필터 `MNTN_NM2`, 타입별 아이콘/집계 `type`, 사진 `image`)가 요구하는 스키마를
채우기 위해 손으로 만든 **Legacy UI Display Dataset**으로 판단했다.

```text
[데이터 확인] 15건 파일은 42건의 정제본/부분집합이 아니라 별도로 가공된 표시용 데이터다
[설계 판단] Phase 8 Raw Accident Source는 42건 파일(2023산악사고_인왕산.geojson)로 확정한다
```

15건 파일은 이번 Phase에서 Import하지 않았고, 42건 `accident_point`와 어떤 방식으로도 join하지
않는다(예: report_no로 보완 관계를 만드는 것도 하지 않음). 기존 frontend의 15건 기반 마커 UI는
그대로 유지되며, 이번 Phase에서 그 frontend 코드를 수정하지 않았다.

## 26. 42건 원본 GeoJSON 구조와 Data Quality

```text
root.type    = FeatureCollection
crs          = CRS84 (WGS84 경위도, EPSG:4326과 동일 datum) — 명시적으로 선언되어 있음
Feature 수    = 42
geometry     = 100% Point, null/empty 0건
좌표 차원     = 항상 2 (Z값 없음)
```

`msfrtn_resc_reprt_no`(42/42 unique), `dsp_ymd`(42/42 유효 YYYYMMDD), `acdnt_cause_asort_nm`
(42/42), `emd_nm`(42/42) 모두 결측 없이 존재함을 확인했다. `gis_x_axis`/`gis_y_axis` property는
`geometry.coordinates`와 42/42 완전히 일치했다 — DB에는 별도 컬럼으로 중복 저장하지 않는다(geom만
저장).

**동일 좌표 중복은 실제 데이터 특성이며 오류가 아니다**: 10개 좌표 그룹(42건 중 21건)이 서로 다른
`report_no`/`properties`를 가진 채 정확히 같은 좌표를 공유한다. 완전 중복(geometry+properties
모두 동일)은 0건이었다. Import Validator는 이를 invalid로 처리하지 않는다 — `UNIQUE(geom)`
제약도 걸지 않았다(이유는 `accident-schema.sql` 상단 주석 참고).

## 27. Schema Mapping

| DB 컬럼 | 원본 property | 비고 |
|---|---|---|
| `report_no` | `msfrtn_resc_reprt_no` | 42/42 unique(이 파일 내부에서만; 전역 UNIQUE 제약은 걸지 않음) |
| `dispatch_date` | `dsp_ymd` | 정수 YYYYMMDD → DATE 변환(아래 28번) |
| `accident_type` | `acdnt_cause_asort_nm` | |
| `location_name` | `emd_nm` | |
| `geom` | `geometry.coordinates` | `ST_MakePoint`(GeoJSON 파싱 불필요, Point이므로 최단 경로 선택) |
| `raw_properties` | `properties` 객체 전체 | 정규 컬럼과 값이 중복되더라도 원본 그대로 보존(29번) |

나머지 ~61개 property(기상 정보, 소방서 출동 타임라인 등)는 정규 컬럼화하지 않았다 — 42행 규모
데이터에 과도한 컬럼을 만들지 않기 위함이며, 필요해지면 `raw_properties`에서 꺼내 쓸 수 있다.

## 28. `dispatch_date` DATE 변환

저장 전 42건 전체의 `dsp_ymd`가 실제 유효한 달력 날짜로 파싱되는지 Python으로 먼저 확인했다
(`datetime.date(y,m,d)` 전량 성공, 예외 0건). 이후 Java Validator에도 동일한 검증을 넣었다 —
`DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT)`를 사용한다.
`STRICT`가 아닌 기본(SMART) 리졸버는 `2023-02-30`처럼 존재하지 않는 날짜를 조용히 `2023-02-28`로
보정해버리므로 명시적으로 배제했다. 또한 패턴에 `yyyy`(year-of-era) 대신 `uuuu`(year)를 쓴다 —
`STRICT` 모드에서 `yyyy`는 Era가 없으면 정상적인 날짜조차 파싱에 실패하는 java.time의 알려진 특성
때문이다(직접 재현 확인함). 42건 전량이 이 검증을 통과했으므로 `dispatch_date DATE NOT NULL`로
확정했다 — 원본 정수값은 `raw_properties`에도 그대로 남아있어 정보 손실은 없다.

## 29. `raw_properties`는 원본 properties 객체 전체를 보존

정규 컬럼에서 제외한 값만 저장하는 대신, **원본 `properties` 객체 전체**를 JSONB로 저장한다(정규
컬럼과 값이 겹치는 필드도 그대로 포함됨). 목적은 원본 추적성, 향후 추가 필드 사용 가능성, 원본과의
parity 검증이다. 65개 property 중 정규 컬럼화한 것은 4개뿐이므로(다수가 시간 필드 중복/상수/기상
정보/완전 결측), `raw_properties JSONB` + 핵심 4컬럼 조합을 선택했다 — 65개 전부를 개별 컬럼화하는
것은 42행 규모에서 과설계로 판단했다.

## 30. Import Validation과 Transaction

`AccidentFeatureValidator`가 DB 기록 전 모든 Feature를 검증한다: `type=Feature`, `geometry`
존재+`type=Point`, `coordinates` 정확히 2개 원소(숫자, 대한민국 bbox 이내), `msfrtn_resc_reprt_no`
존재, `dsp_ymd` 존재+유효 날짜, `acdnt_cause_asort_nm`/`emd_nm` 존재. 하나라도 실패하면 **DB에 아무
것도 쓰기 전에** 전체 Import를 중단한다(Trail Import와 동일 철학) — 42건이 이미 전량 정상으로
분석되었으므로, 검증 실패는 원본이 바뀌었다는 신호로 보고 조용히 건너뛰지 않는다. `root.crs`가
선언되어 있으면 정확히 `CRS84`인지 확인하고(다르면 Import 중단), 없으면 GeoJSON 기본값으로 간주하고
로그만 남긴다.

Trail Import와 동일하게 `source_file` 단위 replace 전략을 `@Transactional`로 묶었다 —
`deleteAccidentPointsBySourceFile` 후 전량 재삽입, 검증 실패나 DB 제약 위반 시 전체 rollback.

## 31. 42건 Import 실측 결과

**[실행 결과]** `spatial-accident-import` 프로필로 실제 실행:

```text
1차 실행: AccidentImportResult{sourceFile=2023산악사고_인왕산.geojson, totalFeaturesInFile=42, importedCount=42}
2차 실행(동일 파일 재실행): accident_point count = 42 (84로 중복되지 않음, idempotency 확인)
```

원본 42건과 DB 42건의 parity를 `source_feature_index`/`report_no`/`dispatch_date`/
`accident_type`/`location_name`/geometry(경도·위도 1e-9 이내)/`raw_properties`(원본 properties
객체와 완전 동일) 기준으로 전수 비교해 **불일치 0건**을 확인했다. `UNIQUE(source_file,
source_feature_index)` 제약은 중복 삽입 시도를 정상적으로 거부했고(`DataIntegrityViolationException`),
동일 좌표를 공유하는 10개 그룹·21건은 예상대로 별도 행으로 모두 저장되었다. 검증 실패를 유발하는
합성 Feature로 실제 rollback 동작(기존 상태가 그대로 유지됨)도 확인했다. 자세한 자동화 테스트는
`backend/src/test/java/com/season/semiproject/spatial/accident/`(`AccidentFeatureValidatorTest`,
`AccidentImportServiceTest`, `AccidentImportIntegrationTest`) 참고.
