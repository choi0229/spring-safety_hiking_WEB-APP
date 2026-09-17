# 07. Testing, Reproducibility & Performance Baseline (Phase 10)

이 문서는 Phase 1~9A까지 구축한 PostgreSQL/PostGIS 공간 데이터 구조가 (1) 처음부터 재현
가능한지, (2) 현재 실제로 어떤 성능 특성을 갖는지를 실측한 결과다. **이번 Phase의 목적은 성능
개선이 아니라 baseline 확정이다** — 측정 결과를 바꾸기 위한 코드 변경(index 추가, 캐시, query
rewrite 등)은 하지 않았다.

## 1. 측정 환경

```text
OS/CPU       : macOS / Apple Silicon(arm64), local Docker
Java         : OpenJDK 17.0.18 (Homebrew)
PostgreSQL   : 16.4 (Debian, postgis/postgis:16-3.4 이미지, amd64 에뮬레이션 위 arm64 호스트)
PostGIS      : 3.4 (USE_GEOS=1 USE_PROJ=1 USE_STATS=1)
Docker       : 29.4.0
실행 위치     : 전부 localhost, 별도 네트워크 홉 없음
```

이하 모든 시간 측정값은 이 환경에서만 유효하다. **production latency로 해석하지 않는다.**

## 2. 전체 Regression

```text
Backend: ./mvnw test  →  93 tests, 0 failures, 0 errors, 0 skipped  (13개 테스트 클래스 합산)
Frontend: npm run build (기존 install된 node_modules 사용) → 성공
  기존 asset-size 경고 2건(폰트/영상/geojson 대용량 파일)만 존재, 이번 Phase로 인한 신규
  경고/에러 0건 — Phase 9A 이후 소스 변경이 없어 build hash(541b63ba6ceee9cc)도 동일.
```

## 3. DB Dataset Integrity 최종 검증 [데이터 확인, 실측]

```text
trail = 4, trail_feature = 1706, trail_node = 2526, trail_segment = 2122, accident_point = 42
```

| 테이블 | geometry type | wrong_type | wrong_srid | empty | invalid | 기타 |
|---|---|---:|---:|---:|---:|---|
| trail_feature | MultiLineString | 0 | 0 | 0 | 0 | — |
| trail_node | Point | 0 | 0 | 0 | — | — |
| trail_segment | LineString | 0 | 0 | 0 | 0(zero-length) | from/to node 결손 0, dangling FK 0 |
| accident_point | Point | 0 | 0 | 0 | 0 | — |

전부 실제 DB를 다시 조회해 확인했다(이전 Phase 결과를 복사하지 않음).

## 4. Raw → Network Coverage 최종 검증 [데이터 확인, 실측]

```text
Raw LineString Part count(ST_Dump)     = 2122
Derived TrailSegment count             = 2122
Raw geometry total length              = 9198.810 m
Derived Network total length           = 9198.810 m
차이                                    = 0.000000 m
Part-Segment 1:1 ST_Equals mismatch    = 0건 (2122건 전수 재검증)
```
Phase 6에서 확인했던 100% coverage가 현재도 그대로 유지됨을 전수 재확인했다.

## 5. Fresh Rebuild 재현성 검증 [실행 결과] — Phase 10의 핵심

**현재 정상 운영 중인 DB/볼륨은 전혀 건드리지 않았다.** 같은 Postgres 컨테이너 안에
`template_postgis`(postgis 확장이 이미 활성화된 템플릿 DB, 공식 이미지가 기본 제공)를 기반으로
별도 데이터베이스 `safety_hiking_bench`를 만들어 전체 파이프라인을 처음부터 재구성했다.

### 실제 사용한 명령 순서
```bash
# 1) 격리된 벤치마크 DB 생성 (기존 DB/볼륨 무변경)
docker exec ... psql -U safety_hiking_app -d postgres -c "DROP DATABASE IF EXISTS safety_hiking_bench;"
docker exec ... psql -U safety_hiking_app -d postgres -c \
  "CREATE DATABASE safety_hiking_bench TEMPLATE template_postgis OWNER safety_hiking_app;"

# 2) 관계형 + 공간 Schema 적용 (실제 repository 파일, 순서 그대로)
psql -d safety_hiking_bench < backend/src/main/resources/db/postgresql/schema.sql
psql -d safety_hiking_bench < backend/src/main/resources/db/postgresql/seed.sql
psql -d safety_hiking_bench < backend/src/main/resources/db/postgis/spatial-schema.sql
psql -d safety_hiking_bench < backend/src/main/resources/db/postgis/network-schema.sql
psql -d safety_hiking_bench < backend/src/main/resources/db/postgis/accident-schema.sql

# 3) Import/Build를 이 DB로 실행 (POSTGRES_DB만 override)
POSTGRES_DB=safety_hiking_bench SPRING_PROFILES_ACTIVE=spatial-import ./mvnw spring-boot:run
POSTGRES_DB=safety_hiking_bench SPRING_PROFILES_ACTIVE=spatial-network-build ./mvnw spring-boot:run
POSTGRES_DB=safety_hiking_bench SPRING_PROFILES_ACTIVE=spatial-accident-import ./mvnw spring-boot:run

# 4) 일반 프로필로 기동해 API 확인 (기존 9000 인스턴스와 충돌 피하려 9001 포트 사용)
POSTGRES_DB=safety_hiking_bench ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9001
```

### 결과
```text
Fresh 환경 최종 Dataset: trail=4, trail_feature=1706, trail_node=2526, trail_segment=2122, accident_point=42
→ 기존 운영 DB와 정확히 일치

GET http://localhost:9001/api/spatial/trails/geojson → FeatureCollection, features=1706

GET http://localhost:9001/api/spatial/accidents/{id}/nearby-segments?distanceMeters=30
→ nearest segment distance=1.46143669m (기존 baseline과 완전 일치)
```
검증 후 `safety_hiking_bench`는 삭제했고, 임시 9001 인스턴스도 종료했다 — 운영 DB/인스턴스는
이번 검증 동안 전혀 중단되지 않았다.

## 6. Import / Network Build / Accident Import 성능 (safety_hiking_bench, 각 3회) [실측]

측정은 **내부 로그 타임스탬프 기준**(순수 parse+validation+DB write, JVM/Spring Boot 기동 제외)과
**전체 wall time**(`SPRING_PROFILES_ACTIVE=... ./mvnw spring-boot:run`의 시작~종료, JVM/Spring
기동·종료 포함) 둘 다 기록한다 — one-shot 배치 잡은 매번 새 JVM을 띄우므로 후자가 "실제 사용자가
체감하는 시간"에 더 가깝다.

| 작업 | 내부 처리 시간(3회) min/median/max | wall time(3회) min/median/max |
|---|---|---|
| Trail Import (1706건) | 457 / 521 / 550 ms | 2824 / 2918 / 3291 ms |
| Network Build (2122 parts→2122 segments) | 794 / 812 / 814 ms | 3101 / 3118 / 3280 ms |
| Accident Import (42건) | 158 / 169 / 169 ms | 2588 / 2627 / 2651 ms |

각 작업 모두 source_file replace 전략(delete-then-reinsert)이므로 3회 전부 동일한 양의 작업을
수행했다(진짜 "빈 테이블에 최초 삽입"은 1회차뿐이지만 delete+reinsert 총량은 동일). wall
time(2.6~3.3초)의 대부분은 JVM 시작+Spring context 로딩이며, 실제 데이터 작업 자체는
150ms~820ms 수준이다 — one-shot 배치 잡 구조상 이 오버헤드는 구조적인 것이라 이번 Phase에서
줄이지 않았다.

## 7. Trail GeoJSON API 성능 [실측, curl `time_total`, warmup 5회 + 측정 30회]

```text
Response: HTTP 200, application/json, Content-Length=458796 bytes (30회 전부 동일 크기)
min=8.04ms  median=8.78ms  average=8.91ms  p95=10.51ms  max=10.87ms
```
동일 localhost, 이미 기동되어 커넥션 풀이 준비된 상태에서 측정한 값이다 — 네트워크 홉이 있는
실제 배포 환경의 latency와 다르다.

### SQL 실행계획
```text
Aggregate (actual time=7.799..7.858, rows=1)
 -> Sort (source_feature_index) — quicksort, Memory 281kB, actual 2.044..2.142ms
    -> Hash Join (trail_feature ⋈ trail) — Seq Scan 양쪽 모두, actual rows=1706/4
Planning Time: 13.497 ms (cold) / Execution Time: 19.263 ms
```
1706건 규모에서 Hash Join + in-memory quicksort 모두 수 ms 이내로 충분히 빠르다 — 이 규모에서는
`source_feature_index`에 별도 index가 필요하지 않다(추가하지 않음).

## 8. Accident ↔ TrailSegment 공간질의 성능 [실측, EXPLAIN ANALYZE]

threshold는 도메인 위험 반경이 아니라 **benchmark input**이다(30m는 Phase 8에서 이미 사용한
값과의 비교를 위해 포함, 100m/300m는 매칭 행 수 변화를 보기 위해 추가).

### Accident(id=6781, report=20231103201R00346) → nearby Segments
| threshold | matched rows | execution time | 비고 |
|---:|---:|---:|---|
| 30m | 24 | 53.5 ms | 세션 첫 질의(catalog cache cold), Seq Scan on trail_segment(2122) |
| 100m | 107 | 19.7 ms | 동일 세션 2번째 질의(warm) |
| 300m | 219 | 34.6 ms | 동일 세션 3번째 질의(warm) |

### TrailSegment(id=2499) → nearby Accidents
| threshold | matched rows | execution time | 비고 |
|---:|---:|---:|---|
| 30m | 1 | 43.4 ms | 세션 첫 질의(cold) |
| 100m | 2 | 0.36 ms | warm |
| 300m | 11 | 0.30 ms | warm |

**측정 방법상 유의점**: 각 EXPLAIN 세션의 "첫 질의"는 PostgreSQL catalog cache가 비어 있어
Planning Time이 10~24ms 튀는 현상이 매 세션마다 재현됐다(같은 세션의 2/3번째 질의는 Planning
Time이 0.2~0.3ms로 급락). 이는 실제 애플리케이션의 커넥션 풀(HikariCP, 상시 연결 유지)에서는
발생하지 않는 현상이며, 7번 API latency 측정(8~11ms, warm pool)이 실제 운영에 더 가까운 값이다.
실행계획 자체(Seq Scan 사용 여부)는 cold/warm과 무관하게 동일했다.

## 9. geometry GiST / geography cast 재확인 [데이터 확인, EXPLAIN 재실행]

Phase 8 결론이 현재도 동일함을 재확인했다.

```text
ST_DWithin(geom::geography, geom::geography, N) → idx_trail_segment_geom(geometry GiST) 미사용,
  Seq Scan on trail_segment(2122건)으로 항상 처리됨(위 8번 실행계획에서 확인)

geometry KNN(ORDER BY geom <-> point, LATERAL 구조) → idx_trail_segment_geom 정상 사용
  (Index Scan using idx_trail_segment_geom ... Order By: (geom <-> a.geom), 실측 확인)
```
이번 Phase에서도 functional geography GiST(`USING GIST ((geom::geography))`)는 추가하지
않았다 — 2122건 규모에서 Seq Scan 자체가 이미 수십 ms 이내이고, 병목이 실측되지 않았다.

## 10. 주요 API Endpoint Smoke Check [실행 결과]

실제 Controller 기준 경로로 확인했다(`TrailGeoJsonController`, `SpatialQueryController`).

| Endpoint | 결과 |
|---|---|
| `GET /api/spatial/trails/geojson` | 200, `application/json`, 1706 Feature |
| `GET /api/spatial/accidents/{id}/nearby-segments?distanceMeters=30` | 200, 24건, 최근접 1.46143669m |
| `GET /api/spatial/trail-segments/{id}/nearby-accidents?distanceMeters=30` | 200, 1건, 동일 accident/거리로 대칭 확인 |

Vue devServer(`localhost:8080`) → Backend(`localhost:9000`) `/api` 프록시도 재확인(Phase 9A와
동일하게 byte-identical, 458796 bytes) — 중복 측정을 늘리지 않고 상태 유지만 확인했다.

## 11. Legacy Compatibility Regression [코드 확인]

```text
MountainDetailView.vue / CompareCourseView.vue / MountainDetailView2.vue /
MobileMountainDetailView.vue
→ `/data/인왕산ele copy.geojson` 참조 7곳 그대로 유지, `/api/spatial/trails/geojson` 참조 0건
```
Phase 7A `LegacySlopeServiceParityTest`(93개 테스트에 포함, 이번에도 PASS)가 이미 Original
Source 기준 legacy 동작을 회귀 검증하므로 새 테스트를 추가하지 않았다.

## 12. Current Bottleneck (실측 기준)

- **없음(현재 Dataset 규모 기준)**: Trail GeoJSON API(8~11ms), 공간질의 warm 상태(0.3~35ms 수준)
  모두 사용자가 체감할 만한 지연이 아니다.
- **명시적 병목**: one-shot Import/Network Build/Accident Import 잡의 wall time(2.6~3.3초)
  대부분이 JVM+Spring Boot 기동 오버헤드다. 이는 배치 잡을 매번 별도 JVM으로 실행하는 구조적
  특성이며, 데이터 처리 자체(150ms~820ms)는 병목이 아니다.

## 13. Future Optimization Candidate (구현하지 않음, 후보만 기록)

```text
- functional geography GiST(USING GIST ((geom::geography))): AccidentPoint/TrailSegment 규모가
  현재보다 수십~수백 배 커지고, geography ST_DWithin이 실제 반복 호출 병목으로 측정될 때 재검토.
- Trail GeoJSON API 응답 caching: FeatureCollection이 TrailFeature 변경 전까지 사실상 불변이므로
  캐싱 여지가 있으나, 현재 8~11ms 자체가 이미 충분히 빨라 근거가 약하다.
- Import/Network Build/Accident Import를 매번 새 JVM 대신 상주 프로세스에서 실행: 현재 데이터
  규모(1706/2122/42건)에서는 재현성/단순성 이점이 더 크다고 판단해 보류.
```
전부 "지금 필요하다"가 아니라 "Dataset이 커지면 재검토할 후보"로만 남긴다.

## 14. Known Limitations (숨기지 않음)

```text
- Trail Raw Source에서 13개 Feature 제외(PMNTN_NM 공백 4건 + geometry invalid 9건)
- Network는 exact-topology baseline이며 snapping을 적용하지 않음
- 마루 코스는 391개 connected component로 크게 분절되어 있음
- DN이 실제 elevation인지는 검증되지 않음(반증에 가까운 정황만 존재)
- Legacy slope 값은 실제 물리적 slope_percent가 아님(diagonal-distance 기반 legacy 공식)
- Accident 42건의 좌표 정밀도(GPS 실측 vs 신고 시스템 근사값)는 검증되지 않음
- Accident 42건 대다수(76%)가 Trail Network에서 100m 이상 떨어져 있음
- 15건 accident display dataset은 Raw Source가 아니라 Legacy UI 표시 전용 데이터
- Legacy slope 4개 화면은 Original Static GeoJSON을 계속 사용함(Phase 9B 보류)
- 실제 browser DOM/육안 확인은 Phase 9A/10 모두 미수행(Node 로직 실행 확인으로 대체)
```
이는 실패 목록이 아니라 현재 Dataset과 설계가 갖는 명확한 경계다.

## 15. 이전 측정값과의 비교 [데이터 확인]

Phase 8에서 기록했던 "Accident → Segment ST_DWithin 약 30-50ms, Segment → Accident 약
0.2ms" 수준의 실행시간은 이번 재측정(8번, 각각 19.7~53.5ms / 0.30~43.4ms, cold/warm 차이
포함)과 같은 자릿수로 일치한다 — 측정 환경(같은 로컬 Docker, 같은 데이터 규모)이 동일해 재현된
결과로 판단한다. 완전히 새로운 값으로 갱신할 필요는 없었으나, 이번에는 cold/warm 세션 차이를
명시적으로 분리해 기록했다는 점이 Phase 8 기록보다 더 정밀하다.
