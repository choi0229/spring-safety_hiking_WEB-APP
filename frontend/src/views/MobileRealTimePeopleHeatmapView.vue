<template>
  <div id="infomain">
      <div class="row" style="margin: 0px; padding: 0px;">
          <!-- 버튼 그룹 : 시작 -->

          <div class="btn-group" role="group" aria-label="Basic radio toggle button group"
              style="position: fixed; top: -15px;  z-index: 100;">
              <div class="d-flex">
                  <button class="button-item ms-2 mt-1" @click="goBack"
                      style="border-radius: 50%; height: 37px; width: 37px; z-index: 10;"><img src="/images/뒤로가기.png" 
                          alt="돋보기 아이콘" width="17" height="17"></button>

                  <!-- 검색창 추가 -->
                  <div class="review-search" style="top: 30px; z-index: 2;">
                      <div class="search-container">
                          <input type="text" placeholder="검색" v-model="searchQuery" />
                          <button class="search-button">
                              <img src="/images/돋보기white.png" alt="Search" />
                          </button>
                      </div>
                  </div>

                  <div class="dropdown">
                      <a role="button" data-bs-toggle="dropdown">
                          <img src="/images/hamburgerIcon.png" id="hambugermenu" alt="돋보기 아이콘" width="37" height="37">
                      </a>
                      <ul class="dropdown-menu">
                          <li><a class="dropdown-item delay-1" @click="goToCourseInfoViewPage(courseData)">정보</a></li>
                          <li><a class="dropdown-item delay-2" @click="goToCoursePreviewPage()">영상</a></li>
                          <li><a class="dropdown-item delay-3" @click="goToCourse3DPage()"
                                  style="margin-left: 0.009rem;">3D</a></li>
                      </ul>
                  </div>
              </div>
          </div>
          <div id="map" ref="mapContainer" class="map-container"></div>

          <div class="RealTimeCard" style="margin-top: 25rem;">
              <p class="RealTime">{{ currentTime }}</p>
          </div>
      </div>
  </div>

  <!-- 드래그 패널 영역 -->
  <div class="drag-panel" :style="{ top: panelTop + 'px' }">
      <div class="drag-handle" @click="togglePanel"></div>
      <div class="content">
          <p id="contentTitle">실시간 코스 이용 현황</p>

          <!-- Wrapper for both sections -->
          <div class="section-wrapper">
              <!-- 금일 유동 인구 TOP 3 -->
              <div class="section">
                  <div id="todayPeopleTOP5" class="d-flex justify-content-center" style="text-align: center">
                      <h3 id="todayPeopleTOP5-title" style="margin-bottom:0px; color: white;">금일 유동 인구 TOP 3</h3>
                  </div>
                  <ul class="station-list">
                      <li>
                          <div class="rank">1</div>
                          <div class="station">마루</div>
                          <div class="number">47 명</div>
                      </li>
                      <li>
                          <div class="rank">2</div>
                          <div class="station">홍제</div>
                          <div class="number">24 명</div>
                      </li>
                      <li>
                          <div class="rank">3</div>
                          <div class="station">무악</div>
                          <div class="number">11 명</div>
                      </li>
                  </ul>
              </div>

              <!-- 누적 유동 인구 TOP 3 -->
              <div class="section me-3">
                  <div id="CoursePeopleTOP5" class="d-flex justify-content-center" style="text-align: center">
                      <h3 id="CoursePeopleTOP5-title" style="margin-bottom:0px; color: white;">누적 유동 인구 TOP 3</h3>
                  </div>
                  <ul class="station-list">
                      <li>
                          <div class="rank">1</div>
                          <div class="station">무악</div>
                          <div class="number">67,969 명</div>
                      </li>
                      <li>
                          <div class="rank">2</div>
                          <div class="station">마루</div>
                          <div class="number">52,164 명</div>
                      </li>
                      <li>
                          <div class="rank">3</div>
                          <div class="station">부암</div>
                          <div class="number">23,164 명</div>
                      </li>
                  </ul>
              </div>
          </div>
      </div>


      <!-- 바 추가 -->
      <div class="divider" style="height: 1px; background-color: #dcdcdc; margin: 25px 0;"></div>

      <!-- 새로운 차트 영역 추가 -->
      <!-- 평균 등산시간 -->
      <div class="circle-chart-container">
          <p class="chart-title">평균 등산 시간</p>
          <div class="circle-chart">
              <svg viewBox="0 0 36 36" class="circular-chart green">
                  <path class="circle-bg" d="M18 2.0845
          a 15.9155 15.9155 0 0 1 0 31.831
          a 15.9155 15.9155 0 0 1 0 -31.831" />
                  <path class="circle" stroke-dasharray="80, 100" d="M18 2.0845
          a 15.9155 15.9155 0 0 1 0 31.831
          a 15.9155 15.9155 0 0 1 0 -31.831" />
                  <text x="18" y="19" class="chart-value">52분</text>
              </svg>

          </div>
          <div class="chart-info">
              <p>[전일 대비] <span class="down">▼ -0.61%</span></p>
              <p>[전주 대비] <span class="down">▼ -0.56%</span></p>
          </div>
      </div>

      <!-- 바 추가 -->
      <div class="divider" style="height: 1px; background-color: #dcdcdc; margin: 25px 0;"></div>

      <div class="circle-chart-container">
          <p class="chart-title">평균 걸음 속도</p>
          <div class="circle-chart">
              <svg viewBox="0 0 36 36" class="circular-chart blue">
                  <path class="circle-bg" d="M18 2.0845
          a 15.9155 15.9155 0 0 1 0 31.831
          a 15.9155 15.9155 0 0 1 0 -31.831" />
                  <path class="circle" stroke-dasharray="65, 100" d="M18 2.0845
          a 15.9155 15.9155 0 0 1 0 31.831
          a 15.9155 15.9155 0 0 1 0 -31.831" />
                  <text x="18" y="19" class="chart-value">4.2km/h</text>
              </svg>

          </div>
          <div class="chart-info">
              <p>[전일 대비] <span class="down">▼ -0.69%</span></p>
              <p>[전주 대비] <span class="up">▲ +0.3%</span></p>
          </div>
      </div>
  </div>
  <MobileFooterView5 class="footer"></MobileFooterView5>
</template>


<script setup>
import MobileFooterView5 from "@/components/MobileFooterView5.vue";
import { ref, onMounted, nextTick, onUnmounted } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet.heat';
import { useRouter } from 'vue-router';

const router = useRouter();

const currentTime = ref('');

const updateTime = () => {
  const now = new Date();
  // 대한민국 표준 시간대 (Asia/Seoul) 기준으로 시간 포맷팅
  currentTime.value = now.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
  });
};

const mapContainer = ref(null);
let leafletMap = null;
let heatmapLayer = null; // 히트맵 레이어

onMounted(async () => {
  await nextTick();
  initializeMap();
  const geojsonData = await loadGeoJSON();
  const getjsonData2 = await loadGeoJSON2();
  initializeHeatmap(geojsonData);
  drawRoutesFromGeoJSON(getjsonData2); // 경로를 지도에 추가
  simulateRealTimeHeatmap(geojsonData); // 실시간 히트맵 데이터 시뮬레이션 시작
  updateTime(); // 초기 실행
  const timer = setInterval(updateTime, 1000); // 매 초마다 시간 업데이트
  // 컴포넌트가 언마운트되면 타이머 제거
  onUnmounted(() => {
      clearInterval(timer);
  });
});

// 지도 초기화
function initializeMap() {
  leafletMap = L.map(mapContainer.value, {
      zoomControl: false  // 기본 zoomControl을 숨깁니다.
  }).setView([37.622955, 126.977834], 13);

  // TileLayer 추가
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
  }).addTo(leafletMap);

  // zoomControl을 직접 추가하면서 위치를 설정합니다.
  const zoomControl = L.control.zoom({
      position: 'topleft'  // 기본 위치는 오른쪽 상단
  }).addTo(leafletMap);

  // zoomControl의 DOM 요소를 가져와서 CSS로 위치를 변경
  const zoomElement = zoomControl.getContainer();

  // CSS로 중앙 배치 (상단과 하단의 중앙)
  zoomElement.style.position = 'absolute';
  zoomElement.style.top = '100%';  // 세로 가운데
  zoomElement.style.left = '80%'; // 가로 가운데
  zoomElement.style.transform = 'translate(0%, 570%)'; // 정확하게 가운데 정렬
}
async function loadGeoJSON2() {
  try {
      const response = await fetch('/data/인왕산ele copy.geojson'); // GeoJSON 파일 경로
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      return await response.json();
  } catch (error) {
      console.error('GeoJSON 파일 로드 실패:', error);
  }
}

// GeoJSON 데이터를 로드
async function loadGeoJSON() {
  try {
      const response = await fetch('/data/인왕산ele copy.geojson'); // GeoJSON 파일 경로
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      return await response.json();
  } catch (error) {
      console.error('GeoJSON 파일 로드 실패:', error);
  }
}

// 히트맵 초기화
function initializeHeatmap(geojsonData) {
  const heatmapData = extractHeatmapDataFromGeoJSON(geojsonData);
  heatmapLayer = L.heatLayer(heatmapData, {
      radius: 25,
      blur: 15,
      maxZoom: 17,
  }).addTo(leafletMap);
}

// GeoJSON에서 히트맵 데이터를 추출
function extractHeatmapDataFromGeoJSON(geojsonData) {
  const heatmapData = [];
  geojsonData.features.forEach((feature) => {
      const { type, coordinates } = feature.geometry;
      if (type === 'LineString') {
          coordinates.forEach(([lng, lat]) => {
              heatmapData.push([lat, lng, 0.5]); // 강도값 0.5
          });
      } else if (type === 'MultiLineString') {
          coordinates.flat().forEach(([lng, lat]) => {
              heatmapData.push([lat, lng, 0.5]);
          });
      }
  });
  return heatmapData;
}

// GeoJSON에서 경로를 지도에 그리기
function drawRoutesFromGeoJSON(geojsonData) {
  geojsonData.features.forEach((feature) => {
      const { type, coordinates } = feature.geometry;

      if (type === 'LineString') {
          const latLngs = coordinates.map(([lng, lat]) => [lat, lng]);
          L.polyline(latLngs, {
              color: 'blue',
              weight: 4,
              opacity: 0.8,
          }).addTo(leafletMap);
      } else if (type === 'MultiLineString') {
          coordinates.forEach((line) => {
              const latLngs = line.map(([lng, lat]) => [lat, lng]);
              L.polyline(latLngs, {
                  color: 'blue',
                  weight: 4,
                  opacity: 0.8,
              }).addTo(leafletMap);
          });
      }
  });
}

// 실시간 히트맵 데이터 시뮬레이션
function simulateRealTimeHeatmap(geojsonData) {
  const allCoordinates = extractHeatmapDataFromGeoJSON(geojsonData).map(([lat, lng]) => ({
      lat,
      lng,
  }));

  // 이동 객체 생성
  const movingObjects = createMovingObjects(allCoordinates, 200); // 50명의 이동 객체

  // 히트맵 데이터 업데이트 함수
  function updateHeatmap() {
      const heatmapData = movingObjects.map((obj) => {
          obj.updatePosition();
          return [obj.currentPosition.lat, obj.currentPosition.lng, 0.7]; // 강도값 0.7
      });

      if (heatmapLayer) {
          heatmapLayer.setLatLngs(heatmapData);
      }
  }

  // 1초마다 히트맵 갱신
  setInterval(updateHeatmap, 100);
}

// 이동 객체 생성 함수
function createMovingObjects(coordinates, count) {
  return Array.from({ length: count }, () => {
      const startIndex = Math.floor(Math.random() * coordinates.length);
      const isReverse = Math.random() > 0.5;

      return {
          currentPosition: coordinates[startIndex],
          currentIndex: startIndex,
          isReverse,
          updatePosition() {
              this.currentIndex += this.isReverse ? -1 : 1;
              if (this.currentIndex < 0) {
                  this.currentIndex = coordinates.length - 1;
              } else if (this.currentIndex >= coordinates.length) {
                  this.currentIndex = 0;
              }
              this.currentPosition = coordinates[this.currentIndex];
          },
      };
  });
}

const panelTop = ref(window.innerHeight - 289); // 초기 위치 (예: 289 아래)
const isPanelOpen = ref(false);

const togglePanel = () => {
  if (isPanelOpen.value) {
      panelTop.value = window.innerHeight - 650; // 원래 위치로 돌아감
  } else {
      panelTop.value = 0; // 화면 상단으로 이동
  }
  isPanelOpen.value = !isPanelOpen.value; // 상태 변경
};

const goBack = () => {
  console.log('뒤로가기')
  router.back(); // 이전 페이지로 이동
};
</script>

<style scoped>
@font-face {
  font-family: 'TheJamsil5Bold';
  src: url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2') format('woff2');
  font-weight: 700;
  font-style: normal;
}

.section-wrapper {
  display: flex;

}

/* 전체적인 레이아웃 스타일 */
#infomain {
  position: relative;
  background-color: #f5f5f5;
  font-family: Arial, sans-serif;
  height: 100vh;
  /* 화면 전체를 차지하게 */
}

/* 버튼 그룹 스타일 */
.btn-group {
  flex-direction: row;
  align-items: left;
  margin: 0%;
  padding: 0%;
  margin-top: 12%;
}

.button-item {
  background-color: #ffffff;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.3s ease;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
}

.button-item img {
  width: 30px;
  height: 30px;
}

.button-item:hover {
  background-color: #e03e00;
}

/* 드롭다운 메뉴 */
.dropdown {
  align-items: right;
  position: fixed;
  top: 0;
  right: 0;
  margin-top: 2rem;
  margin-right: 2.8rem;
}

#hambugermenu {
  background-color: white;
  border-radius: 50%;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
  position: absolute;
}

.dropdown-menu {
  background-color: #fff;
  border: 1px solid #ddd;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-20px);
  transition: opacity 0.3s, visibility 0s 0.3s, transform 0.3s ease;
  margin-top: 10px;
}

.dropdown-menu.show {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
  transition: opacity 0.3s, visibility 0s 0s, transform 0.3s ease;
}

.dropdown-item {
  padding: 10px 15px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.dropdown-item:hover {
  background-color: #f0f0f0;
}

/* 지도 컨테이너 */
#map {
  width: 100%;
  height: 100vh;
  position: relative;
  z-index: 10;
}

/* 범례 스타일 */
.map-legend {
  position: absolute;
  top: 15%;
  right: 30px;
  background-color: rgba(255, 255, 255, 0.85);
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  z-index: 200;
  font-size: 14px;
  font-family: 'TheJamsil4Bold';
  color: #333;
}

.legend-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.legend-icon {
  width: 15px;
  height: 15px;
  margin-right: 10px;
  border-radius: 50%;
}

.legend-item span {
  font-size: 14px;
  color: #333;
}

/* 드래그 패널 스타일 */
.drag-panel {
  position: fixed;
  /* 패널이 화면 전체를 기준으로 움직이도록 설정 */
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #ffffff;
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
  transition: top 0.3s ease-in-out;
  /* 부드럽게 위아래 이동 */
  z-index: 3000;
  /* 지도 위에 표시되도록 설정 */
  overflow-y: auto;
  max-height: calc(100vh - 70px);
  /* 화면 상단 여백 제외하고 최대 높이 */
  box-shadow: 0 -2px 5px rgba(0, 0, 0, 0.3);
}

.drag-panel::-webkit-scrollbar {
  display: none;
}

.drag-handle {
  width: 40px;
  height: 4px;
  background-color: #ddd;
  border-radius: 2px;
  margin: 10px auto;
  cursor: pointer;
}

.content {
  margin-top: 20px;
}

/* 통계 섹션 스타일 */
.stat-section1 p {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.stats-container {
  display: flex;
  justify-content: space-between;
}

.section {
  flex: 1;
  /* Equal width for both sections */
  text-align: center;
  border: 1px solid #ccc;
  /* Optional: Add border for better visual distinction */
  padding: 5px;
  margin-left: 4%;
  border-radius: 5px;
  background-color: #f9f9f9;
  /* Optional: Background color for better visibility */
}

h3 {
  font-size: 16px;
  color: #444;
  margin-bottom: 15px;
}

ul.station-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.station-list li {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.rank {
  font-size: 16px;
  font-weight: bold;
  color: #FF4500;
  width: 30px;
}

.station {
  font-size: 16px;
  color: #333;
  flex-grow: 1;
  text-align: left;
}

.station-list {
  list-style: none;
  /* Remove default bullet points */
  padding: 0;
  margin: 0;
}

.number {
  font-size: 16px;
  color: #888;
  text-align: right;
}

/* 오버레이 및 팝업 */
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  opacity: 0;
  animation: fadeIn 0.8s forwards;
}

.popup-content {
  background-color: #fff;
  padding: 2em;
  border-radius: 20px;
  max-width: 500px;
  max-height: 1000px;
  overflow-y: auto;
  width: 90%;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
  transform: scale(0.8);
  animation: scaleIn 0.8s forwards;
}

#popupTitle {
  margin-top: 2em;
  margin-bottom: 2.5em;
  text-align: center;
  font-family: 'TheJamsil4Bold';
  font-weight: 500;
  font-size: 18px;
}

/* 오버레이 페이드 인 애니메이션 */
@keyframes fadeIn {
  to {
      opacity: 1;
  }
}

/* 팝업 확대 애니메이션 */
@keyframes scaleIn {
  to {
      transform: scale(1);
  }
}

/* 전체 스크롤 방지 */
body.no-scroll {
  overflow: hidden;
}

@font-face {
  font-family: 'TheJamsil3Regular';
  src: url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2') format('woff2');
  font-weight: 600;
  font-style: normal;
}

h1,
h2,
h3,
p {
  /* 특정 요소에 폰트 적용 */
  font-family: 'TheJamsil4Medium', sans-serif;
  font-weight: 700;
}

#contentTitle {
  font-family: 'TheJamsil5Bold', sans-serif;
  font-size: 20px;
  margin-left: 25%;
}

.search-bar {
  position: fixed;
  top: 76px;
  left: 55%;
  transform: translateX(-50%);
  z-index: 200;
  padding: 0 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 65%;
  max-width: 500px;
}

/* 검색창 input 스타일 */
.search-bar input {
  width: 100%;
  height: 34.8px;
  padding: 12px 20px;
  border-radius: 25px;
  border: 1px solid #ffffff;
  font-size: 16px;
  box-sizing: border-box;
  transition: all 0.3s ease;
  background-color: #ffffff;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

/* 포커스 시 테두리 색상과 그림자 추가 */
.search-bar input:focus {
  outline: none;
  border-color: #4CAF50;
  box-shadow: 0 0 10px rgba(76, 175, 80, 0.5);
  background-color: #fff;
}

#CoursePeopleTOP5 {
  background-color: #ff6c1e;
  padding: 5px;
  border-radius: 5px;
  color: white !important;
  /* 흰색 강제 적용 */
}

#todayPeopleTOP5 {
  background-color: #ff6c1e;
  padding: 5px;
  border-radius: 5px;
  color: white !important;
  /* 흰색 강제 적용 */
}


/* 차트 컨테이너 스타일 */
.circle-chart-container {
  display: flex;
  flex-direction: column;
  /* 세로 정렬 */
  align-items: center;
  /* 가로 중앙 정렬 */
  text-align: center;
  /* 텍스트 중앙 정렬 */
  margin-bottom: 10px;
  /* 섹션 간 간격 */
}

.circle-chart {
  position: relative;
  width: 200px;
  height: 200px;
  margin-bottom: 10px;
}

.circle-bg {
  fill: none;
  stroke: #e6e6e6;
  stroke-width: 3.8;
}

.circle {
  fill: none;
  stroke-width: 2.8;
  stroke-linecap: round;
  transform: rotate(-90deg);
  transform-origin: center;
  transition: stroke-dasharray 0.5s;
}

.circular-chart.green .circle {
  stroke: #4caf50;
}

.circular-chart.blue .circle {
  stroke: #2196f3;
}

.chart-value {
  font-family: 'TheJamsil';
  font-weight: 500;
  font-size: 0.35em;
  fill: #333333;
  text-anchor: middle;
  dominant-baseline: middle;

}

.chart-title {
  font-family: 'TheJamsil';
  font-size: 20px;
  font-weight: 400;
  margin-top: 1.3rem;
  text-align: center;
}

.chart-info {
  font-family: 'TheJamsil';
  font-size: 16px;
  font-weight: 300;
  text-align: center;
  margin-top: 1rem;
}

.chart-info p {
  margin: 5px 0;
  font-size: 14px;
}

.down {
  color: red;
}

.up {
  color: blue;
}

.RealTimeCard {
  position: fixed;
  top: 10;
  border: none;
  color: rgb(0, 0, 0);
  margin-top: 21.5rem;
  margin-left: 11rem;
  z-index: 90;
  height: 100px;
}

.RealTime {
  font-family: 'TheJamsil';
  font-size: 23px;
  font-weight: 500;
  color: #ffffff;
  text-shadow: 2px 2px 5px rgba(0, 0, 0, 0.8);
  /* 그림자 추가 */
}

/* 검색창 스타일 */
.review-search input {
  width: 20px;
  padding: 5px;
  border-top-left-radius: 5px;
  border-bottom-left-radius: 5px;
  border: 1px solid #ddd;
}

/* 검색창 스타일 */
.search-container {
  display: flex;
  align-items: center;
  border: none;
  /* 테두리 제거 */
  background-color: transparent;
  /* 배경색 제거 */
  width: 100%;
  /* 부모 요소 기준으로 전체 너비 */
  height: 40px;
  justify-content: center;
  /* 검색창 내부 아이템 중앙 정렬 */
  z-index: 100;
}

/* 검색창 스타일 */
.search-container input {
  flex: 1;
  max-width: 190px;
  /* 검색창의 최대 너비 */
  box-shadow: 0 5px 10px rgba(0, 0, 0, 0.08);
  padding: 0 10px;
  font-size: 16px;
  color: #333;
  outline: none;
  /* 포커스 시 테두리 제거 */
  height: 40px;
  border: solid 1px #868e96;
  z-index: 100;
}

/* 검색 버튼 */
.search-button {
  width: 45px;
  height: 40px;
  background-color: #327C2B;
  /* 버튼 배경색 */
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-top-right-radius: 5px;
  border-bottom-right-radius: 5px;
}

/* 검색 버튼 아이콘 */
.search-button img {
  width: 30px;
  height: 30px;
  filter: brightness(0) invert(1);
}

/* 검색창 placeholder 스타일 */
.search-container input::placeholder {
  color: #aaa;
  font-size: 16px;
}

/* 검색창을 화면 중앙에 정렬 */
.review-search {
  display: flex;
  justify-content: center;
  /* 수평 가운데 정렬 */
  align-items: center;
  /* 수직 가운데 정렬 */
  width: 100%;
  /* 부모 요소 기준 너비 설정 */
  position: fixed;
  /* 화면 고정 */
  top: 20px;
  /* 화면 위쪽에서 20px 아래로 배치 */
  z-index: 20;
  /* 검색창이 위에 보이도록 설정 */
}
</style>