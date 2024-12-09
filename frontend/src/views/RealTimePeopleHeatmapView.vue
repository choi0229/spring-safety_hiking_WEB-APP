<template>
  <HeaderView style="z-index: 4;width: 1920px;"></HeaderView>

  <div class="dashboard">
    <!-- 사이드바 -->
    <aside class="sidebar">
      <div class="header">
        <p @click="toggleExtraSidebar" class="header-title">자세히 보기</p>
      </div>

      <div class="section">
        <h3 id="realTimeTraffic">실시간 트래픽 비교</h3>
        <canvas id="trafficChart"></canvas>
      </div>

      <div class="content">
          <p id="contentTitle">코스 이용현황</p>
          <div class="stats-container">
            <div class="section">
              <h3 id="todayPeopleTOP5">금일 유동인구  TOP 4</h3>
              <ul class="station-list">
                <li>
                  <div class="rank">1</div>
                  <div class="station">마루</div>
                  <div class="number">47</div>
                </li>
                <li>
                  <div class="rank">2</div>
                  <div class="station">무악</div>
                  <div class="number">24</div>
                </li>
                <li>
                  <div class="rank">3</div>
                  <div class="station">홍제</div>
                  <div class="number">11</div>
                </li>
                <li>
                  <div class="rank">4</div>
                  <div class="station">부암</div>
                  <div class="number">9</div>
                </li>
              </ul>
            </div>

            <div class="section">
              <h3 id="CoursePeopleTOP5">누적 코스별 유동인구 TOP 5</h3>
              <ul class="station-list">
                <li>
                  <div class="rank">1</div>
                  <div class="station">무악</div>
                  <div class="number">67,969</div>
                </li>
                <li>
                  <div class="rank">2</div>
                  <div class="station">마루</div>
                  <div class="number">52,164</div>
                </li>
                <li>
                  <div class="rank">3</div>
                  <div class="station">홍제</div>
                  <div class="number">23,164</div>
                </li>
                <li>
                  <div class="rank">4</div>
                  <div class="station">부암</div>
                  <div class="number">7,164</div>
                </li>
              </ul>
            </div>
          </div>
        </div>
    </aside>

    <!-- 지도 -->
    <main class="main">
      <div id="map" ref="mapContainer" class="map-container"></div>
    </main>
  </div>
</template>

<script setup>
import HeaderView from '@/components/HeaderView.vue';
import { ref, onMounted } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet.heat';
import { Chart, BarController, BarElement, CategoryScale, LinearScale, Tooltip } from 'chart.js';

// Register required components
Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip);

const mapContainer = ref(null);
let leafletMap = null;
let heatmapLayer = null; // 히트맵 레이어

onMounted(async () => {
  initializeMap();
  const geojsonData = await loadGeoJSON();
  const getjsonData2 = await loadGeoJSON2();
  initializeHeatmap(geojsonData);
  drawRoutesFromGeoJSON(getjsonData2); // 경로를 지도에 추가
  simulateRealTimeHeatmap(geojsonData); // 실시간 히트맵 데이터 시뮬레이션 시작
  initializeTrafficChart();
});
// 실시간 트래픽 데이터 (예제 데이터)
const realTimeTraffic = ref(0);
const averageTraffic = ref(100); // 평균값 초기화

// 실시간 트래픽 차트 초기화
function initializeTrafficChart() {
  const ctx = document.getElementById("trafficChart").getContext("2d");
  const trafficChart = new Chart(ctx, {
    type: "bar",
    data: {
      labels: ["현재 유동 인구", "평균 유동 인구"],
      datasets: [
        {
          label: "유동 인구",
          data: [realTimeTraffic.value, averageTraffic.value],
          backgroundColor: ["#3498db", "#e74c3c"],
          borderColor: ["#2980b9", "#c0392b"],
          borderWidth: 1,
          borderRadius: 5,
        },
      ],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      },
      scales: {
        y: {
          beginAtZero: true,
        },
      },
    },
  });

  // 실시간 업데이트
  setInterval(() => {
    realTimeTraffic.value = Math.floor(Math.random() * 200); // 현재 유동 인구 값 업데이트
    trafficChart.data.datasets[0].data = [
      realTimeTraffic.value,
      averageTraffic.value,
    ];
    trafficChart.update();
  }, 5000); // 5초마다 업데이트
}


// 지도 초기화
function initializeMap() {
  leafletMap = L.map(mapContainer.value, {
    center: [37.634845475, 126.98081674],
    zoom: 15,
    zoomControl: true, // 줌 컨트롤 활성화
    scrollWheelZoom: true, // 마우스 휠로 줌 활성화
    worldCopyJump: true, // 세계 복사 점프 활성화
    zoomAnimation: true, // 줌 애니메이션 활성화
    easeLinearity: 0.25, // 지도 이동 시 부드럽게 움직이도록 설정
    inertia: true, // 관성 이동 활성화
    inertiaDeceleration: 3000, // 관성 감속 설정
  });

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors',
  }).addTo(leafletMap);
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
</script>

<style scoped>
#trafficChart {
  width: 100%;
  height: 200px;
  margin-top: 20px;
}

.section {
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.section h3 {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #34495e;
}

.dashboard {
  display: grid;
  grid-template-columns: 400px 1fr;
  height: 100vh;
  background-color: #f4f5f7;
}

/* 사이드바 */
.sidebar {
  background: #ffffff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.sidebar .header {
  padding: 10px 0;
  margin-bottom: 15px;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.sidebar .content {
  overflow-y: auto;
  flex-grow: 1;
}

.stats-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section {
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  padding: 15px;
}

.section h3 {
  font-size: 22px;
  font-weight: 1000;
  margin-bottom: 10px;
  color: #2c3e50;
}

.station-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.station-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #e6e6e6;
}

.station-list li:last-child {
  border-bottom: none;
}

.station-list .rank {
  font-weight: bold;
  color: #3498db;
}

.station-list .station {
  color: #2c3e50;
  font-weight: 500;
}

.station-list .number {
  font-weight: bold;
  color: #e74c3c;
}

/* 지도 스타일 */
.map-container {
  height: 100%;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

</style>
