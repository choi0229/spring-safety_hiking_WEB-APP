<template>
  <div class="course-preview-page">
    <button @click="goBack" class="back-button"><img src="/images/moveback_icon.png"  alt="돋보기 아이콘" width="15" height="15"></button>

    <div class="video-container">
      <video
        ref="videoRef"
        controls
        autoplay
        muted
        @timeupdate="syncCourseWithVideo"
        class="video-player"
      >
        <source :src="courseData.courseVideo" type="video/mp4" />
        브라우저가 비디오 태그를 지원하지 않습니다.
      </video>

      <!-- 작은 지도를 클릭하면 크기 변경 -->
      <div
        id="smallMap"
        :class="['small-map', { 'expanded-map': isMapExpanded }]"
        @click="toggleMapSize"
      ></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';

/* global kakao */
const route = useRoute();
const router = useRouter();
const courseData = ref(JSON.parse(route.query.course));
const videoRef = ref(null);
const smallMap = ref(null);
const routeCoordinates = ref([]);
const currentMarker = ref(null);
const isMapExpanded = ref(false);

const goBack = () => router.back();

// Kakao Maps API
function loadKakaoMapScript() {
  return new Promise((resolve, reject) => {
    if (window.kakao && window.kakao.maps) {
      resolve();
    } else {
      const script = document.createElement('script');
      script.onload = () => kakao.maps.load(resolve);
      script.onerror = reject;
      script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false';
      document.head.appendChild(script);
    }
  });
}

// 작은 지도 초기화 함수 (경로 추가)
async function initializeSmallMap() {
  await nextTick();

  const smallMapElement = document.getElementById('smallMap');
  if (!smallMapElement) {
    console.error("smallMap 요소를 찾을 수 없습니다.");
    return;
  }

  smallMap.value = new kakao.maps.Map(smallMapElement, {
    center: new kakao.maps.LatLng(37.66433293993584, 127.01160029114365),
    level: 5,
  });

  const geoJsonPath = `/data/인왕산ele copy.geojson`;
  await loadGeoJSONFromServer(geoJsonPath, smallMap.value);
}

// 경로 데이터 로드 함수
async function loadGeoJSONFromServer(url, targetMap) {
  try {
    const response = await fetch(url);
    const geojsonData = await response.json();

    let linePath = [];
    let isCourseFound = false;

    geojsonData.features.forEach(feature => {
      if (feature.properties.PMNTN_NM.includes(courseData.value.courseName)) {
        isCourseFound = true;
        if (feature.geometry.type === 'MultiLineString') {
          feature.geometry.coordinates.forEach(line => {
            line.forEach(coord => {
              const latLng = new kakao.maps.LatLng(coord[1], coord[0]);
              linePath.push(latLng);
              routeCoordinates.value.push({ lat: coord[1], lng: coord[0], elevation: feature.properties.DN || 0 });
            });
          });
        } else if (feature.geometry.type === 'LineString') {
          feature.geometry.coordinates.forEach(coord => {
            const latLng = new kakao.maps.LatLng(coord[1], coord[0]);
            linePath.push(latLng);
            routeCoordinates.value.push({ lat: coord[1], lng: coord[0], elevation: feature.properties.DN || 0 });
          });
        }
      }
    });

    if (isCourseFound && linePath.length > 0) {
      const polyline = new kakao.maps.Polyline({
        path: linePath,
        strokeWeight: 5,
        strokeColor: '#FF0000',
        strokeOpacity: 0.8,
      });
      polyline.setMap(targetMap);

      const bounds = new kakao.maps.LatLngBounds();
      linePath.forEach(coord => bounds.extend(coord));
      
      targetMap.setBounds(bounds);
      targetMap.setLevel(targetMap.getLevel() - 2);
    } else {
      console.warn("일치하는 코스가 없거나 유효한 좌표가 없습니다.");
    }
  } catch (error) {
    console.error('GeoJSON 파일 로드 중 에러 발생:', error);
  }
}

// 비디오 시간과 경로 동기화 함수
function syncCourseWithVideo(event) {
  if (!smallMap.value || !routeCoordinates.value.length) return;

  const currentTime = event.target.currentTime;
  const totalDuration = event.target.duration;
  const progress = currentTime / totalDuration;
  const currentRouteIndex = Math.floor(progress * routeCoordinates.value.length);
  highlightRouteOnMap(currentRouteIndex);
}

// 특정 경로 지점 강조 함수
function highlightRouteOnMap(index) {
  if (index >= 0 && index < routeCoordinates.value.length && smallMap.value) {
    const point = routeCoordinates.value[index];

    if (currentMarker.value) {
      currentMarker.value.setMap(null);
    }

    currentMarker.value = new kakao.maps.Marker({
      position: new kakao.maps.LatLng(point.lat, point.lng),
      map: smallMap.value,
    });

    smallMap.value.panTo(new kakao.maps.LatLng(point.lat, point.lng));
  }
}

// 지도의 확대/축소 상태를 토글하는 함수
function toggleMapSize() {
  isMapExpanded.value = !isMapExpanded.value;

  if (isMapExpanded.value && routeCoordinates.value.length > 0) {
    const middleIndex = Math.floor(routeCoordinates.value.length / 2);
    const middlePoint = routeCoordinates.value[middleIndex];
    
    if (middlePoint) {
      smallMap.value.setCenter(new kakao.maps.LatLng(middlePoint.lat, middlePoint.lng));
      smallMap.value.setLevel(3); // 확대할 때 레벨을 낮추어 줍니다 (1~3은 좀 더 확대)
    }
  } else {
    smallMap.value.setLevel(5); // 축소할 때 원래 레벨로 되돌립니다.
  }
}

// 페이지 로드 시 비디오 자동 재생 및 지도 초기화
onMounted(async () => {
  await loadKakaoMapScript();
  await initializeSmallMap();
});
</script>

<style scoped>
.course-preview-page {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
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

.video-container {
  position: relative;
  width: 100px;
  height: 100px;
}

.video-player {
  width: 100%;
  height: 100vh;
  object-fit: cover;
}

.small-map {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 100px;
  height: 100px;
  border: 2px solid #ffffff;
  border-radius: 10px;
  box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.3);
  z-index: 10;
  transition: width 0.3s ease, height 0.3s ease;
  cursor: pointer;
}

.small-map.expanded-map {
  width: 200px;
  height: 200px;
}
</style>
