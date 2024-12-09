<template>
  <div>
    <button @click="goBack" class="back-button"><img src="/images/뒤로가기.png" alt="돋보기 아이콘" width="30"
        height="30"></button>
    <!-- 연간 분석 섹션 -->
    <div id="cesiumContainer" style="width:100%; height: 100vh;"></div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

const courseData = ref(JSON.parse(route.query.course));

const goBack = () => router.back();

/* global Cesium */
onMounted(() => {
  // Cesium.js 로딩
  const script = document.createElement('script');
  script.src = 'https://cdn.jsdelivr.net/npm/cesium@1.122.0/Build/Cesium/Cesium.js';

  script.onload = () => {
    initCesium();
  };
  document.head.appendChild(script);

  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = 'https://cesium.com/downloads/cesiumjs/releases/1.95/Build/Cesium/Widgets/widgets.css';
  document.head.appendChild(link);

  async function initCesium() {
    // Cesium Ion 토큰 설정
    Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI5Nzg1OWEzOS0yMWU0LTQ1MTEtOGY5NC1hZjg0YmFhNzgxMDMiLCJpZCI6MjU2MDAxLCJpYXQiOjE3MzE4OTgxMzB9.oNztRJnt40X1wN0JAS90SaLqSFEZIdQBSE7jFqLeH3g';

    // Cesium 뷰어 초기화
    const viewer = new Cesium.Viewer('cesiumContainer', {
      terrainProvider: await Cesium.createWorldTerrainAsync(),  // 고정밀 지형 데이터 사용
      // 로고와 저작권 정보 숨기기
      creditContainer: document.createElement('div'), // 빈 div로 대체
      // 시계와 타임라인 숨기기
      animation: false,

      // 상단 도구 모음 버튼 숨기기
      navigationHelpButton: false,
      homeButton: false,
      sceneModePicker: false,
      baseLayerPicker: false,
      fullscreenButton: false,
      infoBox: false, // 정보 상자 비활성화 (필요 시)
      geocoder: false // 검색 창 숨기기
    });

    const geoJsonData = await fetchGeoJson();
    const filteredData = filterGeoJsonByPMNTN_NM(geoJsonData, courseData.value.courseName);

    displayGeoJson(viewer, filteredData);

    // 경로를 따라 마커 이동 애니메이션 시작
    animateMarker(viewer, filteredData);

    loadMarkers(viewer,"/data/2023산악사고_인왕산2.geojson","/images/danger.png");
  }

  async function fetchGeoJson() {
    const response = await fetch('/data/new북한산2.geojson');
    const data = await response.json();
    return data;
  }

  function filterGeoJsonByPMNTN_NM(geoJsonData, searchKeyword) {
    return {
      ...geoJsonData,
      features: geoJsonData.features.filter(feature =>
        feature.properties.PMNTN_NM.includes(searchKeyword)
      )
    }
  }

  function displayGeoJson(viewer, geoJsonData) {
    viewer.dataSources.add(Cesium.GeoJsonDataSource.load(geoJsonData, {
      stroke: Cesium.Color.fromCssColorString('#00ff00'),
      fill: Cesium.Color.fromCssColorString('#00ff00'),
      strokeWidth: 3,
      clampToGround: true,
    })).then((dataSource) => {
      viewer.flyTo(dataSource);
    });
  }

  function animateMarker(viewer, geoJsonData) {
    let coordinates = [];

    // 좌표를 하나의 배열로 만들기
    geoJsonData.features.forEach((feature) => {
        if (Array.isArray(feature.geometry.coordinates)) {
            feature.geometry.coordinates.forEach(line => {
                if (Array.isArray(line)) {
                    coordinates = coordinates.concat(line);
                }
            });
        }
    });

    if (coordinates.length === 0) {
        console.warn("No valid coordinates found for animation.");
        return;
    }

    // 시작 시간과 종료 시간 설정
    const start = Cesium.JulianDate.now();
    const stop = Cesium.JulianDate.addSeconds(start, coordinates.length, new Cesium.JulianDate());

    // SampledPositionProperty를 사용하여 시간에 따른 위치 설정
    const positionProperty = new Cesium.SampledPositionProperty();

    // 타입에 따라 카메라 시점 설정
    let cameraOffset = new Cesium.Cartesian3(-500, -1200, 1000); // 기본값
    let heading = 30; // 기본값
    let pitch = -25; // 기본값

    if (courseData.value?.courseName === '마루') {
        cameraOffset = new Cesium.Cartesian3(-500, -1200, 1000);
        heading = 30;
        pitch = -25;
    } else if (courseData.value?.courseName === '부암동') {
        cameraOffset = new Cesium.Cartesian3(300, 100, 300);
        heading = 250;
        pitch = -20;
    } else if (courseData.value?.courseName === '무악동') {
        cameraOffset = new Cesium.Cartesian3(50, -200, 330);
        heading = -20;
        pitch = -20;
    } else if (courseData.value?.courseName === '홍제동') {
        cameraOffset = new Cesium.Cartesian3(-150, -200, 330);
        heading = 50;
        pitch = -20;
    }

    // 좌표마다 시간을 추가하여 마커가 부드럽게 이동하도록 함
    coordinates.forEach(([longitude, latitude], i) => {
        const time = Cesium.JulianDate.addSeconds(start, i, new Cesium.JulianDate());
        const position = Cesium.Cartesian3.fromDegrees(longitude, latitude);
        positionProperty.addSample(time, position); // 시간에 따른 위치 샘플 추가
    });

    // 마커를 경로에 맞춰 시간에 따라 이동시키기
    const entity = viewer.entities.add({
        position: positionProperty,
        point: {
            pixelSize: 20,
            color: Cesium.Color.WHITE,
            outlineColor: Cesium.Color.fromCssColorString('#ffa500'),
            outlineWidth: 3, // 테두리 두께
            heightReference: Cesium.HeightReference.CLAMP_TO_GROUND, // 지형에 고정
        }
    });

    viewer.clock.onTick.addEventListener(() => {
        const position = entity.position.getValue(viewer.clock.currentTime);
        if (position) {
            // 카메라의 목표 좌표계를 엔터티 위치로 변환
            const transform = Cesium.Transforms.eastNorthUpToFixedFrame(position);

            // 일정한 거리와 각도로 카메라 설정
            viewer.camera.lookAtTransform(transform, cameraOffset);

            // 카메라 각도 설정 (필요시 조정 가능)
            viewer.camera.setView({
                orientation: {
                    heading: Cesium.Math.toRadians(heading), // 헤딩
                    pitch: Cesium.Math.toRadians(pitch), // 피치
                    roll: 0 // 롤
                }
            });
        }
    });

    // 시간 설정
    viewer.clock.startTime = start.clone();
    viewer.clock.stopTime = stop.clone();
    viewer.clock.currentTime = start.clone();
    viewer.clock.clockRange = Cesium.ClockRange.LOOP_STOP; // 루프 종료 시 정지
    viewer.clock.multiplier = 30; // 시간 배속
    viewer.clock.shouldAnimate = true; // 애니메이션 활성화
    viewer.timeline.zoomTo(start, stop); // 타임라인 설정
}

});

// 마커 추가
async function loadMarkers(viewer, url, defaultImageSrc) {
    try {
        const response = await fetch(url);
        const markerData = await response.json();

        // 좌표의 고도를 얻기 위해 Cesium의 샘플 지형 함수를 사용
        const positions = markerData.features.map(spot => {
            const lat = spot.geometry.coordinates[1];
            const lng = spot.geometry.coordinates[0];
            return Cesium.Cartographic.fromDegrees(lng, lat);
        });

        // 지형을 샘플링하여 정확한 고도 값을 얻음
        const updatedPositions = await Cesium.sampleTerrainMostDetailed(viewer.terrainProvider, positions);

        updatedPositions.forEach((position, index) => {
            const altitude = position.height + 1.0; // 지형 위로 1m 상승
            const spot = markerData.features[index];

            if(spot.properties.MNTN_NM2 == courseData.value.courseName){
              // 타입에 따라 이미지 설정
            let imageSrc = defaultImageSrc; // 기본 이미지
            switch (spot.properties.type) {
                case '실족':
                    imageSrc = '/images/strumble.png';
                    break;
                case '조난':
                    imageSrc = '/images/distress.png';
                    break;
                case '질환':
                    imageSrc = '/images/disease.png';
                    break;
                case '밧줄':
                    imageSrc = '/images/rope.png';
                    break;
                default:
                    imageSrc = defaultImageSrc; // 기본 이미지 사용
            }

            // 마커 생성
            const entity = viewer.entities.add({
                position: Cesium.Cartesian3.fromDegrees(
                    Cesium.Math.toDegrees(position.longitude),
                    Cesium.Math.toDegrees(position.latitude),
                    altitude
                ),
                billboard: {
                    image: imageSrc,
                    verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
                    width: 40, // 마커의 너비를 픽셀 단위로 설정
                    height: 40, // 마커의 높이를 픽셀 단위로 설정
                },
                label: {
                    text: spot.properties.MNTN_NM || '',
                    font: '14pt sans-serif',
                    outlineColor: Cesium.Color.BLACK,
                    outlineWidth: 2,
                    verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
                    pixelOffset: new Cesium.Cartesian2(0, -20),
                },
                description: `<img src="${imageSrc}" alt="사진" style="width: 150px; height: 100px;"/><br><div style="padding:5px;">${spot.properties.MNTN_NM || ''}<br>${spot.properties.SAFE_SPOT2 || ''}</div>`,
            });

            // 클릭 시 인포윈도우 열기
            viewer.selectedEntityChanged.addEventListener((selectedEntity) => {
                if (Cesium.defined(selectedEntity) && selectedEntity === entity) {
                    viewer.infoBox.viewModel.showInfo = true;
                    viewer.infoBox.viewModel.description = entity.description;
                }
            });
            }

            
        });
    } catch (error) {
        console.error(`${url} 파일 로드 중 에러 발생:`, error);
    }
}
</script>

<style scoped>
#cesiumContainer {
  width: 100%;
  height: 100vh;
}

.back-button {
  position: absolute;
  top: 20px;
  left: 20px;
  background-color: rgba(255, 255, 255);
  color: rgb(12, 12, 12);
  border: none;
  padding: 10px;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  z-index: 10;
  cursor: pointer;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.9);
}
</style>
