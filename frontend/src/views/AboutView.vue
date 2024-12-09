<template>
  <div id="app">
    <div id="map" style="width: 100%; height: 500px;"></div>
    <canvas id="elevationChart" width="400" height="200"></canvas> <!-- 경사도 차트를 그릴 캔버스 -->
  </div>
</template>

<script setup>
/* global kakao */
import { onMounted, ref } from 'vue';
import * as toGeoJSON from '@mapbox/togeojson';
import Chart from 'chart.js/auto'; // Chart.js를 사용하여 차트 그리기

const map = ref(null); // 지도 객체
let elevationData = []; // 고도 데이터를 저장할 배열

onMounted(() => {
  initializeMap(); // Vue가 마운트될 때 지도를 초기화
});

function initializeMap() {
  // Kakao 지도 API 스크립트를 로드
  const script = document.createElement('script');
  script.onload = () => kakao.maps.load(createMap); // 스크립트 로드 후 createMap 호출
  script.src =
    'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=services';
  document.head.appendChild(script);
}

function createMap() {
  const container = document.getElementById('map'); // 지도를 표시할 div
  const options = {
    center: new kakao.maps.LatLng(37.60255081802746, 126.946665543971804), // 지도 중심을 북한산 좌표로 설정
    level: 5, // 지도 확대 레벨
  };
  map.value = new kakao.maps.Map(container, options); // 지도 생성

  map.value.setMapTypeId(kakao.maps.MapTypeId.SKYVIEW);

  // 서버에 있는 GPX 파일을 불러와서 지도에 경로 표시
  loadGPXFromServer('/gpx/북한산.gpx');
}

// 서버에서 GPX 파일을 불러오는 함수
async function loadGPXFromServer(url) {
  try {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const gpxText = await response.text(); // GPX 파일의 텍스트 데이터 가져오기
    const parser = new DOMParser();
    const gpxDOM = parser.parseFromString(gpxText, 'application/xml');

    const geojson = toGeoJSON.gpx(gpxDOM); // GPX 파일을 GeoJSON으로 변환

    drawGPXPath(geojson); // GPX 데이터를 지도에 표시
    drawElevationChart(); // 경사도 차트를 그리기 위해 호출
  } catch (error) {
    console.error('GPX 파일 로드 중 에러 발생:', error);
  }
}

// GPX 경로를 지도에 그리는 함수
function drawGPXPath(geojson) {
  geojson.features.forEach((feature) => {
    if (feature.geometry && feature.geometry.coordinates) {
      const path = feature.geometry.coordinates.map((coord) => {
        elevationData.push(coord[2] || 0); // 고도 데이터를 추출 (고도가 없는 경우 0으로 설정)
        return new kakao.maps.LatLng(coord[1], coord[0]);
      });

      // 지도에 경로(폴리라인) 추가
      const polyline = new kakao.maps.Polyline({
        path: path, // 경로 좌표
        strokeWeight: 5, // 경로 두께
        strokeColor: '#FF0033', // 빨간색
        strokeOpacity: 0.85, // 경로 불투명도
        strokeStyle: 'solid', // 경로 스타일
      });

      polyline.setMap(map.value); // 지도에 경로 표시
    }
  });
}

// 경사도 차트를 그리는 함수
function drawElevationChart() {
  const ctx = document.getElementById('elevationChart').getContext('2d');
  const labels = elevationData.map((_, index) => `${index + 1} km`); // 차트의 x축 데이터 (km 단위로 표시)
  
  new Chart(ctx, {
    type: 'line',
    data: {
      labels: labels, // x축 (거리)
      datasets: [
        {
          label: '고도 (m)',
          data: elevationData, // y축 (고도 데이터)
          borderColor: 'rgba(75, 192, 192, 1)',
          borderWidth: 2,
          fill: false,
        },
      ],
    },
    options: {
      responsive: true,
      scales: {
        x: {
          title: {
            display: true,
            text: '거리 (km)',
          },
        },
        y: {
          title: {
            display: true,
            text: '고도 (m)',
          },
        },
      },
    },
  });
}
</script>

<style scoped>
#map {
  height: 100%;
}

canvas {
  margin-top: 20px;
}
</style>
