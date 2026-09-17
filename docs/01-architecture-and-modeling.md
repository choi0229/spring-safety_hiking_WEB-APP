# 01. Architecture & Modeling

이 문서는 공간정보 리팩터링(PostgreSQL/PostGIS 전환)의 아키텍처 결정을 기록한다.
이번 커밋 시점에는 **Phase 1(Docker 환경 구축), Phase 2(DB.txt 기반 관계형 Schema/Seed 재구축),
Phase 3(Spring Boot/MyBatis PostgreSQL 전환), Phase 4(Raw Spatial Model: Trail/TrailFeature),
Phase 5(GeoJSON → Trail/TrailFeature Import), Phase 6(Trail Network Modeling:
TrailNode/TrailSegment), Phase 7A(Legacy Slope Compatibility), Phase 8(AccidentPoint Import +
TrailSegment 공간질의), Phase 9A(Trail GeoJSON API + 안전한 non-slope Frontend DB 전환),
Phase 10(전체 재현 검증 및 성능 Baseline 측정), Phase 11(README/포트폴리오 최종 정리)까지
완료**되었으며, Phase 7B(실제 물리적 경사)와 Phase 9B(Legacy Slope 화면 DB 전환)는 보류 상태다.
`trail` 4건/`trail_feature` 1706건(Raw)과 `trail_node` 2526건/`trail_segment`
2122건(Derived Network), `accident_point` 42건(Accident Raw)이 실제로 적재되어 있다.
Import 세부 내용은 `docs/02-migration-and-data-quality.md`의 "Spatial Data Import"/"Accident Raw
Data Import" 절, Network Modeling 세부 내용은 `docs/03-trail-network-modeling.md`, Legacy Slope
Compatibility 세부 내용은 `docs/04-legacy-slope-compatibility.md`, Accident 공간질의 세부 내용은
`docs/05-accident-spatial-query.md`, Frontend API 전환 세부 내용은
`docs/06-frontend-api-compatibility.md`, 재현성/성능 Baseline 세부 내용은
`docs/07-testing-and-performance.md`, 포트폴리오 요약은 `docs/08-portfolio-summary.md`를 참고.

**Phase 4 설계 정정 안내**: Phase 4를 처음 구현할 때는 원본 GeoJSON Feature를 1:1 보존하는 테이블을
`trail_segment`라는 이름으로 만들었다. 이후 재검토 결과 이 이름이 "실제 이동 네트워크의 구간
(Segment)"이라는, 이 테이블이 갖지 않는 의미를 암시한다는 것을 확인해 `trail_feature`로 이름과
개념을 정정했다(19-1, 20-1~20-3번 참고). 이 문서의 Phase 4 섹션은 처음부터 `trail_feature`였던 것처럼
일관되게 서술한다 — 실제 데이터가 없는 시점에 발견해 바로잡은 명칭 정정이며, "당시엔 다르게
불렀다"는 이력을 남길 실익이 없기 때문이다.

이 문서는 실제로 구현·검증한 내용만 기록하며, 계획 단계 내용은 포함하지 않는다(단, 문서 말미의
"전체 Phase 계획" 섹션은 예외로, 아직 구현하지 않은 Phase 5 이후 계획도 명시적으로 "미착수"로 표시해
포함한다). Phase 2의 상세 내용(스키마 변환, seed 익명화, Known Data Issue 등)은
`docs/02-migration-and-data-quality.md`에 별도로 기록되어 있다.

## 1. 배경

기존 프로젝트(`spring-safety_hiking_WEB-APP`)는 MySQL + MyBatis 기반이었다. 실행 중인 과거 MySQL DB는
현재 로컬에 존재하지 않으며, 저장소 루트의 `DB.txt`에 당시 DDL과 초기 INSERT 데이터가 보존되어 있다.
이번 리팩터링은 이 `DB.txt`를 기준 자료로 삼아 PostgreSQL/PostGIS 기반 환경을 새로 구축하는 것을 목표로 한다.

## 2. 왜 PostgreSQL/PostGIS인가

- 기존 등산로 GeoJSON 데이터가 프론트엔드/백엔드 정적 파일로 이중 보관되고 있으며, 공간 데이터가 DB에서
  전혀 관리되지 않는다는 점이 이번 Phase 착수 전 현행 구조 분석(코드 검색 기반, 별도 문서화하지 않고
  이 절에 직접 반영)에서 확인되었다.
- PostGIS는 `geometry`/`geography` 컬럼, GiST 공간 인덱스, `ST_DWithin`/`ST_Intersects` 등 표준화된 공간 질의
  함수를 제공하여, 향후 등산로(Trail/TrailFeature)와 위험지역(RiskZone) 간의 공간 연산을 DB 레벨에서 처리할 수 있게 한다.
- 사전 분석에서 기존 MyBatis 매퍼의 MySQL 전용 문법(`DATE_FORMAT`, `AUTO_INCREMENT`, `DELIMITER` 트리거 등)이
  제한적으로만 존재함을 확인했고, 이는 전체 DB를 PostgreSQL로 이전하는 결정(기존 DB를 PostgreSQL로 변경한다는
  원래 목표와 일치)의 근거가 되었다.

## 3. 왜 Docker Compose인가

- 로컬에 실행 중인 과거 MySQL DB가 없는 상태이므로, 새 PostgreSQL 환경은 "누구나 동일하게 재현 가능"해야 한다.
- Docker Compose를 사용하면 `git clone → docker compose up → schema/seed 적용 → Spring Boot 연결`이라는
  재현 가능한 순서를 보장할 수 있다(schema/seed 적용은 Phase 2에서 진행 예정, 이번 Phase에는 포함하지 않음).
- 로컬에 PostgreSQL을 직접 설치하지 않아도 되므로, 팀원/평가자의 환경에 영향을 주지 않는다.

## 4. 선택한 이미지와 버전

- 이미지: `postgis/postgis:16-3.4`
- 실제 기동 후 확인된 버전(아래 5번 검증 결과 참고):
  - PostgreSQL: **16.4**
  - PostGIS: **3.4.3**
- 별도로 `ENGINE`/문자셋 등 MySQL 전용 옵션에 대응하는 설정은 필요하지 않았다(PostgreSQL/PostGIS 공식 이미지가
  extension 활성화까지 포함하여 배포됨).

## 5. Volume / Healthcheck / 환경변수 설계

`docker-compose.yml` (저장소 루트):

```yaml
services:
  postgis:
    image: postgis/postgis:16-3.4
    container_name: safety_hiking_postgis
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgis_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 5
      start_period: 10s

volumes:
  postgis_data:
```

- **Volume**: named volume `postgis_data`를 `/var/lib/postgresql/data`에 마운트하여, 컨테이너를 재생성해도
  데이터가 보존되도록 했다. (`docker compose restart`로 컨테이너를 재기동해도 healthy 상태와 PostGIS 버전이
  동일하게 유지됨을 확인함 — 5번 참고.)
- **Healthcheck**: `pg_isready`를 사용해 DB가 실제로 연결을 받을 준비가 되었는지 확인한다. `start_period: 10s`로
  초기 부팅 시간을 감안했다.
- **환경변수 분리**: 비밀번호 등 secret은 `.env`(git-ignored)에만 두고, `docker-compose.yml`에는 변수 참조(`${...}`)만
  남겼다. `.env.example`에는 실제 값 대신 `changeme` 플레이스홀더를 커밋했다.
  - `.env`는 `.gitignore`에 `### Local secrets ### / .env` 항목으로 추가하여 커밋되지 않도록 처리했다.
  - `POSTGRES_PORT`는 기본값 5432를 사용하되, 로컬에 다른 PostgreSQL이 떠 있는 경우를 대비해 오버라이드 가능하게
    `${POSTGRES_PORT:-5432}` 형태로 두었다.

## 6. 실제 검증한 명령과 결과

**[코드 확인/실행 결과]** 아래는 이번 세션에서 실제로 실행하고 확인한 결과다. 추측이나 예상치가 아니다.

1. 컨테이너 기동
   ```
   docker compose up -d
   ```
   결과: `postgis/postgis:16-3.4` 이미지 pull 및 컨테이너 생성 성공.
   단, 다음 경고가 출력됨: `The requested image's platform (linux/amd64) does not match the detected host
   platform (linux/arm64/v8)` — Apple Silicon(arm64) 호스트에서 amd64 이미지를 에뮬레이션으로 실행 중임을
   의미한다(6번 "남은 문제" 참고).

2. Healthcheck 확인
   ```
   docker inspect -f '{{.State.Health.Status}}' safety_hiking_postgis
   ```
   결과: `healthy`

3. PostgreSQL 버전 확인
   ```
   docker exec -e PGPASSWORD=... safety_hiking_postgis psql -U safety_hiking_app -d safety_hiking -c "SELECT version();"
   ```
   결과:
   ```
   PostgreSQL 16.4 (Debian 16.4-1.pgdg110+2) on x86_64-pc-linux-gnu, ...
   ```

4. PostGIS extension 활성화 확인
   ```
   docker exec ... psql ... -c "CREATE EXTENSION IF NOT EXISTS postgis;"
   docker exec ... psql ... -c "SELECT PostGIS_Full_Version();"
   docker exec ... psql ... -c "\dx"
   ```
   결과: `NOTICE: extension "postgis" already exists, skipping` — `postgis/postgis` 이미지가 최초 기동 시 대상 DB에
   PostGIS를 자동으로 활성화해 두었음을 의미한다. `\dx` 결과 `postgis 3.4.3`, `postgis_topology 3.4.3`,
   `postgis_tiger_geocoder 3.4.3`, `fuzzystrmatch 1.2`, `plpgsql 1.0`이 설치되어 있음을 확인했다.

5. 공간 함수 동작 확인
   ```sql
   SELECT ST_AsText(ST_MakePoint(126.9648, 37.5890)) AS sample_point,
          ST_Distance(ST_MakePoint(0,0)::geography, ST_MakePoint(0,1)::geography) AS meters_per_degree_lat;
   ```
   결과: `POINT(126.9648 37.589)`, `110574.3885578`(위도 1도 간 거리 약 110.57km, geography 캐스팅이 미터 단위로
   정상 동작함을 확인).

6. Named volume 영속성 확인 — **컨테이너 완전 제거/재생성**으로 검증
   `docker compose restart`는 같은 컨테이너를 재시작하는 것뿐이라 named volume이 실제로 컨테이너 생명주기와
   분리되어 있는지는 증명하지 못한다. 이를 보완하기 위해 컨테이너 자체를 제거했다가 다시 만드는 방식으로
   재검증했다(`docker compose down -v`는 사용하지 않음 — `-v`는 named volume까지 삭제하므로 이번 검증의
   목적과 반대된다):
   ```
   docker exec ... psql ... -c "CREATE TABLE _volume_check(id serial primary key, note text);
                                  INSERT INTO _volume_check(note) VALUES ('created-before-down-up');"
   docker compose down                 # 컨테이너/네트워크 제거, named volume은 유지
   docker volume ls | grep postgis_data   # 컨테이너 제거 후에도 볼륨이 남아있는지 확인
   docker compose up -d                # 컨테이너 재생성
   docker exec ... psql ... -c "SELECT * FROM _volume_check;"
   ```
   결과:
   - `docker compose down` 이후 `docker volume ls`에 `spring-safety_hiking_web-app_postgis_data`가
     여전히 존재함을 확인했다.
   - 컨테이너를 새로 생성(`docker compose up -d`)한 뒤에도 healthy 상태로 정상 기동했다.
   - `_volume_check` 테이블과 그 안의 `created-before-down-up` 행이 컨테이너 재생성 후에도 그대로
     조회됨을 확인했다 — **named volume이 컨테이너 생명주기와 분리되어 있음이 실증되었다.**
   - 검증에 사용한 `_volume_check` 테이블은 확인 직후 `DROP TABLE`로 제거했다(seed 데이터에 영향 없음).

7. 비밀번호 교체(로그 노출 대응)
   Phase 1 최초 검증 과정에서 `.env`의 로컬 개발용 비밀번호가 실행 로그에 한 번 노출되었으므로,
   운영 중인 DB 롤의 비밀번호를 새 랜덤 값으로 교체했다(볼륨을 재생성하지 않고 `ALTER USER`로 교체):
   ```
   docker exec ... psql ... -c "ALTER USER \"safety_hiking_app\" WITH PASSWORD '<new-random-password>';"
   ```
   그 후 `.env`의 `POSTGRES_PASSWORD`도 동일한 새 값으로 갱신했다. 검증은 `docker exec`(컨테이너 내부
   loopback 경유, `pg_hba.conf`상 `trust`로 처리되어 비밀번호를 검사하지 않음)로는 의미가 없다는 것을
   확인했고, 실제 네트워크 인증 경로(`host all all all scram-sha-256`)를 타도록 별도 컨테이너에서
   `psql -h postgis`로 접속해 재검증했다:
   - 새 비밀번호로 접속 → 성공
   - 노출되었던 기존 비밀번호로 접속 → `FATAL: password authentication failed for user "safety_hiking_app"`로 거부됨
   이로써 노출된 비밀번호는 더 이상 유효하지 않음을 실제로 확인했다. `.env`는 계속 `.gitignore`에 의해
   Git에서 제외된 상태다.

## 6-1. 남은 문제 (Known Issue)

- **플랫폼 불일치(arm64 호스트 + amd64 이미지)**: `postgis/postgis:16-3.4`는 이번 호스트(arm64)에서 에뮬레이션으로
  실행된다. 기능상 정상 동작하는 것은 확인했으나(위 검증 항목 전체 통과), 순수 arm64 환경보다 느릴 수 있다.
  [추가 확인 필요] 향후 arm64 네이티브 이미지(예: 커뮤니티에서 배포하는 멀티 아키텍처 태그)로 교체할지는
  별도 판단 사항으로 남긴다 — 이번 Phase의 완료 조건(healthy 기동 + PostGIS 동작 확인)에는 영향 없음.

## 7. Phase 1에서 다루지 않았던 것 (Phase 2/3에서 진행됨)

- `schema.sql`/`seed.sql` → Phase 2에서 작성, 상세는 `docs/02-migration-and-data-quality.md` 참고
- Spring Boot `application.properties`의 PostgreSQL 전환 → 이하 8번부터 이번 Phase 3 내용
- Trail/TrailFeature 공간 스키마, GeoJSON Import, 경사 계산 이전, Frontend 변경 → 아직 미착수(Phase 4 이후)

---

# Phase 3 — Spring Boot / MyBatis PostgreSQL 전환

이 섹션부터는 Phase 3에서 실제로 수행하고 검증한 내용만 기록한다.

## 8. MySQL → PostgreSQL datasource 변경

**[코드 확인]** `backend/pom.xml`에서 `com.mysql:mysql-connector-j`(버전 미명시, Spring Boot BOM 관리)를
제거하고 동일한 방식(버전 미명시)으로 `org.postgresql:postgresql`을 추가했다:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
`./mvnw dependency:tree` 실행 결과 Spring Boot 3.3.4 BOM이 관리하는 **`org.postgresql:postgresql:42.7.4`**가
실제로 resolve됨을 확인했다(버전을 하드코딩하지 않음).

`backend/src/main/resources/application.properties`의 datasource 설정을 다음과 같이 변경했다:
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB}
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
```
- 자격 증명을 소스에 하드코딩하지 않고, 저장소 루트 `.env`(Phase 1에서 만든 파일)의 값을 OS 환경변수로 주입받는
  방식을 택했다. Spring Boot는 `${VAR:default}` 플레이스홀더로 OS 환경변수를 네이티브하게 읽을 수 있어
  별도 라이브러리(dotenv 등) 추가가 필요 없었다(가장 단순한 방식 우선 원칙에 따름).
- 로컬 실행 방법: `set -a; source .env; set +a` 로 `.env`를 셸에 로드한 뒤 `./mvnw spring-boot:run`(또는 IDE
  실행 설정에 동일한 환경변수 등록) 실행.
- `DB_HOST`(기본값 `localhost`)는 `.env`/`.env.example`에 새로 추가했다. `POSTGRES_PORT`/`POSTGRES_DB`/
  `POSTGRES_USER`/`POSTGRES_PASSWORD`는 Phase 1에서 이미 존재하던 값을 그대로 재사용했다.
- Docker Compose에 backend 컨테이너는 추가하지 않았다 — Spring Boot는 기존처럼 로컬 프로세스로 실행하고,
  DB만 Docker로 유지하는 구조를 그대로 따랐다.

## 9. `user` → `app_user` Mapper 전환

**[코드 확인]** 전체 매퍼 XML(`backend/src/main/resources/mapper/*.xml`)을 `FROM`/`JOIN`/`INSERT INTO`/
`UPDATE`/`DELETE FROM` 절 기준으로 전수 검색(정규식 `\b(from|join|into|update)\s+user\b`)해 bare `user`
테이블 참조 12건을 찾아 전부 `app_user`로 변경했다:

| 파일 | 변경 건수 |
|---|---|
| `mapper-user.xml` | 7 (`tryLogin`, `createUser`, `isIdTaken`, `isEmailTaken`, `isNickNameTaken`, `getAllUserIds`, `findUserById`) |
| `mapper-community.xml` | 2 (`getCommunityById`, `getCommentsByCommunityId`의 `left join user u` → `left join app_user u`) |
| `mapper-notification.xml` | 2 (`getAllUserIds_vo2`, `getUserNickNameByUserId`) |
| `mapper-profile.xml` | 1 (`getUserById`) |

Java 도메인 클래스명(`User.java`), 변수명(`userId`), API 경로(`/api/login`, `/api/userInfo/{userId}` 등)는
**요청대로 변경하지 않았다** — DB 테이블명만 변경했다.

## 10. MySQL 전용 Mapper SQL 변환 (전수 재검색 결과)

**[코드 확인]** 기존에 파악된 `DATE_FORMAT` 4곳(`mapper-community.xml`)을 `TO_CHAR(col, 'YYYY-MM-DD')`로
변경했다. 이어서 전체 매퍼를 아래 패턴으로 다시 전수 검색했다:

```
DATE_FORMAT | IFNULL | STR_TO_DATE | GROUP_CONCAT | LAST_INSERT_ID | ON DUPLICATE KEY | ` (backtick)
UNIX_TIMESTAMP | CURDATE | CURTIME | NOW() | ADDDATE | DATE_ADD | DATE_SUB | LOCATE( | SUBSTRING_INDEX
LIMIT n,m (offset 콤마 문법) | RAND()
```

결과: 위 목록 중 이미 처리한 `DATE_FORMAT` 외에는 **발견되지 않았다.** `NOW()`는 여러 매퍼에 남아있지만
PostgreSQL에서도 표준으로 지원되어 변경하지 않았다(기존 판단 유지).

**새로 발견한 문제 2건 (검색 목록에는 없었으나 실제 실행 중 발견):**

1. **`sysdate()`** (`mapper-community.xml`의 `createCommunity` INSERT) — MySQL 전용 함수로 PostgreSQL에
   존재하지 않는다. `now()`로 교체했다.
2. **`ROUND(avg_speed, 2)`** (`mapper-tracking.xml`의 `getPathList`) — `avg_speed` 컬럼이 `REAL` 타입인데
   PostgreSQL은 `round(double precision, integer)` 2-인자 오버로드가 없어(오직 `round(numeric, integer)`만
   존재) 이 문법 검색만으로는 잡히지 않고 **`/api/pathList` 실제 호출 시에야 `function round(real, integer)
   does not exist` 오류로 발견되었다.** `ROUND(avg_speed::numeric, 2)`로 캐스팅을 추가해 해결했다.

이 두 건은 "MySQL 전용 문법"이라기보다 "MySQL은 관대하게 허용했지만 PostgreSQL은 엄격하게 타입/함수를
따지는 지점"에 해당하며, 정적 텍스트 검색만으로는 전부 잡을 수 없어 실제 API 호출 테스트가 필요했다는
점을 기록해 둔다.

## 11. `useGeneratedKeys` 검증 (PostgreSQL IDENTITY)

**[실행 결과]** `mapper-tracking.xml`의 `insertPathInfo`는 `useGeneratedKeys="true" keyProperty="pathId"`를
그대로 유지했다(수정 불필요). 다음과 같이 실제 HTTP 호출로 검증했다:

```
POST /api/savePathInfo  { "userId":"user01", "pathImg":"/images/test.jpg", "totalTime":"00:10:00" }
→ 응답 본문: 1
```

`TrackingController.saveTrackingData()`는 `dao.insertPathInfo(pathInfo)`가 반환한 `pathInfo.getPathId()`
값을 그대로 HTTP 응답으로 돌려주는데, 이 값이 `null`이나 `0`이 아니라 PostgreSQL이 실제로 생성한 identity
값(`1`)으로 채워져 있음을 확인했다 — **MyBatis의 `useGeneratedKeys`가 PostgreSQL `GENERATED ALWAYS AS
IDENTITY` 컬럼에서도 Java 객체에 정상적으로 값을 주입함을 실제 API 레벨에서 검증했다.**

## 12. Spring Boot 실제 기동 결과

**[실행 결과]**
```
$ set -a; source .env; set +a
$ cd backend && ./mvnw spring-boot:run
...
Tomcat started on port 9000 (http) with context path '/'
Started SemiprojectApplication in 1.37 seconds
```
- 오류 없이 기동 성공, MySQL datasource 관련 오류 없음.
- `./mvnw dependency:tree`로 PostgreSQL 드라이버(`org.postgresql:postgresql:42.7.4`) 사용 확인, MySQL
  드라이버는 트리에서 완전히 사라짐을 확인.
- `No MyBatis mapper was found in '[com.season.semiproject]' package` WARN이 출력되지만, 이는 이 프로젝트가
  MyBatis `@Mapper` 인터페이스가 아니라 `SqlSession`을 직접 주입받아 XML 매퍼를 문자열 id로 호출하는 기존
  아키텍처(`MountainDAO` 등 전부 `@Autowired SqlSession`) 때문에 발생하는 **기존부터 있던 무해한 경고**이며,
  이번 마이그레이션으로 새로 생긴 문제가 아니다.
- HikariCP connection pool은 기본 설정을 그대로 사용했다(별도 튜닝 없음, Spring Boot 기본값). 별도의
  pool 설정 변경은 하지 않았다 — API 호출이 정상적으로 DB에 도달하는 것으로 pool이 정상 구성되었음을
  간접 확인했다(13번 참고).

### 기동 전 해결이 필요했던 무관한 블로커 (Firebase, DB 마이그레이션과 무관)

**[코드 확인]** `FirebaseConfig.initializeFirebaseApp()`(`@PostConstruct`)이 클래스패스에서
`FIREBASE_SERVICE_ACCOUNT` 리소스를 찾지 못하면 `RuntimeException`을 던져 **Spring 컨텍스트 전체가
기동되지 않는다.** 이 파일은 저장소 git 이력 전체를 검색해도 커밋된 적이 없어(원래도 각자 로컬에만
보관하던 파일로 추정), 저장소를 그대로 clone하면 DB 설정과 무관하게 애플리케이션이 기동조차 되지 않는다.

이는 PostgreSQL 마이그레이션과 무관한 사전 조건이므로, 로컬 검증을 위해 **실제 Firebase 프로젝트와 무관한,
문법적으로만 유효한 더미 서비스 계정 키**(`backend/src/main/resources/FIREBASE_SERVICE_ACCOUNT`)를 만들어
`.gitignore`에 추가(커밋되지 않음)한 뒤 기동했다. 이 더미 키는 `GoogleCredentials.fromStream()`의 JSON
파싱을 통과시키기 위한 것일 뿐 실제 Firebase 서비스에 인증되지 않으며, FCM 푸시 등 Firebase 기능 자체는
이번 Phase에서 검증하지 않았다(범위 밖).

## 13. 기존 기능 회귀 테스트 결과 (실제 HTTP API 호출)

**[실행 결과]** 아래는 실제로 실행 중인 서버(`http://localhost:9000`)에 curl로 호출해 확인한 결과다.

| 기능 | 호출 | 결과 |
|---|---|---|
| Mountain 목록 | `GET /api/mountains` | 4건 정상 조회 |
| Mountain 상세 | `GET /api/mountains/1` | 정상 조회 |
| Course 목록 | `GET /api/course` | 15건 정상 조회 |
| Course 상세 | `GET /api/course/1` | `courseRate=3.6`(트리거로 채워진 값) 정상 조회 |
| Course 리뷰 조회 | `GET /api/course/1/reviews` | `ratingDistribution` 포함 정상 조회, `userId`가 익명화된 `user01`/`user02`로 정상 표시 |
| Course 리뷰 등록 | `POST /api/reviews/create` (multipart, course_id=5) | 등록 성공. 등록 전 `course 5`는 `courseRate=0.0`(NULL)/`courseLevel=보통` → 등록 후 `courseRate=5.0`/`courseLevel=쉬움`으로 트리거가 정상 반영됨을 확인. 테스트 후 review 삭제 API가 아래 발견된 사전 버그로 동작하지 않아 **직접 SQL로 리뷰 삭제 + course 값 원복**하여 seed 상태 복원 |
| User 로그인 | `POST /api/login` (user01/devpw1234) | 정상 인증, `app_user` 조회 성공 |
| User 정보 조회 | `GET /api/userInfo/user01` | 정상 조회 |
| Community 목록 | `GET /api/communityList` | 59건 정상 조회, `communityRegDate`가 `TO_CHAR` 변환 결과(`"2024-11-18"` 등)로 기존 `DATE_FORMAT` 출력 형식과 동일하게 표시됨 |
| Community 상세 | `GET /api/detail/1` | `left join app_user`로 닉네임 정상 조회(익명화된 `테스트닉네임2`로 표시, `app_user` 조인이 실제로 동작함을 증명) |
| Complaint 목록 | `GET /api/complaint/list` | 82건 정상 조회 |
| Complaint 최근 목록 | `GET /api/complaint/listRecent` | 정상 조회, `complaintState` 값(`미처리` 등) 정상 표시 |
| Complaint 등록 + 처리 Trigger | `POST /api/complaint/insert` → `POST /api/complaint/insertProcessing` | 등록 전 `complaintState=null`(조회 쿼리가 해당 컬럼 미조회, 기존 동작) → processing 등록 후 실제 DB 값이 `미처리`→`처리완료`로 변경됨을 SQL로 직접 확인. `updated_at`도 `BEFORE UPDATE` 트리거로 갱신됨을 확인. 테스트 후 직접 SQL로 두 행 삭제하여 정리 |
| Tracking 생성 + generated key | `POST /api/savePathInfo` | 응답으로 생성된 `pathId=1` 반환 확인(11번 참고) |
| Tracking 경로 저장 | `POST /api/saveTrackingPath` ×2 | 정상 저장 |
| Tracking 통계 Trigger | `GET /api/pathList` | `ROUND` 캐스팅 수정 후 `totalDistance=0.14`, `avgSpeed=1.5`, `maxAltitude=120.00`으로 트리거가 정상 계산함을 확인. 테스트 후 직접 SQL로 정리 |
| Mentoring(Known Limitation) | `GET /api/mentordetail/list`, `GET /api/mentordetail/count` | 애플리케이션 기동에는 영향 없음. 호출 시 각각 `relation "mentorcontent" does not exist`, `relation "applymentee" does not exist`로 **HTTP 500 실패** — 14번에 기록한 Known Limitation과 정확히 일치하는 동작 |

테스트를 위해 생성한 모든 데이터(course_reviews 1건, complaint/processing 각 1건, path_info/tracking_path
각 1·2건)는 테스트 직후 삭제하거나 원래 값으로 되돌렸고, 최종적으로 전체 테이블 row count가 Phase 2에서
확인한 값(app_user 7 / mountain 4 / complaint 82 / course 15 / course_reviews 40 / community 59 / 나머지 0)과
정확히 일치함을 재확인했다.

### 회귀 테스트 중 발견한, 이번 마이그레이션과 무관한 기존 버그

**[코드 확인]** `mapper-review.xml`에 `getReviewById`라는 이름의 SQL 문이 정의되어 있지 않은데,
`ReviewDAO.getReviewById()`가 `session.selectList("getReviewById", ...)`로 이를 호출한다. 이 때문에
`PUT /api/reviews/edit/{id}`와 `DELETE /api/reviews/delete/{id}`는 **MySQL 환경에서도 동일하게 실패했을
것으로 판단되는, PostgreSQL 전환과 무관한 기존 결함**이다. 이번 Phase 범위(DB 마이그레이션) 밖이라
수정하지 않았고, 위 표의 리뷰 등록 테스트 정리도 이 때문에 API 대신 직접 SQL로 수행했다.

## 14. Mentoring 기능 Known Limitation (최종 확인)

`applymentee`, `mentorcontent` 테이블은 Phase 2에서도 이미 생성하지 않기로 결정했다(`DB.txt`에 원본 DDL
없음). Phase 3에서 실제로 확인한 결과:
- **애플리케이션 시작 자체는 이 매퍼 때문에 실패하지 않는다**(MyBatis는 XML을 파싱만 하고 시작 시점에
  테이블 존재 여부를 검증하지 않음).
- 실제로 `/api/mentordetail/list`, `/api/mentordetail/count`를 호출하면 각각 `relation "mentorcontent"
  does not exist`, `relation "applymentee" does not exist`로 HTTP 500이 반환된다(13번 표 참고).
- `/api/mentordetail/create`(POST)는 이번에 직접 호출 테스트하지 않았으나, 동일하게 `INSERT INTO
  MentorContent`가 존재하지 않는 테이블을 대상으로 하므로 동일한 이유로 실패할 것으로 판단된다
  [추가 확인 필요 — 직접 호출로 확인하지 않았으므로 100% 확정 아님].
- 이 기능을 되살리기 위해 임의로 스키마를 생성하지 않았다(요청대로).

## 15. 검증하지 못한 기존 DB 기능

- **`mapper-mentordetail.xml`의 `create`(POST)**: 위 14번 참고, 테이블 부재로 실패할 것이 확실시되나 직접
  호출로 확정하지는 않았다.
- **`community_comment`/`community_likes`/`path_info`(초기 상태)/`tracking_path`(초기 상태)/`userprofile`/
  `usernotification`/`userhelpcall`의 seed 데이터 기반 조회**: `DB.txt` 원본에 이 테이블들의 INSERT 데이터가
  없어(Phase 2에서 확인) seed 상태에서는 빈 목록 조회만 가능하고, 실제 값이 있는 상태에서의 조회 회귀는
  검증하지 못했다. Tracking/Complaint-Processing은 이번 Phase에서 직접 데이터를 만들어 넣어 검증했지만
  (13번 참고), community_comment/community_likes/userprofile/usernotification/userhelpcall은 시간 관계상
  별도로 테스트 데이터를 만들어 검증하지 않았다 [추가 확인 필요].
- **Firebase 관련 기능(FCM 알림 발송 등)**: 12번에서 설명한 더미 키로는 실제 Firebase 인증이 되지 않으므로
  검증 범위 밖이다.

## 16. MySQL 흔적 최종 확인

**[실행 결과]**
```bash
grep -i mysql backend/pom.xml                                   # 결과 없음
grep -i mysql backend/src/main/resources/application.properties # 결과 없음
grep -rn "jdbc:mysql\|com.mysql" backend/src --include="*.java" --include="*.xml" --include="*.properties"  # 결과 없음
grep -noniE "DATE_FORMAT|IFNULL|STR_TO_DATE|GROUP_CONCAT|LAST_INSERT_ID|ON DUPLICATE KEY|\`|sysdate\(\)" backend/src/main/resources/mapper/*.xml  # 결과 없음
```
실행 경로(코드/설정/매퍼)에는 MySQL 의존이 남아있지 않음을 확인했다. `README.md`, `DB.txt` 등 과거 기록
문서의 "MySQL" 언급은 그대로 두었다(요청대로 — 역사적 기준 자료 성격).

---

# Phase 4 — Trail / TrailFeature PostGIS 공간 Schema

이 섹션은 Phase 4에서 실제로 설계·구현·검증한 내용만 기록한다. **원본 GeoJSON Import(Phase 5)는 아직
수행하지 않았고, `trail`/`trail_feature`는 스키마만 존재하며 실제 데이터는 0건이다.**

## 17. Trail을 기존 Course의 하위 객체로 만들지 않은 이유 — "마루" 이름 충돌

**[데이터 확인]** Phase 4 착수 전 재검토 과정에서 다음 충돌 사례를 확인했다.

- PostgreSQL seed 기준 `course_id=1`은 `course_name='마루'`, `mountain_id='북한산_백운대'`(북한산 코스).
- 반면 이번 공간 데이터 Import 대상 파일 `frontend/public/data/인왕산ele copy.geojson`에도
  `properties.PMNTN_NM='마루'`인 코스가 존재하며, 이 파일의 `MNTN_NM`(산 이름)은 **인왕산**이다.

즉 `course_name` 문자열만 보고 매칭하면 **인왕산의 공간 데이터를 북한산 코스에 잘못 연결**하는
사고가 발생한다. 이는 이전 Phase들에서 이미 확인한 "`course.mountain_id`가 자유 텍스트일 뿐 신뢰
가능한 FK가 아니다"라는 문제와 같은 종류의 위험이 이름(`course_name`) 레벨에서도 재현된 것이다.

**[설계 제안 → 확정]** 따라서 `Trail`을 `Course`의 하위 객체로 설계하지 않고, **원천 공간 데이터를
독립적으로 표현하는 객체**로 정의했다. `Trail`은 그 자체로 완결된 레코드(어떤 파일의 어떤 코스인지)이며,
기존 서비스 `Course`와의 관계는 이름이 같다는 이유만으로 자동 연결하지 않고, **실제로 검증된 경우에만
선택적으로** 연결한다.

> **Phase 5 정정 노트 [데이터 확인]**: 위 406번 줄의 "이 파일의 MNTN_NM은 인왕산이다"는 파일명에서
> 비롯된 잘못된 가정이었다. Phase 5에서 실제 `properties.MNTN_NM`을 코스별로 교차 확인한 결과,
> `PMNTN_NM='마루'`인 Feature의 `MNTN_NM`은 실제로는 **북한산_백운대**였다(즉 DB `course_id=1`과
> 산 이름이 정확히 일치한다). 이 절이 예시로 든 "이름 충돌"은 이 특정 코스에는 해당하지 않는 것으로
> 정정한다. 다만 이 절의 **설계 결론(이름만으로 자동 연결하지 않고 nullable FK + 검증된 경우에만
> 연결)은 그대로 유효**하며, 오히려 Phase 5에서 "마루"의 좌표가 DB `course.course_lat/lon`과 624m
> 떨어져 있어 이름·산이 일치해도 결국 `course_id=NULL`로 남겨야 했던 사례로 뒷받침되었다. 전체
> 근거와 나머지 3개 코스의 검증 결과는 `docs/02-migration-and-data-quality.md`의
> "Spatial Data Import" 절(15-1번)에 기록했다.

## 18. `Trail.course_id`를 nullable로 둔 이유

위 충돌 사례 때문에 Phase 5(GeoJSON Import) 시점에 모든 Trail이 즉시 어떤 Course에 연결될 수 있다고
가정할 수 없다. `course_id`를 `NOT NULL`로 만들면 "아직 연결을 검증하지 못한 원천 데이터"를 저장할
방법이 없어진다. 그래서 `course_id INTEGER`(nullable) + `ON DELETE SET NULL`로 설계했다 — Course가
삭제되더라도 그 공간 원천 데이터(TrailFeature의 geometry들)까지 함께 사라지면 안 되기 때문이다
(원천 데이터의 생명주기는 서비스 Course의 생명주기에 종속되지 않는다).

## 19. TrailFeature = 원본 GeoJSON Feature (1:1) — 그리고 왜 이것이 "Segment"가 아닌가

**[설계 제안 → 확정, Phase 2/3 보충분석 재확인]** 이전 분석(2번째 턴 보충분석)에서 이미 다음을 실측으로
확인했었다: `groupCoordinates(5)` 같은 임의 길이 그룹화나 20~30m 단위 재분할은 원본 Feature 경계와
DN 값을 잃게 하고, 84%의 그룹이 서로 다른 DN을 가진 여러 Feature를 섞어버린다는 문제가 있었다.

Phase 4에서는 이 결론을 그대로 스키마에 반영해 `TrailFeature` 한 행 = 원본 GeoJSON `features[]` 배열의
Feature 한 개로 확정했다. `sequence`(진행 순서, Import 시 검증/부여)와 `source_feature_index`(원본 배열
내 원래 위치, 추적용)를 별도 컬럼으로 분리해, 향후 노이즈 Feature를 건너뛰거나 순서를 보정해야 할 경우에도
원본 위치를 잃지 않도록 했다.

### 19-1. 설계 정정: 이 테이블을 처음에 `trail_segment`로 명명한 것은 개념적으로 틀렸다

Phase 4를 처음 구현할 때는 이 테이블을 `trail_segment`라고 이름 붙였다. 이후 이 이름이 암시하는
"Segment(구간)"이라는 개념과 실제로 이 테이블이 저장하는 것 사이에 간극이 있음을 재검토 과정에서
확인했다. **원본 GIS Feature(=원본 GeoJSON 파일 저작 도구가 나눈 단위) ≠ 실제 이동 네트워크의
Segment(=Node와 Node 사이의 단일 이동 구간)**이기 때문이다.

예를 들어 서로 다른 두 Feature A(A→B 선)와 B(C→D 선)가 있고, 실제로는 C가 A-B 선 위의 한 지점이라고
하자:

```text
Feature A:  A ─────────────── B
Feature B:            C
                       │
                       │
                       D
```

기하학적으로는 B가 A-B의 끝점이고 C-D는 별개의 선이지만, **실제 등산로 네트워크 관점에서는 C 지점이
분기점(교차점) 역할을 하며, 진짜 이동 가능한 구간은 "A→C", "C→B", "C→D" 세 개**여야 한다. 이 분기 구조는
원본 Feature의 시작/끝점만으로는 드러나지 않는다 — C는 Feature A의 endpoint가 아니라 그 선 중간의 한
점이기 때문이다.

`TrailFeature`(구 `trail_segment`)를 원본 Feature 그대로 1:1 보존하는 것 자체는 여전히 옳은 설계다
(19번 본문 참고 — Feature 경계를 유지해야 원본 데이터가 손실 없이 보존된다). 문제는 **이 테이블에
"Segment"라는 이름을 붙여서, 마치 이 행 하나하나가 이미 실제 이동 네트워크의 구간인 것처럼 오해하게
만든 것**이었다. 실제로 교차점을 Node로 추출하고 그 Node를 기준으로 geometry를 분할해 진짜 이동
가능한 Edge를 만드는 것은 완전히 별개의, 이후 단계(위상/topology 분석)에서 수행해야 하는 작업이다.

그래서 이 테이블은 **`trail_feature`로 이름을 바꾸고**(20-1번 참고, 실제 스키마도 변경 완료),
"Raw Spatial Layer(원본을 그대로 보존하는 계층)"의 객체로만 정의를 한정한다. "Node/Segment로 구성된
이동 네트워크"는 이 `TrailFeature`를 입력으로 삼아 **나중에 별도로 파생시키는 계층**이며, 20-2번에서
그 계층을 별도로 정의한다.

## 20. Trail.geom을 두지 않은 이유 / Source of Truth

동일한 geometry를 `Trail`과 `TrailFeature` 두 곳에 중복 저장하지 않는다는 원칙(이전 턴에서 이미 결정)을
그대로 유지했다. `TrailFeature.geom`이 유일한 공간 Source of Truth이며, `Trail` 테이블에는 geometry
컬럼이 없다. 전체 Trail 형상이 필요하면 조회 시점에 구성한다.

**[실행 결과, 중요한 함정 발견]** 이 구성이 실제로 가능한지 synthetic 데이터로 검증하는 과정에서, 처음
시도한 단순한 방식이 **틀렸음을 실제로 확인했다**:

```sql
-- 이렇게 하면 안 됨 (실제로 실행해서 확인함)
SELECT ST_LineMerge(ST_Collect(geom ORDER BY sequence)) FROM trail_feature WHERE trail_id = ...;
-- 결과: GEOMETRYCOLLECTION EMPTY
```

원인: `trail_feature.geom`은 이미 `MultiLineString`(그 자체가 컬렉션 타입)이다. 컬렉션 타입 여러 개를
`ST_Collect`로 다시 모으면 PostGIS는 이를 하나의 `MultiLineString`으로 평탄화하지 않고
`GEOMETRYCOLLECTION`으로 감싸며, `ST_LineMerge`는 `GEOMETRYCOLLECTION` 입력에 대해 조용히 빈 결과를
반환한다. 올바른 방법은 `ST_Dump`로 먼저 개별 `LineString`으로 풀어낸 뒤 모으는 것이다:

```sql
WITH parts AS (
  SELECT sequence, (ST_Dump(geom)).geom AS line_geom
  FROM trail_feature WHERE trail_id = ...
)
SELECT ST_LineMerge(ST_Collect(line_geom ORDER BY sequence)) FROM parts;
-- 결과: 실제 LINESTRING으로 정상 병합됨 (아래 25번 검증 결과 참고)
```

이 패턴을 `spatial-schema.sql`의 주석에 그대로 기록해 두었다. **이번 Phase에서는 이 SQL이 실제로 동작하는지만
검증했고, 이를 사용하는 API/서비스 코드는 만들지 않았다**(요청대로).

### 20-1. 명칭 정정: `trail_segment` → `trail_feature`

19-1번에서 설명한 이유로, Phase 4에서 처음 만든 테이블명·제약명·인덱스명을 전부 실제로 변경했다
(문서상 설명만 바꾸고 스키마 이름을 그대로 두지 않았다 — Schema와 문서가 항상 같은 개념/이름을
가리키도록 유지하기 위함). 아직 실제 데이터는 0건이므로(Phase 5 Import 이전) 이름을 바꾸기에 가장
안전한 시점이었다.

| 이전 (Phase 4 최초) | 이후 (정정) |
|---|---|
| `trail_segment` (테이블) | `trail_feature` |
| `fk_trail_segment_trail` | `fk_trail_feature_trail` |
| `chk_trail_segment_sequence` | `chk_trail_feature_sequence` |
| `chk_trail_segment_feature_index` | `chk_trail_feature_source_index` |
| `uq_trail_segment_sequence` | `uq_trail_feature_sequence` |
| `uq_trail_segment_feature_index` | `uq_trail_feature_source_index` |
| `idx_trail_segment_geom` | `idx_trail_feature_geom` |

컬럼명(`id`, `trail_id`, `sequence`, `source_feature_index`, `geom`, `dn_value`)은 바꾸지 않았다 —
문제는 테이블/제약 "이름이 암시하는 개념"이었지 컬럼 구조 자체가 아니었기 때문이다. 이 문서의 이전
섹션(17~20번 본문)에 등장하는 `trail_segment`/`TrailSegment`라는 표기도 전부 `trail_feature`/
`TrailFeature`로 일괄 정정했다 — 이 문서가 설명하는 대상은 처음부터 지금의 `trail_feature`와 동일한
테이블이었고, 이름만 잘못 붙어 있었기 때문에 "당시엔 다르게 불렀다"는 역사적 표기를 남기지 않는다.

### 20-2. 공간 데이터 계층의 재정의: Raw Spatial Layer vs Derived Network Layer

이번 정정의 핵심은 공간 데이터를 **두 개의 서로 다른 책임을 가진 계층**으로 나누어 생각하는 것이다.

```text
GeoJSON
   ↓
Trail                Raw Spatial Layer
   ↓                 (이번 Phase 4에서 실제로 구현·검증함)
TrailFeature         ← Raw Spatial Source of Truth
   ↓
Topology Processing  (아직 구현하지 않음 — Phase 6 예정)
   ↓
TrailNode            Derived Network Layer
TrailSegment         (아직 테이블조차 만들지 않음 — 설계만 아래에 기록)
```

- **Raw Spatial Layer** (`Trail`, `TrailFeature`): 원본 GeoJSON을 손실 없이 그대로 보존하는 것이 유일한
  책임이다. `TrailFeature.geom`이 공간 데이터의 Source of Truth이며, 이는 이번 Phase 4 정정 이후에도
  바뀌지 않는다.
- **Derived Network Layer** (`TrailNode`, `TrailSegment` — 향후, 미구현): `TrailFeature.geom`을 입력으로
  위상(topology) 분석을 수행해 **실제 이동 가능한 교차점/구간**을 파생시키는 계층이다. 원본을 대체하지
  않으며, 원본이 바뀌거나 분석 로직이 개선되면 **다시 생성(재계산)할 수 있어야 한다.**

이 두 계층을 분리하는 이유(19-1번의 교차점 예시가 보여주는 문제에 대한 해법):

- GeoJSON Feature 경계는 데이터 생성/가공 과정의 산물일 뿐, 실제 이동 구간의 경계라는 보장이 없다.
- Feature 중간에 다른 Feature가 접하거나 교차할 수 있다 — 이 경우 교차점은 어느 Feature의 endpoint도
  아니다.
- 이런 교차점/분기점/연결성을 표현하려면 명시적인 Node 개념과, Node 기준으로 geometry를 분할하는
  별도의 처리 단계가 필요하다 — 이는 원본을 보존하는 것과는 다른 책임이다.
- 두 책임을 하나의 테이블(`trail_segment`라는 이름의 원본 보존 테이블)에 섞으면, 원본 데이터와 파생
  네트워크 데이터를 구분할 수 없고, 향후 네트워크를 다시 계산해야 할 때 원본이 훼손될 위험이 생긴다.

### 20-3. TrailNode / TrailSegment (Derived Network Layer) — 향후 설계 후보, 이번 Phase에서 미구현

**[설계 제안, 아직 구현하지 않음 — Phase 6 예정]** 아래는 향후 Phase 6(Trail Network Modeling)에서
검토할 후보 모델이며, 실제 topology 분석 결과가 나오기 전까지는 확정이 아니다. 이번 Phase에서는
테이블을 만들지 않았고 코드/SQL도 구현하지 않았다.

**TrailNode (후보)** — 다음과 같은 공간적 위치를 표현하는 객체가 될 것으로 예상한다.
- 원본 선형 geometry의 종점(끝점)
- 여러 선형 geometry가 만나는 접점
- 선 중간에 다른 선이 만나는 교차점
- 실제 이동 네트워크에서 Segment 분할 기준이 되는 위치

```text
TrailNode (후보, 미확정)
- id
- geom geometry(Point, 4326)
- node_type   -- 세부 값/필요성 모두 미확정
```

**TrailSegment (후보, 새 의미)** — 앞으로 이 이름은 **오직** "TrailNode ↔ TrailNode 사이의 실제 이동
가능한 단일 선형 구간"만을 가리킨다. 더 이상 "GeoJSON Feature 1개"라는 의미로 쓰지 않는다.

```text
TrailSegment (후보, 미확정)
- id
- trail_id            -- 정말 필요한지 미확정
- from_node_id
- to_node_id
- source_feature_id   -- 단일 FK로 충분한지 미확정 (분할된 Segment가 여러 원본 Feature에 걸칠 수 있음)
- geom geometry(LineString, 4326)
- distance_m          -- 저장할지 조회 시 계산할지 미확정
```

이 두 모델은 **Phase 6에서 실제 topology 분석 결과를 확인한 뒤** 아래 미확정 사항들을 결정해서 최종
확정한다(11번 답변의 "아직 확정하면 안 되는 것" 목록과 동일):
- Node snapping tolerance(같거나 매우 가까운 점을 하나의 Node로 볼 기준)
- 교차점 판정 tolerance
- `TrailSegment`의 최종 컬럼(위 후보는 예상일 뿐)
- Segment의 방향성(단방향/양방향) 필요 여부
- 하나의 Segment가 여러 Trail에 속할 수 있는지
- `sequence` 컬럼 필요 여부

## 21. DN에 elevation 의미를 부여하지 않은 이유 / 파생값을 스키마에서 제외한 이유

`trail_feature.dn_value`는 원본 GeoJSON `properties.DN`을 타입만 `NUMERIC`으로 변환해 그대로 저장한
것이며, 컬럼명에도 "elevation"이라는 단어를 쓰지 않았다. 이전 보충분석에서 확인했듯 인왕산 정상 고도
(약 338m로 알려짐)를 초과하는 DN 값(655)이 실측되었고, 3D 시각화 코드는 DN이 아니라 별도로
`Cesium.sampleTerrainMostDetailed()`로 실제 지형 고도를 얻는다는 정황까지 있어, DN이 실제 고도(m)라는
근거가 없다는 결론을 그대로 유지했다.

같은 이유로 `gap_from_previous_m`, `start_elevation`/`end_elevation`/`elevation_delta`, `distance_m`,
`slope`/`slope_percent`/`slope_grade`, `legacy_slope_value`/`legacy_slope_grade`, 렌더링용 5좌표
group, 20~30m 단위 DerivedSegment는 **이번 스키마에 포함하지 않았다.** 이들은 모두 "원천값을 가공한
파생값"이며, `gap_from_previous_m`조차도 geometry와 sequence로 나중에 다시 계산 가능한 데이터 품질
점검용 파생값이지 원천값이 아니다. 경사 계산은 Phase 6, 검증된 DEM 고도는 이번 범위 밖이다.
`created_at`/`updated_at`도 이 공간 모델에서 실제로 쓰일 곳이 없어 관행적으로 추가하지 않았다.

## 22. geometry 타입 / SRID 선택

`geometry(MultiLineString, 4326)`을 그대로 사용했다 — Import 대상으로 확인된
`인왕산ele copy.geojson`의 모든 Feature가 `MultiLineString`이라는 점(이전 분석에서 실측 확인)과
원본 GeoJSON `crs`가 `CRS84`(WGS84 경위도, 4326과 동일 datum)라는 점을 그대로 반영한 것으로, 새로운
판단을 추가하지 않았다. `ST_SRID(geom)=4326`, `GeometryType(geom)='MULTILINESTRING'`을 synthetic
데이터로 실제 확인했다(25번 검증 결과 참고).

## 23. GiST 인덱스 선택 — geometry 인덱스와 향후 geography 질의의 차이

`trail_feature.geom`에 `CREATE INDEX idx_trail_feature_geom ON trail_feature USING GIST (geom)`을
적용했다. **이 인덱스는 `geometry` 컬럼 자체에 대한 공간 인덱스**이며, 이 사실을 과장하지 않기 위해
다음을 명확히 기록해 둔다:

- Phase 7에서 위험지역과의 근접 질의를 `ST_DWithin(geom::geography, ...::geography, 30)`처럼
  **`geography`로 캐스팅**해서 수행할 계획인데, 이 경우 PostgreSQL/PostGIS가 지금 만든
  `geometry` 위의 GiST 인덱스를 그대로 사용한다는 보장이 없다. 캐스팅된 표현식에 대한 질의는 필요에
  따라 별도의 **functional GiST 인덱스**(예: `USING GIST ((geom::geography))`)가 있어야 인덱스를
  효율적으로 탈 수 있다.
- 이번 Phase에서는 실제 위험지역 질의도, `EXPLAIN ANALYZE` 측정도 하지 않았으므로 **어떤 인덱스 전략이
  필요한지 지금 결론 내리지 않는다.** Phase 7에서 실제 질의를 만들고 `EXPLAIN`/`EXPLAIN ANALYZE`로
  확인한 뒤 (a) 기존 geometry GiST로 충분한지, (b) geography functional GiST를 추가할지, (c) 다른
  방식이 필요한지 결정한다. 지금 조기 최적화를 하지 않는다.

## 24. 관계형 schema와 공간 schema를 분리한 이유

```text
backend/src/main/resources/db/postgresql/schema.sql   = DB.txt(과거 MySQL) 관계형 구조 복원
backend/src/main/resources/db/postgis/spatial-schema.sql = 이번 리팩터링에서 새로 추가한 공간 모델
```

`schema.sql`은 "존재했던 것을 PostgreSQL로 옮긴 것"이고 `spatial-schema.sql`은 "이번에 새로 설계한 것"이라
성격이 다르다. 하나의 파일에 섞으면 "무엇이 원본 복원이고 무엇이 신규 설계인지"가 파일만 봐서는 구분되지
않으므로 분리했다. `trail.course_id`가 `course`를 참조하므로 실행 순서는 `schema.sql` → `seed.sql` →
`spatial-schema.sql`이어야 하며, 이를 `spatial-schema.sql` 파일 상단 주석에 명시했다.

**[실행 결과] 재실행 순서의 실제 위험(직접 재현해 확인함):** 이미 공간 테이블이 존재하는 상태에서
관계형 `schema.sql`을 다시 실행하면(`DROP TABLE course ... CASCADE` 포함), PostgreSQL이 다음 NOTICE를
출력한다:
```
NOTICE:  drop cascades to constraint fk_trail_course on table trail
```
즉 **`trail` 테이블과 그 안의 행은 삭제되지 않고 그대로 남지만, `fk_trail_course` 제약만 조용히
사라진다**(트리거처럼 눈에 띄는 경고 없이 FK만 없어짐). 이후 `spatial-schema.sql`을 재실행하면
`trail`/`trail_feature`가 다시 DROP·CREATE되며 FK도 새로 생성되어 정상 복구됨을 실제로 확인했다.
**운영 규칙(간단하게 유지, 별도 도구 도입 없음): 관계형 `schema.sql`을 재실행했다면 반드시 곧바로
`spatial-schema.sql`도 재실행한다.** Flyway 등 마이그레이션 프레임워크는 이번에도 추가하지 않았다.

**`spatial-schema.sql`의 성격을 정확히 표기**: 이 파일은 **개발환경 전체 재구축용 destructive reset
script**다 — 실행할 때마다 `trail_feature`/`trail`을 무조건 DROP 후 재생성하므로, 이미 들어있던 공간
데이터(Import된 Trail/TrailFeature)는 그대로 삭제된다. "몇 번을 실행해도 항상 같은 빈 스키마로
귀결된다"는 의미에서는 반복 실행 가능하지만, 이것이 **데이터 보존을 보장하는 non-destructive
migration이라는 뜻은 아니다.** 이전 버전 주석에 있던 "safe to re-run any number of times"라는 표현은
이 구분을 흐릴 수 있어 파일 상단 주석에서 "DESTRUCTIVE DEV-ENVIRONMENT RESET SCRIPT, NOT A
NON-DESTRUCTIVE MIGRATION"으로 명확히 정정했다.

**향후 DROP 순서 관련 남은 과제**: Phase 6에서 `TrailNode`/`TrailSegment`(Derived Network Layer)
테이블이 추가되면, 이 테이블들이 `trail_feature`(그리고 어쩌면 `trail`)를 참조하게 되므로
`spatial-schema.sql`(또는 이를 대체할 스크립트)의 DROP 순서에 이들을 포함시켜야 한다. 지금은 해당
테이블이 존재하지 않으므로 다루지 않았고, Phase 6 계획에 과제로 남겨둔다(아래 Phase Plan 참고).

## 25. 실제 synthetic SQL 검증 결과 (전체 ROLLBACK, 최종 데이터 0건)

**[실행 결과]** 아래 모든 항목을 Docker PostgreSQL/PostGIS(`safety_hiking_postgis`, PostGIS 3.4)에서
`BEGIN ... SAVEPOINT ... ROLLBACK`으로 실제 실행하고 검증했다. 최종적으로 `trail`/`trail_feature`는
0건임을 재확인했다.

| # | 검증 항목 | 결과 |
|---|---|---|
| 1 | `course_id=NULL`로 trail 생성 | 성공 |
| 2 | `UNIQUE(source_file, source_course_name)` 중복 삽입 | `duplicate key value violates unique constraint "uq_trail_source"`로 정상 거부 |
| 3 | 유효한 `course_id=1`로 trail 생성 | 성공 |
| 4 | 존재하지 않는 `course_id=999999` | `violates foreign key constraint "fk_trail_course"`로 정상 거부 |
| 5~8 | `trail_feature` 생성, `geometry(MultiLineString,4326)` 저장 | 성공. `ST_SRID(geom)=4326`, `GeometryType(geom)='MULTILINESTRING'` 확인 |
| 9 | 동일 `(trail_id, sequence)` 중복 | `violates unique constraint "uq_trail_feature_sequence"`로 정상 거부 |
| 10 | 동일 `(trail_id, source_feature_index)` 중복 | `violates unique constraint "uq_trail_feature_source_index"`로 정상 거부 |
| 11 | 존재하지 않는 `trail_id=999999` | `violates foreign key constraint "fk_trail_feature_trail"`로 정상 거부 |
| — | (추가) `sequence=-1` CHECK 위반 | `violates check constraint "chk_trail_feature_sequence"`로 정상 거부 |
| 12 | GiST 인덱스 존재 확인 | `pg_indexes`에서 `idx_trail_feature_geom ... USING gist (geom)` 확인 |
| 13 | sequence 순서로 `ST_Collect`/`ST_LineMerge` 구성 | 최초 시도(단순 `ST_Collect(geom ORDER BY sequence)`)는 `GEOMETRYCOLLECTION EMPTY`로 **실패 확인**, `ST_Dump` 선행 방식으로 재검증해 `LINESTRING(126.9648 37.589,126.9649 37.5891,126.965 37.5892)` 정상 병합 확인 (20번 참고) |
| — | (추가) 재현 순서 위험 실증 | 24번 참고 — `course` CASCADE가 `trail`의 FK만 제거하고 데이터는 보존함을 실제 확인, 이후 `spatial-schema.sql` 재실행으로 FK 복구 확인 |
| 14 | synthetic 데이터 cleanup | 전부 `ROLLBACK` 또는 재현 순서 테스트 후 재시딩으로 정리. 최종 `SELECT COUNT(*) FROM trail`/`trail_feature` = 0 확인 |

또한 `SELECT PostGIS_Version();` → `3.4 USE_GEOS=1 USE_PROJ=1 USE_STATS=1`로, 공간 테이블이 PostGIS가
실제 활성화된 동일 DB에 생성되었음을 확인했다.

## 26. 이번 Phase에서 만들지 않은 것 (요청대로)

Trail Controller/Service/DAO/MyBatis Mapper, GeoJSON Importer, API, Frontend 변경, JPA/Hibernate
Spatial, JTS 의존성 — 전부 추가하지 않았다. 현재 프로젝트가 MyBatis 기반이라는 점을 유지했고, 공간
타입을 쓴다는 이유만으로 다른 영속성 프레임워크를 도입하지 않았다. 이번 명칭 정정(`trail_segment`→
`trail_feature`) 작업에서도 동일 원칙을 지켰다 — `TrailNode`/`TrailSegment`(Derived Network Layer)는
20-3번에 "향후 설계 후보"로만 기록했고, 테이블도 코드도 만들지 않았다.

---

# 전체 Phase 계획 (Phase 4 설계 정정 반영)

**[설계 제안]** 이전까지는 대화 중에만 존재하던 Phase 계획을 이번에 문서로 처음 정리했다. Phase
0~4는 이미 완료된 내용을 요약한 것이고, Phase 5 이후는 아직 구현하지 않은 계획이다 — 계획과 완료
여부를 구분해서 표기한다.

| Phase | 이름 | 상태 |
|---|---|---|
| 0 | 현행 코드/GeoJSON 분석 | 완료 |
| 0.5 | DB.txt/Mapper 분석 | 완료 |
| 1 | Docker PostgreSQL/PostGIS 환경 구축 | 완료 |
| 2 | PostgreSQL 관계형 Schema/Seed 재구축 | 완료 |
| 3 | Spring Boot/MyBatis PostgreSQL 전환 | 완료 |
| 4 | Raw Spatial Model (Trail + TrailFeature) | **완료(이번 정정 포함)** |
| 5 | GeoJSON → Trail/TrailFeature Import | **완료** |
| 6 | Trail Network Modeling (TrailFeature → topology 분석 → TrailNode → TrailSegment) | **완료** |
| 7A | Legacy Slope Compatibility (기존 경사 색상 계산 backend 재현) | **완료** |
| 7B | 실제 물리적 경사 계산 (DEM/검증된 elevation 기반) | 미착수(Future Work) |
| 8 | AccidentPoint Import + TrailSegment 공간질의 | **완료** |
| 9A | Trail GeoJSON API + 안전한 non-slope Frontend DB 전환 | **완료** |
| 9B | Legacy Slope 4개 화면 DB 전환 | 미착수(보류, 13번 참고) |
| 10 | 전체 재현 검증 및 성능 Baseline 측정 | **완료** |
| 11 | README/포트폴리오 문서 최종 정리 | **완료** |

### Phase 4 — Raw Spatial Model (완료, 이번 정정 반영)
- **목적**: 원본 GeoJSON을 손실 없이 보존하는 Raw Spatial Layer를 확정한다.
- **핵심 작업**: `Trail`/`TrailFeature` 스키마 설계, `trail_segment`→`trail_feature` 명칭 정정(개념
  오류 수정), Raw/Network 계층 분리 문서화.
- **데이터 모델**: `trail`, `trail_feature`(완료). `TrailNode`/`TrailSegment`는 아직 미생성(향후 후보만
  문서화, 20-3번).
- **갱신한 docs**: `docs/01-architecture-and-modeling.md`.
- **완료 조건**: 이 문서 17~26번 + 25번 검증 결과 전체 충족(이번 요청으로 재검증 완료).

### Phase 5 — GeoJSON → Trail/TrailFeature Import (완료)
- **목적**: `frontend/public/data/인왕산ele copy.geojson`의 실제 Feature를 `trail`/`trail_feature`에
  적재한다.
- **실제 결과**: Trail 4건(마루/무악동구간/홍제동구간/부암동구간), TrailFeature 1706건(전체 1719건
  중 노이즈 4건 + geometry 무효 9건 제외), GeoJSON↔PostGIS parity 1706/1706 완전 일치, rollback/
  idempotency 실검증 완료, 연결성/교차 통계 산출 완료. `TrailNode`/`TrailSegment`(Network Layer)는
  생성하지 않았다. 세부 내용(Mapping Manifest, 제외 근거, 실행 로그, 통계 전부)은
  `docs/02-migration-and-data-quality.md`의 "Spatial Data Import" 절에 기록했다 — 여기서 중복
  서술하지 않는다.
- **데이터 모델**: 기존 `trail`/`trail_feature` 스키마 그대로 사용, 신규 테이블 없음(변경 없음).
- **갱신한 docs**: `docs/02-migration-and-data-quality.md`(신규 절), `docs/01`(이 항목 + 17번 정정
  노트).
- **완료 조건**: 충족(위 실제 결과 참고).

### Phase 6 — Trail Network Modeling (완료)

**목적**: `TrailFeature.geom`(Raw Spatial Layer)을 입력으로 위상(topology) 분석을 수행해, 실제 이동
가능한 교차점(`TrailNode`)과 구간(`TrailSegment`)으로 구성된 Derived Network Layer를 만든다.

**실제 결과**: `TrailNode` 2526건, `TrailSegment` 2122건을 exact-match(tolerance=0) 기준으로 생성.
분기(degree≥3) 0건, connected component 405개(무악동구간은 1개로 완전 연결, 마루는 391개로 크게
분절). Mid-line intersection 0건(따라서 `ST_Split` 미사용). Phase 5에서 보고했던 "무악동구간 분기
후보 3건"은 Part 단위 재분석 결과 분기가 아니라 다중-Part Feature 간의 정상적인 직렬 연결이었음을
정정했다. Raw↔Network geometry coverage 100% 일치(길이 9198.810m로 동일), rebuild idempotency와
트랜잭션 rollback(닫힌 루프 fixture로 실제 재현) 검증 완료. 세부 내용(MultiLineString 분해, tolerance
비교 실험, gap 원인 분석, Schema 설계 근거, 검증 전체)은 **`docs/03-trail-network-modeling.md`**에
기록했다 — 여기서 중복 서술하지 않는다.

**데이터 모델**: `trail_node`(id, geom — `trail_id`/`node_type` 없음), `trail_segment`(id,
from_node_id, to_node_id, geom, source_trail_feature_id, source_part_index — `trail_id`/`sequence`/
`direction`/`distance_m` 없음). 신규 파일 `backend/src/main/resources/db/postgis/network-schema.sql`
(spatial-schema.sql과 분리 — Raw 레이어를 절대 삭제하지 않기 위해).

**갱신한 docs**: `docs/03-trail-network-modeling.md`(신규), `docs/01`(이 항목 + 27번 갱신).

**완료 조건**: 충족(`docs/03-trail-network-modeling.md` 참고).

### Phase 7A — Legacy Slope Compatibility (완료)
- **목적**: 새로운 경사 모델을 만드는 것이 아니라, 기존 4개 frontend view가 공유하던
  `processGeoJSON()`/`groupCoordinates()`/`calculateSlope()`/`getColorBySlope()` 동작을 의미
  변경 없이 Java/Spring backend로 재현하고, 독립적인 JS 재구현 기준과의 parity를 검증한다.
- **핵심 결과**: `com.season.semiproject.spatial.legacy` 패키지(`LegacyCoordinate`,
  `LegacySlopeGroup`, `LegacySlopeResult`, `LegacySlopeCalculator`, `LegacySlopeService`)로 구현.
  입력 Source는 **원본 GeoJSON 파일**(`TrailFeature` DB 아님 — geometry 검증 없는 legacy 동작과
  검증된 DB 레이어가 9건의 무효 geometry 때문에 실제로 다른 결과를 낸다는 것을 실측 확인).
  `groupSize`(5/7/12)는 통일하지 않고 파라미터로 유지. 1-point remainder group은 렌더링에서 완전
  제외(slope=0 아님) 확인. Node.js로 독립 재구현한 참조 fixture와 4개 코스 × 3개 groupSize = 12개
  조합 전부 group 단위 parity 통과, groupSize=5 combined 통계(1177 total/1176 rendered,
  색상 290/335/551)가 사전 분석과 일치함을 재확인. DB Schema 변경 없음, API/Frontend 변경 없음.
  세부 내용은 **`docs/04-legacy-slope-compatibility.md`** 참고.
- **갱신한 docs**: `docs/04-legacy-slope-compatibility.md`(신규), `docs/01`(이 항목 + 27번 갱신).
- **완료 조건**: 충족(`docs/04-legacy-slope-compatibility.md` 참고).

### Phase 7B — 실제 물리적 경사 계산 (예정, Future Work)
- **목적**: DEM 등 검증된 고도 데이터를 기반으로 한 실제 slope_percent를 `TrailSegment`(Network
  Layer) 기준으로 정의한다.
- **전제 미해결 사항**: DN이 실제 elevation인지 미확정(반증에 가까운 정황만 있음, 27번 참고), DEM/
  외부 elevation API 미도입, `TrailFeature`/`TrailSegment` 중 어느 단위에 slope를 귀속시킬지 미확정.
- **완료 조건**: 이번 요청에서는 정의하지 않음. 아직 구현하지 않는다.

### Phase 8 — AccidentPoint Import + TrailSegment 공간질의 (완료)
- **목적**: 사고 원본 GeoJSON을 `AccidentPoint`(Raw Accident Spatial Layer)로 보존하고,
  `TrailSegment`(Derived Network Layer)와의 관계를 FK로 고정하지 않고 `ST_DWithin`/`ST_Distance`
  질의 시점 계산으로만 표현한다.
- **핵심 결과**: 사전 분석 결과 `2023산악사고_인왕산.geojson`(42건)만 Raw Source로 채택하고
  `2023산악사고_인왕산2.geojson`(15건)은 별도 가공된 "Legacy UI Display Dataset"으로 분리해
  Import하지 않았다(근거: 15건 중 실제 고유 report_no는 5개뿐이며 그중 1개가 11회 재사용, 9건이
  TrailSegment 위에 정확히 0.00m로 스냅되어 있음). `accident_point` 42건 Import 성공, 원본↔DB
  parity 완전 일치, 재실행 idempotency 확인. `AccidentPoint`에 `trail_segment_id`/`distance_m`
  등 파생 컬럼을 두지 않았다(근거: 42건 전부 nearest/second-nearest 거리차 5m 미만 — 단일 FK로
  고정할 만큼 안정적인 관계가 아님). `/api/spatial/accidents/{id}/nearby-segments`,
  `/api/spatial/trail-segments/{id}/nearby-accidents` 두 API를 구현했고, `distanceMeters`는
  호출자 파라미터로 두어 특정 값을 위험 반경으로 취급하지 않는다. EXPLAIN ANALYZE로 geometry
  GiST가 geography 캐스팅 질의에 자동으로 쓰이지 않음을 재확인했으나, 현재 데이터 규모(사고
  42건/Segment 2122건)에서는 성능 문제가 없어 functional geography index는 추가하지 않았다.
  Trail/Network 레이어(`trail`/`trail_feature`/`trail_node`/`trail_segment`) 데이터는 변경되지
  않았다. 세부 내용은 **`docs/05-accident-spatial-query.md`**와 `docs/02-migration-and-data-quality.md`의
  "Accident Raw Data Import" 절 참고.
- **데이터 모델**: 신규 `accident_point`(id, source_file, source_feature_index, report_no,
  dispatch_date, accident_type, location_name, geom, raw_properties). 신규 파일
  `backend/src/main/resources/db/postgis/accident-schema.sql`(Trail/Network 레이어와 분리).
- **갱신한 docs**: `docs/02-migration-and-data-quality.md`(신규 절), `docs/05-accident-spatial-query.md`
  (신규), `docs/01`(이 항목).
- **완료 조건**: 충족(`docs/05-accident-spatial-query.md` 참고).

### Phase 9A — Trail GeoJSON API + 안전한 non-slope Frontend DB 전환 (완료)
- **목적**: `TrailFeature`(Validated Raw Layer)를 기존 정적 GeoJSON과 같은 property 계약
  (`PMNTN_NM`/`DN`)으로 노출하는 Backend API를 만들고, Legacy Slope 4개 화면을 제외한 안전한
  Frontend 소비처만 이 API로 전환한다.
- **핵심 결과**: `GET /api/spatial/trails/geojson`(신규, `trail_feature` 1706건, 단일 SQL,
  `source_feature_index ASC` 정렬, `ST_AsGeoJSON(geom,15)`) 구현. 사전 분석에서 DB-derived
  source를 `groupCoordinates()` 기반 경사 계산에 그대로 대입하면 마루/홍제동 경사 색상의
  약 30~40%가 원본과 달라짐을 실측했고, 이 회귀를 "invalid Feature 재삽입"이 아니라
  **Validated DB Source(일반 렌더링)와 Legacy Compatibility Source(경사 렌더링)의 계층 분리**로
  해결했다 — 이것이 이번 Phase의 핵심 설계 판단이다. Frontend는 실제 router에 등록되어 사용되는
  7개 화면(Preview/Community/Heatmap/People 등)만 fetch URL 교체로 전환했고(라우팅되지 않는
  4개 파일은 재검증 단계에서 URL 변경을 원복해 불필요한 diff를 남기지 않음), `MountainDetailView.vue`/
  `CompareCourseView.vue`/`MountainDetailView2.vue`/`MobileMountainDetailView.vue` 4개
  Legacy Slope 화면과 `LegacySlopeService`는 원본 정적 파일을 계속 사용한다. Backend
  API/테스트/proxy 실행 확인, 7개 화면의 실제 API 응답 기반 로직 실행 확인, Legacy 4개 화면
  보호까지 재검증 완료(브라우저 육안 확인은 환경 제약으로 대체 검증). 세부 내용은
  **`docs/06-frontend-api-compatibility.md`** 참고.
- **데이터 모델**: 변경 없음(`trail_feature`를 읽기 전용으로 노출). 신규 파일
  `backend/src/main/resources/mapper/mapper-trail-geojson.xml`,
  `com.season.semiproject.spatial.geojson` 패키지(Controller/Service/DAO).
- **갱신한 docs**: `docs/06-frontend-api-compatibility.md`(신규), `docs/01`(이 항목).
- **완료 조건**: 충족(`docs/06-frontend-api-compatibility.md` 참고).

### Phase 9B — Legacy Slope 4개 화면 DB 전환 (보류)
Phase 9A에서 확인된 경사 색상 차이(마루 29.6%, 홍제동 39.5%, groupSize=5 기준)를 해소할 방법이
확정되기 전까지는 착수하지 않는다. `docs/06-frontend-api-compatibility.md` 13번(Phase 9B 필요
여부 판단) 참고.

### Phase 10 — 전체 재현 검증 및 성능 Baseline 측정 (완료)
- **목적**: 성능 개선이 아니라, 현재까지 구축한 구조가 처음부터 재현 가능한지 확인하고 주요
  Import/Network Build/API/공간질의의 실제 성능을 baseline으로 확정한다.
- **핵심 결과**: Backend 93/93 테스트, Frontend build 성공(신규 경고 0건) 재확인. DB 4개 테이블
  geometry integrity 전수 재검증(위반 0건). Raw→Network coverage 100% 재확인(2122=2122 parts,
  길이 차 0.000000m). 별도 임시 DB(`safety_hiking_bench`, 운영 DB 무변경)에서 스키마 적용부터
  Trail Import→Network Build→Accident Import까지 전체 파이프라인을 처음부터 재구성해 baseline
  Dataset(4/1706/2526/2122/42)과 API(1706 Feature)가 정확히 재현됨을 확인했다. Trail GeoJSON
  API 응답시간 8~11ms(p95, warm, localhost), 공간질의 EXPLAIN에서 geometry GiST가 geography
  cast에는 여전히 쓰이지 않고 KNN 연산자에는 쓰임을 재확인 — 추가 index는 넣지 않았다. 세부
  수치는 **`docs/07-testing-and-performance.md`** 참고.
- **갱신한 docs**: `docs/07-testing-and-performance.md`(신규), `docs/01`(이 항목).
- **완료 조건**: 충족(`docs/07-testing-and-performance.md` 참고).

### Phase 11 — README/포트폴리오 문서 최종 정리 (완료)
- **목적**: Phase 0~10에서 실제 구현·검증한 내용을 기준으로, 다른 개발자/면접관이 짧은 시간에
  문제-설계-검증 흐름을 이해할 수 있도록 root README와 포트폴리오 요약 문서를 정리한다. 새 기능
  개발이나 성능 튜닝은 하지 않았다.
- **핵심 결과**: root `README.md`를 원본 LX 아카데미 팀 프로젝트 소개와 이번 개인 리팩터링을
  명확히 구분해 재작성(Architecture/Spatial Data Model Mermaid 다이어그램, 핵심 Design
  Decision, API, 검증 결과, Quick Start, Known Limitations 포함). `docs/08-portfolio-summary.md`
  (신규)에 면접 대비용 설계 Q&A와 정량 검증 요약을 별도로 정리했다. `docs/01~07` 전체를 재검토해
  수치/링크 일관성을 확인했고, `docs/04`의 Phase 9 관련 서술 1건(당시엔 "Phase 9에서
  `LegacySlopeService` 재사용 예정"이었으나 실제로는 재사용하지 않음)과 `docs/01`의 존재하지
  않는 `00-current-state-analysis.md` 참조 1건을 실제 결과에 맞게 수정했다. 그 외 수치
  불일치나 broken link는 발견되지 않았다.
- **갱신한 docs**: `README.md`, `docs/08-portfolio-summary.md`(신규), `docs/04`(Phase 9 서술
  정정), `docs/01`(이 항목 + 위 정정 1건).
- **완료 조건**: 충족.

## 27. Phase 6에서 해결된 것 / 여전히 확정하면 안 되는 것

**Phase 6에서 실제 데이터로 확정한 것** (자세한 근거는 `docs/03-trail-network-modeling.md` 참고):
- **Node/교차점 tolerance**: 0(exact match만 적용)으로 확정. "필요 없으면 적용하지 않는다"는
  조건을 만족한 것이지, 임의의 tolerance 값을 정한 것이 아니다. 0.5~5m 후보를 실제로 비교 실험한
  결과와 채택하지 않은 이유는 문서화했다.
- **Mid-line intersection**: 이 데이터셋에는 0건임을 확정. `ST_Split` 등 분할 로직은 구현하지 않았다.
- **`TrailSegment`의 컬럼**: `id, from_node_id, to_node_id, geom, source_trail_feature_id,
  source_part_index`로 확정(`trail_id`/`sequence`/`direction`/`distance_m` 제외 확정).
- **Segment 방향성**: 원본에 방향 근거 없음 확인, 양방향으로 해석하기로 확정, 별도 컬럼 추가하지 않음.
- **하나의 Node/Segment와 여러 Trail의 관계**: 현재 데이터엔 cross-trail 공유가 0건(최근접
  271.8m)이었으나, 스키마에는 `trail_id`를 넣지 않아 향후 그런 사례가 나와도 대응 가능하게 설계.

**Phase 7A에서 실제 데이터로 확정한 것** (자세한 근거는 `docs/04-legacy-slope-compatibility.md` 참고):
- **Legacy Compatibility Source**: 원본 GeoJSON 파일로 확정, `TrailFeature` DB는 사용하지 않음
  (9건의 geometry 무효 Feature 때문에 실제 group 경계/개수가 달라짐을 실측 확인).
- **groupSize**: 5/7/12로 통일하지 않고 파라미터로 유지하기로 확정.
- **1-point remainder group**: 렌더링에서 완전 제외(slope=0 아님)로 확정, 4개 파일 코드로 재확인.
- **`LegacySlopeGroup`과 `TrailSegment`의 관계**: 서로 다른 개념으로 확정, Legacy 계산에
  `TrailSegment`/`TrailFeature`를 입력으로 쓰지 않음.

**여전히 확정하면 안 되는 것** (Phase 7B/8 이후 실제 요구사항이 나온 뒤 결정):
- **실제 물리적 경사(Phase 7B) 계산 기준**: `TrailFeature`와 `TrailSegment` 중 어느 단위에 귀속시킬지
  미확정(Network Layer는 존재하므로 Phase 7B에서 실제로 판단 가능).
- **DN의 실제 elevation 의미**: 이전 분석에서 반증에 가까운 정황(인왕산 정상고도 초과값, 3D 뷰에서
  별도 지형 API 사용)만 확인했을 뿐, 확정도 완전한 반증도 아니다. 원본 데이터 제공처 확인 전까지는
  미확정으로 유지한다.
- **pgRouting 필요 여부**: Phase 6 결과(분기 0건, 대부분 단순 사슬 구조)만 보면 필요성 근거가 아직
  보이지 않지만, Phase 8 요구사항이 나온 뒤 최종 판단한다.
- **마루의 391개 connected component 개별 원인**: 4개의 20m+ gap만 원인을 규명했고, 나머지 작은
  gap들은 개별 분류하지 않았다(`docs/03-trail-network-modeling.md` 20번 참고).

이 문서는 Phase가 진행될 때마다 관련 섹션이 추가로 갱신된다(신규 파일을 만들지 않고 이 문서를 계속 갱신하는 방식).
