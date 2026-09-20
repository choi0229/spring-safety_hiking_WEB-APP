# 08. Portfolio Summary — 면접/이력서용 기술 요약

이 문서는 README보다 기술 면접 준비에 가깝게 작성했다. 각 항목은 "실제로 무엇을 했고 왜 그렇게
판단했는가"를 짧게 답할 수 있도록 정리했으며, 모든 수치는 `docs/01~07`에서 실측/재현한 값만
사용한다. 새로운 주장이나 추정치를 추가하지 않는다.

## 1. 문제

기존 등산 안전 플랫폼(2024 LX 공간정보 아카데미 팀 프로젝트)은 등산로/사고 데이터를 정적
GeoJSON 파일로만 관리했고, Frontend가 파일을 직접 fetch해 좌표를 펼쳐 그리는 구조였다. 공간
데이터가 DB에서 구조적으로 관리되지 않았고, 원본 GeoJSON Feature와 위상학적 Network Edge
개념이 분리되어 있지 않았다.

## 2. 기존 구조

```text
Trail/Accident GeoJSON (정적 파일)
        ↓
Frontend: fetch → PMNTN_NM 필터 → coordinate flatten → 지도 렌더링
```
15개 이상의 화면이 같은 파일을 각자 fetch했고, 경사 색상은 좌표 배열을 고정 크기(N=5/7/12,
화면마다 다름)로 자르는 Frontend 로직(`groupCoordinates`)에 의존했다.

## 3. 개선 구조

```text
Original GeoJSON → Validation/Import → Trail/TrailFeature(Validated Raw)
                                              ↓ ST_Dump + exact endpoint matching
                                       TrailNode/TrailSegment(Derived Network baseline)
                                              ↕ ST_DWithin/ST_Distance
Original Accident GeoJSON → AccidentPoint(Raw) ┘

TrailFeature → GET /api/spatial/trails/geojson → non-slope 7개 화면(DB 전환)
Original GeoJSON(그대로) → Legacy slope pipeline → 경사 화면 4개(전환 안 함, 의도적 경계)
```

## 4. 핵심 설계 판단 (Q&A)

### Q1. 왜 TrailFeature와 TrailSegment를 분리했는가?
원본 GeoJSON Feature를 검증 후 그대로 보존하는 책임(Raw)과, 그 geometry를 분해·연결해 Derived
Network baseline을 만드는 책임(Network)은 서로 다르다. 이 둘을 하나의 테이블에 합치면(초기에는
실제로 `trail_segment`라는 이름의 원본 보존 테이블로 만들었다) 원본 데이터와 파생 데이터가
구분되지 않고, 향후 Network를 재계산할 때 원본이 훼손될 위험이 생긴다. 실제로 이 설계 오류를
데이터가 쌓이기 전에 발견해 `trail_feature`로 개명하고 계층을 분리했다.

### Q2. 왜 GeoJSON Feature 1:1을 그대로 Network Segment로 사용하지 않았는가?
GeoJSON Feature 하나가 하나의 선형 구간이라는 가정부터 성립하지 않았다. 실제로 1,706개의
MultiLineString `TrailFeature`를 `ST_Dump`로 분해하면 2,122개의 LineString Part가 생성됐다 —
즉 **Feature ≠ 선형 Part 1개**라는 것을 데이터로 확인했다. 그래서 Raw Feature와 Network Edge를
같은 개념으로 관리하지 않고 계층을 분리했다. 다만 현재 Network는 이 LineString Part의 endpoint를
기준으로 구성한 **exact-topology baseline**이며, "Part 1개 = 교차점↔교차점 사이의 최종 Routing
Segment"라고는 주장하지 않는다 — snapping이나 선 내부 교차점(interior intersection) noding은
적용하지 않았고, 후속 과제로 남겨두었다(`docs/03-trail-network-modeling.md`, Known
Limitations 참고).

### Q3. 왜 AccidentPoint에 `trail_segment_id` FK를 넣지 않았는가?
42건 전체에 대해 최근접 TrailSegment와 두 번째로 가까운 TrailSegment의 거리 차이를 계산한 결과
100%가 5m 미만이었다(TrailSegment 중앙값 길이가 약 3m로 매우 촘촘하기 때문). "가장 가까운 것
하나"를 FK로 영구 고정하면 사실상 임의의 선택을 고정하는 셈이라 데이터를 왜곡한다. 또한
TrailSegment는 `spatial-network-build` 프로필로 언제든 재생성 가능한 Derived 데이터라 그 id가
영구 식별자가 아니다 — 그래서 FK 대신 `ST_DWithin`/`ST_Distance`로 질의 시점에 관계를 계산한다.

### Q4. 왜 geography functional index를 바로 추가하지 않았는가?
`EXPLAIN ANALYZE`로 실제 실행계획을 확인한 결과, `ST_DWithin(geom::geography, ...)`는 기존
`geometry` GiST 인덱스를 사용하지 않고 Seq Scan으로 처리됨을 확인했다(PostGIS의 실제 동작이며
추측이 아니다). 하지만 현재 규모(TrailSegment 2122건, AccidentPoint 42건)에서 이 Seq Scan
자체가 수십 ms 이내였다. 측정 근거 없이 index를 추가하는 조기 최적화를 피하고, "Dataset이
커지면 재검토할 Future Optimization Candidate"로만 문서화했다.

### Q5. 왜 13개 invalid Feature를 다시 API 응답에 섞지 않았는가?
13건 중 9건은 좌표 1개짜리 degenerate LineString처럼 구조적으로 무효한 공간 객체였고, 4건은
코스 라벨(PMNTN_NM)이 공백이었다. Frontend 호환성을 이유로 이런 데이터를 DB API에 다시
포함시키면 "DB가 Source of Truth"라는 원칙이 무너지고, 검증 로직이 있으나 마나 한 것이 된다.

### Q6. (Phase 9A 당시) 왜 Legacy slope 화면만 Original Source를 유지했는가?
DB Validated Source(1706건)를 기존 `groupCoordinates` 파이프라인에 그대로 대입해 독립적으로
재현한 결과, 제외된 13건 때문에 좌표 스트림이 밀리면서(위치 기반 고정크기 청킹의 특성상 중간
좌표가 빠지면 그 이후 전부 밀림) 마루 코스 29.6%, 홍제동구간 39.5%(groupSize=5 기준)의 렌더링
그룹 색상이 원본과 달라짐을 실측했다. 이를 해결하려고 무효 Feature를 되살리는 대신, Validated
DB Source(일반 기능)와 Original Static Source(Legacy 경사 렌더링)의 경계를 의도적으로
분리했다 — "마이그레이션 미완료"가 아니라 설계 결정이다. **이 경계는 Phase 12D에서 해소됐다**:
좌표 청킹 자체를 TrailSegment Network 기반 20m 고정거리 SlopeSection으로 대체하는 모델을
설계·검증(Phase 12A-12C)한 뒤 4개 화면 모두 전환했다 — Q9 참고.

### Q9. 왜 SlopeSection을 TrailSegment가 아니라 별도 분석 단위로 정의했는가?
TrailSegment 중앙값 길이가 약 3m라 두 지점 사이 거리로 slope를 나누면 분모가 작아 극단값이
쉽게 발생한다(Feature-to-Feature 프로토타입에서 실측: baseline 5m 미만 구간에서만 `|slope| >
100%`가 나타남). 또한 1706개 Feature 중 256개가 2개 이상 TrailSegment로 쪼개져 있어, Segment
단위로 DN을 다루면 형제 Segment가 부모 DN을 그대로 복사한 것을 "실측된 평탄함"으로 오인하게
된다. 그래서 TrailSegment/TrailNode Network를 branch-free Chain으로 재구성한 뒤, 그 위에서
고정 거리(10/20/30m 비교 후 20m 채택) 단위로 SlopeSection을 파생했다 — TrailSegment는 Network
topology/geometry 단위로, SlopeSection은 경사 분석 전용 단위로 계속 분리해 둔다.

### Q7. 왜 DN을 실제 고도라고 주장하지 않는가?
DN 최댓값(655)이 인왕산 실제 정상고도(약 338m)를 초과하고, Frontend의 3D 뷰는 DN이 아닌 별도
지형 API로 고도를 샘플링하는 것을 코드로 확인했다. 이는 "DN이 고도가 아니다"에 가까운 정황이지만
원본 데이터 제공처를 확인할 수 없어 완전한 반증도 아니다 — 확정도 반증도 아닌 상태를 그대로
"미검증"으로 남겼다.

## 5. 주요 구현

```text
Trail/TrailFeature Import       — Manifest 기반 명시적 제외 분류(4 blank + 9 invalid),
                                    source_file 단위 replace + rollback/idempotency 검증
TrailNode/TrailSegment 구축      — ST_Dump 기반 Part 분해, exact endpoint 매칭(snapping 없음),
                                    Part-Segment 1:1 geometry parity 검증
LegacySlopeService(Phase 7A)    — 원본 GeoJS 기준 JS↔Java parity 독립 검증(Node 참조 구현),
                                    Backend API로 노출하지 않음(Legacy 화면과 분리 유지)
AccidentPoint Import            — 42건 Raw Import, 15건 Legacy display 데이터와 분리,
                                    raw_properties JSONB로 원본 속성 전체 보존
Spatial Query API               — ST_DWithin/ST_Distance 기반, FK 없는 동적 관계
Trail GeoJSON API               — 단일 SQL(json_build_object+json_agg), source_feature_index
                                    정렬, ST_AsGeoJSON(geom,15), N+1 없음
Hybrid Frontend Migration       — 실사용 7개 화면 먼저 fetch URL 교체(Phase 9A), 미라우팅
                                    4개 파일 변경 원복, Legacy 4개 화면은 대체 모델 검증 후 전환(Phase 12D)
Fresh Rebuild 검증              — 별도 DB에서 전체 파이프라인 재구성, Dataset/API 동일 재현
SlopeSection API(Phase 12B-12D) — NetworkChain 재구성 + Feature 단위 DN sample 선형보간,
                                    Segment 단위 geometry cut(오차 median 0.18%), 4개 화면 전환
```

## 6. 정량 검증

```text
Trail: 4, TrailFeature: 1706 (원본 1719 - 제외 13), TrailNode: 2526, TrailSegment: 2122,
AccidentPoint: 42

Raw Part(ST_Dump) ↔ TrailSegment geometry parity = 2122 / 2122
Raw ↔ Network 전체 길이 차이 = 0.000000 m

Backend 자동 테스트 = 125 / 125 PASS
Frontend production build = PASS

Trail GeoJSON API (local Docker, warmup 5회 + 측정 30회)
  payload = 458,796 bytes
  median = 8.78ms, p95 = 10.51ms

SlopeSection API 20m (전체 4개 Trail 합산)
  section 351건(full 290 + partial 61), |slope|>100% 0건
  network length coverage = 73.0%, elevation-sample coverage 기준 = 90.0%
  geometry 절단 오차(Segment 단위 절단 적용 후) relError median = 0.18%, p95 = 1.58%
  API 응답시간(마루, 최대부하 케이스) median ~60ms, p95 ~63ms

Fresh Rebuild(별도 DB safety_hiking_bench)
  Dataset 4/1706/2526/2122/42 동일 재현, API 1706 Feature 확인
```
과장 표현은 쓰지 않는다 — 예를 들어 "PostGIS 도입으로 성능 2배 향상" 같은, 측정 근거 없는
비교는 쓰지 않았다. Static file fetch와 DB API는 실행 모델 자체가 달라 단순 latency 비교가
성립하지 않는다.

## 7. 한계

```text
- Network는 exact endpoint topology baseline(snapping 미적용) — 마루 코스가 391개 connected
  component로 분절되어 있음
- 원본 1719 Feature 중 13건 제외(PMNTN_NM 공백 4 + geometry 무효 9)
- DN의 물리적 의미(고도 여부) 미검증 — `estimatedSlopePercent`는 measured가 아니라
  DEM-derived로 알려진 값 기반 estimated slope
- Legacy slope 값은 실제 물리적 slope_percent가 아님(diagonal-distance 기반 legacy 공식,
  JS↔Java parity만 검증) — `LegacySlopeService`는 regression/provenance 목적으로 계속 유지
- AccidentPoint 42건의 좌표 정밀도 미검증, 76%가 Trail Network에서 100m 이상 이격
- 15건 accident display dataset은 Raw Source가 아니라 Legacy UI 표시 전용 데이터
- SlopeSection(20m) coverage는 100%가 아니다(전체 Network 73.0% / elevation coverage 기준
  90.0%, 마루만 66.5%/86.6%로 낮음) — 미커버 구간은 Base Trail만 표시하고 slope를 임의로
  채우지 않는다
- SlopeSection의 상승/하강 부호는 chain별 임의 정규화 기준이라 실제 등반 방향과 항상 일치한다는
  보장은 없다
- 실제 browser DOM/육안 확인 미수행(Node 로직 실행 확인으로 대체)
- geometry GiST가 geography cast 질의에 자동으로 쓰이지 않음을 확인했으나, 현재 규모에서
  functional geography index는 추가하지 않음(Future Optimization Candidate)
```

## 8. 내가 설명할 수 있어야 할 질문

```text
1. TrailFeature와 TrailSegment의 관계를 그림 없이 말로 설명할 수 있는가?
2. AccidentPoint와 TrailSegment를 FK로 연결하지 않은 이유를 데이터로 설명할 수 있는가?
3. exact-topology와 snapping-based topology의 차이와, 왜 전자를 먼저 선택했는지 설명할 수 있는가?
4. Legacy slope 화면에서 발견한 회귀(색상 차이)의 원인을 groupCoordinates의 동작 방식으로
   설명할 수 있는가?
5. geometry GiST가 언제 쓰이고 언제 안 쓰이는지(geography cast vs KNN 연산자) 설명할 수 있는가?
6. 이번 리팩터링에서 "성능 개선"이 목표가 아니었다고 말할 수 있고, 그 이유를 설명할 수 있는가?
7. Fresh Rebuild 검증을 어떤 방식으로, 운영 DB에 영향 없이 수행했는지 설명할 수 있는가?
8. DN을 "고도"라고 말하지 않는 이유와, 그럼에도 왜 완전히 반증됐다고도 말하지 않는지 설명할
   수 있는가?
9. Phase 12A에서 "chain 개수 기준 24.5% coverage"라는 지표가 왜 오해를 낳는지, 실제 길이 기준
   coverage로 다시 측정한 이유를 설명할 수 있는가?
```
