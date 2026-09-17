# 03. Trail Network Modeling (Phase 6)

이 문서는 Phase 6에서 실제로 분석·설계·구현·검증한 내용만 기록한다. 측정하지 않은 값은 적지 않는다.
Phase 7(경사 계산), Phase 8(Risk Point), Phase 9(Frontend 전환), 경로탐색(Dijkstra/A*/pgRouting)은
이번 Phase에서 다루지 않았다.

## 0. 계층 재정의

Phase 5까지 완료한 뒤 재검토한 결과, Source of Truth 표현을 다음과 같이 세분화한다.

```text
Original Source
= 원본 GeoJSON(frontend/public/data/인왕산ele copy.geojson) + Import Manifest
  (backend/src/main/resources/spatial-import/trail-import-manifest.json)

Validated Raw Spatial Layer
= Trail + TrailFeature
  (Import 기준을 통과한 원본 Feature의 검증된 공간 저장 계층 -- 원본 전체의 완전한 사본이 아니다.
   원본 1719건 중 13건이 Import 기준에 따라 포함되지 않았다: PMNTN_NM 라벨 누락 4건, geometry
   구조 무효 9건. 두 경우 모두 Import Manifest에 근거와 함께 명시적으로 기록되어 있다.)

Derived Network Layer
= TrailNode + TrailSegment
  (TrailFeature.geom으로부터 파생되며, 언제든 재생성 가능하고, Raw 데이터를 대체하지 않는다.)
```

**[코드 확인]** Phase 5에서 제외한 4건(source_feature_index 61, 167, 189, 1202)은 `PMNTN_NM=' '`이지만
`MNTN_NM='북한산_백운대'`이고 마루 Feature 사이에서 DN이 앞뒤와 연속적이다(예: 61번은 DN=177로
60번(176)과 62번(178) 사이). 이 문서에서는 이 4건을 "noise"라고 부르지 않고 **"코스 라벨이 누락되어
보수적으로 제외한 Source Feature"**로 표현한다. 코드의 카테고리명(`EXCLUDED_SOURCE_FEATURE`)은
기존 Import 결과와의 일관성을 위해 바꾸지 않았다 — Manifest의 `reason` 필드에 이미 이 맥락이 기록되어
있다(Phase 5 완료, 변경 없음). 이 4건이 실제로 Network gap의 원인인지는 10번에서 확인한다(결론: 아니다).

## 1. Feature와 Segment를 분리한 이유 — Phase 5 "분기 후보"가 실제로는 분기가 아니었음

**[데이터 확인, Phase 5 결론 정정]** Phase 5에서는 `TrailFeature`(MultiLineString 전체) 단위로
`ST_Intersects` 자기 대조를 수행해, 무악동구간에서 source-order상 비인접한데도 교차하는 pair 3건
(sequence 5↔7, 64↔66, 65↔67)을 찾았고 "분기/합류 후보"로 보고했다.

Phase 6에서 **LineString Part(= `ST_Dump(geom)`으로 분해한 개별 조각) 단위로 다시 확인한 결과, 이 판단은
틀렸다.** 실제로는 sequence=5인 Feature가 3개 Part, sequence=6인 Feature가 2개 Part로 구성되어 있고,
두 Feature의 Part들이 `B → C → C′ → D → D′ → E` 순서로 정확히 맞물려 교차로 연결되는 하나의 연속된
경로였다(끝점 좌표가 소수점까지 정확히 일치). 즉 "Feature 5와 Feature 7이 직접 만나는 지점"처럼 보였던
것은 **Feature 5의 여러 Part 중 하나가 Feature 6과 정확히 맞닿아 있어서 생긴 착시**였고, 실제로 3개
이상의 서로 다른 Part가 한 점에서 만나는 진짜 분기(branch)는 이 데이터셋 어디에도 없었다(6번 참고,
exact-match degree 분포에 degree≥3이 0건).

이 발견 자체가 이번 Phase의 핵심 설계 원칙을 실증한다: **원본 GeoJSON Feature 경계는 실제 이동
네트워크의 구조와 다르다.** Feature 안에 여러 개의, 서로 연결되지 않은 Part가 섞여 있을 수 있고(4번
참고), 그 Part들이 다른 Feature의 Part와 교차 연결될 수 있다 — 이런 구조는 Feature를 통째로 하나의
Edge 후보로 다뤄서는 절대 드러나지 않는다.

## 2. Network Modeling 기본 흐름

```text
TrailFeature.geom (MultiLineString)
        ↓ ST_Dump
LineString Part (임시 산출물, 영속 테이블 없음)
        ↓ ST_StartPoint / ST_EndPoint, exact 좌표 클러스터링
TrailNode
        ↓ Part 끝점을 Node id로 치환
TrailSegment
```

`TrailFeature.sequence`(source-order)는 이 과정 어디에서도 사용하지 않았다 — Network의 연결 관계는
오직 geometry의 실제 좌표로만 결정했다.

## 3. MultiLineString 분해 결과

**[실행 결과]** `ST_Dump(trail_feature.geom)`으로 1706개 TrailFeature를 전부 분해했다.

| Part 수 | Feature 수 |
|---|---|
| 1 | 1450 |
| 2 | 190 |
| 3 | 32 |
| 4 | 11 |
| 5 | 8 |
| 6 | 5 |
| 7 | 4 |
| 8 | 2 |
| 9 | 2 |
| 10 | 2 |
| **합계 Part 수** | **2122** |

**[데이터 확인, 예상 밖 발견]** 256개(1706−1450)의 다중 Part Feature에 대해 `ST_LineMerge(geom)`을
직접 적용해 보았다 — **256건 전부** 병합 후에도 원래 Part 수 그대로 `MULTILINESTRING`으로 남았다
(하나도 `LINESTRING`으로 합쳐지지 않음). 즉 **다중 Part Feature 내부의 Part들은 서로 연결되어 있지
않다** — 하나의 Feature가 "긴 경로 하나가 여러 조각으로 저장된 것"이 아니라 "우연히 같은 DN
분류값을 가진, 공간적으로 무관한 짧은 조각들의 묶음"에 가깝다는 뜻이다. 이는 Network를 Part 단위로
분해해야 한다는 4번 지시사항의 필요성을 실제 데이터로 다시 한번 확인해 준다.

## 4. Exact Endpoint 연결 결과 (tolerance = 0)

**[실행 결과]** 2122개 Part의 시작점/끝점(4244개 endpoint mention)을 정확한 좌표 일치로 클러스터링했다.

| 지표 | 값 |
|---|---|
| 총 Part(Edge) 수 | 2122 |
| 총 Node 수(distinct 정확 좌표) | 2526 |
| degree=1 (loose end) | 808 |
| degree=2 (단순 통과점) | 1718 |
| **degree≥3 (분기점)** | **0** |
| zero-length Part | 0 |

**결론: 이 데이터셋은 exact-match 기준으로 분기(junction)가 전혀 없는, 단순 경로/사슬(chain)들의
집합이다.** 1번 섹션에서 정정한 "무악동구간 분기 후보"도 이 표에 반영되어 있다 — 실제로는 degree 2
지점들의 연쇄일 뿐이다.

## 5. Near Endpoint 거리 분포와 5-2. Tolerance 비교 실험

**[중요] Phase 5의 "source-order 인접 Feature 간 거리" 통계(0~1m 1680건 등)를 그대로 tolerance
근거로 쓰지 않았다.** 대신 이번 Phase에서 **degree=1(loose end) Node 808개** 각각에 대해 같은 Trail
내 **다른 모든 loose end와의 실제 최근접 거리**를 `ST_Distance(..::geography, ..::geography)`로
다시 계산했다.

### 5-1. 최근접 loose-end 거리 분포 (Trail별)

| Trail | loose end 수 | 0~0.5m | 0.5~1m | 1~2m | 2~5m | 5m 초과 | 최소 | 최대 |
|---|---|---|---|---|---|---|---|---|
| 마루 | 782 | 523 | 141 | 15 | 39 | 64 | 0.002m | 48.596m |
| 무악동구간 | 2 | 0 | 0 | 0 | 0 | 2 | 95.68m | 95.68m(트레일 양끝 서로의 거리일 뿐, 후보 아님) |
| 홍제동구간 | 10 | 2 | 0 | 2 | 2 | 4 | 0.126m | 241.13m |
| 부암동구간 | 14 | 0 | 4 | 6 | 2 | 2 | 0.620m | 110.93m |

전체 808건의 세분화 히스토그램: 0~0.1m 102 / 0.1~0.5m 423 / 0.5~1m 145 / 1~2m 23 / 2~5m 43 /
5~20m 57 / 20m+ 15.

### 5-2. Threshold별 병합 시뮬레이션 (실제로 merge하지 않고 시뮬레이션만 수행)

exact-match 그래프(405개 connected component)에 각 threshold 이하 거리의 loose-end 쌍을 추가
엣지로 넣었을 때 component가 얼마나 줄어드는지, 그리고 하나의 loose end가 threshold 안에 후보를
몇 개 갖는지(모호성)를 계산했다.

| threshold | 추가되는 merge 후보 쌍 | 결과 component 수 | exact 대비 병합된 component 수 | 후보가 2개 이상인 모호한 endpoint 수 |
|---|---|---|---|---|
| exact (0) | 0 | 405 | 0 | 0 |
| ≤0.5m | 293 | 173 | 232 | 61 |
| ≤1m | 592 | 85 | 320 | 286 |
| ≤2m | 869 | 70 | 335 | 360 |
| ≤5m | 1382 | 48 | 357 | 498 |

**패턴**: exact→0.5m 구간이 병합 효과(232개 component 감소) 대비 모호성(61건, 7.5%)이 가장 낮다.
0.5m→1m로 넓히면 병합 효과는 88개 추가에 그치지만 모호성은 286건(전체 loose end의 35%)으로
급증한다. 이후 2m, 5m로 넓혀도 병합 효과는 계속 줄고(15개, 22개) 모호성만 계속 늘어난다(360, 498).

### 5-3. 최종 판단: 이번 Phase에서는 tolerance를 적용하지 않는다

**[설계 제안 → 확정]** "가장 많은 연결을 만든다"는 이유로 큰 tolerance를 선택하지 말라는 원칙에 따라,
그리고 exact-match 자체가 이미 구조적으로 완전히 유효한 Network(6~9번 검증 전항목 통과)를 만들어낸다는
점을 근거로, **이번 Phase에서 실제로 영속화하는 Network는 exact-match(tolerance=0)만 사용한다.**
0.5m 이하 조합이 상대적으로 가장 "설명 가능한" 후보로 보이지만, 그마저도 실제로 merge를 적용하면
좌표를 옮기는(snapping) 효과가 생기고 61건의 모호성(어느 후보와 합칠지 유일하게 결정되지 않음)을
안고 가야 한다. 이번 Phase의 완료 조건("tolerance를 실제 데이터로 결정하거나, 필요 없으면 적용하지
않음")에 따라 **"결정한 tolerance = 0(적용하지 않음)"으로 확정**하고, 위 표는 Phase 8 이후 실제
snapping이 필요해질 경우를 위한 참고 자료로만 남긴다.

## 6. Mid-line Intersection 재검증 — 실제로 0건

**[실행 결과]** 같은 Trail 내에서 `geom && geom`(GiST bbox 사전 필터, `EXPLAIN`으로 인덱스 사용 확인)
+ `ST_Intersects`로 TrailFeature 전체 쌍을 조사했다(Phase 5와 동일 방법, 재확인 목적):

- 전체 교차 pair: 1681, 이 중 source-order 인접(`|seq_a-seq_b|=1`): 1678
- **비인접 pair 3건**(전부 무악동구간, sequence 5↔7, 64↔66, 65↔67) 각각의 교차점을 두 Feature의
  실제 endpoint들과 거리 비교한 결과, **전부 정확히 0m로 어느 한쪽의 endpoint와 일치**했다.

**결론: 원본 Feature의 endpoint가 아닌 위치에서 발생하는 진짜 mid-line intersection은 이번
데이터에서 0건이다.** 3번 섹션의 Part 분해 결과와 함께 보면, 이 3건은 "Feature 5의 중간 Part 경계가
Feature 6의 Part 경계와 정확히 맞닿아 있다"는 것이었을 뿐, 어느 Part의 내부(중간)를 가로지르는
교차는 없었다. 따라서 **`ST_Intersection`/`ST_Split`을 이번 데이터의 어떤 geometry에도 적용하지
않았다** — 실제로 필요한 경우가 없었기 때문이다. Topology 파이프라인 자체는 향후 mid-line
intersection이 발견되면 처리할 수 있도록 설계되어 있지만(2번 섹션의 흐름에 분할 단계를 끼워 넣을 수
있음), 지금 데이터에 없는 split을 억지로 만들지 않았다.

## 7. 20m 초과 gap(마루 4건)의 원인 규명

**[실행 결과]** Phase 5에서 찾은 마루의 20m 초과 gap 4건을 `source_feature_index` 기준으로 재조사했다.

| gap(m) | idx_a | idx_b | 사이 결번 index | 원인 |
|---|---|---|---|---|
| 98.78 | 1275 | 1277 | **1276** | **INVALID_GEOMETRY_SOURCE_FEATURE로 제외된 Feature**(1276, 좌표 1개짜리 degenerate). 제외 때문에 생긴 gap. |
| 33.59 | 1250 | 1251 | (없음, 연속 index) | **설명되지 않는 원본 자체의 단절.** 제외된 Feature와 무관. |
| 21.08 | 1144 | 1145 | (없음, 연속 index) | **설명되지 않는 원본 자체의 단절.** 제외된 Feature와 무관. |
| 21.04 | 997 | 1000 | **998, 999** | **INVALID_GEOMETRY_SOURCE_FEATURE로 제외된 Feature 2개**(998, 999). 제외 때문에 생긴 gap. |

**결론**: 4건 중 2건(98.78m, 21.04m)은 Import 단계에서 제외한 geometry-무효 Feature가 사이에
있었기 때문에 생긴 gap임을 확인했다. 나머지 2건(33.59m, 21.08m)은 제외와 무관한, **원본 데이터
자체에 존재하는 실제 단절**이다. 어느 쪽이든 이번 Phase에서 이 gap을 임의로 이어붙이지 않았다 —
확정할 수 없는 큰 gap은 **Network Component 경계로 그대로 남겼다**(391개 component 중 다수가 이런
경계에서 갈라짐).

PMNTN_NM 공백으로 제외한 4건(61, 167, 189, 1202)은 이 4개 gap 어디와도 인접하지 않아, **Network
단절의 원인이 아님**을 확인했다(0번 섹션에서 예고한 확인 완료).

## 8. Non-simple TrailFeature(자기 근접) 분석

**[데이터 확인]** Phase 5에서 발견한 `ST_IsSimple=false` 1건(`source_feature_index=1190`, 마루)을
재분석했다. 이 Feature는 Part가 1개뿐이다(3번 섹션 표 기준 다중 Part 대상이 아님) — 따라서 이 비단순성은
**Part 간 접촉이 아니라 하나의 LineString 내부의 자기 근접(self-touch)**이다. 좌표를 보면 경로
끝부분에서 `(126.9970429280802, 37.65605035388717)`와 `(126.9970429280812, 37.656050353763895)`처럼
소수점 9자리(약 0.001m) 차이의 두 점을 지나며 사실상 같은 위치로 되돌아온다 — 지형상 헤어핀/급커브
구간으로 해석했다.

이 지점은 **다른 어떤 Part와도 좌표를 공유하지 않는다**(6번 섹션의 Node 클러스터링에서 이 Feature는
degree 1(양 끝 loose end)인 독립된 Part로만 존재) — 즉 실제 Network 연결점(다른 경로와 만나는 지점)이
아니라, 이 Part 하나만의 형태적 특징이다. **이 근접이 "실제 intersection이 필요한 Node"인지 검토한
결과, 다른 Part와 연결되지 않으므로 Node 분할이 필요하지 않다고 판단했다.** `ST_IsSimple=false`라는
이유만으로 이 geometry를 수정하거나 분할하지 않았다.

## 9. Excluded Source Feature가 Network에 미친 영향 (요약)

| 카테고리 | 건수 | Network에 미친 영향 |
|---|---|---|
| 코스 라벨 누락 (구 noise, 61/167/189/1202) | 4 | **없음** — 7번에서 확인한 4개 gap 중 어느 것과도 인접하지 않음 |
| Geometry 구조 무효(degenerate) | 9 | 이 중 3개(1276, 998, 999)가 마루의 4개 gap 중 2개(98.78m, 21.04m)의 직접 원인. 나머지 6개(11, 371~373, 1192, 1522)는 gap 원인으로 특정되지 않음(별도 위치) |

이번 Phase에서 두 카테고리 모두 원본 값을 복구하거나 TrailFeature에 다시 추가하지 않았다.

## 10. TrailNode Schema

```sql
CREATE TABLE trail_node (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    geom geometry(Point, 4326) NOT NULL
);
CREATE INDEX idx_trail_node_geom ON trail_node USING GIST (geom);
```

**[설계 제안 → 확정] `trail_id`를 두지 않았다.** 근거:
- Node의 Trail 소속은 그 Node에 연결된 `trail_segment`(→`source_trail_feature_id`→`trail_feature.trail_id`)를
  통해 언제든 유도 가능하다.
- 실제 데이터로 확인한 결과 **서로 다른 Trail 간 정확히 같은 좌표를 공유하는 Node는 0건**이었고, 가장
  가까운 서로 다른 Trail 쌍(무악동구간↔홍제동구간, 둘 다 인왕산)조차 최근접 거리가 **271.8m**로
  스냅 후보권과 거리가 멀었다. 즉 지금은 `trail_id`가 있어도 틀리지 않지만, Node를 "하나의 Trail에만
  속하는 것"으로 스키마에 고정하는 것은 향후(예: 인왕산의 여러 코스가 실제로 만나는 지점이 데이터로
  확인되는 경우) 잘못된 모델이 될 수 있다. Node는 순수하게 공간적 사실(어떤 좌표에 몇 개의 Edge가
  모이는가)이지 Trail에 종속된 속성이 아니라고 판단했다.

**`node_type`(ENDPOINT/JUNCTION/...) 컬럼도 추가하지 않았다.** degree(연결된 Segment 수)는 항상
`trail_segment`를 집계해 구할 수 있고, 이번 Phase의 exact-match 결과에서는 degree≥3(분기)이 0건이라
분류 자체의 실익도 아직 확인되지 않았다. 저장해 두면 향후 rebuild 시 갱신을 잊어 실제 연결 상태와
어긋날 위험만 생긴다.

## 11. TrailSegment Schema

```sql
CREATE TABLE trail_segment (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    from_node_id            BIGINT NOT NULL REFERENCES trail_node (id),
    to_node_id              BIGINT NOT NULL REFERENCES trail_node (id),
    geom                    geometry(LineString, 4326) NOT NULL,
    source_trail_feature_id BIGINT NOT NULL REFERENCES trail_feature (id) ON DELETE CASCADE,
    source_part_index       INTEGER NOT NULL,
    CONSTRAINT chk_trail_segment_distinct_nodes CHECK (from_node_id <> to_node_id),
    CONSTRAINT uq_trail_segment_source_part UNIQUE (source_trail_feature_id, source_part_index)
);
CREATE INDEX idx_trail_segment_geom ON trail_segment USING GIST (geom);
CREATE INDEX idx_trail_segment_from_node ON trail_segment (from_node_id);
CREATE INDEX idx_trail_segment_to_node ON trail_segment (to_node_id);
```

**[설계 제안 → 확정, 이번 Phase부터 확정 의미]** 지금부터 `TrailSegment`는 **오직** "Node ↔ Node
사이의 실제 LineString Part 1개"만을 의미한다. GeoJSON Feature와 동일시하지 않는다.

- **`trail_id` 없음**: `source_trail_feature_id → trail_feature.trail_id`로 유도 가능. 10번의
  TrailNode와 같은 이유로 중복 저장하지 않았다.
- **`sequence` 없음**: `TrailFeature.sequence`(source-order)를 그대로 복사하지 않았다 — Network
  순서는 "어느 Node를 공유하는가"라는 그래프 구조로 표현되지, 선형 순번이 아니다.
- **`source_part_index`**: `ST_Dump`의 `path[1]`을 그대로 저장해 원본 Part로 역추적 가능하게 했다.
  `(source_trail_feature_id, source_part_index)`에 UNIQUE 제약을 걸어 재빌드 버그로 중복 Segment가
  생기는 것을 방지했다.
- **`CHECK (from_node_id <> to_node_id)`**: 0-length/자기 루프 Segment를 DB 레벨에서 차단한다.
  20번에서 이 제약이 실제로 위반 시도를 막는 것을 검증했다.

### 11-1. 방향성 (섹션 13)

**[데이터 확인]** 원본 GeoJSON property(`PMNTN_NM`, `DN`, `MNTN_NM` 등) 어디에도 일방통행/방향
제한을 나타내는 값이 없다. 따라서 `from_node`/`to_node`는 **LineString 자체의 시작/끝 방향을 보존하기
위한 기술적 표기일 뿐**이며, "from_node에서 to_node로만 이동 가능하다"는 의미를 부여하지 않았다.
현재 데이터는 기본적으로 양방향 이동 가능한 Edge로 해석하는 것이 적절하다고 판단했다. 별도
`direction`/`bidirectional` 컬럼은 실제 필요성이 확인되지 않아 추가하지 않았다.

### 11-2. 길이 (섹션 14)

**[실행 결과]** `ST_Length(geom)`을 4326 geometry에 그대로 적용하면 도(degree) 단위가 나와 미터로
해석할 수 없다(이전 Phase들에서 이미 확인한 원칙 재적용). 길이가 필요하면
`ST_Length(geom::geography)`를 사용해야 함을 실제로 재확인했다(22번 검증에서 사용).

**`distance_m` 컬럼은 추가하지 않았다.** 재사용 빈도(현재 소비자 없음), geometry 불변성(이번 Phase는
snapping을 적용하지 않아 재빌드해도 geometry가 바뀌지 않음), 정합성(캐시가 있으면 재계산 누락 시
어긋날 위험) 세 기준 모두 "필요 시 조회 시점에 `ST_Length(geom::geography)`로 계산"하는 쪽이 유리했다.
성능 문제가 확인되지 않은 지금은 파생값 저장을 최소화하는 방향을 우선했다.

## 12. Raw → Network 추적성

모든 TrailSegment는 `source_trail_feature_id`(→ `trail_feature.id`)와 `source_part_index`로 원본
GeoJSON Feature까지 역추적 가능하다: `trail_segment → trail_feature.source_feature_index → 원본
FeatureCollection.features[index]`. Raw 데이터(`trail`/`trail_feature`)는 이번 Phase에서 한 번도
수정하지 않았다(20번에서 실제로 재확인).

## 13. Network 생성 방식 — 역할 분담

**[코드 확인]**
- **PostGIS**: `ST_Dump`(분해), `ST_StartPoint`/`ST_EndPoint`(끝점 추출), 정확 좌표 `GROUP BY`(Node
  클러스터링), `ST_Intersects`/`ST_Distance`/`ST_DWithin`(6·7·5번 분석), `ST_Length(::geography)`
  (22번 검증) — 실제로 사용한 것만 나열했다. 이번 Phase에 필요 없었던 `ST_Split`,
  `ST_LineLocatePoint`, `ST_LineSubstring`, `ST_Snap`은 코드에 넣지 않았다(6번 결론: mid-line
  intersection이 없어 분할 자체가 필요 없었다).
- **Java**(`com.season.semiproject.spatial.network`): `NetworkBuildService`가 rebuild
  트랜잭션·part/segment 개수 정합성 검증·정책(tolerance=0, split 없음)을 담당.
- **MyBatis**(`mapper-network-build.xml`): 위 SQL을 실행. 새 JTS/Hibernate Spatial/pgRouting
  의존성을 추가하지 않았다.

## 14. Network Build 실행 방식과 Rebuild/Idempotency

**[코드 확인 → 실행 결과]** Phase 5와 동일한 패턴을 따랐다:
- `spatial-network-build` Spring Profile로 게이트(`NetworkBuildRunner`,
  `application-spatial-network-build.properties`에서 `spring.main.web-application-type=none`).
  평상시 `./mvnw spring-boot:run`에서는 이 Bean 자체가 생성되지 않는다.
- Rebuild 전략: 한 트랜잭션(`NetworkBuildService.buildNetwork()`, `@Transactional`) 안에서
  `trail_segment` 전체 삭제 → `trail_node` 전체 삭제 → `trail_feature`로부터 재생성. **`trail`/
  `trail_feature`는 이 서비스의 어떤 메서드에서도 삭제/수정하지 않는다**(DELETE 대상에 아예 포함되지
  않음, 코드로 원천 차단).

**실제 실행 결과**:
```
SPRING_PROFILES_ACTIVE=spatial-network-build ./mvnw spring-boot:run
→ LineString Part 2122개 → TrailNode 2526개 → TrailSegment 2122개, 약 0.7초
```

**Rollback 실검증**: 닫힌 루프(시작점=끝점) geometry를 가진 임시 fixture Trail/TrailFeature를
추가한 뒤 빌드를 실행해, `CHECK (from_node_id <> to_node_id)` 위반을 실제로 발생시켰다. 실패 직후
DB를 조회해 **직전의 정상 Network(Node 2526/Segment 2122)가 그대로 보존됨**을 확인했다 — 삭제 후
재생성 도중 실패해도 빈 상태가 남지 않는다. fixture는 이후 삭제했고 Raw 데이터(4/1706)는 전 과정에서
변화가 없었다.

**Idempotency 실검증**: 동일 빌드를 연속 2회 실행 → 두 번 모두 LineString Part 2122 / Node 2526 /
Segment 2122로 완전히 동일(실행마다 `id` 값 자체는 identity가 계속 증가하므로 달라지지만, "논리적으로
동일한 Dataset"이라는 기준은 충족).

## 15. Network Validation 결과

**[실행 결과]** 아래 전부 Docker PostgreSQL/PostGIS의 실제 데이터로 확인했다.

### Node
| 검사 | 결과 |
|---|---|
| `GeometryType(geom)='POINT'` | 2526/2526 |
| `NOT ST_IsEmpty(geom)` | 2526/2526 |
| `ST_SRID(geom)=4326` | 2526/2526 |
| 중복 좌표 Node | 0건 |

### Segment
| 검사 | 결과 |
|---|---|
| `GeometryType(geom)='LINESTRING'` | 2122/2122 |
| `NOT ST_IsEmpty(geom)` | 2122/2122 |
| `ST_Length(geom) > 0`(0-length 없음) | 2122/2122 |
| `from_node_id <> to_node_id` | 2122/2122 |
| `ST_StartPoint(segment.geom) = from_node.geom` | 불일치 0건 |
| `ST_EndPoint(segment.geom) = to_node.geom` | 불일치 0건 |
| 원본 TrailFeature 역추적 가능(orphan 없음) | 0건 |
| `(source_trail_feature_id, source_part_index)` 중복 | 0건 |

### Connectivity (Trail별)

| Trail | Node | Segment | Connected Component | degree=1 | degree=2 | degree≥3 |
|---|---|---|---|---|---|---|
| 마루 | 2065 | (해당 Part 수) | **391** | 782 | 1283 | 0 |
| 무악동구간 | 220 | 216 | **1** | 2 | 218 | 0 |
| 홍제동구간 | 114 | 108 | **6** | 10 | 104 | 0 |
| 부암동구간 | 127 | 115 | **7** | 14 | 113 | 0 |
| **전체** | **2526** | **2122** | **405** | **808** | **1718** | **0** |

고립 Node(어떤 Segment에도 연결되지 않은 Node, degree=0): **0건**(모든 Node는 정의상 최소 하나의
Part 끝점에서 만들어지므로 구조적으로 발생할 수 없음을 확인).

**무악동구간이 정확히 1개의 connected component(전체가 하나의 사슬로 완전히 연결됨)라는 점은 1번
섹션에서 정정한 "분기 후보"가 사실은 이 Trail이 매우 잘 연결되어 있다는 반대 증거였음을 다시 보여준다.**
반면 마루는 391개 component로 크게 조각나 있다 — 7번에서 확인했듯 이 중 상당수는 원본 데이터
자체의 단절(또는 제외된 Feature로 인한 단절)이며, 이번 Phase에서 임의로 이어붙이지 않았다.

## 16. Raw ↔ Network Geometry Coverage 검증

**[실행 결과]**
- 2122개 Segment 전부에 대해, 원본 `trail_feature.geom`을 다시 `ST_Dump`한 결과와
  `ST_Equals(segment.geom, 재덤프한_part.geom)`을 비교 → **2122/2122 완전 일치**.
- 전체 길이 비교: `SUM(ST_Length(trail_segment.geom::geography))` = **9198.810m**,
  `SUM(ST_Length(dump(trail_feature.geom)::geography))` = **9198.810m** → 완전히 동일.

**snapping을 적용하지 않았으므로(5-3번) geometry 자체가 전혀 변경되지 않았다** — 변경 거리/이동
Feature 수/최대 이동 거리는 측정할 대상 자체가 없다(전부 0). 이 검증은 "geometry를 조금도 바꾸지
않고 관계형 구조(Node/Segment)만 추가했다"는 것을 실측으로 증명한다.

## 17. 공간 SQL 성능 / GiST 사용 결과

**[실행 결과]** `EXPLAIN (ANALYZE)`로 확인했다.

- **정확 좌표 `GROUP BY`(Node 클러스터링)**: `HashAggregate` 사용, GiST 인덱스는 사용되지 않는다
  (PostGIS GiST는 bbox 겹침/근접 연산자를 가속하며, 정확한 값 동등 비교에는 관여하지 않는다 — 이는
  정상적인 동작이다). 2122개 Part 처리에 EXPLAIN ANALYZE 기준 약 0.5~0.6초(대부분 1회성 JIT
  컴파일 오버헤드), 애플리케이션 실측으로는 전체 빌드가 약 0.7초.
- **Segment-Node 결합(정확 좌표 join)**: `Hash Join` 사용, 마찬가지로 GiST 미사용. 이번 규모(2122건)
  에서 병목 없음을 확인했다.
- **`ST_Intersects` 자기 대조(6번 분석)**: `idx_trail_feature_geom` GiST 인덱스가 실제로 사용됨을
  `EXPLAIN`으로 확인(`Index Scan using idx_trail_feature_geom`, bbox `&&` 사전 필터).

**결론**: 이번 데이터 규모(Part 2122건, Node 2526건)에서는 정확 일치 기반 Node 생성에 추가 인덱스가
필요하지 않았다. `idx_trail_node_geom`/`idx_trail_segment_geom`(GiST)은 이번 빌드 과정 자체에서는
쓰이지 않았지만, Phase 8에서 위험지역 좌표와의 근접 질의(`ST_DWithin` 등)에 필요할 것으로 예상되어
스키마에는 유지했다 — 지금 병목이 없다고 인덱스를 되돌리지는 않았지만, 반대로 지금 병목이 없는데
새 인덱스를 추가로 만들지도 않았다.

## 18. 테스트

**[실행 결과]** 새 테스트 프레임워크를 추가하지 않고 기존 JUnit 5 + Mockito(spring-boot-starter-test에
포함)로 작성했다.

- `NetworkBuildServiceTest`(3케이스, Mock DAO, DB 불필요): 정상 빌드 시퀀스(delete→create temp→
  insert nodes→insert segments 순서 검증), 재빌드 시 기존 데이터 삭제 호출 검증, **Part 수와 Segment
  수가 어긋나면 `NetworkBuildException`을 던지는 안전장치** 검증. 전부 통과.
- Topology의 실질적 동작(정확 좌표 클러스터링, 분기 없음, 비인접 교차가 endpoint 공유임, 마루 large
  gap의 원인, non-simple Feature 처리)은 **실제 Docker DB의 실데이터로 직접 검증**했다(본 문서
  1~9, 15~16번) — 이 로직은 대부분 SQL(`mapper-network-build.xml`)로 구현되어 있어, 합성 fixture로
  단위 테스트하기보다 실데이터 검증이 더 신뢰도 높은 근거라고 판단했다(Phase 5의 Import 검증과 동일한
  방침).
- **실제 DB 트랜잭션 rollback**은 닫힌 루프 fixture로 실제로 재현해 검증했다(14번). 이 fixture는
  검증 직후 삭제했다(저장소에는 커밋하지 않음).
- 전체 테스트 스위트(`./mvnw test`, 기존 19개 테스트 전부) 재실행 결과 회귀 없음.

## 19. 기존 기능 영향 없음

`spatial-network-build` 프로필 실행 중에도 Phase 3부터 계속 실행 중이던 일반 인터랙티브 인스턴스
(포트 9000)가 `GET /api/mountains` 등에 정상 응답(200)함을 확인했다. Frontend는 전혀 수정하지 않았고,
TrailNode/TrailSegment를 사용하는 API/Controller/Service/MyBatis 소비자는 이번 Phase에서 만들지
않았다.

## 20. 남은 데이터 품질 문제 / 확정하지 않은 것

- **마루의 391개 connected component**: 대부분 원본 데이터의 실제 단절 또는 Import 제외 Feature로
  인한 것으로 확인했지만, 391개 각각을 전수 분류하지는 않았다 — 7번에서 확인한 4개의 20m+ gap 외
  나머지 작은 gap(0.5~20m 사이)들이 각각 "원본 단절"인지 "좌표 정밀도 문제"인지는 개별 확인하지
  않았다.
- **Snapping tolerance는 여전히 미확정**이다(5-3번, 의도적으로 "적용하지 않음"으로 확정한 것이지
  "0.5m가 정답"이라고 확정한 것이 아니다). Phase 8에서 실제 위험지역 질의 요구사항이 나오면 재검토한다.
- **`node_type`/분기 분류 체계**는 이번 데이터에 분기가 0건이라 설계 필요성 자체가 아직 확인되지
  않았다 — 향후 다른 산 데이터가 추가되어 실제 분기가 발견되면 그때 다시 검토한다.
- **DN의 실제 elevation 의미**는 여전히 미확정이다(이전 Phase들과 동일한 결론 유지, 이번 Phase에서
  새로 확인한 내용 없음).
- **pgRouting 필요 여부**: 이번 Phase의 topology 분석 결과(분기 0건, 대부분 단순 사슬)만 보면 복잡한
  경로탐색 그래프 엔진이 필요할 근거가 아직 보이지 않는다. Phase 8 요구사항이 구체화된 뒤 다시
  판단한다.
