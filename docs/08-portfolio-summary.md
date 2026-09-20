# PostGIS 기반 안전등산 공간 데이터 모델링 및 분석

**역할** 팀장 / Backend · 공간 데이터 처리
**기술** Java 17 · Spring Boot · MyBatis · PostgreSQL/PostGIS · Docker · Vue.js · Kakao Maps · QGIS

> 산림청 등산로 SHP와 DEM 기반 공간 데이터를 PostGIS의 Raw·Network·Analysis 3계층으로
> 모델링하고, 20m 경사 분석과 사고지점 근접 공간질의를 실제 지도 시각화·위험 알림 기능까지
> 연결했다.

이 문서는 면접/서류 검토용 기술 요약이다. 세부 조사·실험 로그는 `docs/01`(아키텍처),
`docs/02`(데이터 정합성), `docs/05`(사고 공간질의), `docs/09`(경사 분석)에 있고, 이 문서는
그 결과 중 최종 구현과 판단만 추린다. 모든 수치는 위 문서에서 실측/재현된 값이며, 이 문서를
위해 새로 만든 값은 없다.

*(참고: 이 프로젝트가 속한 원 팀 프로젝트는 2024 LX 공간정보 아카데미 대상·국토교통부
장관상을 받았다 — 이 문서가 다루는 PostGIS 리팩터링 자체가 그 수상의 근거는 아니며, 수상 이후
개인적으로 진행한 후속 작업이다.)*

---

## 1. 프로젝트 개요 및 요구사항

안전등산 서비스는 등산로의 위치만 보여주는 것을 넘어, **구간별 경사 특성**과 **사고지점
정보**를 함께 제공해야 했다. 두 요구사항 모두 "선 하나짜리 원본 지리 데이터"만으로는 표현하기
어려웠다.

- **경사 분석**: 등산로 전체의 평균 경사가 아니라 구간 단위로 경사를 나눠 보여줘야 한다.
  이를 위해서는 등산로의 연결 관계(어디서부터 어디까지가 이어진 구간인지), 일정한 거리
  단위의 분석 구간, 그 구간의 고도 대표값이 필요했다.
- **사고지점 위험 알림**: 등산로와 "가까운" 사고지점을 판단해야 한다. 이 "가까움"은 저장
  시점에 고정되는 관계가 아니라, 조회 시점의 거리 조건에 따라 달라지는 공간적 관계로
  다뤄야 했다.

원본 데이터는 원본 그대로도 가치가 있다(추적성, 재현성). 그래서 원본을 보존하는 단위와,
서비스가 실제로 소비하는 분석 단위를 같은 테이블에 두지 않기로 했다 — 이 판단이 이 프로젝트
전체를 관통한다.

## 2. 공간 데이터 모델링

이 프로젝트의 중심 판단 하나만 꼽으면 다음과 같다.

> **원본 공간 데이터를 보존하는 단위, Network를 표현하는 단위, 서비스가 분석하는 단위는
> 의미와 생명주기가 서로 다르다고 판단해 계층을 분리했다.**

이 판단은 구체적으로 네 개의 설계 결정으로 이어졌다.

| # | 판단 | 근거 (요약, 상세는 4번) |
|---|---|---|
| 1 | 원본 Feature와 Network Edge를 분리 | GeoJSON 1개 Feature가 실제로는 여러 선형 조각(최대 10개)을 담고 있어, Feature=Edge 가정이 데이터로 깨졌다 |
| 2 | Network Edge와 경사 분석 단위를 분리 | Network Edge(TrailSegment)는 중앙값 길이 ~3m로 너무 짧아 경사 계산의 baseline으로 부적합했다 |
| 3 | 경사 분석 단위를 요청 시점이 아니라 구축 시점에 계산 | 같은 Network·같은 고도값·같은 20m 규칙이면 항상 같은 결과가 나오는 결정적 데이터라고 판단했다 |
| 4 | 사고지점을 FK가 아니라 공간질의로 등산로와 연결 | 최근접 후보가 사실상 임의로 갈릴 만큼 Network가 촘촘해서, 하나를 영구 고정하면 데이터를 왜곡한다 |

## 3. 전체 아키텍처

```text
Source Spatial Data (산림청 등산로 SHP + DEM, QGIS 가공 / 사고 GeoJSON)
        │
        ▼
┌────────────────────────────┐
│ Raw Spatial Layer           │
│                             │
│ Trail / TrailFeature        │
│ AccidentPoint               │
└─────────────┬───────────────┘
              │ ST_Dump + exact endpoint matching
              ▼
┌────────────────────────────┐
│ Network Layer                │
│                              │
│ TrailNode                    │
│ TrailSegment                 │
└───────────┬───────────┬──────┘
            │           │
            │           │ ST_DWithin / ST_Distance
            │           ▼
            │       AccidentPoint
            │           │
            ▼           │
┌────────────────────┐  │
│ Derived Analysis    │  │
│ Layer               │  │
│                      │  │
│ SlopeSection (20m)   │  │
└──────────┬───────────┘  │
           │              │
           ▼              ▼
     Slope API      Accident Candidate API
           │              │
           ▼              ▼
       Map Overlay   User Proximity Alert (Frontend)
```

## 4. 핵심 설계 및 구현

### 4-1. Raw Spatial Layer

`Trail`/`TrailFeature`는 QGIS로 가공한 원본 등산로 GeoJSON을 검증 후 1:1로 보존한다.
`TrailFeature`는 원본 `MultiLineString` geometry, 원본 Feature 순서(`source_feature_index`),
DEM-derived 대표 고도값(`DN`, 아래 4-3 참고)을 그대로 담는다 — 이후 어떤 계층을 재구성하더라도
이 계층으로 항상 돌아올 수 있어야 한다는 원칙에 따른 것이다. 원본 1,719개 Feature 중 좌표가
1개뿐인 degenerate geometry 9건과 코스 라벨이 빈 4건, 총 13건은 Manifest로 명시 분류해
제외했다 — 조용히 버리지 않고 근거를 남겼다.

`AccidentPoint`도 같은 Raw 계층에 속한다. 원본 사고 GeoJSON 42건을 필터링·그룹핑 없이
1:1로 저장하고, 원본 속성 전체는 `raw_properties`(JSONB)에 보존한다.

### 4-2. Network Layer

원본 GeoJSON Feature 하나가 하나의 이어진 구간이라는 가정은 데이터로 확인해보니 성립하지
않았다 — 1,706개 `TrailFeature`(MultiLineString)를 `ST_Dump`로 분해하면 2,122개의
LineString Part가 나온다. 즉 **Feature ≠ Network Edge**다. 그래서 원본을 그대로 보존하는
Raw 계층과, 그 geometry를 분해·연결해 만든 Network 계층을 별도 테이블로 분리했다.

- `TrailNode` = LineString Part의 시작/끝 좌표가 **정확히 일치**하는 지점을 하나로 묶은
  Network 정점.
- `TrailSegment` = 두 `TrailNode` 사이를 잇는 Network Edge(LineString Part 그대로).

Part-Segment geometry는 2,122/2,122 전수 일치, 전체 길이 차이 0.000000m로 검증했다.

TrailSegment의 역할은 세 가지로 한정했다: 공간 연결관계 표현, 사고지점 공간질의의 기준
geometry, SlopeSection 생성의 기반 Network — **경사값을 저장하는 테이블이 아니다**(이유는
4-3).

현재 Network는 endpoint 좌표가 정확히 일치하는 경우만 같은 Node로 묶는 **exact endpoint
matching 기준의 baseline**이다. 미세한 좌표 오차를 보정하는 snapping이나 선 내부 교차점
자동 분할(interior noding)은 적용하지 않았다 — 이 한계는 8번에서 다시 정리한다.

### 4-3. Slope Analysis Layer (SlopeSection)

처음에는 `TrailSegment`에 경사값을 바로 저장하는 방식을 검토했지만 두 가지 문제로 기각했다.

1. `TrailSegment` 길이의 중앙값이 약 3m이고 18% 이상이 1m 미만이다 — 이렌 짧은 거리를
   경사 계산의 분모로 쓰면 작은 고도 오차도 극단적인 경사율로 증폭된다.
2. 1,706개 Feature 중 256개가 2개 이상의 Segment로 쪼개져 있어, 형제 Segment가 같은 부모
   Feature의 대표 고도값을 그대로 공유한다 — Segment 단위로 다루면 이 공유값을 "그 Segment의
   실측 평탄함"으로 오인하게 된다.

그래서 Network 저장 단위(TrailSegment)와 경사 분석 단위를 분리했다. TrailSegment/TrailNode를
분기 없는 Chain으로 재구성(`NetworkChainBuilder`)하고, 각 TrailFeature의 대표 고도값을 Chain의
누적 거리 축에 배치해 선형보간(`ElevationProfile`)한 뒤, 그 위를 일정한 실거리 단위로 잘라
`SlopeSection`을 만든다.

```text
estimatedSlopePercent = Δ estimated elevation / horizontal distance × 100
```

이름 그대로 **추정값**이다 — 원본 GeoJSON의 `DN` 자체가 실측 검증되지 않은 DEM-derived 대표
고도값이라, 이를 기반으로 계산한 경사도 measured가 아니라 estimated로 표현한다.

분석 거리는 10m/20m/30m를 실측 비교해 **20m**를 선택했다. 10m는 짧은 baseline 때문에
`|slope|>100%`인 극단값이 나타났고, 30m는 공간 해상도 손실이 더 컸다 — 극단값 제거·coverage·
해상도의 균형점이 20m였다.

**Geometry 정확도 개선**: 초기에는 긴 Chain을 비율로 한 번에 자르면서 분석 거리와 실제 지도
geometry 길이 사이에 오차가 있었다(relative error median 2.08%). Section 경계가 위치한
개별 TrailSegment 내부에서 local fraction을 계산해 Segment 단위로 자르도록 바꿔 median
0.18%, p95 1.58%로 줄였다 — 경사 계산 결과는 그대로 두고 지도에 표시되는 geometry 정확도만
개선한 것이다.

**Precompute/Persistence**: 같은 Network, 같은 고도 데이터, 같은 20m 규칙이면 항상 같은
`SlopeSection`이 나온다 — 요청마다 달라지는 데이터가 아니라 **결정적으로 재생성 가능한
Derived 데이터**라고 판단해, Network 구축 이후 한 번 계산해 `slope_section` 테이블에
저장하고 API는 조회만 하도록 구조를 바꿨다. 이는 "API가 느려서 캐시했다"가 아니라, 반복 요청마다
같은 공간 분석을 다시 수행할 이유가 없다고 판단해 계산 시점을 Request Time에서 Build Time으로
옮긴 것이다. 전환 전/후 응답을 필드·geometry 단위로 전수 비교해 byte-identical함을 확인한
뒤 교체했다.

`slope_section` 핵심 컬럼:

```text
slope_section
  trail_id, chain_sequence, section_sequence, window_m
  distance_m
  estimated_elevation_start, estimated_elevation_end, estimated_slope_percent
  data_quality_flag   -- 구간이 20m 미만으로 잘렸을 때만 표시
  geom                -- LineString, 4326
```

### 4-4. AccidentPoint 공간관계

사고지점을 특정 `TrailSegment`의 FK로 저장하는 방법도 검토했지만, 42건 전체에 대해 최근접
Segment와 두 번째로 가까운 Segment의 거리를 비교한 결과 100%가 5m 미만이었다(Segment 중앙값
길이가 ~3m로 매우 촘촘하기 때문). 이런 상황에서 "가장 가까운 것 하나"를 영구 고정하면 사실상
임의의 선택을 데이터로 굳히는 셈이다. 또한 `TrailSegment`는 Network 재구축 시 id가 바뀔 수
있는 Derived 데이터라 영구 식별자로 쓰기에도 맞지 않는다. 그래서 FK 대신 조회 시점에
`ST_DWithin`/`ST_Distance`로 관계를 계산한다 — 미터 단위 정확도를 위해 `geometry`가 아니라
`::geography` 캐스팅을 사용한다.

## 5. 서비스 적용

### 5-1. 20m 경사 분석 및 지도 시각화

```text
Slope Build (배치, Network 구축 이후 1회)
  TrailSegment Network → NetworkChain → ElevationProfile → 20m SlopeSection → PostGIS 저장

Service Runtime
  SlopeSection SELECT → GeoJSON FeatureCollection → GET /api/spatial/trails/{trailId}/slope-sections?windowMeters=20 → 지도 Overlay
```

`trailId`는 PostgreSQL이 채번하는 surrogate PK라 Frontend에 고정값으로 넣지 않는다 — Trail
GeoJSON API 응답에 포함된 `trailId` 속성을 코스명 기준으로 조회 시점에 resolve한다.

경사를 계산할 수 없는 구간(고도 sample coverage 한계)이 있는데, 이 구간의 경로 자체가
지도에서 사라지지 않도록 **전체 등산로를 그리는 Base Layer**와 **경사값이 있는 구간만 색을
입히는 Slope Overlay**를 분리했다. 미커버 구간에 임의로 slope=0을 채우지 않는다 — 데이터
품질의 한계를 지도에서 숨기지 않기로 한 판단이다.

### 5-2. 사고지점 공간질의 및 위험 알림

```text
PostGIS
  등록된 Trail Network 주변 AccidentPoint 후보 선별 (ST_DWithin/ST_Distance)

Frontend (RecordView)
  후보 AccidentPoint 목록
        ↓
  사용자 현재 위치(2초 polling)
        ↓
  Kakao Maps 거리 계산
        ↓
  30m 이내 → 음성 위험 알림
```

기본 공간질의는 AccidentPoint↔TrailSegment 양방향 API(사고 1건→주변 Segment 전체, Segment
1건→주변 사고 전체)로 구현했다. 실제 서비스 화면(등산 중 실시간 알림)에는 "현재 Trail 전체
주변의 사고지점 후보"가 필요해, TrailSegment를 하나씩 순회하는 대신 Trail 단위로 한 번에
조회하는 API(`GET /api/spatial/trails/{trailId}/nearby-accidents`)를 추가했다. 하나의
AccidentPoint가 같은 Trail의 여러 Segment 근처에 있을 수 있어(실측: 사고 1건이 24개의 서로
다른 Segment와 30m 이내로 매칭) `GROUP BY accidentId` + `MIN(ST_Distance)`로 중복을
제거한다 — API 응답은 항상 AccidentPoint 1건당 Feature 1개다.

**중요한 경계**: `RecordView`는 현재 등산 중인 코스를 식별하는 UI/state가 없다(진입 경로를
전부 코드로 추적해 확인했다 — 전부 파라미터 없는 하드코딩된 이동이었다). 그래서 이 화면은
"등록된 Trail Network 전체"를 대상으로 후보를 조회한다. **"선택한 Trail 주변 사고지점"이
아니다** — 이 지점을 정확히 구분해서 설명한다. 사용자 현재 위치와 후보 사고지점의 실시간
거리 비교도 PostGIS가 아니라 Frontend(Kakao Maps 거리 계산)가 담당한다: PostGIS는 "어떤
사고지점이 Trail과 공간적으로 관련 있는가"까지만 답하고, "지금 사용자가 그 지점에 실제로
접근했는가"는 Frontend의 책임으로 남겼다.

## 6. 검증 및 결과

| 검증 항목 | 결과 |
|---|---:|
| Trail | 4 |
| TrailFeature (원본 1,719 − 제외 13) | 1,706 |
| TrailNode | 2,526 |
| TrailSegment | 2,122 |
| AccidentPoint | 42 |
| SlopeSection (20m) | 351 |
| Raw Part ↔ TrailSegment geometry parity | 2,122 / 2,122, 길이차 0.000000m |
| SlopeSection geometry 상대오차 (median / p95) | 0.18% / 1.58% |
| SlopeSection API 응답시간 (compute-on-request → precompute, median) | 48.11ms → 9.51ms |
| SlopeSection API 응답시간 (p95) | 54.23ms → 10.98ms |
| SlopeSection precompute 전/후 응답 payload | byte-identical |
| Trail별 30m Accident 후보 (실측) | 마루 0 / 무악동구간 2 / 홍제동구간 0 / 부암동구간 0 |
| Accident 후보 dedup (동일 accidentId, 24개 Segment 매칭 사례) | 24행 → 1행 |
| Backend 자동 테스트 | 146 / 146 PASS |
| Frontend production build / lint | PASS |

Fresh Rebuild(별도 PostgreSQL 인스턴스에서 전체 파이프라인 재구성)로 위 Dataset이 운영
DB와 동일하게 재현됨을 확인했고, 이 과정에서 운영 DB는 전혀 건드리지 않았다. `EXPLAIN
ANALYZE`로 SlopeSection Production 조회가 Index Scan(Seq Scan 아님)을 사용함도 확인했다.

## 7. 설계 판단 요약

| 질문 | 답 |
|---|---|
| 왜 원본 Feature와 Network Edge를 분리했는가 | Feature 1개가 여러 선형 조각을 담을 수 있음을 데이터로 확인했다(4-2) |
| 왜 TrailSegment에 경사값을 바로 저장하지 않았는가 | Segment가 너무 짧아(중앙값 ~3m) 극단값이 발생하고, 형제 Segment가 대표 고도값을 공유한다(4-3) |
| 왜 SlopeSection을 precompute했는가 | 같은 입력이면 항상 같은 결과가 나오는 결정적 데이터라고 판단했다(4-3) |
| 왜 AccidentPoint를 TrailSegment FK로 연결하지 않았는가 | 최근접 후보가 사실상 임의로 갈릴 만큼 Network가 촘촘하고, TrailSegment id 자체가 영구적이지 않다(4-4) |

## 8. 한계 및 후속 개선

```text
- Network는 exact endpoint matching 기준 baseline이다 — snapping, 선 내부 교차점(interior
  intersection) 자동 분할, pgRouting 기반 topology는 구현하지 않았다.
- DN(대표 고도값)은 Feature 단위 DEM-derived 값이며, TrailNode 지점의 실측 GPS 고도가
  아니다. estimatedSlopePercent도 measured가 아닌 estimated 값이다.
- SlopeSection(20m)은 Network 전체를 100% 덮지 않는다(실측 coverage 73.0%, 마루는
  66.5%로 더 낮다) — 미커버 구간은 Base Layer만 표시한다.
- Accident 30m는 서비스가 실제로 쓰는 alert 값을 그대로 재사용한 것이며, 객관적인 위험
  반경으로 검증된 값이 아니다(실측 결과 42건 중 76%가 Trail Network에서 100m 이상
  떨어져 있다).
- 사용자 현재 위치는 PostGIS 공간 객체로 모델링되어 있지 않다 — 사용자↔사고지점 실시간
  거리 비교는 전부 Frontend(Kakao Maps)에서 계산한다.
- 사용자 등산 기록(GPS 경로)은 관계형 point row로만 저장되며, 이번 리팩터링에서
  LineString/geometry로 공간모델링하지 않았다.
- RecordView는 현재 등산 중인 코스를 식별하는 UX가 없어 Trail 단위 필터링 없이 등록된
  전체 Trail Network를 대상으로 사고 후보를 조회한다 — 코스 선택 UX 자체를 새로 설계해야
  하는 별도 범위의 문제로 보고 이번에는 구현하지 않았다.
```
