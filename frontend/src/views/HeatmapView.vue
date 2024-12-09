<template>
  <HeaderView style="z-index: 4;width: 1920px;"></HeaderView>

  <div class="dashboard">
    <!-- 사이드바 : 시작 -->
    <aside class="sidebar">
      <section class="stat-section">
        <div class="search">
          <input type="text" v-model="searchQuery" placeholder="산 검색" />
          <img id="search-img" src="https://s3.ap-northeast-2.amazonaws.com/cdn.wecode.co.kr/icon/search.png" @click="searchPlaces">
        </div>
      </section>

      <div class="date-filter">
  <label for="start-date">시작 날짜:</label>
  <input type="date" id="start-date" v-model="startDate" />

  <label for="end-date">종료 날짜:</label>
  <input type="date" id="end-date" v-model="endDate" />

  <button class="filter-button" @click="applyDateFilter">필터 적용</button>
</div>

      <section class="chart-section">
        <h2>2024 민원 접수 Top3</h2>
        <div v-for="complaint, index in complaintRank" :key="complaint.mountainName">
          <p>{{ index+1 }} : {{ complaint.mountainName }} {{ complaint.countComplaint }}건</p>
        </div>
      </section>

      <div class="chart1-section">
        <h2>민원 신고 유형</h2>
        <canvas id="doughnutChart"></canvas>
      </div>
    </aside>
    <!-- 사이드바 : 끝 -->

    <main class="main">
      <div id="map" ref="mapContainer" class="map-container"></div>

      <div id="grapth">
        <section class="bottomchart-section">
          <h2 id="monthly_stat">월별 민원 통계</h2>
          <canvas id="monthlyChart"></canvas>
        </section>
        <div class="bottomchart-section">
          <h2>계절별 민원</h2>
          <canvas id="statsChart"></canvas>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
/* global kakao */
import HeaderView from '@/components/HeaderView.vue';
import { ref, onMounted } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet.heat';
import axios from 'axios';
import { Chart, registerables } from 'chart.js';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

Chart.register(...registerables);

const mapContainer = ref(null);
const complaintData = ref([]);
const complaintRank = ref([]);
const searchQuery = ref('');
let leafletMap = null;
let kakaoLoaded = ref(false); // Kakao Maps API 로드 여부 확인

const startDate = ref('');
const endDate = ref('');

onMounted(async () => {
  try {
    await loadKakaoMapsAPI();
    kakaoLoaded.value = true; // Kakao Maps API가 로드되었음을 확인
    initializeLeafletMap();
    loadDataAndCharts();
  } catch (error) {
    console.error("Kakao Maps API 로드 중 오류 발생:", error);
  }
});

function applyDateFilter() {
  if (!startDate.value || !endDate.value) {
    alert("날짜를 선택해주세요.");
    return;
  }

  const filteredComplaints = complaintData.value.filter(complaint => {
    const complaintDate = new Date(complaint.createdAt);
    return complaintDate >= new Date(startDate.value) && complaintDate <= new Date(endDate.value);
  });

  // 필터링된 데이터를 차트와 지도에 적용
  updateChartsAndMap(filteredComplaints);
}

function updateChartsAndMap(filteredComplaints) {
  // 1. 지도 업데이트
  updateMap(filteredComplaints);

  // 2. 도넛 차트 업데이트
  updateDoughnutChart(filteredComplaints);

  // 3. 월별 차트 업데이트
  updateMonthlyChart(filteredComplaints);

  // 4. 계절별 차트 업데이트
  updateStatsChart(filteredComplaints);
}

function updateMap(filteredComplaints) {
  // 기존 히트맵 제거
  leafletMap.eachLayer(layer => {
    if (layer instanceof L.HeatLayer) {
      leafletMap.removeLayer(layer);
    }
  });

  // 새 히트맵 추가
  const heatmapData = filteredComplaints.map(complaint => [
    complaint.latitude,
    complaint.longitude,
    1,
  ]);
  L.heatLayer(heatmapData, { radius: 25, blur: 15, maxZoom: 17, maxOpacity: 0.5 }).addTo(leafletMap);
}

function updateDoughnutChart(filteredComplaints) {
  const complaintTypeCount = { '낙석': 0, '파손': 0, '야생동물': 0 };
  filteredComplaints.forEach(complaint => {
    const type = complaint.complaintType;
    if (complaintTypeCount[type] !== undefined) {
      complaintTypeCount[type]++;
    }
  });

  // 도넛 차트 업데이트
  const doughnutChart = Chart.getChart('doughnutChart');
  if (doughnutChart) {
    doughnutChart.data.datasets[0].data = [
      complaintTypeCount['낙석'],
      complaintTypeCount['파손'],
      complaintTypeCount['야생동물'],
    ];
    doughnutChart.update();
  }
}

function updateMonthlyChart(filteredComplaints) {
  const monthCount = {
    '1': 0, '2': 0, '3': 0, '4': 0, '5': 0, '6': 0,
    '7': 0, '8': 0, '9': 0, '10': 0, '11': 0, '12': 0,
  };
  filteredComplaints.forEach(complaint => {
    const month = new Date(complaint.createdAt).getMonth() + 1;
    if (monthCount[month] !== undefined) {
      monthCount[month]++;
    }
  });

  // 월별 차트 업데이트
  const monthlyChart = Chart.getChart('monthlyChart');
  if (monthlyChart) {
    monthlyChart.data.datasets[0].data = Object.values(monthCount);
    monthlyChart.update();
  }
}

function updateStatsChart(filteredComplaints) {
  const seasonCounts = { 봄: 0, 여름: 0, 가을: 0, 겨울: 0 };

  function getSeason(dateString) {
    const month = new Date(dateString).getMonth() + 1;
    if (month >= 3 && month <= 5) return '봄';
    if (month >= 6 && month <= 8) return '여름';
    if (month >= 9 && month <= 11) return '가을';
    return '겨울';
  }

  filteredComplaints.forEach(complaint => {
    const season = getSeason(complaint.createdAt);
    if (seasonCounts[season] !== undefined) {
      seasonCounts[season]++;
    }
  });

  // 계절별 차트 업데이트
  const statsChart = Chart.getChart('statsChart');
  if (statsChart) {
    statsChart.data.datasets[0].data = Object.values(seasonCounts);
    statsChart.update();
  }
}


function initializeLeafletMap() {
  leafletMap = L.map(mapContainer.value).setView([37.580955, 126.957834], 16);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
  }).addTo(leafletMap);
}

function loadKakaoMapsAPI() {
  return new Promise((resolve, reject) => {
    if (window.kakao && window.kakao.maps && window.kakao.maps.services) {
      resolve(window.kakao);
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=services';
    script.onload = () => {
      kakao.maps.load(() => {
        resolve(window.kakao);
      });
    };
    script.onerror = () => {
      reject(new Error('Kakao Maps API 로드 실패. API 키를 확인하세요.'));
    };
    document.head.appendChild(script);
  });
}

function searchPlaces() {
  if (!kakaoLoaded.value || !window.kakao || !window.kakao.maps || !window.kakao.maps.services) {
    console.error("Kakao Maps API가 완전히 로드되지 않았습니다.");
    return;
  }

  const places = new kakao.maps.services.Places();

  places.keywordSearch(searchQuery.value, (data, status) => {
    if (status === kakao.maps.services.Status.OK) {

      const firstPlace = data[0];
      if (firstPlace) {
        leafletMap.setView([firstPlace.y, firstPlace.x], 19); // Move map view to first place with zoom level 15
      }

    } else {
      console.error("장소 검색에 실패했습니다:", status);
    }
  });
}

async function loadDataAndCharts() {
  await loadComplaintList(leafletMap);
  loadComplaintListRank();
  await loadGeoJSONFromServer('/data/인왕산ele copy.geojson', leafletMap);
  loadMonthlyChart();
  loadDoughnutChart();
  loadStatsChart();
}

async function loadComplaintList(map) {
  try {
    const response = await axios.get('/api/complaint/list');
    complaintData.value = response.data;
    const heatmapData = response.data.map(complaint => [complaint.latitude, complaint.longitude, 1]);
    L.heatLayer(heatmapData, { radius: 25, blur: 15, maxZoom: 17, maxOpacity: 0.5 }).addTo(map);
  } catch (error) {
    console.error('Error loading complaint data:', error);
  }
}

async function loadComplaintListRank() {
  try {
    const response = await axios.get('/api/complaint/rank');
    complaintRank.value = response.data;
  } catch (error) {
    console.error('Error loading complaint rank:', error);
  }
}

async function loadGeoJSONFromServer(url, map) {
  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const geojsonData = await response.json();
    processGeoJSON(geojsonData, map);
  } catch (error) {
    console.error('GeoJSON 파일 로드 중 에러 발생:', error);
  }
}

function processGeoJSON(geojsonData, map) {
  let allCoordinates = [];

  geojsonData.features.forEach((feature) => {
    if (feature.geometry.type === 'MultiLineString' || feature.geometry.type === 'LineString') {
      feature.geometry.coordinates.forEach(line => {
        const lineCoordinates = line.map((coord) => {
          if (coord && coord.length >= 2) {
            return { lng: coord[0], lat: coord[1], elevation: feature.properties.DN || 0 };
          }
          return null;
        }).filter(Boolean);
        allCoordinates.push(lineCoordinates);
      });
    }
  });

  addRouteLayer(allCoordinates, map);
}

function addRouteLayer(allCoordinates, map) {
  if (!Array.isArray(allCoordinates)) {
    console.error("allCoordinates is not an array of arrays:", allCoordinates);
    return;
  }

  allCoordinates.forEach(group => {
    if (Array.isArray(group) && group.length > 0) {
      const linePath = group.map(coord => [coord.lat, coord.lng]);
      L.polyline(linePath, {
        color: 'blue',
        weight: 5,
        opacity: 0.8
      }).addTo(map);
    }
  });


}

function loadDoughnutChart() {
  const complaintTypeCount = { '낙석': 0, '파손': 0, '야생동물': 0 };
  complaintData.value.forEach(complaint => {
    const type = complaint.complaintType;
    if (complaintTypeCount[type] !== undefined) {
      complaintTypeCount[type]++;
    }
  });

  const ctx = document.getElementById('doughnutChart').getContext('2d');
  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['낙석', '파손', '야생동물'],
      datasets: [{
        data: [complaintTypeCount['낙석'], complaintTypeCount['파손'], complaintTypeCount['야생동물']],
        backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56'],
        hoverBackgroundColor: ['#FF6384', '#36A2EB', '#FFCE56']
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: 'top',
        },
      }
    }
  });
}

async function loadMonthlyChart() {
  try {
    const monthCount = {'1':0,'2':0,'3':0,'4':0,'5':0,'6':0,'7':0,'8':0,'9':0,'10':0,'11':0,'12':0};
    complaintData.value.forEach(complaint => {
      const month = new Date(complaint.createdAt).getMonth() + 1;
      if (monthCount[month] !== undefined) {
        monthCount[month]++;
      }
    });

    const ctx = document.getElementById('monthlyChart').getContext('2d');
    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['1', '2', '3', '4', '5', '6', '7', '8','9','10','11','12'],
        datasets: [{
          label: '월별 사고',
          data: Object.values(monthCount),
          backgroundColor: 'rgba(255, 99, 132, 1)',
          borderColor: 'rgba(255, 99, 132, 1)',
          borderWidth: 1,
          borderRadius: 10,
        }]
      },
      options: {
        scales: {
          y: {
            beginAtZero: true
          }
        }
      }
    });
  } catch (error) {
    console.error('Error loading monthly chart:', error);
  }
}

function loadStatsChart() {
  const seasonCounts = { 봄: 0, 여름: 0, 가을: 0, 겨울: 0 };

  function getSeason(dateString) {
    const month = new Date(dateString).getMonth() + 1;
    if (month >= 3 && month <= 5) return '봄';
    if (month >= 6 && month <= 8) return '여름';
    if (month >= 9 && month <= 11) return '가을';
    return '겨울';
  }

  complaintData.value.forEach(complaint => {
    const season = getSeason(complaint.createdAt);
    if (seasonCounts[season] !== undefined) {
      seasonCounts[season]++;
    }
  });

  const ctx = document.getElementById('statsChart').getContext('2d');
  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['봄', '여름', '가을', '겨울'],
      datasets: [{
        label: '계절별 사고',
        data: Object.values(seasonCounts),
        backgroundColor: 'rgba(75, 192, 192, 1)',
        borderColor: 'rgba(75, 192, 192, 1)',
        borderWidth: 2,
        borderRadius: 10,
      }]
    },
    options: {
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  });
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body, html {
  overflow: hidden;
  font-family: 'Roboto', sans-serif;
  background-color: #f7f8fa;
  color: #2c3e50;
}

.dashboard {
  display: grid;
  grid-template-columns: 280px 1fr;
  height: 100%;
}

.sidebar {
  background-color: #ffffff;
  padding: 20px;
  box-shadow: 2px 0px 12px rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  height: 100%;
  overflow-y: auto;
}

.date-filter input[type="date"] {
  appearance: none; /* 브라우저 기본 스타일 제거 */
  -webkit-appearance: none;
  -moz-appearance: none;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 5px 10px;
  font-size: 14px;
  background-color: #f7f8fa;
  color: #333;
  width: 100%;
}

.date-filter input[type="date"]:focus {
  outline: none;
  border-color: #3498db;
}

.date-filter label {
  font-size: 14px;
  font-weight: 600;
  color: #555;
}


.date-filter input[type="date"]:focus {
  outline: none;
  border-color: #3498db;
}

.filter-button {
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 5px;
  transition: background-color 0.3s ease;
}

.filter-button:hover {
  background-color: #2c81ba;
}

.filter-button:active {
  background-color: #1d5c8a;
}

.stat-section h3,
.chart-section h2,
.chart1-section h2 {
  font-size: 1.4em;
  color: #34495e;
  font-weight: 600;
  margin-bottom: 10px;
  padding-left: 5px;
  border-left: 4px solid #3498db;
}

.chart-section p {
  font-size: 1em;
  font-weight: 600;
  margin: 10px 0;
  padding: 15px;
  border-radius: 8px;
  color: #fff;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.chart-section p:nth-child(1) {
  background-color: #ff6b6b;
}

.chart-section p:nth-child(2) {
  background-color: #ffa94d;
}

.chart-section p:nth-child(3) {
  background-color: #4dabf7;
}

.chart-section p:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
}

.bottomchart-section {
  width: 100%;
}

.overview-charts {
  display: flex;
  justify-content: space-between;
  gap: 15px;
  margin-top: 15px;
}

.chart2-section h2 {
  font-size: 0.9em;
  color: #7f8c8d;
  margin-bottom: 5px;
}

#highRiskChart,
#mediumRiskChart,
#lowRiskChart {
  width: 70px;
  height: 70px;
  margin: 0 auto;
}

.main {
  padding: 20px;
  background-color: #fafbfc;
  overflow-y: auto;
}

#map {
  height: 55vh;
  border-radius: 12px;
  box-shadow: 0px 6px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 15px;
}

#grapth {
  display: flex;
  justify-content: space-between;
  gap: 15px;
  background-color: #ffffff;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0px 4px 10px rgba(0, 0, 0, 0.08);
  height: 280px;
}

.chart-section,
.chart1-section {
  width: 100%;
  margin-top: 30px;
}

.chart1-section h2,
.chart-section h2 {
  color: #555;
  font-weight: 600;
  margin-bottom: 8px;
}

.chart1-section canvas,
.chart-section canvas {
  width: 100%;
  height: 180px;
  border-radius: 12px;
  box-shadow: 0px 4px 10px rgba(0, 0, 0, 0.05);
  background-color: #f5f8fa;
}

.search {
  position: relative;
  width: 250px;
}

input {
  width: 100%;
  border: 1px solid #bbb;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
}

#search-img {
  position : absolute;
  width: 17px;
  top: 10px;
  right: 12px;
  margin: 0;
  cursor: pointer;
}

</style>
