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

### API C — Trail → distinct nearby AccidentPoint candidates (Phase 13)
```text
GET /api/spatial/trails/{trailId}/nearby-accidents?distanceMeters=N
```
A/B는 accidentId 1개 또는 segmentId 1개를 입력으로 받는다. 실제 서비스 기능(등산 중 위험 알림)에
필요한 것은 "현재 Trail 전체 주변의 사고지점 후보 목록"이므로, Segment를 하나씩 순회하는 N+1 호출
대신 Trail 전체를 한 번에 조회하는 API C를 Phase 13에서 추가했다. 내부적으로는 A/B와 동일한
`ST_DWithin`/`ST_Distance`/`::geography` 패턴을 그대로 재사용하고, `trail_segment` → `trail_feature`
→ `trail_id`로 조인해 Trail 범위로 좁힌다(`trail_segment`에는 `trail_id` 컬럼이 없다 — 저장하지
않기로 한 이유는 `docs/01`, Phase 6 참고). GeoJSON `FeatureCollection<Point>` 형태로 응답한다:
```json
{
  "type": "FeatureCollection",
  "trailId": 11,
  "distanceMeters": 30.0,
  "features": [
    {
      "type": "Feature",
      "properties": {
        "accidentId": 16956,
        "reportNo": "20231103201R00346",
        "dispatchDate": "2023-05-14",
        "accidentType": "질환",
        "locationName": "무악동",
        "distanceToTrailMeters": 1.46143669
      },
      "geometry": { "type": "Point", "coordinates": [126.959075031, 37.578950447] }
    }
  ]
}
```
`distanceToTrailMeters`는 Trail ↔ AccidentPoint 거리(PostGIS, 후보 선별용)이며, 사용자 ↔ AccidentPoint
실시간 거리(Frontend, Kakao Maps `Polyline.getLength()`)와는 다른 값이다 — 이름 자체를 다르게 둬서
혼동을 막는다(`RecordView.vue`의 `ACCIDENT_CANDIDATE_DISTANCE_METERS`/`ACCIDENT_ALERT_DISTANCE_METERS`도
동일한 이유로 값은 같아도 상수를 분리했다).

**중복 제거**: 하나의 AccidentPoint가 같은 Trail의 여러 TrailSegment 근처에 있을 수 있다(무악동구간의
사고 하나는 실제로 24개 서로 다른 Segment가 30m 이내에 걸린다 — Segment 중앙값 길이가 ~3m라서
자연스러운 현상). SQL에서 `GROUP BY a.id` + `MIN(ST_Distance(...))`로 dedup과 최소거리 계산을
동시에 처리하므로, GROUP BY 이전 원본 조인은 24행이지만 API 응답은 항상 accidentId당 1개 Feature다.

두 API 모두(A/B):
- `distanceMeters <= 0` → 400
- 존재하지 않는 accidentId/segmentId → 404
- 결과 없음 → 200 + 빈 배열(에러 아님)

API C도 동일한 계약(`distanceMeters<=0`→400, 존재하지 않는 trailId→404, 후보 0건→200+빈
FeatureCollection)을 따른다.

API A/B는 Phase 9 이후로도 여전히 Frontend에 연결되지 않았다. API C는 Phase 13에서
`RecordView.vue`의 실시간 위험 알림에 연결됐다 — 아래 11번 절 참고.

## 10. 남아 있는 한계 (숨기지 않고 기록)

- 42건 사고 Point 좌표가 GPS 실측값인지, 소방 출동 시스템이 부여한 근사/고정 좌표인지 확정할 수
  없다(동일 좌표를 공유하는 서로 다른 사고 10그룹이 이 가능성을 시사하지만 반증도 확정도 아니다).
- 이 42건 대부분(76%)은 현재 Trail Network에서 100m 이상 떨어져 있다 — "산악사고 대부분이 등산로
  선형 위/근처에서 발생한다"는 가정이 이 특정 데이터셋에는 들어맞지 않는다.
- 따라서 현재 threshold는 어떤 값을 골라도 위험 반경이 아니며, 공간 연관 조회 도구 이상의 의미를
  부여하면 안 된다.
- 사고 건수만으로 위험도를 산출할 수 없다(8번) — Segment/Trail 단위 위험도 API는 이번에도, 향후
  계획도 없다(도입하려면 노출량/이용객 수/사고 심각도 등 정규화 요소가 별도로 필요하다).

## 11. Phase 13: RecordView 실시간 위험 알림 연결

기존 사전 분석(별도 세션 기록)에서 확인된 문제: `RecordView.vue`의 실시간 위험 음성 알림은
`frontend/public/data/2023산악사고_인왕산2.geojson`("Legacy UI Display Dataset", 15건)을 썼는데,
이 파일은 15건 중 5건만 실제 `report_no`를 갖고 그중 1건이 좌표/유형을 조작해 11번 재사용된
것으로, 실제 42건 `accident_point`와는 완전히 다른 데이터였다(`accident-schema.sql` 참고).

**Production 알림 경로에서 이 파일 사용을 제거**하고 API C로 교체했다. 파일 자체는 지우지 않았다
(provenance/과거 회귀 검증 목적) — 삭제가 아니라 사용처 교체다.

### 최종 흐름

```text
RecordView mount
  → fetchTrailGeoJson()                              (기존, Phase 12F에서 추가된 trailId property 재사용)
  → resolveAllTrailIds(trailGeoJson)                  (기존 공유 함수, slopeSection.js)
  → 각 trailId마다 fetchNearbyAccidents(trailId, 30)   (API C, Phase 13 신규)
  → accidentId 기준 전역 dedup (Frontend, Map)
  → Kakao Marker 생성 (accidentType/locationName/dispatchDate/distanceToTrailMeters 사용)

이후 기존 2초 폴링(GET /api/recordlocation) → checkProximityUsingPolyline
  → Kakao Polyline.getLength() 기반 사용자-사고 거리 계산 (변경 없음)
  → distance <= 30m → speakMessage() (변경 없음)
  → triggeredAccidentIds(accident_point.id 기준, 좌표 문자열에서 변경) 로 중복 알림 방지
```

**RecordView는 코스 선택 UI가 없다.** `/record` 라우트는 파라미터가 없고, 진입 지점(footer 버튼,
MainView 카드)도 전부 `window.location.href = '/record'`로 아무 값도 넘기지 않는다 — 즉 현재 코스를
전달할 기존 route query/Pinia/sessionStorage 관례가 애초에 존재하지 않는다. 새로 하나를 만드는 대신,
`MountainDetailView2.vue`가 이미 쓰고 있는 "코스 선택이 없으면 현재 DB에 존재하는 Trail 전체를
순회한다"는 패턴(`resolveAllTrailIds`)을 그대로 재사용했다 — Trail 4개 각각 독립적으로 API C를
호출하고 결과를 합친다(N+1 아님: Trail 개수는 4로 고정, Segment 순회가 아니다).

### 실측: Trail별 30m 후보 수 (운영 DB)

| Trail | trailId(운영 DB) | 30m 후보 수 |
|---|---|---|
| 마루 | 10 | 0 |
| 무악동구간 | 11 | **2** |
| 홍제동구간 | 12 | 0 |
| 부암동구간 | 13 | 0 |

4개 Trail 전체 합산 unique 후보는 2건 — 10번 절의 "42건 중 2건만 30m 이내"와 정확히 일치한다.

### 중복 제거 실측

무악동구간 accidentId(report_no=20231103201R00346)는 GROUP BY 이전 원본 조인에서 **24행**
(TrailSegment 24개가 30m 이내) → GROUP BY 이후 **1행**(distanceToTrailMeters=1.46m)으로 축소됨을
실제 DB에서 확인했다.

### Query 성능 (실측)

`EXPLAIN ANALYZE`, 무악동구간(trailId=11, 216개 TrailFeature 소속 TrailSegment, distanceMeters=30):
Planning ~24ms, Execution ~74ms(3회 반복, JIT off). 42 accident × 216 segment ≈ 9,000쌍 규모의
Nested Loop이며, `accident_point.geom`/`trail_segment.geom` 둘 다 GiST index가 있지만 이 정도
행 수에서는 planner가 index scan을 선택하지 않았다 — 현재 규모(§32 지시대로)에서 추가 인덱스를
넣지 않았다.

### 남아있는 범위 밖 사용처 (제거하지 않음, 정직하게 기록)

`2023산악사고_인왕산2.geojson`은 `RecordView.vue`가 아닌 최소 9개의 다른 화면
(`MountainDetailView.vue`, `MobileMountainDetailView.vue`, `CompareCourseView.vue`,
`3dView.vue`/`3dAnalysisView.vue`/`WebCourse3D.vue`/`MobileCourse3D.vue` 등)에서도 여전히
`loadMarkers`/`loadDangerMarkers` 형태로 지도에 위험 마커를 표시하는 데 쓰이고 있다. 이 화면들에는
`checkProximityUsingPolyline`/`speakMessage` 같은 실시간 접근-판정 로직이 없다 — 정적 마커 표시
기능이며, 이번 Phase의 범위(`RecordView`의 실시간 30m 음성 알림)에 해당하지 않아 손대지 않았다.
"Legacy 사고 데이터가 Production에서 완전히 사라졌다"고 주장할 수는 없고, "RecordView의 실시간
위험 알림 경로에서만 제거하고 실제 42건으로 교체했다"가 정확한 서술이다.

### Phase 14: "현재 Trail 하나만 조회" 축소 시도 — Large Change로 판정, 미구현

Phase 13의 4-Trail 전체 조회 구조를 "사용자가 실제로 선택한 Trail 하나만 조회"로 좁힐 수 있는지
조사했다. 결론: **Large Change로 판정, 구현하지 않고 현재 구조를 유지한다.**

**조사한 것**: `/record`로 이동하는 모든 경로를 repo 전체에서 검색했다 —
`MobileFooterView.vue`~`MobileFooterView5.vue`(전역 하단 네비게이션), `MainView.vue`/
`MobileMainView.vue`의 `gotoMobileRecording`("등산 기록" 카드), `MyCommunity.vue`,
`RecordImgView.vue`(기록 저장 후 복귀). **전부 예외 없이** `window.location.href = '/record'`
또는 파라미터 없는 `router.push({path:'/record'})`이며, `router/index.js`의 `/record` route
정의에도 `params`/`props`가 없다. `MainView.vue`는 코스 목록(`courses`)과 코스 상세 이동
(`goToMountainDetail`, `router.push({query:{course: JSON.stringify(course)}}}`) 관례를 이미
갖고 있지만, "등산 기록" 카드는 그 코스 목록과 **완전히 분리된, 독립적인 버튼**이라 이 관례를
타지 않는다. `MountainDetailView*.vue`(코스가 실제로 확정된 화면) 어디에도 `/record`로 가는
링크가 없다 — 즉 "코스를 이미 아는 상태에서 기록을 시작하는" 진입 경로 자체가 현재 앱에
존재하지 않는다. `sessionStorage`/`localStorage`에도 재사용 가능한 "현재 코스" 값이 없다
(`BalanceView.vue`의 `selectedCourseIds`는 코스 비교 화면 전용이고 RecordView는 읽지 않는다).

**판정 근거**: Small Change의 전제("이미 알고 있는 courseName을 작은 값 전달로 넘긴다")가
성립하려면 애초에 "코스를 알고 있는 시점 → 기록 시작" 흐름이 있어야 하는데, 그 흐름 자체가
없다. 따라서 이 문제를 해결하려면 값 하나를 route query/sessionStorage로 넘기는 것을 넘어서
**"기록 시작 전 코스를 고르게 하는 새 UX 단계"를 설계해 추가**해야 한다 — 이는 원 요청의
Large Change 기준("여러 화면의 navigation 구조를 대규모 변경", "course domain 자체를 다시
설계")에 해당한다.

**유지한 현재 구조**: `resolveAllTrailIds(trailGeoJson)` 기반 4-Trail 전체 조회를 그대로
둔다. 다른 기능(`MountainDetailView2.vue`)에서도 쓰이는 공용 함수이므로 제거 대상도 아니다.

**정확한 Portfolio 문장**: "선택한 Trail 주변 사고지점만 후보로 선별했다"는 **현재 구현과
맞지 않으므로 쓰지 않는다.** 정확한 서술은:
> 사고지점을 독립적인 Point 공간 객체로 모델링하고 TrailSegment와 PostGIS `ST_DWithin`/
> `ST_Distance` 공간질의로 연결했습니다. 등록된 Trail Network 주변 사고지점을 후보로
> 선별한 뒤, 등산 중 사용자의 현재 위치와 후보 사고지점의 거리를 계산해 30m 접근 시 위험
> 알림을 제공했습니다.

**향후 최소 개선안(구현 아님, 제안만)**: "등산 기록" 진입 지점(현재 `gotoMobileRecording`)
바로 앞에 코스 선택 모달/화면을 하나 추가하고, 선택된 courseName을 `router.push`의 `query`로
`/record`에 전달하면(기존 `goToMountainDetail`과 동일한 관례) `resolveTrailId(courseName,
trailGeoJson)`(Phase 12F에서 이미 만든 함수)로 Trail 하나만 resolve할 수 있다 — 이 개선은
"RecordView 코드 수정"이 아니라 "기록 시작 UX에 코스 선택 단계를 추가하는" 별도 제품 결정이
먼저 필요하므로 별도 Phase로 분리해 둔다.

### Known Limitations (Phase 13 시점)

- 사용자 위치는 여전히 Spatial DB Object가 아니다(Backend의 `/api/recordlocation`/`/api/location`은
  JVM 메모리에만 위치를 들고 있다) — 이번 Phase는 이를 바꾸지 않았다.
- 사용자 ↔ AccidentPoint 실시간 비교는 여전히 100% Frontend/Kakao Maps 계산이다. Backend는 Trail
  주변 후보 선별까지만 담당한다.
- 30m는 이번에도 "객관적 위험 반경"이 아니라 기존 서비스가 쓰던 alert 값을 그대로 재사용한
  것이다(3번 절 원칙과 동일).
- `HikingRecord`(path_info/tracking_path)는 이번 Phase에서 공간모델링하지 않았다 — 여전히 순수
  관계형 GPS point row다.
