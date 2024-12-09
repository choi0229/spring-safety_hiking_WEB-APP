<template>
  <div id="notification">
    <p v-for="msg in filteredMessages" :key="msg.id">{{ msg.text }}</p>

    <div class="review-search" style="top: 30px;">
        <div class="search-container">
          <input type="text" placeholder="검색" />
          <button class="search-button">
            <img src="/images/돋보기white.png" alt="Search" />
          </button>
        </div>
      </div>
    <div ref="mapContainer" class="map-container">
      <button id="back-button" @click="back_site">
      <img src="/images/뒤로가기.png" alt="뒤로가기">
      </button>

      <button id="zoom-button" @click="Zoom">
      <img src="/images/내위치2.png" alt="Zoom">
    </button>
    </div>

    <div class="toggle-switch-container2" style="z-index: 10;"> <!-- 기존 z-index를 낮춰 설정 -->
        <div class="toggle-switch2">
          <img src="/images/내위치.png" alt="" style="width: 13px; height: auto; margin-right: 5px;"> 내 위치
        </div>
        <div class="toggle-switch2" >
          <img src="/images/SOS.png" alt="" style="width: 13px; height: auto; margin-right: 5px;"> SOS
        </div>
        <div class="toggle-switch2" style=" font-size: 12px; width: 100px;">
          <img src="/images/국가지점번호.png" alt="" style="width: 13px; height: auto; margin-right: 5px;"> 국가지점번호
        </div>
      </div>
   
    <div v-if="!isSOSActive" class="info-panel">
      <div  id="overline"></div>
      <h3 style="position:relative; text-align: center; top: 25px; z-index: 45;">긴급 도움 요청</h3>
      <div style="position: relative; text-align: center;">
      
        <div style="position: relative; top: 130px; font-size: 12px; color: white;">현재 위치가 주변 사람들( 300M 이내 )에게 알림으로 전송됩니다.</div>
      </div>
      <div id="wrap">
          <button v-if="!isSOSActive" class="overlay-button" @click="handleSendNotification">SOS</button>
          <div v-if="!isSOSActive" class="wave -one"></div>
          <div v-if="!isSOSActive" class="wave -two"></div>
          <div v-if="!isSOSActive" class="wave -three"></div>
      </div>
    </div>

    <div v-else class="info-panel">
      <h3 style="position:relative; text-align: center; top: 25px">긴급 도움 요청</h3>
      <div style="font-size: 20px; font-weight: bold; margin-top: 25px; text-align: center; color: white;">SOS 알림  <span style="color: #38b64a;">전송 완료</span>(300M)</div>

        <div class="button-container">
        <div class="button-text-container">
          <button class="action-button" @click="reSendSOS">
            <img src="/images/리콜.png" alt="다시 보내기">
          </button>
          <div class="description-text" style="font-size: 10px; margin-top: 5px; color: white;">알림 보내기</div>
        </div>
        <div class="button-text-container">
          <button class="action-button" @click="callEmergency">
            <img src="/images/119.png" alt="119">
          </button>
          <div class="description-text" style="font-size: 10px; margin-top: 5px; color: white;">119 전화</div>
        </div>
        <div class="button-text-container">
          <button class="action-button" @click="findNearestPoint">
            <img src="/images/국가표지판.png" alt="국가지점번호">
          </button>
          <div class="description-text" style="font-size: 10px; margin-top: 5px; color: white;">
            국가지점번호
          </div>
        </div>
         <!-- 국가지점번호 모달 -->
         <div v-if="isPointInfoVisible" class="info-display">
          <h3>가장 가까운 국가지점번호</h3>
          <p>지점번호: {{ nearestPoint?.label || '알 수 없음' }}</p>
          <p>위도: {{ nearestPoint?.latitude }}</p>
          <p>경도: {{ nearestPoint?.longitude }}</p>
          <p>거리: {{ nearestPoint?.distance?.toFixed(2) || 0 }} km</p>
          <p>방향: {{ nearestPoint?.direction || '알 수 없음' }}</p>
          <button @click="closePointInfo" class="close-button">닫기</button>
        </div>
      </div>
    </div>
    <MobileFooterView4 class="footer"></MobileFooterView4>
  </div>

</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed} from 'vue';
import axios from 'axios';
import MobileFooterView4 from '@/components/MobileFooterView4.vue';

const messages = ref([]);
const userId = ref(localStorage.getItem('userId') || '');
let socket = null;
let reconnectInterval = null;
const connectionError = ref('');
const successMessage = ref('');
let isConnecting = false;
const isModalOpenForSOS = ref(false); // SOS 모달 상태
const isSOSActive = ref(false);

const isPointInfoVisible = ref(false); // 정보 표시 여부
const nearestPoint = ref(null);

// 사용자 위치
const userLocation = ref({ latitude: null, longitude: null });

// SOS 알림 위치
const sosNotificationLocation = ref({ latitude: null, longitude: null });

// 지도 관련 변수
const mapContainer = ref(null);
let map = null;
let userMarkers = {}; // 각 userId에 해당하는 마커 저장 객체
let sosMarkers = []; // SOS 마커 배열

let autoCenterEnabled = true;

const filteredMessages = computed(() => messages.value.filter(msg => msg.text !== userId.value));

// 방향 계산 함수
const calculateDirection = (lat1, lon1, lat2, lon2) => {
  const dLat = lat2 - lat1;
  const dLon = lon2 - lon1;

  if (Math.abs(dLat) > Math.abs(dLon)) {
    return dLat > 0 ? "남쪽" : "북쪽";
  } else {
    return dLon > 0 ? "서쪽" : "동쪽";
  }
};

// 거리 계산 함수 (위도, 경도로 거리 구하기)
const calculateDistance = (lat1, lon1, lat2, lon2) => {
  const R = 6371; // 지구 반지름 (km)
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) *
      Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // 거리 반환 (km)
};

// 가장 가까운 지점 찾기
const findNearestPoint = async () => {
  if (!userLocation.value.latitude || !userLocation.value.longitude) {
    console.warn("사용자의 위치가 유효하지 않습니다:", userLocation.value);
    alert("현재 위치를 찾을 수 없습니다.");
    return;
  }

    // GeoJSON 데이터 가져오기
    const response = await axios.get(`${window.location.origin}/data/국가지점번호2.geojson`);
    const geojsonData = response.data;

    if (!geojsonData || !geojsonData.features || geojsonData.features.length === 0) {
      alert("국가지점번호 데이터를 불러올 수 없습니다.");
      return;
    }

    let closestPoint = null;
    let minDistance = Infinity;

    geojsonData.features.forEach((feature) => {
      const pointLatitude = feature.properties.Y좌표; // Y좌표 (위도)
      const pointLongitude = feature.properties.X좌표; // X좌표 (경도)
      const pointLabel = feature.properties.지점번호; // 지점번호 (다사52273869 형식)

      const distance = calculateDistance(
        userLocation.value.latitude,
        userLocation.value.longitude,
        pointLatitude,
        pointLongitude
      );

      console.log(
        `지점: ${pointLabel}, 거리: ${distance.toFixed(2)} km, 현재 최소 거리: ${minDistance}`
      );

      if (distance < minDistance) {
        minDistance = distance;
        closestPoint = {
          label: pointLabel,
          latitude: pointLatitude,
          longitude: pointLongitude,
          distance,
          direction: calculateDirection(
            userLocation.value.latitude,
            userLocation.value.longitude,
            pointLatitude,
            pointLongitude
          ),
        };
      }
    });

    if (closestPoint) {
    nearestPoint.value = closestPoint; // 가장 가까운 지점 정보 저장
    console.log("가장 가까운 지점 정보:", nearestPoint.value); // 디버그 로그
    isPointInfoVisible.value = true; // 화면에 표시되도록 설정
    console.log("isPointInfoVisible 상태:", isPointInfoVisible.value); // 디버그 로그
  } else {
    alert("가까운 국가지점번호를 찾을 수 없습니다.");
  }
};

const closePointInfo = () => {
  isPointInfoVisible.value = false; // 정보 숨기기
};

// SOS 버튼 클릭 시 위치 정보 업데이트
const handleSendNotification = async () => {
  isSOSActive.value = true; // SOS 활성화
  isModalOpenForSOS.value = true; // 모달 표시

   // SOS 알림 위치를 업데이트
  sosNotificationLocation.value.latitude = userLocation.value.latitude;
  sosNotificationLocation.value.longitude = userLocation.value.longitude;
  
  try {
    // SOS 알림 전송
    await axios.post("http://172.30.1.53:9000/api/sendNotificationToAll", {
      userId: localStorage.getItem("userId"),
      latitude: sosNotificationLocation.value.latitude,
      longitude: sosNotificationLocation.value.longitude,
    });

    // 내 위치로 지도 카메라 이동
    if (map && userLocation.value.latitude && userLocation.value.longitude) {
      const currentPosition = new window.kakao.maps.LatLng(
        userLocation.value.latitude,
        userLocation.value.longitude
      );
      map.setCenter(currentPosition); // 지도 중심 설정
      map.panTo(currentPosition); // 부드럽게 이동
    }
    // 내 위치 SOS 상태로 갱신
    updateUserMarker();
  } catch (error) {
    console.error("모든 사용자에게 SOS 알림 전송 실패:", error);
  }

  sendNotification();
};

onMounted(() => {
  loadKakaoMapScript(() => {
    initializeMap(); // 지도 초기화
  });

  // 위치 추적 및 WebSocket 연결
  startLocationTracking();
  connectWebSocket();
});

const loadKakaoMapScript = (callback) => {
  if (window.kakao && window.kakao.maps) {
    callback();
  } else {
    const script = document.createElement('script');
    script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=clusterer,services';
    script.onload = () => {
      window.kakao.maps.load(callback);
    };
    script.onerror = () => {
      console.error("카카오 지도 API 로드 실패");
    };
    document.head.appendChild(script);
  }
};

// 지도 초기화 함수
const initializeMap = async () => {
  if (mapContainer.value) {
    // 지도 초기화
    map = new window.kakao.maps.Map(mapContainer.value, {
      center: new window.kakao.maps.LatLng(37.5665, 126.9780), // 서울 중심 좌표
      level: 5, // 지도 확대 레벨
    });

    try {
      const response = await axios.get(`${window.location.origin}/data/국가지점번호2.geojson`);
      const geojsonData = response.data;

      if (!geojsonData.features || geojsonData.features.length === 0) {
        console.error("GeoJSON 데이터에 features가 없습니다.");
        return;
      }

      const customMarkers = geojsonData.features.map((feature) => {
        const lat = feature.properties.Y좌표;
        const lng = feature.properties.X좌표;

        const customMarkerContent = document.createElement("div");
        customMarkerContent.className = "custom-marker";
        customMarkerContent.innerHTML = `
          <div class="marker-icon2">
            <img src="/images/국가지점번호.png" alt="Custom Marker" />
            <div class="marker-label">${feature.properties.지점번호 || "정보 없음"}</div>
          </div>
        `;

        const customOverlay = new window.kakao.maps.CustomOverlay({
          position: new window.kakao.maps.LatLng(lat, lng),
          content: customMarkerContent,
          xAnchor: -0.2,
          yAnchor: -0.1,
        });

        customMarkerContent.addEventListener("click", () => {
          const infoWindow = new window.kakao.maps.InfoWindow({
            content: `<div style="padding:5px;">${feature.properties.지점번호 || "위치 정보"}</div>`,
          });
          infoWindow.open(map, customOverlay);
        });

        return customOverlay; // 커스텀 마커 반환
      });

      // 클러스터 생성 및 관리
      const clusterMarkers = [];
      const clusterRadiusBase = 100; // 기본 클러스터링 반경(픽셀)

      const createClusters = () => {
        clusterMarkers.forEach((cluster) => cluster.setMap(null)); // 기존 클러스터 제거
        clusterMarkers.length = 0;

        const clusters = [];
        const clusterRadius = getClusterRadius(map.getLevel()); // 줌 레벨에 따른 클러스터 반경 계산

        customMarkers.forEach((marker) => {
          let addedToCluster = false;

          for (const cluster of clusters) {
            // 두 지점 간의 거리 계산
            const distance = getPixelDistance(map, cluster.center, marker.getPosition());
            if (distance <= clusterRadius) { // 경계 조건 포함
              cluster.markers.push(marker);
              addedToCluster = true;
              break;
            }
          }

          if (!addedToCluster) {
            clusters.push({
              center: marker.getPosition(),
              markers: [marker],
            });
          }
        });

        clusters.forEach((cluster) => {
          if (cluster.markers.length > 1) {
            // 클러스터 마커 생성
            const clusterContent = document.createElement("div");
            clusterContent.className = "cluster-icon"; // CSS 클래스 추가
            clusterContent.textContent = `${cluster.markers.length}`; // 클러스터 개수 표시

            const clusterOverlay = new window.kakao.maps.CustomOverlay({
              position: cluster.center,
              content: clusterContent,
              xAnchor: 0.5,
              yAnchor: 0.5,
            });

            clusterMarkers.push(clusterOverlay);
            clusterOverlay.setMap(map);
          } else {
            cluster.markers[0].setMap(map); // 클러스터에 포함되지 않은 마커 개별 표시
          }
        });
      };

      const updateMarkersVisibility = () => {
        const zoomLevel = map.getLevel();
        if (zoomLevel > 5) {
          // 클러스터 활성화
          customMarkers.forEach((marker) => marker.setMap(null));
          createClusters();
        } else {
          // 개별 마커 활성화
          clusterMarkers.forEach((cluster) => cluster.setMap(null));
          customMarkers.forEach((marker) => marker.setMap(map));
        }
      };

      const getPixelDistance = (map, pos1, pos2) => {
        const proj = map.getProjection();
        const p1 = proj.pointFromCoords(pos1);
        const p2 = proj.pointFromCoords(pos2);
        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
      };

      const getClusterRadius = (zoomLevel) => {
        // 줌 레벨에 따라 반경 조정 (줌 레벨이 낮을수록 클러스터 반경이 커짐)
        return clusterRadiusBase * Math.pow(1, 6 - zoomLevel); // 예: 줌 레벨 7->100px, 레벨 6->200px, ...
      };

      // 이벤트 등록
      window.kakao.maps.event.addListener(map, "zoom_changed", () => {
        updateMarkersVisibility();
      });

      updateMarkersVisibility(); // 초기 상태 업데이트
    } catch (error) {
      console.error("GeoJSON 데이터를 로드할 수 없습니다:", error);
    }
  } else {
    console.error("Map container가 존재하지 않습니다.");
  }
};

// 서버에서 위치 정보 가져오기


// 위치 추적 시작
const startLocationTracking = () => {
  setInterval(async () => {
    try {
      const response = await axios.get(`http://172.30.1.53:9000/api/location`);
      const location = response.data[userId.value]; // 현재 로그인한 사용자의 위치만 가져옴

      if (location?.latitude && location?.longitude) {
        userLocation.value.latitude = location.latitude;
        userLocation.value.longitude = location.longitude;

        console.log("사용자 위치 업데이트:", userLocation.value); // 로깅 추가
        // 내 위치 갱신
        updateUserMarker(userId.value, location.latitude, location.longitude);
      } else {
        console.warn("위치 정보를 찾을 수 없습니다:", location);
      }
    } catch (error) {
      console.error("위치 정보 갱신 실패:", error);
    }
  }, 1000);
};



const updateUserMarker = (userId, latitude, longitude = false ) => {
  if (!map || !latitude || !longitude) return;

  const position = new window.kakao.maps.LatLng(latitude, longitude);

  const markerContent = `
    <div class="marker-icon">
       <img src="${isSOSActive.value ? '/images/SOS.png' : '/images/내위치.png'}">
        <div class="wave2 ${isSOSActive.value ? '-sos-one' : '-one'}"></div>
        <div class="wave2 ${isSOSActive.value ? '-sos-two' : '-two'}"></div>
        <div class="wave2 ${isSOSActive.value ? '-sos-three' : '-three'}"></div>
    </div>`;

  if (!userMarkers[userId]) {
    userMarkers[userId] = new window.kakao.maps.CustomOverlay({
      position,
      content: markerContent,
      xAnchor: 0.5,
      yAnchor: 1,
    });
    userMarkers[userId].setMap(map);
  } else {
    userMarkers[userId].setPosition(position);
    userMarkers[userId].setContent(markerContent);
  }
   // 지도 중심 설정 (내 위치만)
   if (userId === localStorage.getItem('userId') && autoCenterEnabled) {
    map.setCenter(position);
    autoCenterEnabled = false;
  }
};

const updateSOSMarkers = (latitude, longitude) => {
    if (!latitude || !longitude) {
        console.error("유효하지 않은 SOS 위치 데이터입니다.");
        return;
    }

    const sosPosition = new window.kakao.maps.LatLng(latitude, longitude);

    // 기존 SOS 마커 삭제
    sosMarkers.forEach((marker) => marker.setMap(null));
    sosMarkers = [];

    // 새 SOS 마커 생성
    const sosMarkerContent = `
        <div class="marker-icon">
            <img src="/images/SOS.png">
            <div class="wave2 -sos-one"></div>
            <div class="wave2 -sos-two"></div>
            <div class="wave2 -sos-three"></div>
        </div>`;
    const sosMarker = new window.kakao.maps.CustomOverlay({
        position: sosPosition,
        content: sosMarkerContent,
        xAnchor: 0.5,
        yAnchor: 1,
    });

    sosMarker.setMap(map); // 지도에 SOS 마커 추가
    sosMarkers.push(sosMarker);

    // 지도 중심 이동
    map.panTo(sosPosition);
};

// 알림 클릭으로 SOS 마커 추가
const handleNotificationClick = (latitude, longitude) => {
  if (!latitude || !longitude) {
    console.error("유효하지 않은 SOS 위치 데이터입니다.");
    return;
  }

  const sosPosition = new window.kakao.maps.LatLng(latitude, longitude);

  // 동일 위치에 기존 SOS 마커가 있는지 확인 및 제거
  sosMarkers = sosMarkers.filter((marker) => {
    const isSamePosition = marker.getPosition().equals(sosPosition);
    if (isSamePosition) {
      marker.setMap(null); // 기존 마커를 지도에서 제거
    }
    return !isSamePosition; // 남아있는 마커만 필터링
  });

  // 새 SOS 마커 추가
  const sosMarkerContent = `
    <div class="marker-icon">
      <img src="/images/SOS.png">
      <div class="wave2 -sos-one"></div>
      <div class="wave2 -sos-two"></div>
      <div class="wave2 -sos-three"></div>
    </div>`;
  const sosMarker = new window.kakao.maps.CustomOverlay({
    position: sosPosition,
    content: sosMarkerContent,
    xAnchor: 0.5,
    yAnchor: 1,
    zIndex: 100,
  });

  sosMarker.setMap(map); // 지도에 SOS 마커 추가
  sosMarkers.push(sosMarker); // sosMarkers 배열에 추가

  // 지도 중심을 SOS 위치로 이동
  map.panTo(sosPosition);
};


window.showLocationOnMap = (latitude, longitude) => {
  if (latitude && longitude) {
    handleNotificationClick(parseFloat(latitude), parseFloat(longitude));
  }
};

// 지도 중심을 내 위치로 이동하는 함수
function Zoom() {
  if (map && userLocation.value.latitude && userLocation.value.longitude) {
    const currentPosition = new window.kakao.maps.LatLng(
      userLocation.value.latitude,
      userLocation.value.longitude
    );
    map.setCenter(currentPosition); // 내 위치로 중심 이동
    autoCenterEnabled = false; // 버튼을 눌러 이동 후에도 자동 중심은 유지하지 않음
  } else {
    console.error("사용자 위치 정보를 찾을 수 없습니다.");
  }
}

// 다시 보내기 버튼
const reSendSOS = () => {
  isSOSActive.value = true;
  handleSendNotification(); 
};

// 119 가짜 기능
const callEmergency = () => {
  alert("119에 연결 중입니다. 잠시만 기다려 주세요."); 
};

// WebSocket 연결 설정
const connectWebSocket = () => {
  if (isConnecting || (socket && socket.readyState === WebSocket.OPEN)) return;
  isConnecting = true;
  const wsUrl = `ws://172.30.1.53:3000/ws`;

  socket = new WebSocket(wsUrl);

  socket.onopen = () => {
    console.log('WebSocket 연결 성공!');
    clearInterval(reconnectInterval);
    connectionError.value = '';
    isConnecting = false;
    if (userId.value) sendUserId(userId.value);
  };

  socket.onmessage = (event) => {
  try {
    const message = JSON.parse(event.data);

    if (message.type === "location_update") {
      const { userId, latitude, longitude } = message;

      // 내 위치만 갱신
      if (userId === localStorage.getItem('userId')) {
        updateUserMarker(userId, latitude, longitude);
      }
    }

    if (message.type === "sos_notification") {
      const { userId, latitude, longitude } = message;

      // SOS 알림 클릭 시 상대방 위치 표시
      handleNotificationClick(userId, latitude, longitude);
       // SOS 마커 업데이트
       updateSOSMarkers(latitude, longitude);
    }
  } catch (error) {
    console.error("WebSocket 메시지 처리 오류:", error);
  }
};


  socket.onerror = (error) => {
    console.error('WebSocket 에러:', error);
    isConnecting = false;
  };

  socket.onclose = () => {
    console.log('WebSocket 연결이 종료되었습니다. 재연결 시도 중...');
    connectionError.value = 'WebSocket 연결이 종료되었습니다. 재연결 중...';
    isConnecting = false;
    attemptReconnect();
  };
};

// WebSocket 재연결 시도
const attemptReconnect = () => {
  if (!reconnectInterval) {
    reconnectInterval = setInterval(() => {
      connectWebSocket();
    }, 2000);
  }
};

// WebSocket을 통해 userId 전송
const sendUserId = async (userId) => {
  try {
    await axios.post('/api/saveNotificationData', { userId });
  } catch (error) {
    console.error("userId 전송 실패:", error);
  }
};

// 알림 전송
const sendNotification = async (isToAll = false) => {
  isModalOpenForSOS.value = true;
  try {
    const endpoint = '/api/sendNotification';
    const payload = {
      title: '비상 알림',
      body: '비상 알림을 보냈습니다.',
      latitude: userLocation.value.latitude,
      longitude: userLocation.value.longitude,
      notificationType:'send'
    };

    const userId = localStorage.getItem('userId');
    if (!userId) throw new Error("사용자 ID를 찾을 수 없습니다.");
    payload.userId = userId;

    await axios.post(endpoint, payload, { timeout: 5000 });

    successMessage.value = isToAll ? '모든 사용자에게 알림 전송 성공!' : '알림 전송 성공!';
  } catch (error) {
    console.error('알림 전송 실패:', error);
    connectionError.value = '알림 전송 실패: ' + error.message;
  }
};


onBeforeUnmount(() => {
  if (socket) socket.close();
  clearInterval(reconnectInterval);
});

function back_site() {
  window.location.href='/mobilemainview';
}
</script>



<style>
/* 전체 레이아웃 설정 */
#notification {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}

/* 지도 영역 스타일 */
.map-container {
  position: relative;
  width: 100%;
  height: 65%;
  overflow: hidden;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.15);
  background-color: #e0e0e0;
  border: solid 1px rgb(189, 189, 189);
}

/* 기본 스타일 초기화 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#wrap {
  position: absolute;
  bottom: 45px; /* info-panel 내에서 조금 위로 띄운 위치 */
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 20;
}

.overlay-button {
  position: relative;
  width: 90px;
  height: 90px;
  background-color: #d32f2f;
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 1.5em;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 33;
  cursor: pointer;
  box-shadow: 0px 8px 12px rgba(0, 0, 0, 0.25);
  transition: transform 0.2s;
}

.overlay-button:hover {
  transform: scale(1.1);
}

/* 파동 효과 */
.wave {
  position: absolute;
  border: 2.5px solid #d32f2f; /* 두께 증가 및 더 진한 색상 */
  border-radius: 50%;
  width: 80px;
  height: 80px;
  top: 50%;
  left: 50%;
  transform-origin: center;
  transform: translate(-50%, -50%) scale(0);
  animation: drift 1.8s infinite ease-out; /* 더 짧은 애니메이션 지속 시간 */
  z-index: 20;
  opacity: 0.8; /* 더 불투명하게 설정 */
}

.wave.-two {
  border-color: rgba(211, 47, 47, 0.6); /* 투명도 감소로 더 진하게 */
  animation-duration: 3.5s;
}

.wave.-three {
  border-color: rgba(211, 47, 47, 0.4); /* 투명도 감소로 더 진하게 */
  animation-duration: 4s;
}

@keyframes drift {
  from {
    transform: translate(-50%, -50%) scale(0);
    opacity: 1;
  }
  to {
    transform: translate(-50%, -50%) scale(3.5); /* 크기 약간 증가 */
    opacity: 0;
  }
}

/* 추가된 정보 패널 스타일 */
.info-panel {
  position: absolute;
  left: 50%;
  bottom: 55px;
  transform: translate(-50%, 0);
  width: 100%;
  height: 208px;
  background-color: rgba(63, 63, 62, 0.95); /* 약간 투명한 흰색 배경 */
  text-align: center;
  border-radius: 12px; /* 부드러운 모서리 */
  box-shadow: 0px 0px 12px rgba(0, 0, 0, 0.3); /* 진한 그림자 */
  z-index: 10;
  border: solid 0.5px rgb(230, 230, 230);
}

.info-panel h3 {
  color:white;
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 10px;
  position: relative;
  display: inline-block;
}

.info-panel h3::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: -5px;
  width: 100%;
  height: 2px;
  background-color: #d32f2f;
  animation: blink 1s infinite; /* 밑줄 깜빡이는 효과 */
}

.info-panel div {
  font-size: 16px;
  color: #333; /* 짙은 회색 글씨 */
  margin-bottom: 5px;
}


/* 깜빡이는 밑줄 효과 */
@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* 정보 표시 영역 스타일 */
.info-display {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  background-color: rgba(255, 255, 255, 0.95);
  border: 1px solid #ddd;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
  padding: 20px;
  z-index: 100;
  text-align: center;
}

.info-display h3 {
  margin-bottom: 10px;
  color: #d32f2f;
  font-size: 18px;
  font-weight: bold;
}

.info-display p {
  margin: 5px 0;
  color: #333;
  font-size: 14px;
}

.close-button {
  margin-top: 15px;
  padding: 10px 20px;
  background-color: #d32f2f;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

.close-button:hover {
  background-color: #a50000;
}


/* makerImage css */
.custom-marker {
  transform: translate(-50%, -50%); 
  z-index: 50; 
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.15);
}

.sos-marker {
  transform: translate(-50%, -50%); 
  z-index: 50;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.15);
}

.marker-icon {
  width: 16px;
  height: auto;
  border-radius: 50%;
  overflow: hidden;
  z-index: 51; 
  margin-bottom: 1px;
}

.marker-icon img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

/* 기본 wave2 스타일 (SOS 모드가 아닐 때) */
.wave2 {
  position: absolute;
  border-radius: 50%;
  opacity: 0.5;
  width: 30px;
  height: 30px;
  top: 50%;
  left: 50%;
  transform-origin: center;
  transform: translate(-50%, -50%) scale(0);
  animation: drift2 3s infinite ease-out;
  z-index: 52; 
}

.wave2.-one {
  border: 1px solid #27a4f2;
  width: 20px;
  height: 20px;
}

.wave2.-two {
  border: 1px solid #bbe5ff;
  width: 20px;
  height: 20px;
  animation-duration: 4s;
}

.wave2.-three {
  border: 1px solid #0ca1fd;
  width: 20px;
  height: 20px;
  animation-duration: 5s;
}

/* SOS 모드 wave 스타일 */
.wave2.-sos-one {
  position: absolute;
  border: 2px solid #d32f2f;
  border-radius: 50%;
  opacity: 0.5;
  width: 210px;
  height: 210px;
  top: 50%;
  left: 50%;
  transform-origin: center;
  transform: translate(-50%, -50%) scale(0);
  animation: sosWave1 2s infinite ease-out;
  z-index: 52;
}

.wave2.-sos-two {
  border: 2px solid rgba(211, 47, 47, 0.6);
  width: 230px;
  height: 230px;
  opacity: 0.8;
  animation: sosWave2 4s infinite ease-out;
}

.wave2.-sos-three {
  border: 2px solid rgba(211, 47, 47, 0.3);
  width: 250px;
  height: 250px;
  opacity: 0.9;
  animation: sosWave3 5s infinite ease-out;
}

/* wave 애니메이션 */
@keyframes sosWave1 {
  from {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 0.6;
  }
  to {
    transform: translate(-50%, -50%) scale(3);
    opacity: 0;
  }
}

@keyframes sosWave2 {
  from {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 0.5;
  }
  to {
    transform: translate(-50%, -50%) scale(3.5);
    opacity: 0;
  }
}

@keyframes sosWave3 {
  from {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 0.4;
  }
  to {
    transform: translate(-50%, -50%) scale(4);
    opacity: 0;
  }
}

@keyframes drift2 {
  from {
    transform: translate(-50%, -50%) scale(0);
    opacity: 1;
  }
  to {
    transform: translate(-50%, -50%) scale(3);
    opacity: 0;
  }
}

/* 뒤로가기 버튼 */
#back-button {
  position: fixed;
  top: 30px;
  left: 10px;
  z-index: 100;
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
}

#back-button:hover {
  transform: scale(1.1); 
  background-color: rgba(255, 255, 255, 1); 
}

#back-button img {
  width: 20px;
  height: 20px;
}

/* Zoom 버튼 */
#zoom-button {
  position: fixed;
  bottom: 280px;
  right: 20px;
  z-index: 100;
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
}

#zoom-button:hover {
  background-color: rgba(255, 255, 255, 1); 
}

#zoom-button img {
  width: 20px;
  height: 20px;
}

/* 버튼 컨테이너 */
.button-container {
  display: flex;
  justify-content: space-around; /* 버튼 간 동일한 간격 배치 */
  align-items: center;
  margin-top: 10px;
  padding: 0 10px; /* 좌우 패딩 추가 */
}

/* Action 버튼 스타일 (다시 보내기, 119 등) */
.action-button {
  background-color: rgba(255, 255, 255, 0.9); 
  border: none;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0px 4px 10px rgba(0, 0, 0, 0.3); 
  cursor: pointer;
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.action-button:hover {
  transform: scale(1.1); 
  background-color: rgba(255, 255, 255, 1); 
}

.action-button img {
  width: 30px;
  height: 30px;
}


/* 버튼 텍스트 컨테이너 스타일 */
.button-text-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-top: 5px;
}

#overline {
  position: absolute;
  border-top: solid 3px #b3b3b3; /* 검정색 대신 좀 더 부드러운 색상 */
  width: 30px; /* 길이를 약간 늘려 중앙에 적합하게 설정 */
  left: 50%; /* 화면의 수평 중앙에 배치 */
  transform: translateX(-50%); /* 중앙 정렬을 위한 수평 이동 */
  margin-top: 10px; /* 적당한 상단 여백 추가 */
  opacity: 0.8; /* 약간의 투명도를 주어 시각적 효과 */
  border-radius: 25%;
}

/* 클러스터링 */
.cluster-icon {
  width: 50px;
  height: 50px;
  background-color: rgba(34, 197, 94, 0.7); /* 배경색 */
  border-radius: 50%; /* 원형 */
  color: white; /* 텍스트 색상 */
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
  border: 2px solid white; /* 외곽선 */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2); /* 그림자 효과 */
}

.marker-label {
  position: absolute;
  top: 15px; /* 마커 아이콘 아래에 위치 */
  left: 50%; /* 중앙 정렬 */
  transform: translateX(-50%);
  background-color: rgba(255, 255, 255, 0.7); /* 흰색 반투명 배경 */
  padding: 5px 8px; /* 여백 추가 */
  border-radius: 4px; /* 부드러운 모서리 */
  font-size: 12px; /* 텍스트 크기 */
  font-weight: bold; /* 텍스트 강조 */
  color: #333; /* 텍스트 색상 */
  box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.2); /* 그림자 효과 */
  white-space: nowrap; /* 텍스트 줄바꿈 방지 */
  z-index: 10; /* 다른 요소보다 위에 표시 */
  pointer-events: none; /* 라벨 클릭 불가능 */
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

.toggle-switch-container2 {
    position: fixed;
    z-index: 21;
    display: flex;
    justify-content: center;
    gap: 10px;
    margin-top: 2.4rem;
    width: 100%;
    top: 40px;
  }
  
  .toggle-switch2 {
    width: 70px;
    height: 25px;
    background-color: #ffffff;
    border-radius: 15px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-family: 'TheJamsil4Bold';
    font-weight: 400;
    font-size: 13px;
    color: #000;
    transition: background-color 0.3s, color 0.3s;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
    z-index: 15;
    border: solid 1px #868e96;
  }
  
  .toggle-switch2.active {
    background-color: #327C2B; /* 활성화된 상태의 색상 */
    color: #fff; /* 활성화된 상태의 글자 색상 */
  }
  
  .marker-icon2{
    width: 14px;
    height: auto;
  }
</style>
