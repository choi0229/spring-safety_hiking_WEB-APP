<template>
    <HeaderView style="z-index: 4;width: 1920px;"></HeaderView>
    <div class="dashboard">
      <aside class="sidebar">
        <section class="stat-section">
          <div class="search">
            <input type="text" v-model="searchQuery" placeholder="산 검색" />
            <img id="search-img" src="https://s3.ap-northeast-2.amazonaws.com/cdn.wecode.co.kr/icon/search.png" @click="searchPlaces">
          </div>
        </section>
        <section class="chart-section">
          <h2>2024 산악사고 Top3</h2>
          <div v-for="(accident, index) in accidentRank" :key="index">
            <p>{{ index + 1 }}: {{ accident.mountainName }} ({{ accident.countAccident }}건)</p>
          </div>
        </section>
        <div class="chart1-section">
          <h2>산악 사고 유형</h2>
          <canvas id="doughnutChart"></canvas>
        </div>
      </aside>
      <main class="main">
        <div id="map" ref="mapContainer"></div>
        <div id="grapth">
        <section class="bottomchart-section">
          <h2 id="monthly_stat">월별 사고 통계</h2>
          <canvas id="monthlyChart"></canvas>
        </section>
        <div class="bottomchart-section">
          <h2>계절별 사고</h2>
          <canvas id="statsChart"></canvas>
        </div>
      </div>
      </main>
    </div>
  </template>
  
  <script setup>
  import HeaderView from '@/components/HeaderView.vue';
  import { ref, onMounted } from "vue";
  import L from "leaflet";
  import "leaflet/dist/leaflet.css";
  import "leaflet.heat";
  import "leaflet.markercluster";
  import "leaflet.markercluster/dist/MarkerCluster.css";
  import "leaflet.markercluster/dist/MarkerCluster.Default.css";
  import { Chart, registerables } from "chart.js";
  
  delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

  Chart.register(...registerables);
  
  const mapContainer = ref(null);
  const accidentData = ref([]);
  const accidentRank = ref([]);
  const searchQuery = ref("");

  let leafletMap = null;
  let monthlyChartInstance = null;
  let doughnutChartInstance = null;
  let statsChartInstance = null;
  let heatmapLayer = null;
  
  onMounted(async () => {
    try {
      initializeLeafletMap();
      await loadAccidentData();
      
      await loadAccidentListRank();
      loadCharts();
    } catch (error) {
      console.error("초기화 오류:", error);
    }
  });
  
  function initializeLeafletMap() {
    leafletMap = L.map(mapContainer.value).setView([37.658955, 126.977834], 8);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", { maxZoom: 19 }).addTo(leafletMap);
  }
  
  async function loadAccidentData() {
  try {
    const response = await fetch("/data/2023산악사고 copy.geojson"); // GeoJSON 파일 경로
    const geojsonData = await response.json();

    accidentData.value = geojsonData.features.map((feature) => ({
      latitude: feature.geometry.coordinates[1], // 위도
      longitude: feature.geometry.coordinates[0], // 경도
      mountainName: feature.properties.acdnt_place_nm || "알 수 없음", // GeoJSON 속성에서 가져오기
      countAccident: 0,
      accidentType: feature.properties.acdnt_cause_asort_nm || "알 수 없음",
      month: feature.properties.dclr_mnth || 0,
      season: feature.properties.season_se_nm || "알 수 없음",
    }));

    addHeatmapLayer();
    addMarkersToMap(); // 마커 추가
  } catch (error) {
    console.error("사고 데이터 로드 오류:", error);
  }
}

function addHeatmapLayer() {
    if (heatmapLayer) {
      leafletMap.removeLayer(heatmapLayer); // 기존 히트맵 제거
    }
  
    const heatmapData = accidentData.value.map((accident) => [
      accident.latitude,
      accident.longitude,
      0.5, // 히트맵 강도 (값은 조정 가능)
    ]);
  
    heatmapLayer = L.heatLayer(heatmapData, {
      radius: 25, // 히트맵 점의 크기
      blur: 15, // 흐림 효과
      maxZoom: 17,
    }).addTo(leafletMap);
  }
  
  function addMarkersToMap() {
    const markerClusterGroup = L.markerClusterGroup();
  
    accidentData.value.forEach((accident) => {
      const marker = L.marker([accident.latitude, accident.longitude]);
      marker.bindPopup(`
        <b>사고 위치:</b> ${accident.mountainName}<br>
        <b>사고 유형:</b> ${accident.accidentType}
      `);
      markerClusterGroup.addLayer(marker);
    });
  
    leafletMap.addLayer(markerClusterGroup);
  }
  
 // 사고 데이터로 순위 계산
 function loadAccidentListRank() {
  const mountainAccidentCounts = {};
  accidentData.value.forEach((accident) => {
    const mountainName = accident.mountainName || "알 수 없음";
    mountainAccidentCounts[mountainName] =
      (mountainAccidentCounts[mountainName] || 0) + 1;
  });

  accidentRank.value = Object.entries(mountainAccidentCounts)
    .map(([mountainName, count]) => ({ mountainName, countAccident: count }))
    .sort((a, b) => b.countAccident - a.countAccident)
    .slice(0, 3); // Top 3
}

  
  function loadCharts() {
    loadDoughnutChart();
    loadMonthlyChart();
    loadStatsChart();
  }
  
  function loadDoughnutChart() {
  const accidentTypeCount = {};
  accidentData.value.forEach((accident) => {
    accidentTypeCount[accident.accidentType] = (accidentTypeCount[accident.accidentType] || 0) + 1;
  });

  const ctx = document.getElementById("doughnutChart").getContext("2d");
  if (doughnutChartInstance) doughnutChartInstance.destroy();

  doughnutChartInstance = new Chart(ctx, {
    type: "doughnut",
    data: {
      labels: Object.keys(accidentTypeCount),
      datasets: [
        {
          data: Object.values(accidentTypeCount),
          backgroundColor: ["#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0"],
        },
      ],
    },

  });
}
 
  function loadMonthlyChart() {
    const monthCount = Array(12).fill(0);
    accidentData.value.forEach((accident) => {
      if (accident.month > 0) monthCount[accident.month - 1]++;
    });
  
    const ctx = document.getElementById("monthlyChart").getContext("2d");
    if (monthlyChartInstance) monthlyChartInstance.destroy();
  
    monthlyChartInstance = new Chart(ctx, {
      type: "bar",
      data: {
        labels: ["1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"],
        datasets: [
          {
            label: "월별 사고",
            data: monthCount,
            backgroundColor: "rgba(75, 192, 192, 0.6)",
            borderColor: "rgba(75, 192, 192, 1)",
            borderWidth: 1,
          },
        ],
      },
      options: {
        scales: {
          y: { beginAtZero: true },
        },
      },
    });
  }
  
  function loadStatsChart() {
    const seasonCounts = { 봄: 0, 여름: 0, 가을: 0, 겨울: 0 };
    accidentData.value.forEach((accident) => {
      if (seasonCounts[accident.season] !== undefined) seasonCounts[accident.season]++;
    });
  
    const ctx = document.getElementById("statsChart").getContext("2d");
    if (statsChartInstance) statsChartInstance.destroy();
  
    statsChartInstance = new Chart(ctx, {
      type: "bar",
      data: {
        labels: Object.keys(seasonCounts),
        datasets: [
          {
            label: "계절별 사고",
            data: Object.values(seasonCounts),
            backgroundColor: "rgba(153, 102, 255, 0.6)",
            borderColor: "rgba(153, 102, 255, 1)",
            borderWidth: 1,
          },
        ],
      },
      options: {
        scales: {
          y: { beginAtZero: true },
        },
      },
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
  