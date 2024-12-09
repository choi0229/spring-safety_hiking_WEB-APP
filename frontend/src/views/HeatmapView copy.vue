<template>
  <HeaderView style="z-index: 4;width: 1920px;"></HeaderView>

  <div class="dashboard">

    <aside class="sidebar">
      <section class="stat-section">
        <h3>인왕산 사고발생지역</h3>
        <div class="overview-charts">
          <div class="chart2-section">
            <h2>낙석</h2>
            <canvas id="highRiskChart"></canvas>
          </div>
          <div class="chart2-section">
            <h2>추락</h2>
            <canvas id="mediumRiskChart"></canvas>
          </div>
          <div class="chart2-section">
            <h2>산불</h2>
            <canvas id="lowRiskChart"></canvas>
          </div>
        </div>
      </section>

      <section class="chart-section">
        <h2>위험 분포</h2>
        <canvas id="doughnutChart"></canvas>
      </section>

      <section class="chart-section">
        <h2 id="monthly_stat">월별 사고 통계</h2>
        <canvas id="monthlyChart"></canvas>
      </section>
    </aside>

    <main class="main">
      <div id="map" ref="mapContainer" class="map-container"></div>

      <div id="grapth">
        <div class="chart1-section">
          <h2>사고 유형</h2>
          <canvas id="typeChart"></canvas>
        </div>
        <div class="chart1-section">
          <h2>계절별 사고</h2>
          <canvas id="statsChart"></canvas>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import HeaderView from '@/components/HeaderView.vue';
import { ref, onMounted } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet.heat';
import axios from 'axios';
import { Chart, registerables } from 'chart.js';

const mapContainer = ref(null);
Chart.register(...registerables);
const complaintData = ref([]);

onMounted(() => {
  const map = L.map(mapContainer.value).setView([37.658955, 126.977834], 13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
  }).addTo(map);
  
  loadGeoJSONFromServer('/data/인왕산ele copy.geojson', map);
  loadMonthlyChart();
  loadDoughnutChart();
  loadTypeChart();
  loadStatsChart();
  loadHighRiskChart();
  loadMediumRiskChart();
  loadLowRiskChart();
  loadComplaintList(map);
});

async function loadComplaintList(map){
  try{
    const response = await axios.get('/api/complaint/list')
    complaintData.value = response.data;
    const heatmapData = response.data.map(complaint => {
      return[complaint.latitude, complaint.longitude,1]
    })

    L.heatLayer(heatmapData, {
      radius: 25,
      blur: 15,
      maxZoom: 17,
      maxOpacity: 0.5
    }).addTo(map);
  }catch(error){
    console.log(error);
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

// processGeoJSON 함수 수정
function processGeoJSON(geojsonData, map) {
  let allCoordinates = [];

  geojsonData.features.forEach((feature) => {
    if (feature.properties.PMNTN_NM && feature.properties.PMNTN_NM.includes('홍제동','무악동','부암동')) {
      let coordinates = [];

      if (feature.geometry.type === 'MultiLineString') {
        feature.geometry.coordinates.forEach(line => {
          coordinates = line.map((coord) => ({
            lng: coord[0],
            lat: coord[1],
            elevation: feature.properties.DN || 0
          }));
          allCoordinates.push(coordinates); // Wrap the coordinates in an array
        });
      } else if (feature.geometry.type === 'LineString') {
        coordinates = feature.geometry.coordinates.map((coord) => ({
          lng: coord[0],
          lat: coord[1],
          elevation: feature.properties.DN || 0
        }));
        allCoordinates.push([coordinates]); // Wrap the coordinates in an array
      }
      addRouteLayer(allCoordinates, map);
    }
  });
}


function addRouteLayer(allCoordinates, map) {
  // Ensure allCoordinates is an array of arrays
  if (!Array.isArray(allCoordinates)) {
    console.error("allCoordinates is not an array of arrays:", allCoordinates);
    return;
  }

  allCoordinates.forEach(group => {
    if (Array.isArray(group)) {
      const linePath = group.map(coord => [coord.lat, coord.lng]);

      L.polyline(linePath, {
        color: 'blue',
        weight: 5,
        opacity: 0.8
      }).addTo(map);
    } else {
      console.error("Group is not an array:", group);
    }
  });

  // Adjust map view to the bounds of the routes
  const bounds = new L.LatLngBounds(allCoordinates.flat().map(coord => [coord.lat, coord.lng]));
  map.fitBounds(bounds);
}



// Load Doughnut Chart
function loadDoughnutChart() {
  const ctx = document.getElementById('doughnutChart').getContext('2d');
  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['High Risk', 'Medium Risk', 'Low Risk'],
      datasets: [{
        data: [75, 20, 5],
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

function loadHighRiskChart() {
  const ctx = document.getElementById('highRiskChart').getContext('2d');
  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['High Risk'],
      datasets: [{
        data: [75],
        backgroundColor: ['#FF6384'],
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      }
    }
  });
}

// Load Medium Risk Doughnut Chart
function loadMediumRiskChart() {
  const ctx = document.getElementById('mediumRiskChart').getContext('2d');
  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Medium Risk'],
      datasets: [{
        data: [20],
        backgroundColor: ['#36A2EB'],
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      }
    }
  });
}

// Load Low Risk Doughnut Chart
function loadLowRiskChart() {
  const ctx = document.getElementById('lowRiskChart').getContext('2d');
  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Low Risk'],
      datasets: [{
        data: [5],
        backgroundColor: ['#FFCE56'],
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      }
    }
  });
}

// Load Monthly Statistics Chart
async function loadMonthlyChart() {
  try{
    const response = await axios.get('/data/2023산악사고.geojson');
    const geojsonData = response.data;

    const monthCount = {'1':0,'2':0,'3':0,'4':0,'5':0,'6':0,'7':0,'8':0,'9':0,'10':0,'11':0,'12':0};

    geojsonData.features.forEach(feature => {
      const month = feature.properties.dclr_mnth;
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
          data: [monthCount[1],monthCount[2],monthCount[3],monthCount[4],monthCount[5],monthCount[6],monthCount[7],monthCount[8],monthCount[9],monthCount[10],monthCount[11],monthCount[12]],
          backgroundColor: 'rgba(255, 99, 132, 0.2)',
          borderColor: 'rgba(255, 99, 132, 1)',
          borderWidth: 1
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
    console.log(error);
  }
  
}

// Load Type of Accidents Chart
async function loadTypeChart() {
  try{
    const response = await axios.get('/data/2023산악사고.geojson');
    const geojsonData = response.data;

    const acdntNmCount = {사고부상 : 0, '기타 산악':0, '조난·수색' : 0, 질환 : 0};

    geojsonData.features.forEach(feature => {
      const acdnt = feature.properties.acdnt_cause_asort_nm;
      if(acdntNmCount[acdnt] != undefined ){
        acdntNmCount[acdnt]++;
      }
    })

    const ctx = document.getElementById('typeChart').getContext('2d');
    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['사고부상', '기타 산악', '조난·수색', '질환'],
        datasets: [{
          label: '사고유형',
          data: [acdntNmCount.사고부상, acdntNmCount['기타 산악'], acdntNmCount['조난·수색'],acdntNmCount.질환],  // 임시 데이터
          backgroundColor: 'rgba(54, 162, 235, 0.2)',
          borderColor: 'rgba(54, 162, 235, 1)',
          borderWidth: 1
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
  } catch(error){
    console.error('Error loading GeoJSON:', error);
  }
  
  
}

async function loadStatsChart() {
  try {
    const response = await axios.get('/data/2023산악사고.geojson');
    const geojsonData = response.data;

    // Count occurrences of each season
    const seasonCounts = { 봄: 0, 여름: 0, 가을: 0, 겨울: 0 };  

    geojsonData.features.forEach(feature => {
      const season = feature.properties.season_se_nm;
      if (seasonCounts[season] !== undefined) {
        seasonCounts[season]++;
      }
    });

    // Create chart based on season counts
    const ctx = document.getElementById('statsChart').getContext('2d');
    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['봄', '여름', '가을', '겨울'],
        datasets: [{
          label: '계절별 사고',
          data: [seasonCounts.봄, seasonCounts.여름, seasonCounts.가을, seasonCounts.겨울],
          backgroundColor: 'rgba(75, 192, 192, 0.2)',
          borderColor: 'rgba(75, 192, 192, 1)',
          borderWidth: 2,
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
    console.error('Error loading GeoJSON:', error);
  }
}

</script>

<style scoped>
.dashboard {
  display: grid;
  grid-template-columns: 300px 1fr;
  grid-template-rows: auto 1fr auto;
  height: 100vh;
  gap: 10px;
}


.sidebar {
  padding: 15px;
  background-color: #f1f1f1;
  justify-content: center;
}

.overview-charts {
  margin-top: 40px;
}

.chart-section {
  margin-top: 30px;
}

.map-container {
  height: 63vh;
  width: 100%;
  border-radius: 10px;
}

.overview-charts {
  display: flex; /* Use flexbox for horizontal alignment */
  justify-content: space-between; /* Space between the charts */
  margin-top: 10px; /* Optional: adds some space above the charts */
}

#grapth {
  grid-column: 1 / -1;
  background-color: #f1f1f1;
  padding: 10px;
  display: flex;
  justify-content:space-around;
}

.chart1-section {
  margin-top: 20px;
  text-align: center;
}

.chart2-section {
  flex: 1; /* Allow sections to grow equally */
  text-align: center;
}

#highRiskChart,
#mediumRiskChart,
#lowRiskChart {
  width: 80%; /* Set a percentage width to reduce size */
  height: 150px; /* Set a specific height */
  max-width: 300px; /* Optional: limit max width */
  margin: 0 auto; /* Center the chart */
}

#typeChart,
#statsChart {
  width: 100%; /* width를 100%로 설정 */
  max-width: none; /* max-width 제한 제거 */
  height: 20vh;
}

</style>
