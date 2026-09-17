# 05. Accident ↔ TrailSegment Spatial Query (Phase 8)

이 문서는 `AccidentPoint`(Raw Accident Spatial Layer, Phase 8)와 `TrailSegment`(Derived Network
Layer, Phase 6) 사이의 공간 관계를 어떻게 질의하는지 다룬다. 원본 데이터 선정/Import/Schema
Mapping은 `docs/02-migration-and-data-quality.md`의 "Accident Raw Data Import" 절에 이미
기록했으므로 여기서 다시 서술하지 않는다. Legacy Slope Compatibility(Phase 7A,
`docs/04-legacy-slope-compatibility.md`)와도 무관한 별개의 기능이다 — 섞어서 설명하지 않는다.

## 1. 전체 구조

```text
Original Accident Source (2023산악사고_인왕산.geojson, 42건)
        ↓ Import
AccidentPoint (Raw Spatial Layer)
        │
        │  ST_DWithin / ST_Distance (질의 시점에 계산)
        ▼
TrailSegment (Derived Network Layer, Phase 6)
```

`AccidentPoint`는 원본 사고 좌표를 그대로 보존하는 Raw 데이터이고, `AccidentPoint`와
`TrailSegment`의 근접 관계는 **Derived Spatial Relation**이다 — 이 둘을 하나의 FK로 합치지
않는다.

## 2. FK/nearest/distance를 영속화하지 않은 이유 [데이터 확인] [설계 판단]

Phase 8 사전 분석에서 42건 전체에 대해 최근접 Segment와 두 번째로 가까운 Segment의 거리를
비교한 결과:

```text
nearest ↔ second-nearest 거리차 < 5m: 42/42건 (100%)
nearest ↔ second-nearest 거리차 < 1m: 26/42건 (62%)
```

즉 "가장 가까운 Segment 하나"를 고정된 FK로 저장하는 것 자체가 대부분의 경우 사실상 동률에 가까운
후보 중 하나를 임의로 골라 고정하는 셈이다. 원인은 `TrailSegment`가 매우 잘게 나뉘어 있기
때문이다(중앙값 길이 약 3m) — 이는 Phase 6 topology 처리의 산물이 아니라 원본 GeoJSON 자체가
짧은 LineString 조각들로 디지타이징되어 있었기 때문임을 확인했다(무악동/홍제동/부암동 세 코스 모두
Feature:Segment 비율이 1.01~1.04).

추가로 `TrailSegment`는 `spatial-network-build` 프로필로 언제든 재생성 가능한 Derived 데이터이므로
그 `id` 자체가 영구적인 식별자가 아니다 — Network를 재생성하면 이전 FK 값이 유효하지 않게 될 수
있다.

이 두 가지 이유로 다음을 모두 **저장하지 않았다**:

```text
accident_point.trail_segment_id / nearest_segment_id
accident_point.distance_m / within_30m / nearby_segment_count
trail_segment.accident_count / risk_score
```

모두 `mapper-spatial-query.xml`에서 질의 시점에 `ST_DWithin`/`ST_Distance`로 계산한다.

## 3. threshold를 API 파라미터로 둔 이유 — 30m는 위험 반경이 아니다 [데이터 확인] [설계 판단]

Phase 8 사전 분석 결과:

```text
nearest 거리 median  ≈ 256.8m
100m 이상 이격       = 32/42건 (76%)
30m 이내 매칭        = 2/42건 (4.8%)
```

30m든 50m든 이 데이터셋의 대다수를 설명하지 못하며, 어떤 특정 값이 "안전/위험을 가르는 반경"이라고
주장할 근거가 전혀 없다. 그래서 `distanceMeters`는 **호출자가 지정하는 공간 연관 조회 반경**일
뿐이며, API 어디에도 30m를 기본값이나 상수로 고정하지 않았다. Controller는 `distanceMeters > 0`
외에는 임의의 상한(도메인 근거 없는 max-distance)을 두지 않았다.

**용어 사용 원칙**: 이 API와 관련 코드/문서에서 `distanceMeters`를 "위험 반경", "안전 반경",
"사고 영향 반경"이라고 표현하지 않는다. 어디까지나 공간 연관 조회 반경이다.

## 4. 미터 단위 거리 계산 — geography 캐스팅 [코드 확인]

```sql
ST_DWithin(accident.geom::geography, segment.geom::geography, #{distanceMeters})
ST_Distance(accident.geom::geography, segment.geom::geography)
```

`::geography` 캐스팅 없이 raw geometry 좌표(degree 단위)로 거리를 비교하면 위도에 따라 실제
미터 환산 비율이 달라지므로 사용하지 않았다. 모든 거리 계산은 `mapper-spatial-query.xml`에서
이 방식으로 통일했다.

## 5. Exhaustive 검색 — KNN LIMIT을 쓰지 않은 이유 [설계 판단]

Phase 8 사전 분석에서 geometry KNN 연산자(`ORDER BY geom <-> geom LIMIT n`)가 "가장 가까운
Segment 하나 찾기"에는 효율적이었지만, 이번 API가 요구하는 것은 **"N미터 이내의 모든 Segment"**다.
고정 LIMIT으로 후보를 먼저 좁힌 뒤 `ST_DWithin`을 적용하면, threshold 안에 실제로 존재하지만 KNN
후보 창 밖에 있는 Segment를 누락할 수 있다. 그래서 두 질의(`findSegmentsNearAccident`,
`findAccidentsNearSegment`) 모두 KNN LIMIT 없이 전체 테이블에 대해 `ST_DWithin`을 그대로
적용하는 exhaustive 방식으로 구현했다. 실측으로도 이 방식이 실제로 24개의 결과(한 사고 지점 기준
30m 이내 24개 Segment, 전부 같은 트레일의 촘촘한 인접 조각들)를 하나도 누락 없이 반환함을
확인했다(`SpatialQueryIntegrationTest.exhaustiveResultsIncludeMatchesOutsideAnySmallKnnWindow`).

## 6. geometry GiST vs geography cast — 실제 EXPLAIN ANALYZE [실행 결과]

기존 `trail_segment.geom`에는 `idx_trail_segment_geom`(geometry GiST)이 있다. `accident_point`
Import 시점에는 geometry GiST를 추가하지 않았다(22/27번 참고).

**Query A (Accident → 30m 이내 Segment, 실제 데이터로 실행)**:
```text
Nested Loop (실측 24 rows)
  Join Filter: st_dwithin((a.geom)::geography, (ts.geom)::geography, '30'::double precision, true)
  -> Seq Scan on trail_segment ts (2122 rows)
  -> Index Scan using accident_point_pkey (id = 조회 대상)
Execution Time: ~48-50ms (trail_feature/trail과의 3-way join 포함)
```
geometry GiST(`idx_trail_segment_geom`)는 사용되지 않았다 — `::geography` 캐스팅이 걸린 조건에는
기존 geometry 인덱스가 자동으로 쓰이지 않는다는 것을 실제 실행계획으로 재확인했다(추측이 아님).

**Query A, 300m(더 현실적인 규모)**: 결과 219 rows, Execution Time ~34ms — 데이터 규모(2122건)
자체가 작아 Seq Scan 비용이 여전히 무시할 만한 수준이다.

**Query B (Segment → 사고 이내 Accident)**: `trail_segment_pkey`로 대상 Segment 1건을
Index Scan한 뒤 `accident_point`(42건) 전체를 Seq Scan — 30m/300m 모두 Execution Time
0.2ms 수준.

## 7. Functional Geography Index를 추가하지 않은 이유 [설계 판단]

6번 실측 결과 기준으로 판단했다:

- `accident_point`는 42건뿐이라 Seq Scan 비용이 사실상 0에 가깝다. GiST를 추가해도 이 규모에서는
  planner가 그것을 쓸 유인 자체가 크지 않다.
- `trail_segment`(2122건)에 대한 Query A도 실행시간 대부분이 `trail_feature`/`trail`과의 3-way
  join에서 발생하며, `ST_DWithin` 자체의 Seq Scan 비용은 작다.
- 즉 `USING GIST ((geom::geography))` 같은 functional index를 지금 추가할 근거(반복되는 병목,
  실행계획상 명확한 개선 필요성)가 실측되지 않았다.

**결론**: 이번 Phase에서는 functional geography GiST를 추가하지 않는다. 데이터 규모가 훨씬
커지거나 실제 병목이 관측되면 그때 `EXPLAIN`으로 다시 판단한다 — 조기 최적화를 하지 않는다는 이
프로젝트의 기존 원칙(`docs/01` 23번)을 그대로 따른다.

## 8. Segment accident count를 위험도로 쓰지 않은 이유 [데이터 확인] [설계 판단]

30m 기준으로 사고와 연관된 Segment는 2122건 중 25건뿐이었고, 그 25건 전부 사고 건수가 0 또는
1이었다. 게다가 이 25건 대부분은 **하나의 사고 지점**이 촘촘한 인접 Segment 24개와 동시에
매칭된 결과였다(5번 참고) — 즉 "이 Segment가 위험하다"가 아니라 "이 지점 근처에 매우 짧은
Segment가 많다"는 사실을 반영할 뿐이다. 이 pair count를 `trail_segment.accident_count`나
risk score로 저장하면 실제로는 데이터 세밀도의 인공물을 위험도처럼 보이게 만드는 왜곡이 생긴다.
그래서 이런 컬럼은 만들지 않았고, 사고 건수 집계 API(Trail 단위든 Segment 단위든)도 이번
Phase에서는 구현하지 않았다.

## 9. 구현한 API

### API A — Accident → nearby TrailSegments
```text
GET /api/spatial/accidents/{accidentId}/nearby-segments?distanceMeters=N
```
응답(거리 오름차순, threshold 내 전체 후보):
```json
[
  {
    "segmentId": 2499,
    "trailId": 11,
    "sourceCourseName": "무악동구간",
    "sourceTrailFeatureId": 3140,
    "distanceMeters": 1.46143669
  }
]
```

### API B — TrailSegment → nearby AccidentPoints
```text
GET /api/spatial/trail-segments/{segmentId}/nearby-accidents?distanceMeters=N
```
응답(거리 오름차순):
```json
[
  {
    "accidentId": 53,
    "reportNo": "20231103201R00346",
    "dispatchDate": "2023-05-14",
    "accidentType": "질환",
    "locationName": "무악동",
    "distanceMeters": 1.46143669
  }
]
```

두 API 모두:
- `distanceMeters <= 0` → 400
- 존재하지 않는 accidentId/segmentId → 404
- 결과 없음 → 200 + 빈 배열(에러 아님)

Frontend는 아직 연결하지 않았다(Phase 9).

## 10. 남아 있는 한계 (숨기지 않고 기록)

- 42건 사고 Point 좌표가 GPS 실측값인지, 소방 출동 시스템이 부여한 근사/고정 좌표인지 확정할 수
  없다(동일 좌표를 공유하는 서로 다른 사고 10그룹이 이 가능성을 시사하지만 반증도 확정도 아니다).
- 이 42건 대부분(76%)은 현재 Trail Network에서 100m 이상 떨어져 있다 — "산악사고 대부분이 등산로
  선형 위/근처에서 발생한다"는 가정이 이 특정 데이터셋에는 들어맞지 않는다.
- 따라서 현재 threshold는 어떤 값을 골라도 위험 반경이 아니며, 공간 연관 조회 도구 이상의 의미를
  부여하면 안 된다.
- 사고 건수만으로 위험도를 산출할 수 없다(8번) — Segment/Trail 단위 위험도 API는 이번에도, 향후
  계획도 없다(도입하려면 노출량/이용객 수/사고 심각도 등 정규화 요소가 별도로 필요하다).
