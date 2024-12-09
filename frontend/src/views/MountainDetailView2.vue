<template>
  <div id="app">
    <HeaderView style="z-index: 4;width: 1920px;"></HeaderView>

    <div class="d-flex">    
      <div class="row" style="margin: 0px; padding: 0px;">
        <div id="map" style="width: 100%; height: 680px; margin: 0px;"></div>
        <canvas id="elevationChart" width="400px" height="100px"></canvas>
      </div>
    </div>

  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import Chart from 'chart.js/auto';
import axios from "axios";

/* global kakao */
const map = ref(null);
const routeCoordinates = ref([]);
let chartInstance = null;
const currentMarker = ref(null);

onMounted(() => {
  initializeMap();
  fetchWeatherData(37.66433293993584, 127.01160029114365);
});

// 내 위치를 가져오는 함수


function initializeMap() {
  // Kakao Maps API 스크립트 로드
  const script = document.createElement('script');
  script.onload = () => kakao.maps.load(() => {
    map.value = new kakao.maps.Map(document.getElementById('map'), {
      center: new kakao.maps.LatLng(37.66433293993584, 127.01160029114365),
      level: 5
    });

    loadGeoJSONFromServer('/data/인왕산ele copy.geojson');

    // 마커 추가 JSON 데이터 로드
    loadMarkers("/data/헬기장spot.geojson" , '/images/helipad.png');
    loadMarkers("/data/벤치spot.geojson", '/images/bench.png'); // 마커 데이터 경로
    loadMarkers("/data/2023산악사고_인왕산.geojson", '/images/danger.png'); // 마커 데이터 경로
  });
  script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false';
  document.head.appendChild(script);
}

async function loadGeoJSONFromServer(url) {
  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const geojsonData = await response.json();
    processGeoJSON(geojsonData);
  } catch (error) {
    console.error('GeoJSON 파일 로드 중 에러 발생:', error);
  }
}

// JSON 데이터를 사용하여 마커 추가
async function loadMarkers(url, imageSrc) {
  try {
    const response = await fetch(url); // JSON 파일에서 마커 정보를 가져오기
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const markerData = await response.json();
    console.log(`${url} 마커 데이터 로드 성공:`, markerData);

    // MNTN_NM이 "북한산", "도봉산", "수리산" 인 항목만 필터링
    const filteredMarkers = markerData.features.filter((feature) => {
      mountainName.value = feature.properties.MNTN_NM;

      return mountainName.value == '북한산_백운대';
    });

    const imageSize = new kakao.maps.Size(35,45);
    const imageOpation = { offset: new kakao.maps.Point(12, 35)};
    const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imageOpation);

    // GeoJSON 데이터의 features 배열에서 마커 추가
    filteredMarkers.forEach((spot) => {
      const lat = spot.geometry.coordinates[1]; // 위도
      const lng = spot.geometry.coordinates[0]; // 경도
      const markerPosition = new kakao.maps.LatLng(lat, lng); // 마커 좌표

      // 마커 생성
      const marker = new kakao.maps.Marker({
        position: markerPosition, // 마커 위치
        map: map.value, // 마커를 표시할 지도 객체
        title: spot.properties.MNTN_NM, // 마커 제목
        image: markerImage,
      });

      // 마커에 대한 정보창 추가
      const infowindow = new kakao.maps.InfoWindow({
        content: `<div style="padding:5px;">${spot.properties.MNTN_NM}<br>${spot.properties.SAFE_SPOT2}</div>`, // 정보창 내용
      });

      // 마커에 마우스오버 이벤트 등록
      kakao.maps.event.addListener(marker, 'mouseover', () => infowindow.open(map.value, marker));
      kakao.maps.event.addListener(marker, 'mouseout', () => infowindow.close());
    });
  } catch (error) {
    console.error(`${url} 파일 로드 중 에러 발생:`, error);
  }
}

// 좌표 그룹화 함수
function groupCoordinates(coordinates, groupSize) {
  const groups = [];
  for (let i = 0; i < coordinates.length; i += groupSize) {
    groups.push(coordinates.slice(i, i + groupSize));
  }
  return groups;
}


// 경사도에 따른 색상 결정 함수
function getColorBySlope(slope) {
  if (slope > 30) return '#FF4500'; // 급경사 (빨강)
  if (slope < -15) return '#1E90FF'; // 급한 내리막 (파랑)
  return '#32CD32'; // 평지에 가까움 (초록)
}



// processGeoJSON 함수 수정
function processGeoJSON(geojsonData) {
  let allCoordinates = [];

  geojsonData.features.forEach((feature) => {
    if (feature.properties.PMNTN_NM && feature.properties.PMNTN_NM.includes('')) {
      let coordinates = [];

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
      routeCoordinates.value = allCoordinates;  // 경로 좌표를 할당
      allCoordinates = allCoordinates.concat(coordinates);
    }
  });
  
  if (allCoordinates.length > 0) {
    const groupedCoordinates = groupCoordinates(allCoordinates, 7); // 5개씩 그룹화
    addRouteLayer(groupedCoordinates);
    drawElevationChart(allCoordinates);
  } else {
    console.log('유효한 구간 데이터가 없습니다.');
  }
}


// addRouteLayer 함수 수정
function addRouteLayer(groupedCoordinates) {
  const baseCoordinates = groupedCoordinates.flat(); // 경로를 평탄하게 만듦
  drawBaseRoute(baseCoordinates); // 기본 녹색 경로 그리기

  groupedCoordinates.forEach((group) => {
    if (group.length > 1) {
      const startPoint = group[0];
      const endPoint = group[group.length - 1];
      const slope = calculateSlope(startPoint, endPoint);
      const color = getColorBySlope(slope);

      const linePath = group.map(coord => new kakao.maps.LatLng(coord.lat, coord.lng));

      const polyline = new kakao.maps.Polyline({
        path: linePath,
        strokeWeight: 5,
        strokeColor: color,
        strokeOpacity: 0.8,
        strokeStyle: 'solid'
      });

      polyline.setMap(map.value);
    }
  });

  // 경로에 맞게 지도 중심과 줌 조정
  const bounds = new kakao.maps.LatLngBounds();
  groupedCoordinates.flat().forEach(coord => bounds.extend(new kakao.maps.LatLng(coord.lat, coord.lng)));
  map.value.setBounds(bounds);
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

function calculateDistance(index, coord, coordinates) {
  if (index === 0) return 0; // 첫 번째 좌표의 거리는 0

  const prevCoord = coordinates[index - 1];

  const R = 6371; // 지구 반지름 (km)
  const dLat = deg2rad(coord[1] - prevCoord[1]);
  const dLng = deg2rad(coord[0] - prevCoord[0]);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(deg2rad(prevCoord[1])) * Math.cos(deg2rad(coord[1])) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // 거리 반환
}

function calculateHaversineDistance(coord1, coord2) {
  const R = 6371e3; // 지구 반지름 (미터 단위)
  const lat1 = deg2rad(coord1.lat);
  const lat2 = deg2rad(coord2.lat);
  const deltaLat = deg2rad(coord2.lat - coord1.lat);
  const deltaLon = deg2rad(coord2.lng - coord1.lng);

  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) *
    Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  // 두 지점 간의 거리 반환 (미터 단위)
  return R * c;
}

function calculateSlope(start, end) {
  // 하버사인 공식을 사용하여 두 지점 간의 수평 거리 (지구 곡률 반영)
  const horizontalDistance = calculateHaversineDistance(
    { lat: start.lat, lng: start.lng },
    { lat: end.lat, lng: end.lng }
  );

  // 고도 차이 계산
  const elevationChange = end.elevation - start.elevation;

  // 피타고라스 정리를 사용해 두 지점 간의 대각선 거리 계산
  const diagonalDistance = Math.sqrt(
    Math.pow(horizontalDistance, 2) + Math.pow(elevationChange, 2)
  );

  // 수평 거리가 너무 짧으면 경사도 계산을 무시하고 기본 경사도 0으로 설정
  if (horizontalDistance < 1) { // 예시: 수평 거리가 10m 미만일 경우
    return 0; // 경사도 0으로 간주
  }

  // 경사도 = 고도 차이 / 대각선 거리 * 100
  return (elevationChange / diagonalDistance) * 100;
}

function deg2rad(deg) {
  return deg * (Math.PI / 180);
}

function drawElevationChart(data) {
  const ctx = document.getElementById('elevationChart').getContext('2d');

  // 누적 거리 계산 (기존의 총 거리를 계산)
  const distances = calculateCumulativeDistances(data.map(point => [point.lng, point.lat]));
  const totalDistance = distances[distances.length - 1]; // 총 거리
  const scaleFactor = 645 / totalDistance; // 645m에 맞추기 위한 스케일링 비율

  // 각 누적 거리를 스케일링
  const scaledDistances = distances.map(distance => distance * scaleFactor);
  const elevations = data.map(point => point.elevation); // y축: 고도

  if (chartInstance) {
    chartInstance.destroy(); // 기존 차트 제거
    chartInstance = null;
  }

  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels: scaledDistances, // x축: 스케일링된 누적 거리
      datasets: [
        {
          label: '고도 (m)',
          data: elevations,  // y축: 고도
          borderColor: 'rgba(75, 192, 192, 1)',
          borderWidth: 2,
          fill: false,
        },
      ],
    },
    options: {
      scales: {
        x: {
          title: {
            display: true,
            text: '거리 (m)', // x축 제목 (미터 단위로 표시)
          },
          ticks: {
            autoSkip: true, // 자동으로 레이블을 생략함
            maxTicksLimit: 10, // 최대 레이블 개수를 10개로 제한
            maxRotation: 0, // 레이블을 비스듬히 하지 않음
            minRotation: 0, // 레이블을 수평으로 유지
            callback: function(value) {
              return value.toFixed(2) + ' m'; // 원하는 형식으로 레이블 포맷
            }
          }
        },
        y: {
          title: {
            display: true,
            text: '고도 (m)', // y축 제목
          },
        },
      },
      interaction: {
        mode: 'index',
        intersect: false,
      },
      plugins: {
        tooltip: {
          enabled:false,
        },
      },
      onClick: (event, elements) => {
        if (elements.length > 0) {
          const index = elements[0].index;
          highlightRouteOnMap(index); // 클릭 시 지도에서 경로 강조
        }
      },
      onHover: (event, elements) => {
        if (elements.length > 0) {
          const index = elements[0].index;
          highlightRouteOnMap(index); // 마우스 오버 시 경로 강조
        }
      },
    },
  });
}

function calculateCumulativeDistances(coordinates) {
  let cumulativeDistances = [];
  let totalDistance = 0;

  for (let i = 0; i < coordinates.length; i++) {
    if (i > 0) {
      totalDistance += calculateDistance(i, coordinates[i], coordinates);
    }
    cumulativeDistances.push(totalDistance);
  }

  return cumulativeDistances;
}


function highlightRouteOnMap(index) {
  if (index >= 0 && index < routeCoordinates.value.length) {
    const point = routeCoordinates.value[index];  // 해당 인덱스의 좌표를 가져옴

    console.log("마우스 오버 인덱스:", index);
    console.log("해당 좌표:", point);

    // 기존 마커 제거
    if (currentMarker.value) {
      currentMarker.value.setMap(null);
    }

    // 새로운 마커 추가
    currentMarker.value = new kakao.maps.Marker({
      position: new kakao.maps.LatLng(point.lat, point.lng),  // 마커 위치 설정
    });

    currentMarker.value.setMap(map.value);  // 지도에 마커 표시

    // 해당 경로로 지도 이동
    map.value.panTo(new kakao.maps.LatLng(point.lat, point.lng));
  } else {
    console.error(`Invalid index: ${index}`);
  }
}



// 날씨
const mountainName = ref("");
const dailyWeather = ref([]);
const sunriseTimes = ref([]);
const sunsetTimes = ref([]);


// 날씨 정보 조회
const fetchWeatherData = async (latitude, longitude) => {
  try {
    const apiKey = "5c48577c775896e979e7bcc3b225b730";
    const response = await axios.get(
      `https://api.openweathermap.org/data/2.5/forecast?lat=${latitude}&lon=${longitude}&appid=${apiKey}`
    );

    console.log(response.data); // 응답 확인용 콘솔 로그

    const groupedByDay = {};
    response.data.list.forEach(item => {
      const date = item.dt_txt.split(" ")[0];
      if (!groupedByDay[date]) groupedByDay[date] = [];
      groupedByDay[date].push(item);
    });

    const selectedWeather = [];
    Object.keys(groupedByDay).forEach(date => {
      const { currentTemp, minTemp, maxTemp } = calculateMinMaxTemp(groupedByDay[date]);
      const daySummary = {
        date,
        currentTemp,
        minTemp,
        maxTemp,
        ...groupedByDay[date][0],
      };
      selectedWeather.push(daySummary);
    });

    dailyWeather.value = selectedWeather;

    const sunriseSunsetPromises = dailyWeather.value.map(day =>
      axios.get(
        `https://api.sunrise-sunset.org/json?lat=${latitude}&lng=${longitude}&date=${day.date}&formatted=0`
      )
    );

    const sunriseSunsetResults = await Promise.all(sunriseSunsetPromises);
    sunriseTimes.value = sunriseSunsetResults.map(result => formatTime(result.data.results.sunrise));
    sunsetTimes.value = sunriseSunsetResults.map(result => formatTime(result.data.results.sunset));

  } catch (error) {
    console.error("Error fetching weather data:", error);
  }
};


const calculateMinMaxTemp = (dayData) => {
  const temps = dayData.map(item => item.main.temp);
  const currentTemp = dayData[0].main.temp;
  const minTemp = Math.min(...temps);
  const maxTemp = Math.max(...temps);
  return { currentTemp, minTemp, maxTemp };
};

// 시간 포맷 변환 함수
const formatTime = (time) => {
  const date = new Date(time);
  return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", timeZone: "Asia/Seoul" });
};

</script>

<style scoped>
#map {
  height: 600px;
  width: 100%;
  position: relative; 
}

canvas {
  height: 200px;
  width: 100%;
}
.weather-forecast {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 100%;
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
  font-size: 30px;
  font-weight: bold;
  color: #333; /* 텍스트 색상을 진하게 */
}

.weather-icon {
  font-size: 30px; /* 날씨 이모티콘 크기를 키움 */
  margin-top: 10px;
}

.description {
  font-size: 16px;
  color: #555;
  margin-top: 5px;
}

.temperature-range {
  margin-top: 5px;
}

.min-temp, .max-temp {
  font-size: 16px;
  color: #555;
}

.sun-info {
  margin-top: 10px;
}

.icon-small {
  width: 16px;
  height: 16px; 
  vertical-align: middle;
  margin-right: 5px; /* 아이콘과 텍스트 간의 간격 추가 */
}

.rain-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.rain-icon {
  width: 20px;
  height: 30px;
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
  background-color: rgba(30, 144, 255, 1);
}

.rain-chance {
  font-size: 14px;
  font-weight: bold;
}

</style>
