<template>
    <MobileHeaderView class="header" />
    <!-- 상단 버튼 그룹 -->
    <div class="btn-group-container">
        <div class="btn-group" role="group" aria-label="Basic radio toggle button group">
            <input type="radio" class="btn-check" name="btnradio" id="btnradio1" autocomplete="off" checked>
            <label class="btn btn-outline-success" for="btnradio1">운동</label>
            <input type="radio" class="btn-check" name="btnradio" id="btnradio2" autocomplete="off" @click="goToRecordImg()">
            <label class="btn btn-outline-success" for="btnradio2">기록</label>
            <input type="radio" class="btn-check" name="btnradio" id="btnradio3" autocomplete="off" @click="goToMyCom()">
            <label class="btn btn-outline-success" for="btnradio3">나의 커뮤니티</label>
        </div>
    </div>
    <!-- 지도 및 주요 콘텐츠 -->
    <MobileHeaderView class="header" />
        <div class="app-main">
            <div class="map-container">
                <div id="map"></div>
                <!-- 가운데 시작 버튼 -->
                <div class="timer-display">
                    <span>{{ formattedHours }}</span>:<span>{{ formattedMins }}</span>:<span>{{ formattedSecs }}</span>
                </div>
                <div class="centered-button">
                    <button @click="toggleTracking" class="start-button">
                    {{ isTracking ? "저장" : "시작" }}
                    </button>
                </div>

                <!-- 하단 목표 설정 버튼 -->
                <button class="goal-button" @click="stopTracking">
                <img src="/images/정지4.png" alt="" style="width: 20px; height: 20px;">
                </button>
            <button class="TTS"><img src="/images/음성.png" style="width: 32px; height: auto;"></button>
            </div>
        </div>
        <MobileFooterView3 class="footer"></MobileFooterView3>

<div class="modal" id="saveModal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="staticBackdropLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><img src="/images/저장.png" alt="" style="width: 20px; height: 20px; margin-right: 3px;">기록 저장</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <p style="border-bottom: solid 1px #333;">등산 기록을 저장하시겠습니까?</p>
        <div class="d-flex flex-column mb-3 fv-row">
            <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
            <span class="required">사진 등록</span>
            </label>
            <input
            type="file"
            id="image"
            @change="onFileChange"
            ref="image"
            accept="image/*"
            />
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" @click="delteRecord()">삭제</button>
        <button type="button" class="btn btn-primary" @click="saveRecord()">저장</button>
      </div>
    </div>
  </div>
</div>


</template>

<script setup>
import MobileFooterView3 from "@/components/MobileFooterView3.vue";
import MobileHeaderView from "@/components/MobileHeaderView.vue";
/* global kakao */
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue';
import axios from 'axios';
import router from '@/router';
import { Modal } from "bootstrap";

// 위치 상태 관리
const latitude = ref(37.641774041520812);
const longitude = ref(126.98750152017325);
const altitude = ref(null);
const speed = ref(null);
const bearing = ref(null);
const time = ref("");
let polyline, map, redDot, whiteDot, BigDot;

// 타이머 관련 상태 관리
let hours = ref(0);
let mins = ref(0); 
let secs = ref(0);
let interval;
let path = reactive([]); // 경로 저장
const isTracking = ref(false); // 경로 기록 상태
const isTrackingComplete = ref(false); // 기록 저장 버튼 활성화 상태

// 음성
const message = ref('30m 앞에 위험 구간입니다. 주의하세요.');
const triggeredMarkers = new Set(); // 경고가 이미 발생한 마커 ID를 저장하는 Set
const isVoiceLoaded = ref(false); // 스크립트 로드 상태 추적

// 지도 초기화 함수
function initializeMap() {
    const script = document.createElement('script');
    script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=clusterer,services';
    document.head.appendChild(script);

    script.onload = () => {
        if (window.kakao && window.kakao.maps) {
            window.kakao.maps.load(() => {
                const mapContainer = document.getElementById('map');
                const mapOption = {
                    center: new kakao.maps.LatLng(latitude.value, longitude.value),
                    level: 3
                };
                map = new kakao.maps.Map(mapContainer, mapOption);

                // 점 및 경로 표시 초기화
                initOverlays();
                initPolyline();
                loadDangerMarkers("/data/2023산악사고_인왕산2.geojson", '/images/danger.png');
                // 지도 로드 후에 위치 업데이트
                if (latitude.value && longitude.value) {
                    console.log('실행');
                    updateMapPosition();
                }
            });
        }
    };
}

// 사용자 위치와 위험 지점 위치를 경로로 추가하여 거리 계산하기
function calculateDistanceUsingPolyline(userPosition, dangerPosition) {
  const linePath = [
    userPosition,
    dangerPosition
  ];
  const polyline = new kakao.maps.Polyline({ path: linePath });
  return polyline.getLength();
}

// 위험 구역 접근 확인 함수
function checkProximityUsingPolyline(userPosition) {
  console.log("Checking proximity for position:", userPosition); // 확인 로그
  dangerMarkers.value.forEach((dangerMarker) => {
    const markerPosition = dangerMarker.getPosition();
    const markerId = `${markerPosition.getLat()},${markerPosition.getLng()}`;

    if (triggeredMarkers.has(markerId)) return;

    const distance = calculateDistanceUsingPolyline(userPosition, markerPosition);
    if (distance <= 30) {
      console.log('위험 구역 접근:', dangerMarker.getTitle());
      speakMessage();
      triggeredMarkers.add(markerId);
    }
  });
}

function loadResponsiveVoice() {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://code.responsivevoice.org/responsivevoice.js?key=VGYcmCsP';
    script.onload = () => {
      isVoiceLoaded.value = true;
      resolve();
    };
    script.onerror = () => reject(new Error('ResponsiveVoice.js 로드 실패'));
    document.head.appendChild(script);
  });
}

// 음성 안내 함수
function speakMessage() {
  if (isVoiceLoaded.value && window.responsiveVoice) {
    const text = message.value;
    const lang = "Korean Female";
    window.responsiveVoice.speak(text, lang);
  } else {
    alert('음성 합성 라이브러리가 로드되지 않았습니다.');
  }
}

const dangerMarkers = ref([]);  // 위험구역 마커 목록을 저장할 ref 변수 (삭제되지 않음)

// JSON 데이터를 사용하여 위험 마커 추가
async function loadDangerMarkers(url, imageSrc) {
  try {
    const response = await fetch(url); // JSON 파일에서 마커 정보를 가져오기
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const markerData = await response.json();
    console.log(`${url} 마커 데이터 로드 성공:`, markerData);

    const imageSize = new kakao.maps.Size(35, 45);
    const imageOpation = { offset: new kakao.maps.Point(12, 35) };
    const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imageOpation);

    // GeoJSON 데이터의 features 배열에서 마커 추가
    markerData.features.forEach((spot) => {
      const lat = spot.geometry.coordinates[1]; // 위도
      const lng = spot.geometry.coordinates[0]; // 경도
      const markerPosition = new kakao.maps.LatLng(lat, lng); // 마커 좌표

      // 마커 생성
      const marker = new kakao.maps.Marker({
        position: markerPosition, // 마커 위치
        map: map, // 마커를 표시할 지도 객체
        title: spot.properties.MNTN_NM, // 마커 제목
        image: markerImage,
        id: `${lat},${lng}`, // 마커의 고유 ID로 좌표 사용
      });

      // 위험구역 마커를 dangerMarkers에 추가
      dangerMarkers.value.push(marker);

      // 마커에 대한 정보창 추가
      const infowindow = new kakao.maps.InfoWindow({
        content: `<div style="padding:5px;">${spot.properties.MNTN_NM}<br>${spot.properties.SAFE_SPOT2}</div>`, // 정보창 내용
      });

      // 마커에 마우스오버 이벤트 등록
      kakao.maps.event.addListener(marker, 'mouseover', () => infowindow.open(map.value, marker));
      kakao.maps.event.addListener(marker, 'mouseout', () => infowindow.close());
    });

    // 추가 좌표 (예시: 37.5677020596, 126.82765689400)
    // 추가 좌표 (예시: 37.56883961294, 126.8310780462)
    const extraLat = 37.515772995522;
    const extraLng = 127.03493276089;
    const extraMarkerPosition = new kakao.maps.LatLng(extraLat, extraLng);
    
    const extraMarker = new kakao.maps.Marker({
      position: extraMarkerPosition,
      map: map,
      title: "추가 위험 지역",
      image: markerImage,
      id: `${extraLat},${extraLng}`,
    });

    // 추가 마커 dangerMarkers 목록에 추가
    dangerMarkers.value.push(extraMarker);
    //마커 추가 끝
  } catch (error) {
    console.error(`${url} 파일 로드 중 에러 발생:`, error);
  }
}

// 서버에서 위치 정보들을 가져오는 함수
async function fetchLocation() {
    try {
        const response = await axios.get('/api/recordlocation');
        console.log("Received response:", response.data); // 응답 데이터 확인
        latitude.value = response.data.latitude;
        longitude.value = response.data.longitude;
        altitude.value = response.data.altitude;
        speed.value = response.data.speed;
        bearing.value = response.data.bearing;
        time.value = response.data.time;

        // 지도와 경로를 갱신
        updateMapPosition();
    } catch (error) {
        console.error("Error fetching location:", error);
    }
}

// 지도와 마커 위치 갱신 함수
function updateMapPosition() {
    
    if (!map) return;  // Kakao Maps가 로드되지 않았으면 반환

    const newLatLng = new kakao.maps.LatLng(latitude.value, longitude.value);

    // 경로 추가 (isTracking이 true일 때만 기록)
    if (isTracking.value) {
        path.push({
            latitude: latitude.value,
            longitude: longitude.value,
            altitude: altitude.value,
            speed: speed.value,
            bearing: bearing.value,
            time: time.value
        });
        polyline.setPath(path.map(point => new kakao.maps.LatLng(point.latitude, point.longitude)));

    }

    // 마커 위치 업데이트
    if (redDot && whiteDot && BigDot) {
        redDot.setPosition(newLatLng);
        whiteDot.setPosition(newLatLng);
        BigDot.setPosition(newLatLng);
    }

    // 지도 중심 이동
    map.panTo(newLatLng);

    // 사용자 위치가 위험 구역 근처인지 확인하여 음성 안내 재생
    checkProximityUsingPolyline(newLatLng);
}

// CustomOverlay 초기화 함수
function initOverlays() {
    const position = new kakao.maps.LatLng(latitude.value, longitude.value);
    BigDot = new kakao.maps.CustomOverlay({
        position,
        content: '<div style="width: 24px; height: 24px; border-radius: 50%; background-color: rgba(255, 0, 0, 0.2);"></div>',
        yAnchor: 0.5
    });
    BigDot.setMap(map);

    whiteDot = new kakao.maps.CustomOverlay({
        position,
        content: '<div style="width: 14px; height: 14px; border-radius: 50%; background-color: #FFFFFF;"></div>',
        yAnchor: 0.5
    });
    whiteDot.setMap(map);

    redDot = new kakao.maps.CustomOverlay({
        position,
        content: '<div style="width: 10px; height: 10px; background-color: #FF0000; border-radius: 50%;"></div>',
        yAnchor: 0.5
    });
    redDot.setMap(map);
}

// Polyline 초기화 함수
function initPolyline() {
    polyline = new kakao.maps.Polyline({
        map: map,
        path: path,
        strokeWeight: 5,
        strokeColor: '#0000FF',
        strokeOpacity: 0.7,
        strokeStyle: 'solid'
    });
}

// 타이머 관련 함수
function startTimer() {
    secs.value++;
    if (secs.value >= 60) {
        secs.value = 0;
        mins.value++;
    }
    if (mins.value >= 60) {
        mins.value = 0;
        hours.value++;
    }
}

// 경로 추적 상태 토글 함수
function toggleTracking() {
    if (isTracking.value) {
        resetTracking(); // 정지 시 기록 저장 활성화
        isTrackingComplete.value = true;
    } else {
        startTracking(); // 시작 시 기록 저장 비활성화
        isTrackingComplete.value = false;
    }
}

// 타이머 및 경로 추적 함수들
function startTracking() {
    if (!isTracking.value) {
        isTracking.value = true;
        interval = setInterval(startTimer, 1000);
    }
}

function stopTracking() {
    clearInterval(interval);
    isTracking.value = false;
}

function resetTracking() {
    clearInterval(interval);
    isTracking.value = false;
    showSaveModal();
}

// 시간 포맷팅 함수
const formattedHours = computed(() => hours.value.toString().padStart(2, '0'));
const formattedMins = computed(() => mins.value.toString().padStart(2, '0'));
const formattedSecs = computed(() => secs.value.toString().padStart(2, '0'));

const totalTime = computed(() => `${formattedHours.value}:${formattedMins.value}:${formattedSecs.value}`);

// 모달 관련 함수, 변수
let saveModal;

function showSaveModal(){
    const elem = document.querySelector('#saveModal');
    saveModal = new Modal(elem);
    saveModal.show();
}

function delteRecord() {
    hours.value = 0;
    mins.value = 0;
    secs.value = 0;
    path.splice(0, path.length); // 경로 초기화
    polyline.setPath(path);
}

async function saveRecord() {
    console.log("경로:", path, "타입:", typeof path);

    // requestInsertPath 호출 시 각 객체에서 모든 속성 추출
    const trackingPathList = path.map(point => ({
        latitude: point.latitude,
        longitude: point.longitude,
        altitude: point.altitude,
        speed: point.speed,
        bearing: point.bearing,
        time: point.time
    }));

    console.log("바꾼 경로 : ",trackingPathList);

    const id = await requestInsertPathInfo();
    console.log("id = " + id);

    if (id) { // pathId가 유효한 경우에만 저장
        await requestInsertTrackingPath(trackingPathList, id);
    } else {
        console.error("Path ID is not valid.");
        return; // ID가 유효하지 않으면 종료
    }
    
    saveModal.hide();
    delteRecord();
    router.push({path:'/recordImg'});
}

async function requestInsertPathInfo() {
    const imagePath = await uploadImage();
    const data = {
        userId: localStorage.getItem("userId"),
        pathImg: imagePath || '',
        totalTime: totalTime.value
    }
    try {
        const response = await axios.post('/api/savePathInfo', data);
        console.log('등산 기록 코스에 대한 응답 데이터:', response.data);
        return response.data;
    } catch (error) {
        console.error("Error fetching location:", error);
    }
}

async function requestInsertTrackingPath(trackingPathList, pathId) {
    for (const tracking of trackingPathList) {
        const data = {
            pathId: pathId,
            latitude: tracking.latitude,
            longitude: tracking.longitude,
            altitude: tracking.altitude,
            speed: tracking.speed,
            bearing: tracking.bearing,
            time: tracking.time
        };
        
        try {
            const response = await axios.post('/api/saveTrackingPath', data);
            console.log('코스 경로 정보에 대한 응답 데이터:', response.data);
        } catch (error) {
            console.error("Error saving tracking path:", error);
        }
    }
}


function goToRecordImg(){
    router.push({path:'/recordImg'});
}

function goToMyCom(){
    router.push({path:'/myCommunity'});
}

// 컴포넌트가 마운트될 때 지도 초기화 및 위치 추적 시작
onMounted(async() => {
    try{
        await loadResponsiveVoice();
        
        initializeMap();
        fetchLocation();
        setInterval(fetchLocation, 2000); // 3초마다 위치 데이터 가져오기
    }catch(error){
        console.log(error);
    }
});

// 컴포넌트가 언마운트될 때 인터벌 클리어
onBeforeUnmount(() => {
    clearInterval(fetchLocation);
});

// ==== 이미지 관련 코드 ====
const selectedFile = ref(null);

const onFileChange = (event) => {
  selectedFile.value = event.target.files[0];
};

const uploadImage = async () => {
  if (!selectedFile.value) {
    return null;
  }
  const formData = new FormData();
  formData.append('file', selectedFile.value);

  try {
    const response = await axios.post('/api/complaint/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  } catch (error) {
    console.error('Error uploading image', error);
    return null;
  }
};


</script>



<style scoped>
h1,
h2,
h3,
.btn-group .btn,
.start-button,
p {
  /* 특정 요소에 폰트 적용 */
  font-family: "TheJamsil5Bold", sans-serif !important;
}

.map-container {
    width: 100%;
    height: 670px;
    overflow: hidden; /* Prevent scroll */
    position: relative;
}

#map {
    width: 100%;
    height: 100%; /* 부모 높이에 맞추기 */
}

.time {
    position: absolute;
    bottom: 90px; /* 지도 하단에서 약간 떨어뜨려 표시 */
    left: 53%;
    transform: translateX(-50%);
    background-color: rgba(255, 255, 255, 0.8); /* 반투명 배경 */
    padding: 10px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    width: 90%;
    justify-content: space-between;
    z-index: 10;
}

.time p {
    margin: 0;
}

.time button {
    margin-left: 5px;
}


.icon-button img {
    width: 24px; /* 아이콘 크기 */
    height: 24px;
}

.icon-button:hover {
    background-color: #848484; /* 마우스 오버 시 약간 진한 회색 */
}

.icon-button:active {
    background-color: #ff4d4d; /* 클릭 시 빨간색 */
}

.btn-group-container {
  position: fixed;
  top: 13px; /* 헤더 위에 나타나도록 조정 */
  right: 10px; /* 오른쪽 끝에 배치 */
  z-index: 50; /* 헤더 위에 나타나도록 설정 */
  background-color: white;
  border-radius: 8px; /* 둥근 모서리 */
  display: flex;
  align-items: center;
  gap: 5px; /* 버튼 간 간격 */
  padding: 5px;
}

/* 버튼 스타일 */
.btn-group .btn {
  font-size: 12px; /* 버튼 글씨 크기 축소 */
  padding: 4px 8px; /* 버튼 패딩 축소 */
  color: black;
  border: 1px solid #4caf50;
  border-radius: 4px; /* 약간 둥글게 */
  margin: 0;
  white-space: nowrap; /* 텍스트 줄바꿈 방지 */
}

.btn-group .btn:hover {
  background-color: #4caf50;
  color: white;
}

.btn-group .btn-check:checked + .btn {
  background-color: #4caf50;
  color: white;
}

.header {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 30;
    background-color: white; /* 헤더 배경색 */
    border-bottom: 1px solid #ddd; /* 헤더 아래 구분선 */
    height: 60px; /* 헤더 높이 설정 */
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 15px;
}

.timer-display{
    position: absolute;
    bottom: 8%; 
    left: 50%;
    transform: translate(-50%, 50%);
    z-index: 12; /* 지도 위에 표시되도록 */   
    font-size: 30px;
    font-weight: bold;
    color: #333333;
    display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
    border-bottom: solid 1px #4caf50;
    padding-left: 4px;
    padding-right: 4px;
}

.centered-button {
  position: absolute;
  bottom: 20%; 
  left: 50%;
  transform: translate(-50%, 50%);
  z-index: 10; /* 지도 위에 표시되도록 */
}

/* 하단 목표 설정 버튼 */
.goal-button {
    width: 50px; 
    height: 50px;
    position: absolute;
    bottom: 16%;
    left: 25%;
    transform: translateX(-50%);
    background-color: #ffffff;
    border: 1px solid #cccccc;
    border-radius: 50%;
    font-size: 14px;
    color: #333333;
    cursor: pointer;
    z-index: 10;
    box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.2);
    transition: all 0.3s ease;
}

/* 호버 효과 */
.goal-button:hover {
    background-color: #f8f8f8; /* 약간 밝아지는 효과 */
    border-color: #bdbdbd;
    box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.3); /* 그림자 강화 */
}

/* 클릭 효과 */
.goal-button:active {
    background-color: #e0e0e0; /* 클릭 시 색상 변경 */
    transform: translateX(-100%) scale(0.95); /* 클릭 시 약간 작아짐 */
}

.start-button {
    width: 130px;
    height: 130px;
    position: relative;
    bottom: 0px;
    border: none;
    border-radius: 50%;
    background-color: #51b951; /* 버튼 배경색 (노란색) */
    font-size: 22px;
    font-weight: bold;
    color: white;
    cursor: pointer;
    box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.2); /* 약간의 그림자 */
    transition: all 0.3s ease;
}

.start-button:hover {
    background-color: #a6ec02; /* hover 시 더 짙은 노란색 */
    color: black; /* 텍스트를 흰색으로 변경 */
}

.start-button:active {
    background-color: #ffa500; /* 클릭 시 색상 변경 */
    transform: scale(0.95); /* 클릭 시 약간 줄어듦 */
}

/* 하단 목표 설정 버튼 */
.TTS {
    position: absolute;
    bottom: 16%;
    left: 75%;
    transform: translateX(-50%);
    background-color: #ffffff;
    border: 1px solid #cccccc;
    border-radius: 50%; /* 원형 버튼 */
    width: 50px; /* 버튼의 너비 */
    height: 50px; /* 버튼의 높이 (너비와 동일하게 설정) */
    display: flex; /* 내부 텍스트를 중앙 정렬 */
    align-items: center;
    justify-content: center;
    font-size: 14px;
    color: #333333;
    cursor: pointer;
    z-index: 10;
    box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.2); /* 약간의 그림자 */
    transition: all 0.3s ease;
}

/* 호버 효과 */
.TTS:hover {
    background-color: #f8f8f8; /* 약간 밝아지는 효과 */
    border-color: #bdbdbd;
    box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.3); /* 그림자 강화 */
}

/* 클릭 효과 */
.TTS:active {
    background-color: #e0e0e0; /* 클릭 시 색상 변경 */
    transform: scale(0.95); /* 클릭 시 약간 작아짐 */
}

.footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: 60px; /* 적절한 높이 */
    z-index: 1000; /* 위에 고정 */
    background-color: white; /* 배경색 */
    box-shadow: 0px -1px 5px rgba(0, 0, 0, 0.1); /* 위쪽 그림자 */
}

/* 반응형 조정 */
@media (max-width: 768px) {
  .start-button {
    width: 100px;
    height: 100px;
    font-size: 25x;
  }

  .goal-button {
    font-size: 12px;
  }
}

.modal-content {
  border-radius: 15px; /* 둥근 모서리 */
  box-shadow: 0px 4px 10px rgba(0, 0, 0, 0.2); /* 그림자 */
  background: linear-gradient(145deg, #f9f9f9, #ffffff); /* 부드러운 배경 그라데이션 */
  padding: 20px; /* 내부 간격 */
}

/* 모달 헤더 */
.modal-header {
  border-bottom: none; /* 구분선 제거 */
  text-align: center; /* 텍스트 중앙 정렬 */
  padding-bottom: 10px;
}

.modal-title {
  font-family: "TheJamsil5Bold", sans-serif;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.btn-close {
  background-color: #f0f0f0;
  border-radius: 50%;
  padding: 5px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-close:hover {
  background-color: #e0e0e0;
}

/* 모달 바디 */
.modal-body {
  font-family: "TheJamsil5Bold", sans-serif;
  color: #666;
  line-height: 1.6;
  font-size: 14px;
  text-align: center; /* 텍스트 중앙 정렬 */
}

.modal-body p {
  font-size: 16px;
  margin-bottom: 20px;
  font-weight: 600;
  color: #444;
}

/* 사진 등록 파일 업로드 스타일 */
input[type="file"] {
  display: block;
  margin: 10px auto;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  width: 80%; /* 업로드 창 크기 */
  transition: all 0.3s ease;
}

input[type="file"]:hover {
  background-color: #f9f9f9;
  border-color: #bbb;
}

/* 모달 푸터 */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: none; /* 구분선 제거 */
}

.modal-footer button {
  font-family: "TheJamsil5Bold", sans-serif;
  font-size: 14px;
  padding: 10px 20px;
  border-radius: 10px; /* 둥근 버튼 */
  cursor: pointer;
  transition: all 0.3s ease;
}

.modal-footer .btn-secondary {
  background-color: #e0e0e0;
  color: #333;
  border: none;
}

.modal-footer .btn-secondary:hover {
  background-color: #d6d6d6;
}

.modal-footer .btn-primary {
  background-color: #4caf50;
  color: white;
  border: none;
}

.modal-footer .btn-primary:hover {
  background-color: #45a047;
}

/* 추가 장식 */
.modal-footer .btn-primary::before {
  content: "✔ ";
  font-weight: bold;
}

.modal-footer .btn-secondary::before {
  content: "✖ ";
  font-weight: bold;
}
</style>