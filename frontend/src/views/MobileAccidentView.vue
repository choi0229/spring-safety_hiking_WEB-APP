<template>
    <div id="infomain">
      
      <div class="row" style="margin: 0px; padding: 0px;">   
        <!-- 검색창과 버튼 -->
        <div class="review-search" style="top: 30px; z-index: 2;">
          <div class="search-container">
            <input type="text" placeholder="검색" v-model="searchQuery" />
            <button class="search-button">
              <img src="/images/돋보기white.png" alt="Search" />
            </button>
          </div>
        </div>
        <!-- zoom -->
        <button id="zoom-button" @click="Zoom" style="z-index: 20;">
        <img src="/images/내위치2.png" alt="Zoom">
       </button>
        
        <!-- 버튼 그룹 : 시작 -->
        <div class="btn-group" role="group" aria-label="Basic radio toggle button group" style="position: fixed; top: 30px;  z-index: 21; justify-content: center;">
          <button class="button-item" @click="goBack" style="border-radius: 50%; height: 40px; width: 40px; margin-right: 16em;">
            <img src="/images/뒤로가기.png"  alt="돋보기 아이콘" width="24" height="24"></button>
    
          <div class="custom-dropdown" style="z-index: 100;">
          <a role="button" @click="toggleDropdown" aria-expanded="false">
            <img src="/images/hamburgerIcon.png" id="hambugermenu" alt="햄버거 아이콘" width="40" height="40">
          </a>
          <div class="custom-dropdown-menu" :class="{ show: isMenuOpen }">
            <button class="custom-dropdown-item delay-1" @click="goToCourseInfoViewPage(courseData)">정보</button>
            <button class="custom-dropdown-item delay-2" @click="goToCoursePreviewPage()">영상</button>
            <button class="custom-dropdown-item delay-3" @click="goToCourse3DPage()">3D</button>
            <button class="custom-dropdown-item delay-3" @click="goToBalancePage()">비교</button>
            <button class="custom-dropdown-item delay-3" @click="goToAccidentHeatmap()">사고</button>
            <button class="custom-dropdown-item delay-3" @click="goToRealtimeHeatmap()">실시간</button>
          </div>
        </div>
        </div>
  
  
            
        <div id="map" ref="mapContainer" style="height:450px;">
          
        </div>
        
        <!-- 버튼 그룹 -->
      </div>
    </div>
    
    <!-- 드래그 패널 영역 -->
    <div
    class="drag-panel"
    :style="{ top: panelTop + 'px' }"
    >
    
    <div class="drag-handle" @click="togglePanel"></div>
    <div class="content">
      <section class="chart-section">
            <h2>2024 산악사고 Top3</h2>
            <div v-for="(accident, index) in accidentRank" :key="index">
              <p>{{ index + 1 }}: {{ accident.mountainName }} ({{ accident.countAccident }}건)</p>
            </div>
          </section>
      <!-- 사고 유형 차트 -->
        <section class="chart-section">
        <h2>산악 사고 유형</h2>
        <canvas id="doughnutChart"></canvas>
      </section>

      <!-- 월별 사고 통계 -->
      <section class="chart-section">
        <h2>월별 사고 통계</h2>
        <canvas id="monthlyChart"></canvas>
      </section>

      <!-- 계절별 사고 통계 -->
      <section class="chart-section">
        <h2>계절별 사고 통계</h2>
        <canvas id="statsChart"></canvas>
      </section>
    </div>   
  </div>
    <MobileFooterView2/>
      
        
  </template>
    
    <script setup>
    import { ref, onMounted } from "vue";
    import { useRouter } from "vue-router";
    import L from "leaflet";
    import "leaflet/dist/leaflet.css";
    import "leaflet.heat";
    import "leaflet.markercluster";
    import "leaflet.markercluster/dist/MarkerCluster.css";
    import "leaflet.markercluster/dist/MarkerCluster.Default.css";
    import { Chart, registerables } from "chart.js";
  import MobileFooterView2 from "@/components/MobileFooterView2.vue";
    
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
    const router = useRouter();
  
    let leafletMap = null;
    let monthlyChartInstance = null;
    let doughnutChartInstance = null;
    let statsChartInstance = null;
    let heatmapLayer = null;
    
    const panelTop = ref(window.innerHeight - 289); // 초기 위치 (예: 289 아래)
    const isPanelOpen = ref(false);

    const togglePanel = () => {
if (isPanelOpen.value) {
  panelTop.value = window.innerHeight - 289; // 원래 위치로 돌아감
} else {
  panelTop.value = 0; // 화면 상단으로 이동
}
isPanelOpen.value = !isPanelOpen.value; // 상태 변경
};

const goBack = () => router.back();

// 드롭다운 메뉴 동작
const goToCourseInfoViewPage = (course) => {
  console.log("Navigating with course data:", course); 
  router.push({
    name: 'mobilecourseinfoview',
    query: {
      course: JSON.stringify(course),
    },
  });
};

const courseData = ref(null);

function goToCoursePreviewPage() {
  router.push({ name: 'mobileCoursePreview', query: { course: JSON.stringify(courseData.value) } });
}

function goToCourse3DPage() {
  router.push({ name: 'mobileCourse3d', query: { course: JSON.stringify(courseData.value) }});
}

function goToBalancePage() {
  router.push({ name: 'balance', query: { course: JSON.stringify(courseData.value) }});
}

function goToRealtimeHeatmap() {
  router.push({ name: 'mobilerealtimeheatmap'});
}

function goToAccidentHeatmap() {
  router.push({ name: 'mobileaccident'});
}

const isMenuOpen = ref(false);

const toggleDropdown = () => {
  isMenuOpen.value = !isMenuOpen.value;

  const dropdownMenu = document.querySelector('.custom-dropdown-menu');
  if (isMenuOpen.value) {
    dropdownMenu.classList.add('show');
  } else {
    dropdownMenu.classList.remove('show');
  }
};

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
      leafletMap = L.map(mapContainer.value, {
    center: [37.658955, 126.977834],
    zoom: 12,
    zoomControl: false, // 기본 확대/축소 컨트롤 비활성화
    attributionControl: false // 크레딧 제거
  });
  
  // 타일 레이어 추가 시 maxZoom 설정
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19, // 최대 줌 레벨 지정
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(leafletMap);
  
  // 확대/축소 버튼을 원하는 위치에 추가
  L.control.zoom({ position: 'bottomleft' }).addTo(leafletMap);
  
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
    @font-face {
      font-family: 'TheJamsil5Bold';
      src: url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2') format('woff2');
      font-weight: 600;
      font-style: normal;
    }
  
    
    /* 버튼 그룹 스타일 */
    .button-group {
    position: absolute;
    top: 0;
    right: 0;
    z-index: 1000;
    }
    
    .btn-group-vertical {
      position: absolute;
      font-size: 0.5rem !important;
      top: 100px;
      right: 0;
      z-index: 2;
      width: 85px;
    }

    .btn-group{
      height: 50px;
    }
    
    .button-item {
    width: 45px;
    height: 45px;
    background-color: #ffffff;
    color: #000000;
    border: none;
    font-weight: bold;
    cursor: pointer;
    font-size: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
    border-radius: 0; /* 기본 라운드 제거 */
    }
    
    /* 첫 번째 버튼에 위쪽만 라운드 적용 */
    .first-button {
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    }
    
    /* 중간 버튼 경계선 스타일 적용 */
    .middle-button {
    border-top: 1px solid #d3d3d3; /* 위쪽 경계선만 살짝 회색 */
    }
    
    /* 마지막 버튼에 아래쪽만 라운드 적용 */
    .last-button {
    border-bottom-left-radius: 30px;
    border-bottom-right-radius: 30px;
    }
    
    .drag-panel {
      position: fixed; /* 패널이 화면 전체를 기준으로 움직이도록 설정 */
      bottom: 0;
      left: 0;
      right: 0;
      background-color: #ffffff;
      border-top-left-radius: 20px;
      border-top-right-radius: 20px;
      transition: top 0.3s ease-in-out; /* 부드럽게 위아래 이동 */
      z-index: 3000; /* 지도 위에 표시되도록 설정 */
      overflow-y: auto;
      max-height: calc(100vh - 80px); /* 화면 상단 여백 제외하고 최대 높이 */
      box-shadow: 0 -2px 5px rgba(0, 0, 0, 0.3);
    }
    .drag-panel::-webkit-scrollbar{
      display:none;
    }
    
    .drag-handle {
    width: 20px;
    height: 2px;
    background-color: #ccc;
    margin: 10px auto;
    border-radius: 3px;
    cursor: pointer;
    }
    
    .content {
    padding: 5px;
    overflow: auto;
    
    /* height: calc(100vh - 100px); */
    }
    
    .card-container {
    display: flex;
    flex-direction: column;
    gap: 10px;
    }
    
    .mobileInfor{
    display: row;
    align-items: center;
    background-color: #f9f9f9;
    border: 1px solid #e0e0e0;
    border-radius: 10px;
    padding: 10px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
    }
    
    .mobileInfor:hover {
      transform: scale(1);
      cursor:pointer;
    }
    
    .card-image {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    margin-right: 10px;
    }
    
    .card-content {
    text-align: left;
    }
    
    #map {
    height: 600px;
    width: 100%;
    position: relative;
    z-index: 1;
    }

    .leaflet-control {
z-index: 1000 !important; /* Leaflet 컨트롤 버튼이 항상 위로 보이도록 설정 */
}
    
    #courseinfo{
      margin-left: 1.6rem;
      margin-top: -1.8rem;
    }
    
    #courseName{
      font-family: 'TheJamsil5Bold';
      font-weight: 700;
      font-size: 20px;
    }
    
    #courseAverage{
      font-family: 'TheJamsil3Bold';
      font-weight: 300;
      font-size: 12px;
      margin-top: -1rem;
    }
    
    canvas {
    height: 150px;
    width: 100%;
    
    }
    
    #sideInformationBar {
    background-color: #f9f9f9;
    }
    
    .day-buttons {
    display: flex;
    justify-content: center;
    margin-bottom: 15px;
    }
    
    /* 날씨 예보 관련 스타일 */
    .weather-forecast {
    display: flex;
    flex-direction: column;
    align-items: center;
    max-width: 100%;
    margin-top: 1rem
    }
    
    .forecast-header {
    text-align: center;
    margin-bottom: 20px;
    }
    
    .forecast-grid {
    display: grid;
    grid-template-columns: repeat(6, 1fr); /* 5일 날씨에 맞게 5개의 칸으로 */
    gap: 5px;
    width: 100%; /* 부모 요소의 너비를 꽉 채움 */
    }
    
    .forecast-item {
    text-align: center;
    padding: 5px;
    border: 1px solid #ccc;
    border-radius: 10px;
    width: 300px;
    background-color: #f7f7f7; /* 배경 색상 추가로 가독성 향상 */
    box-shadow: 0px 2px 10px rgba(0, 0, 0, 0.1); /* 가벼운 그림자 추가 */
    }
    
    .forecast-date {
    font-weight: bold;
    margin-bottom: 10px; /* 날짜와 온도 사이 간격 추가 */
    }
    
    .forecast-info {
    margin-top: 10px;
    }
    
    .current-temperature {
    font-family: 'TheJamsil5Bold';
    font-weight: 700;
    font-size: 16px;
    color: #333; /* 텍스트 색상을 진하게 */
    }
    
    .weather-icon {
    font-size: 30px; /* 날씨 이모티콘 크기를 키움 */
    margin-top: 10px;
    margin-left: 0.4rem;
    }
    
    .description {
    margin-top: -1.5rem;
    margin-left: 0.45rem;
    font-family: 'TheJamsil4Bold';
    font-weight: 300;
    font-size: 14px;
    color: #333; /* 텍스트 색상을 진하게 */
    }
    
    .temperature-range {
    margin-top: -0.7rem;
    margin-left: 0.45rem;
    }
    
    .sun-info {
    margin-top: 3.6rem;
    margin-left: 2.2rem;
    width: 90%;
    }
    
    #sunrise{
      margin-top: -2.5rem;
      margin-bottom: -0.1rem;
      margin-left: 2.2rem;
      font-family: 'TheJamsil4Bold';
      font-weight: 300;
      font-size: 13px;
    }
    
    #sunset{
      margin-left: 2.2rem;
      margin-top: -3rem;
      margin-bottom: -0.5rem;
      font-family: 'TheJamsil4Bold';
      font-weight: 300;
      font-size: 13px;
    }
    
    .icon-small {
    width: 15px;
    height: 15px;
    vertical-align: middle;
    margin-right: 5px; /* 아이콘과 텍스트 간의 간격 추가 */
    opacity: 0.7;
    margin-top: -0.3rem;
    }
    
    .rain-info {
    display: flex;
    align-items: center;
    height: 10px;
    margin-left: 7.5rem;
    margin-bottom: 1.5rem;
    }
    
    .rain-icon {
    width: 20px;
    height: 20px;
    background-color: rgba(173, 216, 230, 0.5);
    border-radius: 50%;
    position: relative;
    overflow: hidden;
    }
    
    .rain-icon::before {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: var(--rain-level, 0);
    background-color: rgb(78, 130, 182);
    }
    
    .rain-chance {
      font-family: 'TheJamsil5Bold';
      font-weight: 400;
      font-size: 14px;
    }
    
    /* 범례 스타일 */
    .map-legend {
        position: absolute;
        top: 0;
        right: 0;
        margin-top: 23em;
        margin-right: 255px;
        background-color: rgba(255, 255, 255, 0.8); /* 반투명 배경 */
        padding: 5px;
        border-radius: 10px;
        font-size: 12px;
        z-index: 10; /* 지도 위에 표시되도록 설정 */
        font-family: 'TheJamsil4Bold';
        font-weight: 400;
        font-size: 16px;
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
        
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
    
      .map-check1{
        position: fixed;
        top: 0;
        left: 0;
        width: 60px;
        height: 30px;
        margin-top: 2.5em;
        margin-left: 7em;
        border: none;
        background-color: white;
        font-family: 'TheJamsil4Bold';
        font-weight: 400;
        font-size: 16px;
        color: #000;
      }
    
    
      .map-check2{
        position: fixed;
        top: 0;
        left: 0;
        width: 60px;
        height: 30px;
        margin-top: 2.5em;
        margin-left: 12em;
        border:none;
        background-color: white;
        font-family: 'TheJamsil4Bold';
        font-weight: 400;
        font-size: 16px;
        color: #000;
      }
    
    
      /* 드롭다운 애니메이션 스타일 */
    
    #hambugermenu{
      background-color: white;
      border-radius: 50%; /* 왼쪽 위 */
      box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
      margin-left: 4em;
      border: solid 0.5px #868e96;
    }
    
    /* 드롭다운 메뉴 스타일 */
    .custom-dropdown-menu {
    display: flex; /* 버튼들을 가로로 정렬 */
    flex-direction: column; /* 세로로 정렬 */
    align-items: center;
    justify-content: center;
    gap: 5px; /* 버튼 간격 추가 */
    min-width: unset;
    width: auto; /* 메뉴 크기는 버튼 크기에 맞춤 */
    height: auto;
    opacity: 0;
    visibility: hidden;
    transition: opacity 0.3s ease, visibility 0.3s ease, transform 0.3s ease;
    border: none;
    margin-left: 60px;
    background-color: transparent;
    z-index: 50;
  }
  
  /* 드롭다운 메뉴 활성화 상태 */
  .custom-dropdown-menu.show {
    opacity: 1;
    visibility: visible;
    transform: translateY(0);
    z-index: 30;
  }
  
  /* 드롭다운 아이템 버튼 스타일 */
  .custom-dropdown-item {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    width: 45px; /* 버튼 크기 */
    height: 45px;
    background-color: #ffffff; /* 버튼 배경색 */
    border-radius: 50%; /* 버튼을 원형으로 설정 */
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); /* 버튼 그림자 */
    text-align: center;
    cursor: pointer;
    transition: transform 0.3s, opacity 0.3s;
    font-size: 14px;
    font-family: 'TheJamsil4Bold';
    color: black;
    opacity: 0; /* 초기 상태를 투명하게 설정 */
    z-index: 30;
    border: solid 1px #327C2B;
  }
  
  /* 드롭다운 아이템 호버 상태 */
  .custom-dropdown-item:hover {
    transform: scale(1.1); /* 호버 시 버튼 확대 */
    background-color: #327C2B; /* 버튼 배경색 변경 */
    color: white; /* 글자 색 변경 */
    z-index: 30;
  }
  
  /* 버튼 등장 애니메이션 */
  .custom-dropdown-item {
    opacity: 0;
    transform: translateY(-10px);
    animation: dropdownFadeIn 0.4s forwards ease;
    z-index: 30;
  }
  
  /* 각 항목별 딜레이 */
  .delay-1 {
    animation-delay: 0.1s;
  }
  .delay-2 {
    animation-delay: 0.4s;
  }
  .delay-3 {
    animation-delay: 0.5s;
  }
  
  /* 드롭다운 페이드인 애니메이션 */
  @keyframes dropdownFadeIn {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
    
    /* 팝업 콘텐츠 */
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
      animation: scaleIn 0.8s forwards; /* 팝업 확대 애니메이션 */
    }
    
    #popupTitle{
      margin-top:2em;
      margin-bottom: 2.5em;
      text-align: center;
      font-family: 'TheJamsil4Bold';
      font-weight: 500;
      font-size: 18px;
    }
    
    #showHelpButton{
      position: fixed; 
      top: 169px;
      margin-top: 25.2em;
      margin-left: 18.8em;
      z-index: 15;
      border: none;
      border-radius: 50%;
      background-color:#415d37;;
      color: white;
      font-family: 'TheJamsil5Bold';
      font-weight: 500;
      font-size: 13px;
      width: 28px;
      height: 28px;
      box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
    }
    
    
    #closepopup{
      position: fixed;
      top: 0;
      right: 0;
      margin: 2em;
    }
    
    .text{
      align-items: center;
      margin-bottom: 2.5em;
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
    
    .date-selector {
      display: flex;
      justify-content: space-around;
      width: 100%;
      margin-bottom: 10px;
    }
    
    .date-button {
      width: 30px;
      height: 30px;
      border-radius: 50%; /* 버튼을 원형으로 만듦 */
      background-color: #ffffff; /* 기본 색상 */
      border: none;
      font-size: 14px;
      font-family: 'TheJamsil4Bold';
      transition: background-color 0.3s;
    }
    
    .date-button.active {
      background-color: #415d37; /* 선택된 버튼의 색상 */
      color: #fff; /* 선택된 버튼의 글자 색상 */
    }
    
    .forecast-date {
      font-family: 'TheJamsil4Bold';
      font-weight: 500;
      font-size: 17px;
    }
    
    .current-temperature {
      font-family: 'TheJamsil4Bold';
      font-weight: 500;
      font-size: 70px;
      font-weight: bold;
      color: #333; /* 텍스트 색상을 진하게 */
    }
    
    .weather-icon {
      font-size: 50px; /* 날씨 이모티콘 크기를 키움 */
      margin-top: 1rem;
    }
    
    
    .min-temp, .max-temp {
      font-family: 'TheJamsil4Bold';
      font-weight: 300;
      font-size: 13px;
      color:rgb(80, 80, 80);
      margin-top: 0.1rem;
    }
    
    .sun-info{
      font-family: 'TheJamsil3Bold';
      font-weight: 400;
      font-size: 16px;
      color:rgb(130, 130, 130);
    }
    
    .date-button-wrapper {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    
    .day-label {
      font-family: 'TheJamsil4Bold';
      font-size: 12px;
      color: #333;
      margin-bottom: 5px;
    }
    
    #zoom-button {
    position: fixed;
    bottom: 300px;
    right: 20px;
    border: none;
    cursor: pointer;
    background-color: rgba(255, 255, 255, 0.9); 
    width: 40px; 
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%; 
    box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.3);
    transition: transform 0.2s ease, background-color 0.2s ease;
    z-index: 21;
  }
   
  /* 실험 */
  /* 태그 버튼 설정 */
  .tag-buttons {
    display: flex;
    gap: 10px;
    padding:5px;
    justify-content: center;
    background-color: #ffffff;
  }
  
  /* 버튼 스타일 */
  .btn-custom {
    background-color: #ffffff;
    border-radius: 10px;
    border-color: transparent;
  
    color: black;
    font-size: 14px; /* 글자 크기 */
    padding: 1px 8px; /* 높이와 좌우 여백 조정 */
  }
  
  .btn-custom:hover {
    transform: scale(1.05);
    background-color: #84da77;
    cursor: pointer;
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
    border: none; /* 테두리 제거 */
    background-color: transparent; /* 배경색 제거 */
    width: 100%; /* 부모 요소 기준으로 전체 너비 */
    height: 40px;
    justify-content: center; /* 검색창 내부 아이템 중앙 정렬 */
    z-index: 100;
  }
  
  /* 검색창 스타일 */
  .search-container input {
    flex: 1;
    max-width: 190px; /* 검색창의 최대 너비 */
    box-shadow: 0 5px 10px rgba(0, 0, 0, 0.08);
    padding: 0 10px;
    font-size: 16px;
    color: #333;
    outline: none; /* 포커스 시 테두리 제거 */
    height: 40px;
    border: solid 1px #868e96;
    z-index: 100;
  }
  
  /* 검색 버튼 */
  .search-button {
    width: 45px;
    height: 40px;
    background-color: #327C2B; /* 버튼 배경색 */
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
    justify-content: center; /* 수평 가운데 정렬 */
    align-items: center; /* 수직 가운데 정렬 */
    width: 100%; /* 부모 요소 기준 너비 설정 */
    position: fixed; /* 화면 고정 */
    top: 20px; /* 화면 위쪽에서 20px 아래로 배치 */
    z-index: 20; /* 검색창이 위에 보이도록 설정 */
  }
  
  .chart-section {
    padding: 20px;
    padding-top: 15px;
    border-radius: 15px;
    box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.1); /* 부드러운 그림자 */
    margin: 10px auto;
    max-width: 95%; /* 섹션 너비 제한 */
    color: #34495e;
    font-family: 'TheJamsil5Bold';
    font-size: 20px;
  }
  
  .chart-section h2 {
    font-size: 1.5rem;
    color: #2c3e50;
    font-weight: bold;
    text-align: center;
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
  
  
  @media (max-width: 768px) {
    .chart-section h2 {
      font-size: 1.2rem; /* 모바일 환경에서 제목 크기 줄임 */
    }
  
    .chart-section p {
      font-size: 0.9rem; /* 카드 텍스트 크기 줄임 */
      padding: 10px;
    }
  
    .chart-section canvas {
      max-width: 100%; /* 모바일에서 캔버스가 너비를 꽉 채우도록 */
    }
  }
  
    </style>
    
    
    