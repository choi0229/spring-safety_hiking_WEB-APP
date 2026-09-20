<template>
  <div id="app">
      <MobileHeaderView></MobileHeaderView>
      <div class="app-main">
          <div class="map-container">
          <div class="button-group">
              <button class="button-item" @click="goBack" style="border-radius: 50%; height: 40px; width: 40px;"><img src="/images/뒤로가기.png"  alt="뒤로가기 아이콘" width="24" height="24"></button>
          </div>
          <div class="button-group2">
              <button class="button-item" @click="handleClick()" style="border-radius: 50%; height: 40px; width: 40px;">
                  <i class="bi bi-eye-fill"></i>
              </button>
          </div>
          <div class="button-group3">
              <button class="button-item" style="border-radius: 50%; height: 40px; width: 40px;" data-bs-toggle="modal" data-bs-target="#exampleModal">
                <i class="bi bi-question-lg"></i>
              </button>
          </div>
          <div class="map-legend ms-4">
            <div class="legend-item">
              <div class="legend-icon" style="background-color:#FF4500;"></div>
              <span>오르막</span>
            </div>
            
            <div class="legend-item">
              <div class="legend-icon" style="background-color:#32CD32;"></div>
              <span>평지</span>
            </div>
            
            <div class="legend-item">
              <div class="legend-icon" style="background-color:#1E90FF;"></div>
              <span>내리막</span>
            </div>
          </div>
          
              <div id="map">
                  <!-- 오버레이 카드 2개-->
                  <div class="card-container">
                    <div 
                      v-for="(course, index) in courseData" 
                      :key="index" 
                      class="card" 
                      :class="{ clicked: isClicked[index] }" 
                      @click="handleClickCard(index)"
                    >
                      <span>{{ course.mountainName }}</span>
                      <h4 style="font-weight: 700;" class="m-0">{{ course.courseName }}코스</h4>
                      <span style="font-size: small;">{{ course.distance }} | {{ course.duration }}</span>
                      
                      <!-- 사고 유형과 발생 건수를 표시 -->
                      <span class="mt-1" v-if="incidentInfo[index]?.typeCounts" style="font-size: small;">
                        <span class="m-0" v-for="(count, type) in incidentInfo[index].typeCounts" :key="type" style="display: block;  margin-bottom: 5px;">
                          <span style="display: inline-flex; align-items: center;">
                            {{ type }} : {{ count }} 
                            <!-- 가로 막대기 차트 -->
                            <div
                              class="bar"
                              :style="{ width: (count * 20) + 'px', 
                              backgroundColor: getBarColor(count, type) }">
                            </div>
                          </span>
                        </span>
                      </span>
                    </div>
                  </div>

              </div>
          </div>
      </div>
      <MobileFooterView></MobileFooterView>
  </div>

<!-- START::상세 비교 표 보기 모달 -->
<div class="modal fade" id="exampleModal" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">
<div class="modal-dialog modal-dialog-centered">
  <div class="modal-content p-0">
    <div class="modal-body pt-3 pb-3 ps-2 pe-2">
      <table class="comparison-table">
          <thead>
              <tr>
                  <th>코스</th>
                  <th>{{ courseData[0]?.courseName }}코스</th>
                  <th>{{ courseData[1]?.courseName }}코스</th>
              </tr>
          </thead>
          <tbody>
              <tr>
                <th>산 이름</th>
                <td v-for="(course, index) in courseData" :key="'mountainName-' + index">
                    {{ course.mountainName }}
                </td>
              </tr>
              <tr>
                <th>총 거리</th>
                <td v-for="(course, index) in courseData" :key="'distance-' + index">
                    {{ course.distance }}
                </td>
              </tr>
              <tr>
                <th>난이도</th>
                <td v-for="(course, index) in courseData" :key="'distance-' + index"  :style="{color: getLevel(course.courseLevel)}">
                    {{ course.courseLevel }}
                </td>
              </tr>
              <tr>
                <th>후기</th>
                  <td v-for="(course,index) in courseStars" :key="index">
                    <div class="stars">
                      <i v-for="n in course.fullStars" :class="'bi bi-star-fill'" :key="'full-' + n + '-' + index"></i>
                      <i v-if="course.halfStar" class="bi bi-star-half"></i>
                      <i v-for="n in course.emptyStars" class="bi bi-star" :key="'empty-' + n + '-' + index"></i>
                    </div>
                  </td>
              </tr>
              <tr>
                <th>최고 고도</th>
                  <td v-for="(elev, index) in maxElev" :key="'elevation-' + index">
                    {{ elev }}m
                  </td>
              </tr>
              <tr>
                <th>예상 소요 시간</th>
                <td v-for="(course, index) in courseData" :key="'estimatedTime-' + index">
                    {{ course.duration }}
                </td>
              </tr>
              <tr v-for="(type) in typeList" :key="type">
                <th>{{ type }}</th>
                  <td v-for="(course, index) in incidentInfo" :key="index" :style="{
                    color: (course.typeCounts[type] === undefined ? 0 : course.typeCounts[type]) === getMinCount(type) ? 'black' : 'red'
                  }">
                  {{ course.typeCounts[type] || 0 }}건
                  </td>
              </tr>
          </tbody>
      </table>
    </div>
  </div>
</div>
</div>
<!-- END::상세 비교 표 보기 모달 -->

</template>

<script setup>
import MobileFooterView from "@/components/MobileFooterView2.vue";
import MobileHeaderView from "@/components/MobileHeaderView.vue";
/* global kakao */
import { ref, onMounted} from "vue";
import { useRouter } from 'vue-router';
import { getCourseById } from "@/api/complaint";
import {
  resolveTrailId,
  SLOPE_WINDOW_METERS,
  getEstimatedSlopeColor,
  fetchTrailGeoJson,
  fetchSlopeSections,
} from '@/api/slopeSection.js';
//import { set } from "core-js/core/dict";

const router = useRouter();

const goBack = () => router.back();

const addCustomOverlay = () => {
// routeCoordinates.value 배열에서 각 코스에 대해 반복
routeCoordinates.value.forEach(courseData => {
  if (map.value) {
    // 해당 코스의 좌표들에서 최소 위도(minLat)와 최소 경도(minLon) 구하기
    const minLat = Math.min(...courseData.coordinates.map(coord => coord.lat));
    const minLon = Math.min(...courseData.coordinates.map(coord => coord.lng));

    // 해당 코스의 최고 고도 구하기
    maxElev.value.push(Math.max(...courseData.coordinates.map(coord => coord.elevation)));

    // 커스텀 오버레이 HTML 내용 설정
    const content = `
      <div class="overlay-container" style="display: inline-block;">
        <div class="label" style="background-color: white; padding: 4px 8px; border-radius: 8px; box-shadow: 0px 2px 6px rgba(0, 0, 0, 0.15); color: black; font-size: 14px; text-align: center;">
          <span class="center">${courseData.courseName}</span>
        </div>
      </div>
    `;

    // 경계 좌측 상단을 기준으로 오버레이 위치 지정
    const position = new kakao.maps.LatLng(minLat + 0.0015, minLon);

    // 커스텀 오버레이 생성
    const customOverlay = new kakao.maps.CustomOverlay({
      position: position,
      content: content,
      zIndex: 9999 // 마커들보다 위에 오버레이 표시
    });

    // 커스텀 오버레이를 지도에 표시
    customOverlay.setMap(map.value);
    console.log("오버레이 성공함");
  } else {
    console.log("오버레이 실패함");
  }
});

}

// 버튼 클릭 시 호출되는 함수
function handleClick() {
if (map.value) {  // map이 정의되었을 때만 호출
  setMapBoundsToAllCourse(map.value);
} else {
  console.log("Map is not ready yet.");
}
}

const isClicked = ref([true, true]); // 각 카드의 클릭 여부 상태를 추적

// 클릭 시 실행되는 함수
const handleClickCard = (index) => {
// 해당 인덱스 카드 클릭 시 상태를 반전
isClicked.value[index] = !isClicked.value[index];

// 상태에 따라 setMapBoundsToCourse 호출
if (isClicked.value[index]) {
    handleClick();
    console.log("클릭 해제");
  } else {
    setMapBoundsToCourse(routeCoordinates.value[index]);
}
};

const map = ref(null);
const routeCoordinates = ref([]);
let courseData = ref([]);
let incidentInfo = ref([]);
let typeList = ref([]);

let maxElev = ref([]);

function setMapBoundsToCourse(courseRoute) {
  const bounds = new kakao.maps.LatLngBounds();

  // 경로의 모든 좌표를 경계에 추가
  courseRoute.coordinates.forEach(coord => {
  bounds.extend(new kakao.maps.LatLng(coord.lat, coord.lng));
  });

  // 지도의 경계를 설정하여 해당 코스가 화면에 맞게 조정
  map.value.setBounds(bounds);
}

// onMounted로 컴포넌트가 마운트된 후에 실행되도록 설정
onMounted(async () => {
  initializeMap(); // Vue가 마운트될 때 지도를 초기화
  await init();
});

async function init(){
  const selectedId = JSON.parse(sessionStorage.getItem("selectedCourseIds"));

  // 코스 ID 배열을 순회하며 데이터를 가져오기
const courses = await Promise.all(
  selectedId.map(async (id) => {
    return await getCourseById(id);
  })
);

// 가져온 코스 데이터를 courseData 배열에 할당
courseData.value = courses;
console.log("코스데이터:", JSON.stringify(courseData.value[0], null, 2));
courseStars.value = courseData.value.map((course) => {
  return {
    fullStars: Math.floor(course.courseRate),
    halfStar: course.courseRate % 1 >= 0.5,
    emptyStars: 5 - Math.floor(course.courseRate) - (course.courseRate % 1 >= 0.5 ? 1 : 0)
  };
});

}

let courseStars = ref([]);

function initializeMap() {
  // Kakao 지도 API 스크립트를 동적으로 로드
  const script = document.createElement('script');
  script.onload = () => {
      kakao.maps.load(() => {
          createMap();
      });
  };
  script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=clusterer,services'; // autoload=false로 설정
  document.head.appendChild(script);
}

async function createMap() {
  map.value = new kakao.maps.Map(document.getElementById('map'), {
    center: new kakao.maps.LatLng(37.66433293993584, 127.01160029114365),
    level: 5,
  });
  await loadGeoJSONFromServer();
  await loadMarkers("/data/헬기장spot.geojson", '/images/helipad.png');
  await loadMarkers("/data/화장실.geojson", '/images/toilets.png');
  await loadMarkers("/data/2023산악사고_인왕산2.geojson", '/images/danger.png');
  incidentInfo.value = await countIncidentTypes("/data/2023산악사고_인왕산2.geojson",courseData);
  
}

// Trail GeoJSON API 데이터 로드 함수 (Phase 12D -- Validated DB TrailFeature 기반, 기존 static
// '/data/인왕산ele copy.geojson' 직접 fetch를 대체. 해당 static 파일 자체는 원본 provenance/
// Legacy regression 용도로 계속 보존한다 -- docs/09-slope-section-analysis.md 참고.)
async function loadGeoJSONFromServer(targetMap = map.value) {
try {
  const geojsonData = await fetchTrailGeoJson();
  console.log('Trail GeoJSON data loaded:', geojsonData);
  processGeoJSON(geojsonData, targetMap);  // GeoJSON 데이터를 처리하여 경로를 그리는 함수 호출
  addCustomOverlay();
} catch (error) {
  console.error('Trail GeoJSON API 로드 중 에러 발생:', error);
}
}

async function countIncidentTypes(url, courseData) {
try {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
  
  const geojsonData = await response.json();
  if (!geojsonData || !geojsonData.features) {
    console.error("Invalid GeoJSON data:", geojsonData);
    return {}; // 유효하지 않은 데이터일 경우 빈 객체 반환
  }

  // 각 course에 대한 incident type 빈도 객체 초기화
  const courseTypeCounts = courseData.value.map(course => ({
    courseName: course.courseName,
    mountainName: course.mountainName,
    typeCounts: {}
  }));

  geojsonData.features.forEach(feature => {
    const { MNTN_NM, MNTN_NM2, type } = feature.properties;

    // 각 course와 MNTN_NM, MNTN_NM2가 일치하는지 확인
    courseData.value.forEach((course, index) => {
      if (course.mountainName === MNTN_NM && course.courseName === MNTN_NM2 && type) {
        // type 카운트 증가
        courseTypeCounts[index].typeCounts[type] = 
          (courseTypeCounts[index].typeCounts[type] || 0) + 1;
      }
    });
  });

  // 사고 유형을 알파벳 순으로 정렬
  courseTypeCounts.forEach(course => {
    const sortedTypeCounts = {};
    const sortedKeys = Object.keys(course.typeCounts).sort(); // 사고 유형 키를 알파벳 순으로 정렬
    typeList.value = [...new Set([...typeList.value, ...sortedKeys])].sort();
    sortedKeys.forEach(key => {
      sortedTypeCounts[key] = course.typeCounts[key];
    });
    course.typeCounts = sortedTypeCounts;
  });

  console.log("Incident Type Counts by Course:", courseTypeCounts);
  console.log("typeList;", typeList.value);
  return courseTypeCounts;
  

} catch (error) {
  console.error('Error loading GeoJSON file:', error);
  return []; // 오류 발생 시 빈 배열 반환
}
}

function getLevel(level) {
if(level == '쉬움') {
  return 'green'
} else if(level == '보통') {
  return 'orange'
} else {
  return 'red'
}
}

function getMinCount(type) {
let minCount = Infinity;

if (incidentInfo.value && Array.isArray(incidentInfo.value)) {
  incidentInfo.value.forEach(incident => {
    const count = incident.typeCounts && incident.typeCounts[type] !== undefined 
                  ? incident.typeCounts[type] 
                  : 0;  // undefined일 경우 0으로 처리
    minCount = Math.min(minCount, count);
  });
}
return minCount;
}

function getBarColor(count, type) {
let color = 'orange';
let maxCount = 0;

// incidentInfo가 존재하는지 체크
if (incidentInfo.value && Array.isArray(incidentInfo.value)) {
  // 각 course를 순회하여 해당 typeCounts[type]의 최대값을 찾음
  incidentInfo.value.forEach(incident => {
    if (incident.typeCounts && incident.typeCounts[type]) {
      maxCount = Math.max(maxCount, incident.typeCounts[type]);
    }
  });
}

// maxCount와 count 값에 따라 색상 결정
if(incidentInfo.value[0].typeCounts[type]!=incidentInfo.value[1].typeCounts[type]){
    if (maxCount === count) {
    color = 'red'; // 최대값인 경우 빨간색
  }
}


return color;
}


function processGeoJSON(geojsonData, targetMap) {
  // courseData에 있는 각 courseName에 해당하는 좌표들을 구분하여 저장
  routeCoordinates.value = courseData.value.map(course => ({
      courseName: course.courseName,
      coordinates: []
  }));

  geojsonData.features.forEach((feature) => {
      // GeoJSON feature의 PMNTN_NM이 courseData의 courseName 중 하나와 일치하는지 확인
      courseData.value.forEach((course, index) => {
          if (feature.properties.PMNTN_NM && feature.properties.PMNTN_NM.includes(course.courseName)) {
              let coordinates = [];

              // MultiLineString을 처리하기 위해 중첩 배열을 펼침
              if (feature.geometry.type === 'MultiLineString') {
                  feature.geometry.coordinates.forEach(line => {
                      coordinates = coordinates.concat(line.map((coord) => ({
                          lng: coord[0],
                          lat: coord[1],
                          elevation: feature.properties.DN || 0
                      })));
                  });
              } else if (feature.geometry.type === 'LineString') {
                  coordinates = feature.geometry.coordinates.map((coord) => ({
                      lng: coord[0],
                      lat: coord[1],
                      elevation: feature.properties.DN || 0
                  }));
              }

              // courseName별로 좌표를 저장
              routeCoordinates.value[index].coordinates.push(...coordinates);
          }
      });
  });

  // 지도에 Base 경로 추가 (경사 색상은 renderSlopeOverlay가 코스별로 별도로 그린다)
  routeCoordinates.value.forEach((courseRoute) => {
      if (courseRoute.coordinates.length > 0) {
          addRouteLayer(courseRoute.coordinates, targetMap);
          renderSlopeOverlay(courseRoute.courseName, targetMap, geojsonData); // Phase 12D: 20m SlopeSection Overlay
      }
  });

  if (routeCoordinates.value.every(courseRoute => courseRoute.coordinates.length === 0)) {
      console.log('유효한 구간 데이터가 없습니다.');
  }
  console.log("좌표나눈거"+JSON.stringify(routeCoordinates));
}

let allCoordinates = [];

// 경로(Base Trail) 레이어 추가 함수 -- Phase 12D부터 경사 색상은 이 함수가 아니라
// renderSlopeOverlay()가 코스별로 별도 Overlay로 그린다 (Base + Overlay 구조, docs/09 참고).
function addRouteLayer(coordinates, targetMap) {
  allCoordinates.push(...coordinates);
  drawBaseRoute(coordinates); // 기본 경로 그리기
  setMapBoundsToAllCourse(targetMap);
}

// Phase 12D: 20m SlopeSection Backend API 기반 경사 Overlay. 이 화면은 여러 코스를 동시에
// 비교하므로 코스별로 독립적으로 호출/렌더링된다 -- 코스별 Overlay가 서로 섞이지 않도록
// slopeOverlayPolylinesByCourse에 course 이름별로 분리해서 추적하고, course 재렌더링 시
// 그 코스의 이전 Overlay만 제거한다.
const slopeOverlayPolylinesByCourse = {};

async function renderSlopeOverlay(courseName, targetMap, trailGeoJson) {
  if (slopeOverlayPolylinesByCourse[courseName]) {
    slopeOverlayPolylinesByCourse[courseName].forEach((polyline) => polyline.setMap(null));
  }
  slopeOverlayPolylinesByCourse[courseName] = [];

  try {
    const trailId = resolveTrailId(courseName, trailGeoJson);
    const slopeGeoJson = await fetchSlopeSections(trailId, SLOPE_WINDOW_METERS);
    slopeGeoJson.features.forEach((feature) => {
      const color = getEstimatedSlopeColor(feature.properties.estimatedSlopePercent);
      if (color == null) {
        // invalid/missing slope (not the same thing as a measured ~0% slope) -- skip this
        // Overlay Feature entirely; the Base Trail Layer already shows this geometry.
        return;
      }
      const path = feature.geometry.coordinates.map(([lng, lat]) => new kakao.maps.LatLng(lat, lng));
      const polyline = new kakao.maps.Polyline({
        path,
        strokeWeight: 5,
        strokeColor: color,
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
      });
      polyline.setMap(targetMap);
      slopeOverlayPolylinesByCourse[courseName].push(polyline);
    });
  } catch (error) {
    // SlopeSection API 실패는 해당 코스의 경사 Overlay만 생략시킨다 -- Base Trail은 유지된다.
    console.error(`SlopeSection API 로드 중 에러 발생 (${courseName}, Base Trail은 계속 표시됩니다):`, error);
  }
}

function setMapBoundsToAllCourse(targetMap) {
  // 지도 경계를 모든 경로 좌표에 맞게 설정
  const bounds = new kakao.maps.LatLngBounds();
  allCoordinates.forEach(coord => bounds.extend(new kakao.maps.LatLng(coord.lat, coord.lng)));
  targetMap.setBounds(bounds);
  // 지도 경계 설정 후에 위쪽으로 약간 이동
  setTimeout(() => {
  targetMap.panBy(0, 80); // 화면 위쪽으로 100px 이동 (필요에 따라 조정 가능)
  }, 200);
}

// 새로운 함수: 기본 녹색 경로를 그리는 함수
function drawBaseRoute(coordinates) {
const linePath = coordinates.map(coord => new kakao.maps.LatLng(coord.lat, coord.lng));

const basePolyline = new kakao.maps.Polyline({
  path: linePath,
  strokeWeight: 5,
  strokeColor: '#32CD32', // 기본 경로 색상 (녹색)
  strokeOpacity: 1, // 기본 경로는 약간 투명하게 설정
  strokeStyle: 'solid'
});

basePolyline.setMap(map.value);
}

// Phase 12D: 경사(slope) 계산은 더 이상 Frontend 책임이 아니다 -- 기존
// calculateHaversineDistance()/calculateSlope()/deg2rad()/getColorBySlope()는 제거했다.
// 색상 매핑은 src/api/slopeSection.js의 getEstimatedSlopeColor()가, 경사 계산 자체는
// Backend SlopeSectionService가 담당한다 (docs/09-slope-section-analysis.md).

// 마커 추가
async function loadMarkers(url, imageSrc) {
  try {
      const response = await fetch(url);
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      const markerData = await response.json();

      if (markerData && markerData.features) {
          const imageSize = new kakao.maps.Size(35, 45);
          const imageOpation = { offset: new kakao.maps.Point(12, 35) };
          const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imageOpation);

          markerData.features.forEach((spot) => {
              // MNTN_NM2 속성값이 courseData 내의 courseName과 일치하는 경우에만 마커 추가
              const isSelectedCourse = courseData.value.some(course => course.courseName === spot.properties.MNTN_NM2);
              
              if (isSelectedCourse) {
                  const lat = spot.geometry.coordinates[1];
                  const lng = spot.geometry.coordinates[0];
                  const markerPosition = new kakao.maps.LatLng(lat, lng);

                  const marker = new kakao.maps.Marker({
                      position: markerPosition,
                      map: map.value,
                      title: spot.properties.MNTN_NM2,
                      image: markerImage,
                  });

                  const infowindow = new kakao.maps.InfoWindow({
                      content: `<img src="/images/${spot.properties.image}" alt="사진" style="width: 150px; height: 100px;"/><br><div style="padding:5px;">${spot.properties.MNTN_NM2}<br>${spot.properties.SAFE_SPOT2}</div>`,
                  });

                  kakao.maps.event.addListener(marker, 'click', () => {
                      if (infowindow.getMap()) {
                          infowindow.close();
                      } else {
                          infowindow.open(map.value, marker);
                      }
                  });
              }
          });
      } else {
          throw new Error("Invalid GeoJSON structure.");
      }
  } catch (error) {
      console.error(`${url} 파일 로드 중 에러 발생:`, error);
  }
}



</script>

<style scoped>
#app {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  display: flex;
  justify-content: center;
}

.map-container {
  position: relative;
  flex: 1;
  width: 100%;
  height: 100%;
}

#map {
  width: 100%;
  height: 100%;
  position: relative;
}

.overlay-container {
display: inline-block;
}

.label {
background-color: white;
padding: 8px 16px;
border-radius: 8px;
box-shadow: 0px 2px 6px rgba(0, 0, 0, 0.15);
color: black;
font-size: 14px;
text-align: center;
}

.button-group {
position: absolute;
top: 10px;
left: 10px;
z-index: 2;
}

.button-group2 {
position: absolute;
top: 10px;
right: 10px;
z-index: 2;
}

.button-group3{
position: absolute;
bottom: 245px;
right: 10px;
z-index: 2;
}

.map-legend {
position: absolute;
top: 10px;
right: 50px;
margin-right: 1.25em;
background-color: rgba(255, 255, 255, 0.8); 
border-radius: 10px;
font-size: 12px;
z-index: 10; /* 지도 위에 표시되도록 설정 */
font-family: 'TheJamsil4Bold';
font-weight: 400;
font-size: 16px;
box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
display: flex; 
flex-direction: row; 
align-items: center; 
}

.legend-item {
display: flex;
align-items: center;
margin-bottom: 3px;
margin-top: 2px;
margin-right: 0.5rem;
font-family: 'TheJamsil4Bold';
font-weight: 400;
font-size: 14px;
}

.legend-icon {
width: 10px;
height: 10px;
margin-right: 5px;
margin-left: 5px;
border-radius: 10px;
}

.button-item {
  background-color: #ffffff;
  color: #000000;
  border: none;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
}

.button-item i {
font-size: 18px;  /* 예시로 크기를 24px로 설정 */
color: gray;     /* 아이콘 색상 설정 */
}

.card-container {
  display: flex;
  justify-content: space-between;
  position: absolute;
  bottom: 80px; 
  width: 100%;
  padding: 0 20px;
  z-index: 10;
}

.card {
  width: 155px; /* 카드의 너비 설정 */
  height: 150px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 10px;
  display: flex;
  flex-direction: column;
  justify-content: start;
  transition: transform 0.3s ease, box-shadow 0.3s ease, background-color 0.3s ease, color 0.3s ease; /* 각각의 속성에 대한 애니메이션 */
  cursor: pointer; /* 커서가 포인터 모양으로 변하게 설정 */
}

.card:hover:not(.clicked) {
transform: translateY(-10px);
box-shadow: 0 15px 30px rgba(0, 0, 0, 0.2);
background-color: #04663f;
color: white;
}


/* 스크롤바 및 스크롤 트랙을 투명하게 설정 */
.comparison-container::-webkit-scrollbar,
.comparison-container::-webkit-scrollbar-thumb,
.comparison-container::-webkit-scrollbar-track {
  background-color: transparent;
  width: 8px; /* 스크롤바 너비 */
}

.bar {
height: 10px; /* 막대기 차트의 높이 */
background-color: #04663f; /* 막대기 색상 */
border-radius: 5px; /* 모서리 둥글게 */
transition: width 0.3s ease-in-out; /* 길이 변화에 애니메이션 추가 */
margin-left: 10px;
}

/* 모달 배경의 불투명도를 조정 */
div.modal-backdrop {
background-color: rgba(0, 0, 0, 0.041); /* 불투명도를 0.2로 조정하여 덜 어둡게 설정 */
}

/* 테이블 CSS */
.comparison-table {
  width: 100%;
  border-collapse: collapse;
}

.comparison-table th,
.comparison-table td {
  border: 1px solid #ddd;
  padding: 0.5rem;
  text-align: center;
}

.comparison-table th {
  background-color: #f5f5f5;
  font-weight: bold;
}

.comparison-table td:first-child {
  font-weight: bold;
  background-color: #fafafa;
} 

.stars {
display: flex;
gap: 5px;
color: #28a745;
}

</style>
