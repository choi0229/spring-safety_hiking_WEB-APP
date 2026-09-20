# 09. Network 기반 SlopeSection 모델 (Phase 12A-12D)

이 문서는 Legacy Frontend의 좌표-개수 기반 경사 시각화(`groupCoordinates(5/7/12)`)를 대체할
가능성을 검토한 Phase 12A 사전 분석, Backend SlopeSection 모델 구현(Phase 12B), 실사용 적합성
검증 및 geometry 정확도 보완(Phase 12C), 그리고 4개 Legacy Frontend 화면의 실제 전환(Phase
12D)까지를 정리한다. **Phase 12D 완료 시점 기준으로 4개 화면 모두 이 문서의 SlopeSection API를
사용하도록 전환됐다** — §36 이하 참고. Phase 12A/12B/12C 서술(§1-35)은 전환 이전 시점의 분석/
설계 기록이므로 "아직 검증용" 같은 과거 시제 표현은 그 시점 기준으로 읽는다.

## 1. Legacy slope의 문제 (Phase 12A 요약)

기존 `calculateSlope()`는 `Δheight / sqrt(horizontal² + Δheight²) × 100`을 좌표 N개(5/7/12,
화면마다 다름) 단위로 계산한다. 이는:

- 화면마다 청킹 크기가 달라 같은 지점도 화면에 따라 다른 색으로 렌더링될 수 있다.
- 좌표 개수 기준이라 실제 거리 단위가 화면/구간마다 들쭉날쭉하다.
- DB Validated Source(1706건)를 그대로 대입하면 제외된 13건 때문에 좌표 스트림이 밀려 마루
  29.6%, 홍제동구간 39.5%(groupSize=5)의 렌더링 그룹 색상이 원본과 달라짐(Phase 9A에서 실측,
  `docs/08-portfolio-summary.md` Q6 참고) — 이것이 Legacy 4개 화면을 여전히 Original Static
  Source에 묶어두는 이유다.

## 2. DN provenance `[사용자 제공 전제]`

`DN`은 `등산로 원본 SHP + DEM`을 QGIS 등으로 intersect해 "해당 선형 구간과 DEM이 겹치는 부분의
평균 고도(m)"로 생성했다고 사용자가 기억하는 값이다. **repository 내부에서 이 생성 과정을 증명할
스크립트/커밋/문서는 발견되지 않았다** (Phase 12A에서 전체 재검색, DEM 관련 흔적 0건 확인) — 그리고
DN 최댓값(마루 655)이 인왕산 실제고도(약 338m)를 초과하는 반증에 가까운 정황도 여전히 유효하다
(`README.md`, `docs/08` Q7). 따라서 이번 Phase에서도 DN을:

```text
DEM-derived representative elevation sample (미검증)
```

로만 취급한다. 코드/API에서 `measuredElevation`/`exactElevation`/`actualElevation` 같은 이름은
쓰지 않았고, 모든 응답에 `estimatedElevationSource` 필드로 출처를 명시한다(§8 참고).

## 3. TrailSegment에 직접 slope를 두지 않는 이유 `[데이터 확인]`

Phase 12A 실측:

- TrailSegment 길이 중앙값 3.04m, 18.4%가 1m 미만 — 분모가 작아 극단값이 생기기 쉽다.
- Feature-to-Feature 프로토타입(네트워크 인접, 1680쌍)에서 `|slope| > 100%` 21건이 나왔고
  **전부 baseline 거리 5m 미만**이었다(<1m 3건, 1-5m 18건; 5m 이상에서는 0건).
- 1706개 Feature 중 256개(672개 Segment, 31.7%)가 형제 Segment와 동일한 부모 DN을 공유한다 —
  이 형제 Segment 사이 ΔDN=0을 "평지"로 계산하면 원본 DN 복사를 실측 평탄함으로 오인하는 것이다.

그래서 `trail_segment`에 `start_elevation`/`end_elevation`/`slope_percent`/`slope_grade` 컬럼을
추가하지 않았다 — 실제로 존재하지 않는 endpoint elevation을 만들어내야만 계산이 가능해지기
때문이다.

## 4. SlopeSection 모델

```text
TrailFeature.dn_value (DEM-derived representative elevation)
        │
        ▼
TrailSegment Network (from_node_id / to_node_id)
        │  Network traversal (branch-free linear chain 구성)
        ▼
NetworkChain (0..chainLength 누적거리 축)
        │  Feature 단위 ElevationSample 배치 + 선형 보간
        ▼
ElevationProfile (piecewise-linear, sample 범위 밖 외삽 없음)
        │  고정거리 window(10/20/30m) 절단
        ▼
SlopeSection (estimatedElevationStart/End/Delta, estimatedSlopePercent)
        │  ST_LineSubstring로 실제 geometry 절단
        ▼
GET /api/spatial/trails/{trailId}/slope-sections?windowMeters=N (GeoJSON FeatureCollection)
```

`SlopeSection ≠ TrailSegment`다. TrailSegment는 Network의 topology/geometry 단위이고,
SlopeSection은 이 API 전용의 "고정 거리 경사 분석 단위"로, 저장되지 않고 매 요청마다 계산된다
(compute-on-request, §7 참고).

## 5. Network chain 구성 방식

**실제 데이터 확인**: 현재 TrailNode degree 분포는 `{1: 808개, 2: 1718개}`뿐이다 — **degree 3
이상(분기)도, degree-2로만 이루어진 순수 cycle도 현재 데이터에는 없다** (2526 node − 2122
segment = 404, 모두 단순 path와 대수적으로 정합). 따라서 실제 운영 데이터에서는 아래 branch/cycle
로직이 실행되지 않지만, 향후 다른 Trail 데이터에서 발생할 수 있으므로 일반적으로 구현하고
단위테스트로 검증했다(`NetworkChainBuilderTest`).

- **경계(boundary) 정의**: degree != 2인 node(끝점 또는 분기점)를 chain의 시작/끝으로 삼는다.
- **분기(junction) 처리**: degree >= 3 node에 닿으면 그 node에서 chain을 끝내고, 그 node에서
  갈라지는 나머지 미방문 Segment마다 새 chain을 시작한다 — 임의로 한쪽 branch를 선택해 계속
  이어가지 않는다.
- **cycle 처리**: boundary node가 하나도 없는 성분은(현재 데이터에는 없음) 가장 작은 id의
  미방문 Segment부터 시작해 한 바퀴 돌아 시작 node로 돌아오면 `closed` chain으로 처리한다 —
  결정적(deterministic)이지만 방향 자체는 임의다.
- **orientation**: chain은 항상 "더 작은 id를 가진 경계 node"에서 시작하도록 정규화한다
  (`NetworkChainBuilder.canonicalize`) — 그래서 같은 데이터를 다시 계산해도 항상 같은 부호의
  `estimatedSlopePercent`가 나온다. 다만 이 방향은 실제 "오르막"이 아니라 임의의 정규화
  기준일 뿐이다(§7 참고).
- **Trail/컴포넌트 오염 방지**: `findSegmentsForTrail` 쿼리 자체가 `trail_id`로 필터링하므로
  chain은 구조적으로 다른 Trail의 Segment를 포함할 수 없다. 서로 다른 connected component는
  애초에 공유 node가 없으므로 하나의 chain으로 섞일 수 없다 — `SlopeSectionServiceIntegrationTest`
  에서 4개 Trail 전부 개별 계산이 성공함을 재확인했다.

## 6. DN sample 배치

**1 TrailFeature = 1 chain당 1개 sample**(TrailSegment 단위 아님). 한 Feature가 여러
Segment로 쪼개진 경우(256/1706, §3), 그 Feature의 Segment들이 해당 chain에서 차지하는
`[최소 시작거리, 최대 끝거리]` 구간의 중점을 sample 위치로 삼는다(`ElevationProfile.build`,
Candidate B). Feature 중심점을 chain에 `ST_LineLocatePoint`로 재투영하는 Candidate A 대신 이
방식을 택한 이유:

- 이미 chain 구성 단계에서 계산해 둔 누적거리(cumulative distance)를 그대로 재사용해
  추가 PostGIS 왕복 없이 정확한 위치를 얻는다.
- Feature geometry의 centroid/midpoint가 부동소수점 오차로 병합된 chain 위에 정확히 놓이지
  않을 가능성을 피한다.

동일 위치(같은 distance)에 서로 다른 Feature의 sample이 겹치는 경우(현재 데이터에는 없는
edge case) DN을 평균해 하나로 합친다(`ElevationProfileTest.samplesAtIdenticalDistanceAreMergedByAveragingDn`).

## 7. Elevation interpolation과 외삽 금지

두 sample 사이는 선형 보간한다. **sample 커버리지 범위 밖으로는 절대 외삽하지 않는다** —
SlopeSection window 자체가 `[coverageStart, coverageEnd]` 안에서만 생성된다
(`ElevationProfile.elevationAt`, `SlopeSectionCalculator`). 이 값은
"DN sample을 이용한 interpolated **estimated** elevation"이지 TrailNode의 실측 고도가
아니다.

## 8. Slope 공식과 방향

```text
estimatedSlopePercent =
    (estimatedElevationEnd - estimatedElevationStart) / distanceMeters × 100
```

Legacy의 `Δheight / sqrt(horizontal² + Δheight²)`(대각선 거리로 나눔, 수평거리 1m 미만이면
무조건 0 반환)를 그대로 쓰지 않았다 — Legacy 공식은 저 chunk 단위 호환성을 위한 것이었고, 이번
모델은 일반적인 percent grade 정의(수평/geodesic 거리로만 나눔)를 따른다. Signed 값을 그대로
반환하며(`+`=chain 정규화 방향 기준 상승, `-`=하강), 이 방향은 §5에서 설명한 "작은 id가 시작"이라는
임의의 정규화 기준이지 실제 등반 방향이 아니다.

## 9. 10/20/30m 실측 비교 (전체 4개 Trail 합산, Docker 실 데이터)

| window | 총 section 수 | full | partial | min | median\|s\| | avg\|s\| | p75 | p90 | p95 | max\|s\| | \|s\|>50% | \|s\|>100% |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 10m | 729 | 641 | 88 | -115.82 | 22.76 | 24.58 | 34.01 | 44.63 | 50.68 | 115.82 | 37건(5.1%) | 1건(0.1%) |
| 20m | 351 | 290 | 61 | -79.23 | 22.27 | 23.61 | 32.73 | 40.69 | 48.13 | 79.23 | 15건(4.3%) | 0건 |
| 30m | 225 | 179 | 46 | -46.17 | 22.51 | 23.46 | 31.96 | 40.23 | 43.96 | 62.69 | 5건(2.2%) | 0건 |

Trail별 section 수: 마루(10)=530/251/158, 무악동구간(11)=98/49/33, 홍제동구간(12)=40/21/14,
부암동구간(13)=61/30/20 (10/20/30m 순).

**극단값은 window가 커질수록 확실히 줄지만(>100% 21건이던 Phase 12A Feature-to-Feature 대비,
20m/30m에서는 0건), median/avg는 22~25%로 거의 그대로 유지된다** — 원래의 경사 변화 신호는
보존하면서 짧은 baseline noise만 줄어드는 것으로 해석된다.

**chain 커버리지 손실**: 전체 404개 chain 중 실제로 1개 이상 section을 만들어낸 chain 수는
10m=142, 20m=99, 30m=77(4개 Trail 합산, distinct chainIndex 기준)이다 — window가 커질수록
짧은 chain(마루의 다수 fragment 등)이 아예 커버되지 않게 되는 손실이 있다.

## 10. Partial section 정책 (§ "component 끝 처리")

마지막 남은 거리(remainder)가 `windowMeters × 0.5` 이상이면 `PARTIAL_SECTION` 플래그를 달아
포함하고, 그 미만이면 버린다(`SlopeSectionCalculator.PARTIAL_MIN_FRACTION = 0.5`). 두 극단(A.
전부 partial로 포함 / B. 조금이라도 못 채우면 제외)의 절충안으로, window 크기 자체에 비례하는
기준을 택해 window마다 별도 상수를 정할 필요가 없게 했다. 실측 결과 10m에서는 729건 중 88건
(12.1%)이 partial, 20m은 351건 중 61건(17.4%), 30m은 225건 중 46건(20.4%)이 partial이다 —
window가 커질수록 남는 remainder가 우연히 짧은 window보다 상대적으로 커지는 경향이 있다.

## 11. 기본 window 추천: **20m** `[설계 판단]`

| 기준 | 10m | 20m | 30m |
|---|---|---|---|
| 극단값(>100%) | 1건 있음 | 0건 | 0건 |
| p95 | 50.68% | 48.13% | 43.96% |
| 원 신호 보존(median/avg) | 22.76/24.58 | 22.27/23.61 | 22.51/23.46 (거의 동일) |
| section 수(해상도) | 729 | 351 | 225 |
| chain 커버리지 | 142개 chain 도달 | 99개 chain | 77개 chain |
| Frontend polyline 개수 | 많음(렌더링 부담) | 중간 | 적음 |

10m은 여전히 100%를 넘는 극단값이 1건 남아 있고, 30m은 chain 커버리지를 20m 대비 22% 더
잃으면서(99→77) p95 개선폭은 크지 않다(48.13→43.96). **20m이 극단값 제거·해상도·커버리지의
균형점**이라고 판단해 기본값으로 제안한다 — 단, API는 셋 다 계속 지원한다(§13).

## 12. API

```text
GET /api/spatial/trails/{trailId}/slope-sections?windowMeters=10|20|30
```

`windowMeters`가 이 세 값이 아니면 400, 존재하지 않는 `trailId`면 404. 응답은 GeoJSON
FeatureCollection이며 최상위에 `trailId`, `windowMeters`, `estimatedElevationSource`(DN
provenance 고지 문자열)를 함께 반환한다. 각 Feature의 `properties`:

```json
{
  "chainIndex": 0,
  "sectionIndex": 3,
  "distanceMeters": 20.0,
  "estimatedElevationStart": 214.7,
  "estimatedElevationEnd": 218.1,
  "estimatedElevationDelta": 3.4,
  "estimatedSlopePercent": 17.0,
  "dataQualityFlag": null
}
```

`dataQualityFlag`는 `PARTIAL_SECTION`이거나(마지막 남은 구간) `null`이다 — 매 요청마다 값이
거의 항상 같은 `INTERPOLATED` 같은 플래그는 만들지 않았다(§22 요구사항대로 의미 있는 것만).
내부 DB id(segmentId, nodeId 등)는 노출하지 않고, `chainIndex`는 이 API 응답 내부에서만
의미가 있는 로컬 인덱스임을 문서에 남긴다(안정적인 외부 식별자가 아님).

## 13. 계산할 수 없는 section 처리

Elevation sample이 2개 미만이거나, 구간 양끝의 보간값을 구할 수 없거나, 거리가 0 이하인
section은 **생성하지 않는다** — `slope=0`으로 채우는 fallback은 어디에도 없다
(`SlopeSectionCalculator.addSectionIfComputable`). 실 데이터 12개 조합(4 Trail × 3
window) 전체 응답에서 `NaN`/`Infinity` 문자열이 하나도 없음을 확인했다.

## 14. 알려진 정밀도 한계 `[데이터 확인]`

`distanceMeters`는 Segment별 `ST_Length(geom::geography)`를 누적한 실제(geodesic) 거리이지만,
실제 반환되는 section geometry는 `ST_LineSubstring(geometry타입, startFraction, endFraction)`으로
자른 것이다 — PostGIS의 `ST_LineSubstring`은 `geometry` 타입에 대해 **평면(도 단위) 길이 비율**로
자르지, geography 길이 비율로 자르지 않는다. 위도가 바뀌는 구간에서는 위도-도-길이와
경도-도-길이의 실제 미터 환산 비율이 달라, 자른 geometry의 실제 길이가 `distanceMeters`와
완벽히 일치하지 않을 수 있다. 20m section 20개를 표본 검사한 결과 **실제 오차는 최대 0.93m
(약 4.6%)**, 대부분 0.1~0.4m였다 — 무시할 정도는 아니지만 이번 Phase의 검증 목적(정확한
측량이 아니라 재현 가능한 상대 비교)에서는 수용 가능한 수준으로 판단해 그대로 두었다. §15에서
명시적으로 `ST_LineSubstring` 사용을 요구했고, Segment 단위로 직접 보간해 자르는 대안은 이번
Phase 범위를 넘는 것으로 판단했다.

## 15. 저장 모델: 여전히 compute-on-request

이번 Phase에서도 `slope_section` 테이블은 만들지 않았다. Trail당 API 응답 시간은 마루
(가장 큰 Trail, 391개 chain, window=10m, 530 section)에서도 median 60.32ms, p95
63.49ms(warmup 5 + 측정 30회)였고, 다른 3개 Trail(window=20m)은 median 13.35ms, p95
14.46ms였다 — 현재 규모에서 캐시/영속화가 필요할 정도로 느리지 않다.

## 16. 테스트

```text
Unit (DB 없음): NetworkChainBuilderTest(7), ElevationProfileTest(5), SlopeSectionCalculatorTest(7)
  - linear chain / reversed orientation / 정규 방향(canonicalize) / junction 분기 / cycle
  - DN sample 배치·중복제거 / 선형보간 / 외삽 금지
  - fixed window 절단 / partial 임계값 / 양/음 slope / Legacy 공식과 다른 공식 / sample 부족 시 빈 결과

Integration (실 Docker DB): SlopeSectionServiceIntegrationTest(7)
  - 4개 Trail × 3 window 전부 계산 가능, section 0건 아님
  - geometry LineString/좌표가 인왕산 bounding box 안(=SRID/좌표계 정합 방증)
  - slope 재계산값이 응답값과 일치(공식 검증), NaN/Infinity 없음, distance>0
  - window가 커질수록 section 수가 줄거나 같음(단조성)
  - 계산 전후 trail/trail_feature/trail_node/trail_segment/accident_point count 불변

API 계약 (MockMvc, 실 DB): SlopeSectionApiTest(5)
  - 정상 응답 shape, 10/20/30 허용, 그 외 값 400, 존재하지 않는 trailId 404
  - dataQualityFlag가 null 또는 PARTIAL_SECTION만
```

전체 backend 회귀(기존 93 + 신규 31): **124 / 124 PASS**(BUILD SUCCESS, 실 Docker
PostgreSQL/PostGIS 대상).

## 17. Legacy 대비, 프론트엔드 전환 시점

Legacy는 좌표-개수 기준이라 화면마다 다른 지점에서 색이 바뀔 수 있는 반면, 이 모델은 실제 거리
기준으로 어느 화면에서 호출해도 같은 section 경계를 준다 — "클라이언트 독립적"이라는 목표는
충족한다. 다만 §14의 geometry 절단 오차, §9의 여전히 존재하는 4~5%의 `|slope|>50%` section,
§11에서 확인한 chain 커버리지 손실(20m 기준 404개 중 99개만 도달)을 고려하면 **아직 4개 Legacy
화면을 이 API로 전환하기에는 이르다**고 판단한다 — Phase 12C/D 진입 판단(완료 보고 참고).

## 18. 데이터 한계 재확인 (Phase 12B 시점)

1. DN은 TrailNode의 실측 고도가 아니다 — DEM-derived representative elevation sample(미검증)이다.
2. Elevation profile은 Feature 단위 sample 사이의 선형 보간 결과다.
3. 원본 DEM의 공간 해상도/처리 과정은 repository에서 확인할 수 없다 — "1:5000 DEM"처럼 축척을
   단정하는 표현은 쓰지 않았다.
4. 결과는 measured slope가 아니라 estimated slope다.
5. Network는 exact-endpoint baseline이며 snapping/interior noding을 적용하지 않았다
   (`docs/03-trail-network-modeling.md`).
6. section geometry는 `ST_LineSubstring`의 평면 비율 절단 특성으로 인해 `distanceMeters`
   대비 최대 약 5%(관측치)의 길이 오차를 가질 수 있다(§14) — **Phase 12C에서 개선, §19-27 참고.**

---

# Phase 12C: 실사용 적합성 검증 및 Geometry 정확도 보완

Phase 12B는 chain **개수** 기준(`20m → 99/404 chain`)으로 coverage를 이야기했다. 이 절부터는
실제 **거리(길이) 기준** coverage로 재평가하고, §14에서 발견한 geometry 절단 오차를 실제로
줄인다. Frontend는 여전히 변경하지 않았다.

## 19. Length Coverage 정의

- **분모 A (전체 Network 기준)** = 그 Trail의 모든 TrailSegment 길이 합(`ST_Length(geom::geography)` 누적).
- **분모 B (Elevation Sample Coverage 기준)** = `ElevationProfile`이 실제로 정의된 범위
  (각 chain의 `[coverageStart, coverageEnd]`) 길이 합 — sample이 2개 미만인 chain은 전체가
  제외된다.
- **분자** = 실제 생성된 SlopeSection들의 `distanceMeters` 합(해당 window).

B가 Slope 모델 자체의 상한선이다 — 외삽을 하지 않는 한 A의 100%에는 어차피 도달할 수 없다.

## 20. Length Coverage 실측 (window-무관, 실 데이터)

| Trail | Network Length | Elevation Coverage | vs Network |
|---|---:|---:|---:|
| 마루 | 7117.10m | 5464.61m | 76.8% |
| 무악동구간 | 979.57m | 976.72m | 99.7% |
| 홍제동구간 | 448.27m | 407.90m | 91.0% |
| 부암동구간 | 653.87m | 608.58m | 93.1% |
| **전체** | **9198.81m** | **7457.80m** | **81.1%** |

마루만 fragmentation 때문에 76.8%로 낮고, 나머지 3개 코스는 91~99.7%다. 나머지 18.9%(1741.01m)는
`insufficientSamples`(chain 전체가 sample 2개 미만, 619.35m) + `outsideInterpolation`(sample
범위 밖 chain 끝부분, 1121.66m)으로 구성된다.

## 21-23. 10/20/30m Section Coverage (Trail별 + 전체)

| window | Trail | Network | ElevCoverage | SectionCoverage | vsNetwork | vsElevCoverage |
|---|---|---:|---:|---:|---:|---:|
| 10m | 마루 | 7117.10 | 5464.61 | 5108.25 | 71.8% | 93.5% |
| 10m | 무악동구간 | 979.57 | 976.72 | 976.72 | 99.7% | 100.0% |
| 10m | 홍제동구간 | 448.27 | 407.90 | 400.00 | 89.2% | 98.1% |
| 10m | 부암동구간 | 653.87 | 608.58 | 604.26 | 92.4% | 99.3% |
| **10m** | **전체** | 9198.81 | 7457.80 | **7089.23** | **77.1%** | **95.1%** |
| 20m | 마루 | 7117.10 | 5464.61 | 4732.12 | 66.5% | 86.6% |
| 20m | 무악동구간 | 979.57 | 976.72 | 976.72 | 99.7% | 100.0% |
| 20m | 홍제동구간 | 448.27 | 407.90 | 407.60 | 90.9% | 99.9% |
| 20m | 부암동구간 | 653.87 | 608.58 | 594.97 | 91.0% | 97.8% |
| **20m** | **전체** | 9198.81 | 7457.80 | **6711.40** | **73.0%** | **90.0%** |
| 30m | 마루 | 7117.10 | 5464.61 | 4400.96 | 61.8% | 80.5% |
| 30m | 무악동구간 | 979.57 | 976.72 | 976.72 | 99.7% | 100.0% |
| 30m | 홍제동구간 | 448.27 | 407.90 | 407.60 | 90.9% | 99.9% |
| 30m | 부암동구간 | 653.87 | 608.58 | 589.29 | 90.1% | 96.8% |
| **30m** | **전체** | 9198.81 | 7457.80 | **6374.57** | **69.3%** | **85.5%** |

**Phase 12B의 "20m → 99/404 chain(24.5%)"이라는 chain-count 지표는 실제 coverage를 심하게
과소평가했다** — 실제 길이 기준 coverage는 20m에서 전체 network 대비 73.0%, elevation-sample이
정의된 범위 대비로는 90.0%다. Chain-count 지표는 앞으로 coverage 판단 기준으로 사용하지 않고
보조 지표로만 남긴다(§8 지시 반영).

## 24. 누락 구간 원인 분류 (window=20m 기준)

| 원인 | 전체 길이 | 설명 |
|---|---:|---|
| `INSUFFICIENT_DN_SAMPLES` | 619.35m | chain 전체에 DN sample이 2개 미만(짧은 fragment 위주) |
| `OUTSIDE_INTERPOLATION_COVERAGE` | 1121.66m | sample 범위 밖 chain 양 끝(외삽 금지 정책) |
| `REMAINDER_BELOW_PARTIAL_THRESHOLD` | 746.40m | window 절반 미만의 마지막 remainder(정책상 폐기) |

가장 큰 개별 손실은 `INSUFFICIENT_DN_SAMPLES` 최대 40.51m, `OUTSIDE_INTERPOLATION_COVERAGE`
최대 155.22m(마루 chainIndex=108, 전체 network 7117m의 2.2%), `REMAINDER_BELOW_PARTIAL_THRESHOLD`
최대 9.96m 뿐이다 — **top 5 손실 사례 15건 중 14건이 마루(trail=10)** 였다. 이는 §6에서 예상한
대로 **마루의 component fragmentation(391개)이 coverage 손실의 주원인**임을 데이터로
확인해준다. 155.22m를 제외하면 나머지는 전부 30m 미만의 짧은 조각이며, "코스의 중요한 긴 구간이
통째로 빠졌다"고 볼 근거는 없다.

## 25. Geometry Distance Error — 전체 데이터셋 재측정 (개선 전)

| window | 구분 | n | absError median | p90 | p95 | max | relError median | p90 | p95 | max |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 10m | full | 641 | 0.21m | 0.85m | 1.00m | 1.40m | 2.09% | 8.48% | 9.98% | 14.00% |
| 20m | full | 290 | 0.42m | 1.57m | 1.90m | 2.78m | 2.08% | 7.87% | 9.52% | 13.89% |
| 30m | full | 179 | 0.59m | 2.26m | 2.75m | 3.76m | 1.96% | 7.52% | 9.16% | 12.54% |

Phase 12B의 20-section 표본(§14, max 0.93m/4.6%)보다 **전체 데이터셋의 실제 tail 오차가 훨씬
컸다**(p95 9.5%, max 13.9~14.0%) — 작은 표본으로 과소평가됐던 것이다. 이 오차는 `distanceMeters`
자체(=slope 계산에 쓰이는 값)에는 영향이 없다 — slope는 geography 누적거리로 계산되고 geometry는
표시용으로만 별도 절단되기 때문이다. 따라서 이 문제는 **분석 정확도가 아니라 표시 geometry와
metadata(`distanceMeters`) 정합성** 문제다(§13 요구사항대로 구분).

## 26. Geometry Cut 후보 검토

### Candidate A — 현재(Phase 12B) 방식 유지
전체 chain WKT + 전역 fraction으로 `ST_LineSubstring` 1회. 구현 단순하지만 §25의 오차가 그대로
남는다.

### Candidate B — Projected CRS로 변환 후 절단
`ST_Transform`으로 미터 단위 평면 좌표계로 바꾼 뒤 자르고 다시 4326으로 되돌리는 방식. 임의로
"한국이니까 UTM"을 고르지 않고 repository를 확인한 결과, **EPSG:5179(Korea 2000 / Unified CS)가
이미 이 프로젝트의 사고 데이터 CRS 검증 로직에서 다뤄지는 값**이었다(`AccidentImportServiceTest`,
`AccidentImportService`) — 근거 없는 임의 선택은 아니었을 것이다. 다만 이 방식은 왕복 좌표계
변환 비용과 "왜 이 EPSG인가"를 문서화해야 하는 부담이 있어 채택하지 않았다.

### Candidate C — Segment 단위 직접 절단 (채택)
Chain 전체가 아니라 **개별 TrailSegment(중앙값 3m) 단위로 `ST_LineSubstring`을 적용**한다.
이미 chain 구성 단계에서 각 Segment의 시작/끝 누적거리(geography 기준)를 알고 있으므로, section
경계가 어느 Segment 안에 있는지 찾아 그 Segment "안에서의" local fraction만 계산해 자른다
(`ChainDistanceLocator`). 짧고 거의 직선인 Segment 하나 안에서는 평면 비율과 geography 비율의
불일치가 무시할 수준으로 작아진다 — CRS 변환도, 새 의존성도 필요 없고 기존 `ST_LineSubstring`
호출 패턴을 그대로 재사용한다.

| 기준 | A(유지) | B(CRS 변환) | C(Segment 단위, 채택) |
|---|---|---|---|
| 거리 정확도 | 낮음(§25) | 높음 | 높음(§27 실측) |
| 구현 복잡도 | 최소 | 중간(CRS 선정/왕복 변환) | 중간(경계 탐색 로직 추가) |
| PostGIS 활용 | O | O | O(더 잘게 활용) |
| 테스트 가능성 | 보통 | 보통 | 좋음(순수 로직으로 분리 가능) |
| 기존 architecture 영향 | 없음 | Mapper에 CRS 상수 도입 | Service 내부에만 국한 |
| API 성능 | 기존과 동일 | 왕복 변환 오버헤드 | 기존과 동일(§29) |

## 27. 적용한 개선: Candidate C

`SlopeSectionService`의 geometry 절단 로직을 전면 교체했다: `ChainDistanceLocator`가 section의
시작/끝이 어느 `ChainSegmentUsage`에 속하는지, 그 안에서의 local fraction이 얼마인지 계산하고,
두 지점이 다른 Segment에 속하면 [시작 Segment의 꼬리 절단] + [중간 Segment 전체(DB 호출 불필요,
이미 메모리에 있는 좌표 재사용)] + [끝 Segment의 머리 절단]을 이어붙인다. `mapper-slope-section.xml`
은 **변경하지 않았다** — 기존 범용 `cutSections` 쿼리(idx/wkt/startFrac/endFrac)를 chain 전체
WKT 대신 개별 Segment WKT에 그대로 재사용했다. Slope 계산 모델(`SlopeSectionCalculator`,
`ElevationProfile`)은 전혀 건드리지 않았다.

## 28. 개선 전/후 Geometry Error 비교

| window | 지표 | 개선 전(Phase 12B) | 개선 후(Phase 12C) | 개선폭 |
|---|---|---:|---:|---:|
| 10m | absError median | 0.21m | 0.018m | 11.4x |
| 10m | relError p95 | 9.98% | 2.13% | 4.7x |
| 20m | absError median | 0.42m | 0.036m | 11.7x |
| 20m | relError median | 2.08% | 0.18% | 11.5x |
| 20m | relError p95 | 9.52% | 1.58% | 6.0x |
| 20m | relError max | 13.89% | 5.34% | 2.6x |
| 30m | relError p95 | 9.16% | 0.94% | 9.8x |

절대오차/상대오차 모두 window에 상관없이 10배 안팎으로 개선됐다. 남은 max 오차(20m 기준 5.34%,
1.07m)는 여전히 0은 아니지만 Phase 12B 대비 훨씬 작고, 실사용(지도 시각화) 목적에는 충분하다고
판단한다.

## 29. Slope 결과 Parity (개선 전/후)

Geometry 절단 로직만 바꿨으므로 slope 수치는 완전히 동일해야 한다 — 실측으로 확인했다: 20m
기준 section 수(729/351/225, full/partial 641+88/290+61/179+46), `min/median/avg/p75/p90/p95/max`
(`-115.82/22.76/24.58/34.01/44.63/50.68/115.82` 등, 10m 기준)가 **개선 전후 소수점까지
완전히 동일**했다. `SlopeSectionServiceIntegrationTest.noSectionIsASilentZeroFallback`(slope
재계산값과 응답값 일치 검증)도 개선 후에도 그대로 통과했다.

## 30. API 성능 재검증 (개선 전/후)

| 케이스 | 개선 전 median/p95/max | 개선 후 median/p95/max |
|---|---|---|
| 부암동구간, 20m (경량) | 13.35 / 14.46 / - ms | 8.15~8.63 / 12.4 / 12.9 ms |
| 마루, 10m (최대 부하, 729건 중 530건) | 60.32 / 63.49 / 65.83 ms | 57.79~63.00 / 63.27~89.44 / 64.38~103.92 ms |

경량 케이스는 오히려 개선(Segment 단위 WKT가 chain 전체 WKT보다 짧아 DB 왕복 payload가 작아짐).
최대 부하 케이스는 반복 측정 결과 변동폭 안(median은 거의 동일, 한 번의 실행에서 p95/max가
튀었으나 재측정 시 원래 수준으로 복귀)이었다 — 정확도 개선이 성능을 유의미하게 악화시키지
않았다고 판단한다.

## 31. 대표 Section 추적 (window=20m)

| 유형 | Trail | slope | elevStart→End | bracket DN samples |
|---|---|---:|---|---|
| 상승 | 마루 | +67.46% | 476.09→489.58 | Feature 2619(dn=476)↔2618(dn=477) 시작측, 2606(dn=489)↔2605(dn=490) 끝측 |
| 하강(=급경사) | 무악동구간 | -79.23% | 214.87→199.03 | Feature 3084(dn=217)↔3083(dn=211) 시작측, 3080(dn=200)↔3079(dn=199) 끝측 |
| 완만 | 부암동구간 | -0.73% | 137.00→136.85 | Feature 3345(dn=138)↔3346(dn=136) 시작측, 3346↔3347(dn=137) 끝측 |
| partial | 마루 | +45.22% (13.27m) | 359.00→365.00 | Feature 1942(dn=359)↔1943(dn=360) 시작측, 1947(dn=364)↔1948(dn=365) 끝측 |

모든 값이 어느 두 TrailFeature의 DN 사이를 어떻게 보간했는지까지 역추적 가능함을 확인했다
(`ElevationProfile.getSamplesForTrace()`, 테스트/디버그 전용, Production API에는 노출하지 않음).

## 32. 20m 최종 판단: 유지

| 기준 | 10m | 20m | 30m |
|---|---|---|---|
| Length coverage(vs elev) | 95.1% | 90.0% | 85.5% |
| Extreme noise(>100%) | 1건 | 0건 | 0건 |
| Geometry rel error median(개선후) | 0.18% | 0.18% | 0.16% |
| 해상도(section 수) | 729 | 351 | 225 |

20m은 극단값 0건, coverage 90%, geometry 오차 개선 후 median 0.18%(무시 가능)를 모두 만족하며
30m 대비 해상도 손실이 적다 — **기본값 20m을 유지한다.**

## 33. Frontend 전환(Phase 12D) Go/No-Go 판단

```text
1. 20m length coverage        : 전체 73.0%(vs network) / 90.0%(vs elevation coverage) -- 마루 제외 3개 코스는 91~99.7%
2. uncomputable 구간 원인 설명 : O (§24, 마루 fragmentation이 주원인, 최대 단일 손실 155m/2.2%)
3. geometry 오차              : O 대폭 개선(median 0.18%, p95 1.58%)
4. >100% short-baseline noise : O 20m/30m에서 0건
5. section geometry valid     : O (bounding box, NaN/Infinity 없음, §14 오차 개선)
6. 4개 Trail 모두 충분한 section: O (21~251개)
7. API 성능                    : O (median <65ms, p95 <90ms)
8. client-independent boundary : O (거리 기준, 화면 무관)
```

**판정: GO** — 단, 마루의 length coverage(73.0%/86.6%)가 다른 3개 코스(91~99.7%)보다 뚜렷이
낮다는 조건부 사실은 Phase 12D 설계에 반드시 반영해야 한다: 마루 화면에서는 SlopeSection이
비어 있는 구간(전체 27% 정도)이 시각적으로 드러날 수 있다 — 이를 "버그"가 아니라 "DN sample이
없거나 해당 chain이 짧아 계산 자체가 불가능한 구간"으로 Frontend에서 명시적으로 처리(예: 회색
선 또는 미표시)해야 한다. Legacy는 이런 구간에서도 억지로 색을 칠했다는 점에서 오히려 새 모델이
더 정직하다고 볼 수 있다.

## 34. Phase 12D 아키텍처 후보 (제안만, 미구현)

```text
TrailSegment Network
        ↓
SlopeSectionService (Phase 12B/12C)
        ↓
20m SlopeSection GeoJSON (기본 window)
        ↓
4개 Legacy slope views
        ↓
estimatedSlopePercent → color (+ 커버리지 없는 구간은 미표시)
```

제거 후보(Phase 12D에서 실제 코드 기준 재확인 후 결정): `processGeoJSON`의 slope 전용 분기,
`groupCoordinates(5/7/12)`, `calculateSlope()`. `LegacySlopeService`는 이번에도 삭제하지
않는다(§27 지시, Phase 12D 이후 별도 결정).

## 35. Portfolio 문장 검증 (§40)

> "개별 Network Edge가 지나치게 짧아 DEM 기반 경사 계산 노이즈가 커지는 문제를 데이터로
> 확인하고, Network 연결관계를 따라 고정 거리 SlopeSection을 구성했습니다. 이를 통해 기존
> 좌표 개수 기반 청킹보다 클라이언트 독립적인 공간 분석 단위를 정의했으며, DEM-derived 대표
> 고도값을 보간해 Backend에서 estimated slope를 계산했습니다."

**판정: 정확히 사용 가능.** 1문장(짧은 Edge→노이즈 확인→고정거리 SlopeSection 구성), 2문장
(클라이언트 독립적 분석 단위), 3문장(DEM-derived 대표 고도값 보간→estimated slope 계산) 모두
Phase 12A~12C에서 실제로 수행하고 실측한 내용과 정확히 일치한다 — "DEM 기반"이라는 표현도
"DEM-derived 대표 고도값"이라는 이미 확립된 hedge 표현과 짝을 이루어 쓰여서 미검증 전제를
사실처럼 단정하지 않는다.

---

# Phase 12D: 4개 Legacy Frontend 화면 전환

Phase 12C까지는 검증용 API였다. 이 절부터는 실제로 `MountainDetailView.vue`,
`CompareCourseView.vue`, `MountainDetailView2.vue`, `MobileMountainDetailView.vue` 4개 화면을
20m SlopeSection API 기반으로 전환한 결과를 기록한다. **Backend는 이번 Phase에서 전혀
수정하지 않았다** — `SlopeSectionService`/`SlopeSectionController`/Mapper 모두 Phase 12C
상태 그대로다.

## 36. Frontend 전환 대상과 공통 모듈

신규 파일 `frontend/src/api/slopeSection.js` 하나를 4개 화면이 공유한다:

```text
TRAIL_ID_BY_COURSE_NAME   -- Trail GeoJSON API의 PMNTN_NM 값 기준 (마루/무악동구간/홍제동구간/부암동구간)
resolveTrailId(courseName) -- course DB 테이블의 courseName("무악동" 등, "구간" 접미사 없음)과
                               PMNTN_NM("무악동구간" 등)이 다르다는 것을 실제 DB로 확인한 뒤 추가한
                               resolver. Legacy의 `PMNTN_NM.includes(courseName)` 부분일치
                               필터와 동일한 방식으로 매칭한다.
SLOPE_WINDOW_METERS = 20  -- 4개 화면 전체 공통, UI에서 조절 불가
getEstimatedSlopeColor(estimatedSlopePercent) -- 색상 매핑 단일 지점
fetchTrailGeoJson() / fetchSlopeSections(trailId, windowMeters) -- API 호출
```

`resolveTrailId`는 사전 구현 중 실제 DB(`SELECT course_id, course_name FROM course`)를 조회해
`course_name`이 "마루"/"무악동"/"홍제동"/"부암동"(구간 접미사 없음)임을 확인한 뒤 추가했다 —
`TRAIL_ID_BY_COURSE_NAME`을 courseName으로 직접 인덱싱했다면 마루를 제외한 3개 코스에서 조용히
실패했을 것이다. Node 스모크 테스트로 4개 코스 전부 올바른 Trail ID로 resolve됨을 확인했다(§43).

## 37. Base + Overlay 구조

```text
Trail GeoJSON API (/api/spatial/trails/geojson)
        ↓ PMNTN_NM 필터
Base Trail Layer (neutral, 기존 기본 경로 색 #32CD32 재사용)
        ↓
SlopeSection API (/api/spatial/trails/{trailId}/slope-sections?windowMeters=20)
        ↓
Slope Overlay Layer (estimatedSlopePercent 기반 색상, Base 위에 겹쳐 그림)
```

Base Trail은 항상 먼저 그려지고, Slope Overlay는 그 위에 별도 Polyline들로 추가된다 — 기존
`drawBaseRoute()`(녹색 기본선)는 그대로 유지하고, 기존에 `addRouteLayer` 내부에서 하던
groupCoordinates 기반 색칠 루프만 제거한 뒤 그 자리에 `renderSlopeOverlay()`를 새로 호출한다.

## 38. Course 변경 시 Overlay 정리

- `MountainDetailView.vue`/`MobileMountainDetailView.vue`: 모듈 스코프의 `slopeOverlayPolylines`
  배열에 그려둔 Polyline을 추적하다가, `renderSlopeOverlay()` 재호출 시작 시 전부
  `setMap(null)` 한 뒤 새로 채운다.
- `CompareCourseView.vue`: 여러 코스를 동시에 비교하므로 `slopeOverlayPolylinesByCourse`
  (course 이름별 배열)로 분리 추적한다 — 한 코스의 Overlay를 지울 때 다른 코스 Overlay가
  같이 사라지지 않는다.
- `MountainDetailView2.vue`: 코스 선택 기능이 원래 없다(§41 참고, PMNTN_NM 필터가 사실상
  no-op). 4개 Trail 전부의 SlopeSection을 한 번에 겹쳐 그리고, 재호출 시 전체를 지운다.

## 39. 미커버 구간 처리

`renderSlopeOverlay()`는 SlopeSection API가 반환하는 Feature만 그린다 — 반환되지 않는 구간(§20
"Length Coverage")은 Slope Overlay 없이 Base Trail(녹색)만 남는다. 임의로 slope=0을 채우거나
인접 section의 색을 복사하지 않는다. API 실패 시에도(catch 블록) Base Trail은 이미 그려져
있으므로 그대로 유지되고 Overlay만 생략된다. 반대로 Trail GeoJSON API 자체가 실패하면
`processGeoJSON`이 호출되지 않아 Base/Overlay 둘 다 그려지지 않는다 — 기존 화면의 에러 처리
관례(콘솔 로그만)를 그대로 따른다.

**마루 화면에서는 이것이 실제로 눈에 보인다** — Network coverage(§20) 66.5%(vs network)/
86.6%(vs elevation coverage)이므로, 마루의 약 1/4~1/3 구간은 Base Trail만 보이고 색이 없다.
이는 버그가 아니라 §13/§39(원본 요청)에서 명시한 대로 "경로 geometry ≠ 경사 계산 가능 영역"을
그대로 UI에 반영한 것이다.

**Defensive fix (Phase 12D 후속)**: `getEstimatedSlopeColor()`가 원래 `undefined`/`NaN` 같은
invalid 입력을 완만(초록) 색으로 fallback하고 있었다 — "계산 불가"와 "계산 결과가 0에 가까움"을
혼동시키는 결함이었다. `typeof === 'number' && Number.isFinite(...)`로 명시 검증해 invalid
입력에는 `null`을 반환하도록 고치고, 4개 화면의 Overlay 렌더링 루프에서 `color == null`이면
해당 Feature의 Polyline 자체를 만들지 않도록(= Base Trail만 남도록) 수정했다. 실제 API 응답에는
invalid 값이 존재하지 않음을 확인했으므로(4개 Trail 전수 확인) 이 변경은 순수 방어 코드다.

## 40. Slope Color Rule

```js
STEEP_UP_THRESHOLD = 40   // estimatedSlopePercent > 40  -> #FF4500 (빨강, 오르막)
STEEP_DOWN_THRESHOLD = -40 // estimatedSlopePercent < -40 -> #1E90FF (파랑, 내리막)
그 외                      -> #32CD32 (초록)
```

`CompareCourseView.vue`의 기존 범례(`오르막=#FF4500`, `평지=#32CD32`, `내리막=#1E90FF`)와
정확히 같은 3색 팔레트를 재사용했다 — **팔레트(디자인)는 유지하고 threshold(도메인 규칙)만
새로 정의**했다(§16 원칙). 기존 Legacy threshold(`>30`/`< -15`)를 그대로 재사용하지 않은 이유:
Legacy는 대각선 거리로 나눈 값이고 새 모델은 순수 수평/geodesic 거리로 나눈 값이라 같은
숫자가 다른 의미를 가진다(`docs/04`).

**±40을 고른 근거**: 실제 20m `estimatedSlopePercent` 분포(Phase 12C, 전체 4개 Trail
351건)에서 `|slope|`의 p90이 약 40%였다 — ±40 기준으로 실측 분류하면 상승 8.8%(31건)/
하강 1.7%(6건)/완만 89.5%(314건)로, "대부분 초록, 이따금 빨강, 드물게 파랑"이라는 기존 UI의
시각적 성격을 유지한다. 이는 **객관적인 등산 난이도 기준이 아니라 이 UI만을 위한 presentation
rule**이다(§40 원본 요청 지시대로 명시).

방향(sign)은 보존한다 — `estimatedSlopePercent > 0`은 chain의 정규화된 순회 방향 기준
상승이다. **알려진 한계**: 이 방향은 391개(마루)+3개(나머지)=404개 chain마다 "더 작은 node id가
시작"이라는 임의의 기준으로 정규화된 것이라, 반드시 실제 등반 방향(들머리→정상)과 일치한다는
보장은 없다(`docs/09` §5, chain canonicalization). 실측한 20m 분포에서 상승(+)이 하강(-)보다
훨씬 많이 나타난 것(23.1% vs 8.3%, ±30 기준)은 3개 코스(무악/홍제/부암)가 대체로 단조 상승
코스라는 실제 지형 특성과 일치하는 정황이지만, 마루처럼 조각난 코스에서는 chain별로 부호가
뒤집혀 보일 가능성을 완전히 배제할 수 없다 — 향후 실사용 중 시각적으로 부자연스러운 부분이
발견되면 후속 Phase에서 elevation trend 기반 방향 정규화를 검토할 수 있다.

## 41. 화면별 Before/After

| 화면 | Before data source | Before groupSize | After Base | After Overlay |
|---|---|---|---|---|
| MountainDetailView.vue | `/data/인왕산ele copy.geojson` | 5 | Trail GeoJSON API | SlopeSection API, 20m |
| CompareCourseView.vue | 위와 동일 | 5 | Trail GeoJSON API (코스별) | SlopeSection API, 20m (코스별) |
| MountainDetailView2.vue | 위와 동일 | 7 | Trail GeoJSON API (전체, 코스 필터 no-op) | SlopeSection API, 20m × 4개 Trail 전부 |
| MobileMountainDetailView.vue | 위와 동일 | 12 | Trail GeoJSON API | SlopeSection API, 20m |

MountainDetailView2.vue는 원래부터 `PMNTN_NM.includes('')`(빈 문자열)로 필터해 사실상 모든
코스를 한 번에 그리고 있었다 — 이 no-op 필터는 그대로 두고(§26 지시대로 불필요한 rename/동작
변경 회피), Slope Overlay만 4개 Trail 전부에 대해 순회 호출하도록 구현했다.

`routeCoordinates`(진행률 마커, 최고 고도 비교 등 slope 외 기능)와 `drawElevationChart`(고도
차트)는 4개 파일 모두에서 그대로 유지했다 — `processGeoJSON`의 좌표 flatten/DN 추출 로직 자체는
건드리지 않고, slope 색칠 루프만 제거했다(§4/§26 지시).

## 42. Desktop/Mobile 동일성

`MountainDetailView.vue`(Desktop)와 `MobileMountainDetailView.vue`(Mobile) 모두
`SLOPE_WINDOW_METERS = 20`을 같은 모듈에서 import해서 쓴다 — 같은 Trail이면 서버가 반환하는
section 개수/좌표/`estimatedSlopePercent`가 완전히 동일하다(같은 API, 같은 파라미터). Mobile만
30m로 낮추는 등의 예외를 두지 않았다(§24 지시).

## 43. 검증 결과

**Lint**: 4개 파일 + `slopeSection.js` 전부 `vue-cli-service lint --no-fix` 통과(에러 0건).

**Production build**: `vue-cli-service build`(임시 dest) 성공. 경고 2건은 폰트 파일 크기/vendor
CSS 크기 관련 기존 경고로, 이번 변경과 무관하다.

**Node 레벨 로직 스모크 테스트** (실제 Backend API 대상, 브라우저 아님 — 아래 참고):

```text
Trail GeoJSON API: 1706 features 확인
마루(course_name="마루")    -> resolvedTrailId=10, base 1267 features, 20m sections=251
무악동(course_name="무악동") -> resolvedTrailId=11, base 216 features,  20m sections=49
홍제동(course_name="홍제동") -> resolvedTrailId=12, base 108 features,  20m sections=21
부암동(course_name="부암동") -> resolvedTrailId=13, base 115 features,  20m sections=30
```

모든 section의 geometry가 LineString/좌표 배열 형태로 정상 파싱됐고, `dataQualityFlag`는
`null`/`PARTIAL_SECTION`만 나타났으며, `getEstimatedSlopeColor`가 4개 색상 중 하나를 항상
반환함을 확인했다. `resolveTrailId(undefined)`/`resolveTrailId('')`도 예외 없이 `undefined`를
반환한다.

**실제 browser visual inspection은 이번 Phase에서도 수행하지 않았다** — 이 환경에는 실제
브라우저가 없다. 위 Node 스모크 테스트는 실제 화면 렌더링(카카오맵 Polyline 표시, 겹침 여부,
z-order, 실제 클릭/코스 전환 UX)을 검증하지 않는다 — Node-level 테스트를 browser test라고
부르지 않는다(원본 요청 §48 지시대로 명확히 구분).

**Backend regression**: Backend 코드는 이번 Phase에서 전혀 수정하지 않았으므로 재실행 결과도
동일했다 — **125/125 PASS**, BUILD SUCCESS.

**DB 보호**: `trail=4, trail_feature=1706, trail_node=2526, trail_segment=2122,
accident_point=42` 불변 확인.

## 44. 제거된 Frontend 코드

4개 파일에서 실제로 삭제한 함수(실사용처가 slope뿐이었음을 각 파일별로 확인 후 삭제):

```text
groupCoordinates()          -- 4개 파일 전부에서 삭제 (다른 용도로 재사용되지 않음을 확인)
calculateSlope()            -- 4개 파일 전부에서 삭제
getColorBySlope()           -- 4개 파일 전부에서 삭제
calculateHaversineDistance() -- 4개 파일 전부에서 삭제 (calculateSlope 전용이었음)
```

**유지한 것**: `deg2rad()`는 `MountainDetailView.vue`/`MountainDetailView2.vue`/
`MobileMountainDetailView.vue`에서 `drawElevationChart()`의 거리 계산(`calculateCumulativeDistances`
→`calculateDistance`)에도 쓰이고 있어 그대로 유지했다(`CompareCourseView.vue`는 elevation
chart가 없어 `deg2rad`까지 완전히 제거). `processGeoJSON`의 PMNTN_NM 필터/좌표 flatten/DN
추출 로직, `drawElevationChart`, `routeCoordinates` 기반 진행률 마커 기능은 모두 그대로
유지했다 — slope 색칠 외의 책임을 침범하지 않았다(§4/§26).

Backend `LegacySlopeService`/`LegacySlopeCalculator`는 이번에도 삭제하지 않았다(§30 지시).
`frontend/public/data/인왕산ele copy.geojson` 파일 자체도 삭제하지 않았다 — 4개 화면이 더 이상
직접 fetch하지 않을 뿐, Trail Import source/Legacy regression baseline 역할은 유지한다.
Production에서 여전히 이 파일을 사용하는 4개 화면(`RecordStatisticsView.vue`,
`MountainDetailView3.vue`, `WebCoursePreview.vue`, `HeatmapView copy.vue`)은 Phase 9A에서
이미 "실사용 라우팅 없음"으로 확인된 파일들로, 이번 Phase 범위 밖이라 그대로 뒀다.

## 45. Coverage 표현 정확성 재확인

이 문서(§9, §11, §20-23)는 이미 "vs Network"/"vs ElevationProfile"을 항상 구분해서 표기하고
있었다 — Phase 12D 진행 중 다시 검토했고 "20m coverage = 90%"처럼 분모를 명시하지 않은 단독
표현은 없음을 확인했다. 마루의 두 수치(66.5% vs network, 86.6% vs elevation coverage) 역시
§20/§32/§39에서 일관되게 병기하고 있다.

## 46. Frontend 전환 결과 최종 판단

```text
1. 4 slope 화면 static Trail fetch 제거          : O (§44, 함수 삭제 + fetch 소스 교체 확인)
2. Trail GeoJSON API + SlopeSection API 사용     : O
3. windowMeters=20 고정                          : O (SLOPE_WINDOW_METERS 단일 상수)
4. Base Trail Layer + Slope Overlay Layer 구조   : O (§37)
5. 미커버 구간에서도 Base Trail 유지, 0% 조작 없음 : O (§39)
6. groupCoordinates/calculateSlope 제거          : O (§44)
7. Desktop/Mobile 동일 20m 분석 단위             : O (§42)
8. Backend 변경 없음, 전체 tests PASS            : O (125/125)
9. DB 5개 count 불변                             : O
```

Frontend production slope 계산 책임은 이번 Phase로 Backend `SlopeSectionService`로 완전히
이전됐다 — 남은 것은 API 호출/코스 선택/색상 매핑/지도 시각화뿐이다(원본 요청의 "최종 책임
분리" 정의와 일치).
