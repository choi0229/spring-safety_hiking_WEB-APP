# Phase 7A — Legacy Slope Compatibility

이 문서는 Phase 7A(기존 frontend 경사 색상 계산 로직을 backend에서 의미 변경 없이 재현하고
JS↔Java parity를 검증하는 단계)의 분석·설계·구현·검증 결과를 기록한다. `docs/01`에서 이미 다룬
전체 Phase 계획/Raw·Network 계층 구분은 여기서 다시 서술하지 않는다.

**Phase 7A의 목적을 다시 명확히 한다: 이것은 새로운 경사 모델이 아니다.** 기존 JavaScript 동작을
정확히 이해하고, 그 동작을 Java로 그대로 재현 가능한 상태로 만든 단계다. 계산 공식, 색상 임계값,
그룹화 방식 중 어느 것도 이번 Phase에서 "개선"하지 않았다.

## 1. 기존 Legacy 처리 흐름 [코드 확인]

4개의 frontend view(`MountainDetailView.vue`, `CompareCourseView.vue`,
`MountainDetailView2.vue`, `MobileMountainDetailView.vue`)가 공유하는 흐름:

```text
원본 GeoJSON (frontend/public/data/인왕산ele copy.geojson)
    ↓ PMNTN_NM.includes(courseName)  (courseName = DB course.course_name)
    ↓ MultiLineString/LineString coordinates flatten (Feature/Part 경계 보존 안 함)
    ↓ groupCoordinates(coordinates, groupSize)  (비중첩 고정크기 chunk, 나머지는 작은 그룹으로 보존)
    ↓ if (group.length > 1) 만 렌더링
    ↓   calculateSlope(group[0], group[last])
    ↓   getColorBySlope(slope)
    ↓ Kakao Polyline
```

`calculateSlope`/`getColorBySlope`/`calculateHaversineDistance`/`groupCoordinates` 본문은 4개 파일
전부 byte-identical이다. 유일한 실질적 차이는 `groupCoordinates`에 넘기는 `groupSize` 값이다:

| 파일 | groupSize |
|---|---|
| MountainDetailView.vue | 5 |
| CompareCourseView.vue | 5 |
| MountainDetailView2.vue | 7 (주석은 "5개씩"으로 남아있음 — 실제 인자와 불일치, 수정하지 않음) |
| MobileMountainDetailView.vue | 12 (주석 동일 문제) |

## 2. 왜 `TrailSegment`(Network Layer)를 입력/출력으로 쓰지 않는가 [설계 판단]

`groupCoordinates(N)`이 만드는 group은 Feature/Part 경계와 무관하게 좌표를 N개씩 기계적으로 자른
결과다. 반면 `TrailSegment`(Phase 6)는 `ST_Dump`로 분해한 LineString Part 경계를 그대로 보존한다.
실측 결과 groupSize=5 기준 대부분의 group이 Part 경계를 가로지른다 — 즉:

```text
LegacySlopeGroup ≠ TrailSegment
```

이 둘을 같은 개념으로 취급하면 안 된다는 결론은 Phase 7 사전 분석에서 이미 내렸고, 이번 구현에서도
유지했다. `LegacySlopeService`는 `trail_segment`/`trail_feature` 테이블을 전혀 참조하지 않는다.

## 3. Legacy Compatibility Source vs Validated Spatial Source [코드 확인] [데이터 확인]

```text
Legacy Compatibility Source = 원본 GeoJSON 전체 + PMNTN_NM.includes(courseName) 필터만 적용
                              (geometry 구조 검증 없음)

Validated Spatial Source    = Trail / TrailFeature (Phase 5, geometry 검증 통과분만)
```

`processGeoJSON()`은 `line.map(...)`으로 좌표를 그대로 펼칠 뿐, sub-line의 좌표 개수 등 geometry
구조를 전혀 검증하지 않는다. 반면 Phase 5 Importer는 좌표 2개 미만인 sub-line 9건(`마루` 8건,
`홍제동구간` 1건)을 `INVALID_GEOMETRY_SOURCE_FEATURE`로 명시 제외했다. 따라서:

| | blank-PMNTN_NM 4건 | invalid-geometry 9건 |
|---|---|---|
| 기존 JS(`processGeoJSON`) | 제외(필터가 자연 배제) | **포함**(검증 없음) |
| TrailFeature DB(Phase 5) | 제외 | 제외 |

**실측 결과(원본 GeoJSON 직접 재현, groupSize=5)**:

| 구간 | 좌표 수(Legacy) | 좌표 수(TrailFeature-DB 동등) | 차이 |
|---|---|---|---|
| 마루 | 4175 | 4160 | 15 |
| 무악동 | 885 | 885 | 0 |
| 홍제동 | 386 | 380 | 6 |
| 부암동 | 434 | 434 | 0 |
| **합계** | **5880** | **5859** | **21** |

group 수(groupSize=5): 마루 835→832, 홍제동 78→76(무악동/부암동 불변).

**결론**: `TrailFeature` DB를 Phase 7A 입력으로 사용하면 실제 legacy 동작과 다른 group 경계/개수가
나온다. 따라서 **Phase 7A의 입력은 반드시 원본 GeoJSON 파일**이며, `TrailFeature`는 사용하지 않는다.
이 차이는 데이터 품질 문제(9건의 무효 geometry)에서 비롯된 것이며, `TrailFeature`를 legacy에 맞춰
되돌리지 않는다 — Raw Layer는 계속 검증된 상태를 유지한다.

## 4. groupSize를 통일하지 않는 이유 [설계 판단]

groupSize는 5/7/12로 파일마다 다르고, 이를 하나로 통일할 근거가 코드/커밋 이력 어디에도 없다
(저장소 히스토리가 단일 squash 커밋이라 원본 의도를 확인할 수 없음). Phase 7A는 backend
재현/검증만이 목적이므로 groupSize를 하나로 강제하지 않고 **호출자가 지정하는 파라미터**로 유지했다.
5/7/12를 하나로 통일할지는 Phase 7A 범위 밖(Future Work)이다.

## 5. 1-point remainder group 처리 [코드 확인]

4개 파일 전부 `if (group.length > 1) { ...calculateSlope... }` 가드를 사용한다. 즉 그룹 크기가
1이면:

- `calculateSlope` 호출 안 함
- 색상 계산 안 함
- Polyline 생성 안 함
- 결과 목록에서도 완전히 제외됨 (slope=0으로 대체되는 것이 아니다)

groupSize=5, 전체 4개 코스 합산 기준: `groupCoordinates` 원시 출력은 1177개 그룹이지만 실제
렌더링(및 Phase 7A `LegacySlopeResult.renderedGroups()`)은 1176개 — 홍제동 구간의 마지막 1-point
그룹 1개가 이 차이의 원인이다(78 total → 77 rendered). 이 값은 Java 구현 회귀 테스트
(`LegacySlopeServiceParityTest.groupSizeFiveAllCoursesCombinedMatchesPriorLegacyStatistics`)로
고정되어 있다.

## 6. 계산 공식 [코드 확인]

Haversine(수평 거리, 미터):
```text
R = 6371e3
a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlon/2)
c = 2·atan2(√a, √(1-a))
distance = R·c
```

Legacy slope 공식(표준 slope_percent = elevation_delta/horizontal_distance×100과 다름 — 분모가
수평 거리가 아니라 대각선 거리):
```text
deltaDn = end.DN - start.DN
diagonal = sqrt(horizontalDistance² + deltaDn²)
legacySlopeValue = horizontalDistance < 1m ? 0 : (deltaDn / diagonal) * 100
```

색상 임계값(이름 있는 enum이 원본 코드에 없음 — Java에서도 새 의미 이름을 계약으로 만들지 않음):
```text
legacySlopeValue > 30   → #FF4500
legacySlopeValue < -15  → #1E90FF
그 외                    → #32CD32
```

`DN`은 원본 GeoJSON `properties.DN` 값을 그대로 옮긴 것이며, **실제 elevation이라고 확정하지
않는다**(인왕산 정상고도 초과값, 3D 뷰의 별도 Cesium 지형 샘플링 등 반증에 가까운 정황만 있음 —
`docs/01` 27번 참고). Java 모델(`LegacyCoordinate`)도 필드명을 `elevation`이 아닌 `dnValue`로
지었다.

## 7. Java 구현

```text
com.season.semiproject.spatial.legacy
├── LegacyCoordinate     (record: lng, lat, dnValue)
├── LegacySlopeGroup     (record: groupIndex, coordinates, legacySlopeValue, color)
├── LegacySlopeResult    (record: totalGroupCount, renderedGroups)
├── LegacySlopeCalculator(순수 정적 메서드: haversineMeters/calculateLegacySlope/
│                          colorForLegacySlope/groupCoordinates/toRenderedGroups — GeoJSON,
│                          Spring, DB 의존성 없음)
└── LegacySlopeService   (@Service, GeoJSON JsonNode를 입력받아 flatten+filter 수행 후
                           LegacySlopeCalculator에 위임)
```

`LegacySlopeService`/`LegacySlopeCalculator`는 파일 경로를 전혀 모른다 — GeoJSON 내용(`JsonNode`),
`courseName`, `groupSize`만 파라미터로 받는다. 실제 원본 파일 경로
(`frontend/public/data/인왕산ele copy.geojson`)는 회귀 테스트에서만 사용한다(Phase 5의
`SpatialImportRunner`가 이미 같은 상대 경로 관례를 사용 중). Controller/API는 만들지 않았다.

**갱신(Phase 9A 결과 반영)**: 이 문서를 처음 쓴 시점에는 "Phase 9에서 재사용"을 열어뒀지만,
실제 Phase 9A는 이 Service를 API로 노출하지 않았다 — Frontend가 여전히 자체
`processGeoJSON`/`groupCoordinates`/`calculateSlope`를 직접 수행하고, Backend는 별도의
`TrailGeoJsonService`(Validated `TrailFeature` 기반, `docs/06-frontend-api-compatibility.md`
참고)로 non-slope 화면만 지원한다. `LegacySlopeService`는 여전히 Phase 7A 회귀 검증
전용이며, Legacy Slope 4개 화면과는 아무 코드 경로도 공유하지 않는다.

DB Schema는 변경하지 않았다 — `trail`/`trail_feature`/`trail_node`/`trail_segment` 중 어디에도
경사 관련 컬럼을 추가하지 않았고, 별도 테이블도 만들지 않았다. Phase 7A의 산출물은 순수 계산
결과(비영속 DTO)뿐이다.

## 8. JS ↔ Java Parity 검증 방식 [실행 결과]

Java 구현이 스스로를 검증하는 구조(자기참조)를 피하기 위해, **Java와 독립적인 Node.js
재구현**(`backend/src/test/resources/legacy/generate-fixture.js`, 신규 npm 의존성 없음, Node
내장 기능만 사용)을 만들어 실제 원본 GeoJSON에 대해 4개 코스 × 3개 groupSize(5/7/12) = 12개
조합의 참조 결과(`legacy-slope-fixture.json`, 656KB)를 생성했다. 이 JS는 frontend
`MountainDetailView.vue`의 `calculateHaversineDistance`/`calculateSlope`/`getColorBySlope`/
`groupCoordinates`/`processGeoJSON`를 그대로 옮긴 것이며, 원본 GeoJSON 파일이 바뀌면
`node generate-fixture.js`로 재생성할 수 있다.

Java 테스트(`LegacySlopeServiceParityTest`)는 이 fixture와 실제 GeoJSON을 모두 읽어 비교한다. 먼저
**flatten coordinate count**(`LegacySlopeService.flattenCourseCoordinates()`가 반환하는 좌표
개수)를 각 case의 `totalCoordinateCount`와 exact match로 비교한다 — Phase 7A의 전제인 "Original
GeoJSON → Legacy flatten 결과"의 동일성 자체를 group 생성 이전 단계에서 직접 검증하기 위함이다.
그 다음 group 단위로 `groupIndex` 순서까지 대응시켜 비교한다:

- `groupIndex`, `size`
- start/end의 `lng`/`lat`/`dn`
- `legacySlopeValue`
- `color`

색상, group 개수(`totalGroupCount`/`renderedGroupCount`), flatten coordinate count는 **exact
match**를 요구한다. 좌표값은 `1e-9`, `legacySlopeValue`(삼각함수/제곱근 연산 누적 오차 가능성
고려)는 `1e-6`을 사전에 정한 안전 상한(tolerance)으로 적용했다.

**실측 결과**: 테스트가 매 group 비교마다 `|expected - actual|`을 계산해 그중 최댓값을 추적하며
(`LegacySlopeServiceParityTest.reportMaxObservedError()`), 12개 조합(2509개 렌더링 group) 전체에서
실제 측정된 최대 오차는:

```text
7.105427357601002E-15
```

이다. 이는 `1e-6` tolerance보다 8자리 이상 작은, double 부동소수점 연산 자체의 반올림 오차
수준(machine epsilon 근방)이며, JS(V8)와 Java(JVM)가 동일한 산술 연산을 사실상 동일한 순서로
수행함을 보여준다. `1e-6`은 이 실측값에 여유를 둔 사전 설정 상한이지, 실측값 자체를 대체하는
임의의 숫자가 아니다.

## 9. Parity 결과 [실행 결과]

**범위를 명확히 한다**: 아래는 "임의의 GeoJSON에 대해 JS와 완전히 동일하다"는 일반화된 주장이
아니다. **현재 legacy source인 `인왕산ele copy.geojson`의 실제 4개 코스(마루/무악동/홍제동/부암동)
× 실제 groupSize 3종(5/7/12), 총 12개 조합에 대해서만** flatten coordinate count 및 group 단위
parity를 검증했다.

12개 조합 전부에서 flatten coordinate count exact match + group 단위 exact/tolerance 비교가
통과했다. groupSize=5 combined 통계(모든 코스 합산)는 Phase 7 사전 분석 수치와 정확히 일치한다:

| groupSize=5 | totalGroupCount | renderedGroupCount | #FF4500 | #1E90FF | #32CD32 |
|---|---|---|---|---|---|
| 마루 | 835 | 835 | 176 | 259 | 400 |
| 무악동 | 177 | 177 | 36 | 72 | 69 |
| 홍제동 | 78 | 77 | 39 | 2 | 36 |
| 부암동 | 87 | 87 | 39 | 2 | 46 |
| **합계** | **1177** | **1176** | **290** | **335** | **551** |

groupSize=7: 마루 597/597, 무악동 127/127, 홍제동 56/55, 부암동 62/62.
groupSize=12: 마루 348/348, 무악동 74/74, 홍제동 33/33, 부암동 37/37.
(groupSize 7/12는 홍제동 좌표 수(386)가 7과 12로 나누어떨어지지 않지만 나머지가 1보다 커서
render 손실 없음 — 1-point remainder는 홍제동/groupSize=5 조합에서만 발생했다.)

## 10. Phase 7B(실제 물리적 경사)는 아직 구현할 수 없음 [설계 판단]

Phase 7B(검증된 elevation/DEM 기반 실제 slope_percent 계산)는 다음이 모두 미해결이므로 이번
Phase에서 시작하지 않는다:
- DN이 실제 elevation인지 미확정(반증 정황만 있음, 27번 참고)
- DEM/외부 elevation API를 아직 도입하지 않음(이번에도 도입하지 않았음)
- `TrailFeature`/`TrailSegment` 중 어느 단위에 slope를 귀속시킬지 미확정

## 11. Known Legacy UI Defect (routeCoordinates, 수정하지 않음) [코드 확인]

`MountainDetailView.vue`의 `processGeoJSON()`은 `routeCoordinates.value = allCoordinates`를
**매 Feature 순회마다 그 시점까지 누적된 값으로 계속 덮어쓴 뒤** 마지막에
`allCoordinates.concat(coordinates)`를 실행한다. 즉 `routeCoordinates.value`는 마지막 매칭
Feature의 좌표가 반영되기 **직전** 상태로 남는다. 이는 modal 등 `routeCoordinates.value`를 다시
사용하는 별도 UI/state 경로의 결함이며, `calculateSlope`/`groupCoordinates`/`getColorBySlope` 등
공유 Legacy Algorithm 자체의 결함이 아니다. Phase 7A Java Core는 정상적인
`processGeoJSON → allCoordinates → groupCoordinates → addRouteLayer` 경로만 재현하며, 이 UI
결함을 의도적으로 복제하지 않았다. Frontend 코드는 수정하지 않았다.

## 12. 이번 Phase에서 하지 않은 것

- DB Schema 변경 (컬럼 추가, 신규 테이블) 없음
- `TrailSegment`/`TrailFeature` 데이터 변경 없음
- Controller/REST API 없음
- Frontend 수정 없음 (groupSize 통일 없음, 공식/임계값 수정 없음, modal 버그 수정 없음)
- Phase 7B(실제 물리적 slope, DEM, elevation API) 미구현
- 기존 Spatial Import/Network Build 테스트 전부 회귀 없이 통과 확인
