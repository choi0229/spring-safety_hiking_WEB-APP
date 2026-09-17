# 06. Frontend API Compatibility — Trail GeoJSON (Phase 9A)

이 문서는 Trail(`trail_feature`)을 GeoJSON API로 노출하고, 기존 정적 GeoJSON 파일을 사용하던
Frontend 화면 중 일부를 이 API로 전환한 Phase 9A의 설계와 결과를 기록한다. Phase 9 사전 분석
결과(대화 로그에 기록, docs 미작성)에서 나온 수치를 실제 구현·측정값으로 재확인한 내용이다.

## 1. 핵심 판단 — Hybrid Migration을 선택한 이유 [설계 판단]

> DB 정합성을 위해 invalid Feature 13건을 제거했지만, 기존 UI의 경사 색상 계산이
> **위치 기반 고정크기 청킹(`groupCoordinates`)**에 의존하기 때문에, 제외된 Feature가
> 좌표 스트림 중간에 있으면 그 이후 모든 그룹의 경계가 밀리면서 경사 색상이 크게 달라지는
> 회귀가 사전 분석에서 발견됐다(마루 groupSize=5 기준 공통 그룹의 29.6%, 홍제동 39.5% 색상 변경).
>
> 이 문제를 "invalid Feature를 API에 다시 섞어 compatibility를 맞추는" 방식으로 해결하지 않고,
> **Validated DB Source(일반 렌더링)와 Legacy Compatibility Source(경사 렌더링)의 경계를
> 분리**하는 방식을 선택했다.

```text
                        ┌──────────────────────────┐
                        │ Original Static GeoJSON  │
                        │ Legacy Compatibility     │
                        └─────────────┬────────────┘
                                      │
                               slope 화면 4개
                                      │
                                      ▼
                            Legacy slope rendering
                    (색상 계산 결과, 사용자 경험 변경 없음)


PostgreSQL / PostGIS
        │
        ▼
TrailFeature (Validated Raw Layer, 1706건)
        │
        ▼
Trail GeoJSON API  (GET /api/spatial/trails/geojson)
        │
        ▼
Non-slope Frontend Screens
Preview / Community / Heatmap / People / Statistics 등 11개 화면
```

이 판단이 이번 Phase의 핵심 기술적 의사결정이다 — "DB로 전부 옮기는 것"이 항상 옳은 것이 아니라,
**기존 로직의 구조적 특성(fixed-size chunking)이 데이터 정제와 충돌하는 지점을 실측으로 찾아내고,
그 경계에서만 레거시를 의도적으로 유지**한 사례다.

## 2. Backend GeoJSON 계약

```text
GET /api/spatial/trails/geojson
Content-Type: application/json

{
  "type": "FeatureCollection",
  "features": [
    { "type": "Feature",
      "properties": { "PMNTN_NM": "마루", "DN": 116 },
      "geometry": { "type": "MultiLineString", "coordinates": [...] } }
  ]
}
```

- **Source**: `trail_feature` JOIN `trail` (Raw Layer만 사용, `trail_segment`/`trail_node` 미사용 —
  원본 Feature 경계를 보존해야 하는 것이 기존 Frontend 계약이기 때문).
- **정렬**: `ORDER BY source_feature_index ASC`. 현재는 `(trail_id, sequence)` 정렬과 결과가
  동일하지만(실측 0건 불일치), 이는 `trail_id`가 우연히 원본 파일의 코스 등장 순서와 같은 숫자로
  배정됐기 때문이지 구조적으로 보장된 성질이 아니다. `source_feature_index`는 원본
  FeatureCollection의 절대 순서를 직접 담고 있으므로 이것을 정렬 기준으로 채택했다.
- **Property**: 원본 `PMNTN_NM`/`DN` 이름을 그대로 유지했다 — 이번 Phase의 목적이 "기존 Frontend
  최소 변경"이므로, `courseName`/`dnValue` 같은 도메인 이름으로 바꾸는 리팩터링은 하지 않았다.
  `source_feature_index`/`sequence`/`trail_id` 같은 내부 provenance 값은 Production 응답에
  포함하지 않는다 — 기존 Frontend 계약에 없던 필드를 추가로 노출할 이유가 없기 때문이다(디버깅이
  필요해지면 그때 추가 여부를 재검토한다).
- **정밀도**: `ST_AsGeoJSON(tf.geom, 15)`. 기본 인자(9자리)를 그대로 쓰면 원본 좌표가
  `126.95848586400908` → `126.958485864`로 절삭된다 — 공간적으로는 무시할 수준(~0.1mm)이지만
  Frontend가 받는 숫자 자체가 원본과 달라지므로 15자리를 명시했다.
- **단일 SQL**: `json_build_object` + `json_agg(... ORDER BY source_feature_index) ` +
  `ST_AsGeoJSON(...,15)::json`으로 1706건 전체를 한 번의 쿼리로 조립한다. Java에서 row를
  geometry 객체로 재조립하지 않는다(N+1 없음).
- **빈 데이터셋**: `json_agg`가 0행에 대해 `NULL`을 반환하는 PostgreSQL 특성 때문에
  `COALESCE(json_agg(...), '[]'::json)`으로 감쌌다 — 그렇지 않으면
  `{"type":"FeatureCollection","features":null}`이 나간다(실제 회귀 테스트로 확인).
- **Double serialization 방지**: Mapper가 이미 완성된 JSON 문자열을 반환하므로, Controller는
  `ResponseEntity<String>` + `MediaType.APPLICATION_JSON`으로 그대로 흘려보낸다. Jackson이
  String을 다시 객체로 감싸 `"{\"type\":...}"`처럼 재직렬화하지 않도록, 반환 타입을 DTO가 아닌
  raw `String`으로 유지한 것이 핵심이다. 실제 브라우저/HTTP 클라이언트가 응답을 순수 JSON 객체로
  파싱 가능함을 테스트로 확인했다(`TrailGeoJsonApiTest.responseBodyIsNotDoubleSerialized`).

## 3. Backend 구조

```text
com.season.semiproject.spatial.geojson
├── TrailGeoJsonController  (GET /api/spatial/trails/geojson)
├── TrailGeoJsonService     (얇은 위임)
└── TrailGeoJsonDAO         (SqlSession, 단일 select)

backend/src/main/resources/mapper/mapper-trail-geojson.xml
```
기존 spatial 하위 패키지들(accident/legacy/network/query)과 동일한 3계층(Controller-Service-DAO)
스타일을 유지했다 — API 하나를 위해 이 이상의 계층은 추가하지 않았다.

## 4. Known Difference — Original(1719) vs API(1706)

```text
Original Feature = 1719
API Feature(TrailFeature) = 1706
제외 = 13건 (PMNTN_NM 공백 4건 + geometry invalid 9건 — Phase 5, trail-import-manifest.json)
좌표 수 차이 = 21개 (마루 -15, 홍제동구간 -6, 무악동구간/부암동구간 0)
```

**이 13건은 이번 Phase에서도 API에 다시 삽입하지 않는다.** `TrailGeoJsonKnownDifferenceTest`가
이 차이를 "의도된 차이"로 회귀 고정한다 — manifest의 제외 Feature가 API 응답에 나타나지 않는지,
그리고 API가 반환하는 1706건 전부가 원본의 동일 위치 Feature와 `PMNTN_NM`/`DN`/geometry
(좌표 1e-9 이내)까지 정확히 일치하는지를 자동 테스트로 검증한다.

**지도 형상에 미치는 영향**: 마루 길이 차이 약 0.12%(7125.4m→7117.1m), 홍제동구간 약
0.88%(452.2m→448.3m). 이를 "시각적으로 완전히 동일함"이라고 단정하지 않는다 — 실측된 차이는
있으나, 전체 코스 길이에 비해 작은 수준이라는 것이 정확한 표현이다. 9번 최종 검증 단계에서 실제
API 응답(1706 Feature)을 각 화면의 실제 소비 로직에 직접 흘려 넣어 정상 처리됨을 확인했다.

## 5. Legacy Slope Compatibility — 왜 4개 화면은 전환하지 않았는가

`MountainDetailView.vue`, `CompareCourseView.vue`, `MountainDetailView2.vue`,
`MobileMountainDetailView.vue`는 `processGeoJSON()` → `groupCoordinates(N)` → `calculateSlope()`
→ `getColorBySlope()` 파이프라인을 사용한다(N=5/5/7/12). `groupCoordinates`는 좌표 배열을 고정
크기로 자르는 **위치 기반** 연산이라, 제외된 13건 때문에 좌표 스트림 중간이 밀리면 그 이후 모든
그룹의 시작/끝점이 바뀐다. 사전 분석에서 독립 재구현(Java `LegacySlopeService`가 아닌 별도 Python
재현)으로 실측한 결과:

```text
groupSize=5 기준 (Original vs DB-derived source 사용 시)
마루:   공통 832개 그룹 중 246개(29.6%) 색상 상이, 첫 차이는 group index 18
홍제동: 공통 76개 그룹 중 30개(39.5%) 색상 상이, 첫 차이는 group index 20
```

이 수치는 **Production Legacy slope 화면에는 적용하지 않는다** — 이 4개 화면이 여전히 원본 정적
파일(`/data/인왕산ele copy.geojson`)을 사용하는 한 사용자가 보는 경사 색상은 전혀 바뀌지 않는다.
이 수치는 오직 "왜 이 4개 화면을 이번 DB 전환에서 제외했는가"를 설명하는 설계 근거로만 존재한다.

`com.season.semiproject.spatial.legacy.LegacySlopeService`(Phase 7A)도 이번 Phase에서 수정하지
않았다 — 이 클래스의 역할은 "Original Source 기준 legacy 동작 재현 검증"이며, Phase 9A의 DB 전환과
무관하게 원본 파일을 계속 읽는다.

## 6. 이번 Phase에서 전환한 Frontend 화면 (실사용 7개)

Phase 9A 최초 구현에서는 11개 파일의 fetch URL을 교체했으나, 재검증 단계에서 router/컴포넌트
import를 다시 전수 확인한 결과 아래 4개 파일이 현재 빌드의 어떤 실행 경로에서도 도달 불가능함을
재확인했다(router 등록 없음, 다른 `.vue`/`.js`에서의 import 없음, 문자열 기반 동적 참조 없음 —
`src/router/index.js`의 43개 route 전체와 `src/main.js`를 직접 확인):

```text
WebCoursePreview.vue
RecordStatisticsView.vue
HeatmapView copy.vue
MountainDetailView3.vue
```

**"현재 사용되지 않는 코드까지 불필요하게 수정하지 않는다"는 원칙에 따라, 이 4개 파일의 Trail
source URL 변경만 원래 static 경로로 되돌렸다**(파일 전체를 git restore한 것이 아니라 Phase 9A가
바꾼 그 한 줄만 되돌림 — `git diff` 기준 이 4개 파일은 현재 Phase 9A 이전 상태와 완전히 동일하다).
따라서 실제 DB API로 전환된 화면은 다음 7개(모두 router에 등록되어 실행 가능함을 재확인)로
확정한다.

| 파일 | 기존 source | 신규 source | 로직 변경 | 라우팅 경로 |
|---|---|---|---|---|
| `MobileCoursePreview.vue` | 정적 파일 | API | 없음(URL만) | `/mobileCoursePreview` |
| `RealTimePeopleHeatmapView.vue`(함수 2개) | 정적 파일 | API | 없음 | `/realtimeheatmap` |
| `MobileRealTimePeopleHeatmapView.vue`(함수 2개) | 정적 파일 | API | 없음 | `/mobilerealtimeheatmap` |
| `MobileCommunityView.vue` | 정적 파일 | API | 없음 | `/mobilecommunity` |
| `CommunityView.vue` | 정적 파일 | API | 없음 | `/community` |
| `RealTimePeopleView.vue` | 정적 파일 | API | 없음 | `/realtime` |
| `HeatmapView.vue` | 정적 파일 | API | 없음 | `/heatmap` |

모든 변경은 **fetch URL 문자열 교체만**이며, `processGeoJSON`/`PMNTN_NM` 필터/geometry flatten/
bounds 계산/heatmap 계산/community grouping 로직은 그대로 두었다. 각 파일이 기존에 `fetch()`를
쓰면 `fetch()`를, `axios.get()`을 쓰면 `axios.get()`을 그대로 유지했다(HTTP client 통일 안 함).
새 API 호출 유틸리티(`trailApi.js` 등)는 만들지 않았다 — URL 문자열 9곳(2개 파일은 함수가 2개씩)
교체만으로 충분했고, 기존 프로젝트에도 이런 용도의 공용 헬퍼가 없었다(각 화면이 각자 `axios`/
`fetch`를 직접 호출하는 것이 기존 컨벤션). API 경로는 기존 `axios.get('/api/...')` 상대경로
컨벤션(Vue CLI devServer의 `/api` → `http://localhost:9000` 프록시, production 빌드는 Spring
Boot가 같은 오리진에서 정적 파일까지 서빙)을 그대로 따랐다 — 새 base URL이나 하드코딩된 호스트를
추가하지 않았다.

## 7. 전환하지 않은 화면 (그대로 유지)

```text
MountainDetailView.vue
CompareCourseView.vue
MountainDetailView2.vue
MobileMountainDetailView.vue
```
grep으로 재확인: 위 4개 파일의 `/data/인왕산ele copy.geojson` 참조(총 7곳 — 모달/사진맵 변형
포함)가 전부 그대로 남아 있다.

## 8. Payload / 응답 시간 실측값 [데이터 확인]

```text
SQL 실행 시간(psql \timing)         : 약 38ms
API 응답 크기(Content-Length)        : 458,796 bytes
Original 정적 파일 크기              : 1,118,265 bytes
```
API 응답이 원본의 약 41%(59% 감소) 크기다 — 이는 미사용 property(FID, PMNTN_SN, MNTN_CODE 등
17개)를 응답에서 제외한 결과이지 성능 최적화의 성과가 아니다. 별도의 API 응답시간(HTTP round-trip)
측정 도구는 이번에 추가하지 않았으며, curl 기준 SQL 실행+직렬화+전송을 포함해도 체감 지연은
없었다(수 밀리초~수십 밀리초 수준).

## 9. 검증 구분 — 자동 테스트 / API·Proxy 실행 확인 / Node 로직 실행 확인 / 실제 브라우저 확인

**자동 테스트(완료)**:
- `TrailGeoJsonServiceTest`(6개): 1706건, PMNTN_NM/DN 매핑, `source_feature_index` 순서,
  15자리 정밀도, 4개 코스 건수, 빈 데이터셋 처리
- `TrailGeoJsonApiTest`(2개): HTTP 200/Content-Type/FeatureCollection 계약, double
  serialization 없음
- `TrailGeoJsonKnownDifferenceTest`(3개): 1719/1706/13 회귀 고정, 제외 Feature 미포함,
  1706건 전체 원본 parity

**Frontend dependency/build(완료)**: `npm ci`로 설치(`package.json`/`package-lock.json` 변경
없음 확인), `npm run build` 성공 — 이번 변경으로 인한 컴파일 에러/경고 0건(기존에도 있던 asset
size 경고 2건만 존재, 폰트/영상/geojson 원본 파일 등 이번 변경과 무관).

**API/Proxy 실행 확인(완료)**: `./mvnw spring-boot:run`으로 이미 기동 중인 Backend(포트 9000)에
대해 `GET /api/spatial/trails/geojson`이 직접 호출(200, `application/json`, 1706건)과 Vue
devServer(`npm run serve`, 포트 8080) 프록시 경유 호출 양쪽에서 바이트 단위로 동일한 응답을
반환함을 확인했다 — `/api → localhost:9000` 프록시가 정상 동작한다.

**Node 로직 실행 확인(완료, 실제 브라우저 DOM/콘솔 확인은 아님)**: 이 작업 환경에는 디스플레이가
없어 실제 브라우저를 열 수 없다. 대신 7개 전환 화면 각각의 GeoJSON 소비 함수 본문을 원본
그대로 복사해, kakao.maps/Leaflet의 최소 stub과 함께 **실제 라이브 API 응답**에 대해 Node로
직접 실행했다(신규 테스트 프레임워크 도입 아님 — Node 내장 fetch만 사용). 결과:

| 화면 | 실행한 함수 | 결과 | 비고 |
|---|---|---|---|
| `CommunityView`/`MobileCommunityView` | `loadCourseData` | PASS | 코스 4개(마루/무악동구간/홍제동구간/부암동구간) 정상 그룹핑, 좌표수 4160/885/380/434 |
| `HeatmapView` | `processGeoJSON` | PASS | 2122개 line group, 총 5859 좌표, 예외 없음 |
| `RealTimePeopleHeatmapView`/`MobileRealTimePeopleHeatmapView` | `extractHeatmapDataFromGeoJSON`+`drawRoutesFromGeoJSON` | PASS | heatmap 포인트 5859개, polyline 2122개 |
| `RealTimePeopleView` | `processGeoJSON`(마루 필터) | PASS | 좌표 4160개, `kakao.maps.LatLng` 생성 정상 |
| `MobileCoursePreview` | `processGeoJSON`(코스별) | PASS(4개 코스 전부) | 마루/무악동/홍제동/부암동 각각 4160/885/380/434 포인트, bounds 계산 정상 |

7개 화면 전부 예외 없이 실행되었고, `PMNTN_NM`/`DN`/`geometry.coordinates`를 정상적으로 읽었으며,
출력 좌표 수는 사전 분석에서 확정한 Known Difference 수치(5859 = 5880-21)와 정확히 일치했다.
**이것은 실제 DOM 렌더링/지도 타일 표시/브라우저 콘솔 확인을 대체하지 않는다** — 함수가 예외 없이
올바른 데이터를 만들어내는 것까지만 증명하며, 실제 지도에 그려지는 모습이나 CSS/레이아웃 문제는
확인 범위 밖이다. **실제 배포 전 브라우저에서 7개 화면을 최소 1회 육안으로 확인하는 것을 권장한다.**

Legacy Slope 보호 확인: `MountainDetailView.vue`의 실제 `calculateSlope`/`groupCoordinates`/
`getColorBySlope` 로직을 Vue devServer가 그대로 서빙하는 원본 정적 파일(`/data/인왕산ele
copy.geojson`, 200, 1,118,265 bytes — 원본과 완전히 동일한 크기)에 대해 동일한 방식으로 실행한
결과, 무악동 코스 groupSize=5 기준 177 total groups/177 rendered/색상
{`#32CD32`:69,`#FF4500`:36,`#1E90FF`:72}로 Phase 7A에서 확정한 baseline과 완전히 일치했다 —
Phase 9A가 Legacy 경로에 어떤 영향도 주지 않았음을 재확인했다.

## 10. Original Static GeoJSON 유지 이유

`frontend/public/data/인왕산ele copy.geojson`을 삭제/이동하지 않았다. 이 파일은 현재도:
- Legacy Slope 4개 화면의 Production compatibility source
- Phase 7A `LegacySlopeServiceParityTest`/`LegacySlopeServiceTest`의 회귀 기준
- Trail Import(`SpatialImportRunner`)의 provenance 원본

세 가지 역할을 동시에 수행 중이라 삭제하면 안 된다. 위치도 `frontend/public/data/`를 그대로
유지했다 — 이미 여러 백엔드 테스트/러너가 `../frontend/public/data/...` 상대경로로 이 위치를
참조하도록 구축되어 있어, 옮기면 그 경로들을 전부 함께 수정해야 하는 불필요한 연쇄 변경이 생긴다.

## 11. Accident 기능과의 분리

Phase 8의 `AccidentPoint`/`/api/spatial/accidents/...`/`/api/spatial/trail-segments/...` API는
이번 Phase에서 전혀 건드리지 않았다. `MountainDetailView3.vue`의
`loadMarkers("/data/2023산악사고_인왕산.geojson", ...)` 같은 사고 마커 fetch도 그대로 두었다 —
이번 변경은 오직 Trail GeoJSON(`인왕산ele copy.geojson`) 관련 fetch에만 적용된다.

## 12. Known Issue (Phase 9A와 무관, 수정하지 않음)

최종 검증 중 `npm run build`를 실행해 보니 `frontend/vue.config.js`의
`outputDir: '../src/main/resources/static'`이 `frontend/` 기준 상대경로라 실제로는
`backend/src/main/resources/static/`이 아니라 **repository 루트의 `src/main/resources/static/`**
로 빌드된다는, Phase 9A와 무관한 기존 설정 오류를 발견했다. 이번 Phase 범위 밖의 문제이므로
`vue.config.js`는 수정하지 않았고, 검증 과정에서 생성된 잘못된 위치의 빌드 산출물은 삭제했다
(repository에는 반영되지 않음). Production 배포 파이프라인을 실제로 사용할 때는 이 경로를
바로잡아야 한다.

## 13. 최종 검증 상태

Phase 9A는 이번 재검증으로 **COMPLETE** 상태다: Backend API/테스트, Frontend build, API/Proxy
실행, 7개 실사용 화면의 로직 실행 확인, Legacy 4개 화면 보호 확인까지 모두 완료했다. 유일한 남은
간극은 실제 브라우저 DOM/육안 확인(9번에서 이유와 대체 검증 범위를 명시)이며, 이는 기능적
정합성과는 무관한 시각적 확인 단계다.
