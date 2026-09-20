<template>

  <div id="infomain">
    <div class="row" style="margin: 0px; padding: 0px;">   
      <!-- 검색창과 버튼 -->
      <div class="review-search" style="top: 30px;">
        <div class="search-container">
          <input type="text" placeholder="검색" v-model="searchQuery" />
          <button class="search-button">
            <img src="/images/돋보기white.png" alt="Search" />
          </button>
        </div>
      </div>
      <!-- zoom -->
      <button id="zoom-button" @click="Zoom">
      <img src="/images/내위치2.png" alt="Zoom">
     </button>
      
      <!-- 버튼 그룹 : 시작 -->
      <div class="btn-group" role="group" aria-label="Basic radio toggle button group" style="position: fixed; top: 30px;  z-index: 21; justify-content: center;">
        <button class="button-item" @click="gotomobilecourse()" style="border-radius: 50%; height: 40px; width: 40px;margin-right: 16em;"><img src="/images/뒤로가기.png"  alt="돋보기 아이콘" width="24" height="24"></button>
  
  
        <div class="dropdown" style="z-index: 100;">
          <a role="button" @click="toggleDropdown" aria-expanded="false">
            <img src="/images/hamburgerIcon.png" id="hambugermenu" alt="햄버거 아이콘" width="40" height="40">
          </a>
          <div class="dropdown-menu">
            <button class="dropdown-item delay-1" @click="goToCourseInfoViewPage(courseData)">정보</button>
            <button class="dropdown-item delay-2" @click="goToCoursePreviewPage()">영상</button>
            <button class="dropdown-item delay-3" @click="goToCourse3DPage()">3D</button>
            <button class="dropdown-item delay-3" @click="goToBalancePage()">비교</button>
            <button class="dropdown-item delay-3" @click="goToAccidentHeatmap()">사고</button>
            <button class="dropdown-item delay-3" @click="goToRealtimeHeatmap()">실시간</button>
          </div>
        </div>
      </div>

      <!-- 실족, 조난, 질환 버튼 -->
      <div class="toggle-switch-container2" style="z-index: 10;"> <!-- 기존 z-index를 낮춰 설정 -->
        <div class="toggle-switch2" @click="toggleMap('default')" :class="{ active: showMap === 'default' }">
          <img src="/images/기본.png" alt="" style="width: 16px; height: auto; margin-right: 5px;"> 기본
        </div>
        <div class="toggle-switch2" @click="toggleMap('photo')" :class="{ active: showMap === 'photo' }">
          <img src="/images/사진.png" alt="" style="width: 16px; height: auto; margin-right: 5px;"> 사진
        </div>
        <div class="toggle-switch2" @click="applyFilter('실족')" :class="{ active: selectedFilter === '실족' }">
          <img src="/images/strumble.png" alt="" style="width: 16px; height: auto; margin-right: 5px;"> 실족
        </div>
        <div class="toggle-switch2" @click="applyFilter('조난')" :class="{ active: selectedFilter === '조난' }">
          <img src="/images/distress.png" alt="" style="width: 16px; height: auto; margin-right: 5px;"> 조난
        </div>
        <div class="toggle-switch2" @click="applyFilter('질환')" :class="{ active: selectedFilter === '질환' }">
          <img src="/images/disease.png" alt="" style="width: 16px; height: auto; margin-right: 5px;"> 질환
        </div>
      </div>
          
      <div v-if="showMap === 'default'" id="map" style="width: 100%; height: 700px; margin: 0px;">
        
        <!-- 메인 페이지 내용 -->
      <button id="showHelpButton" @click="showHelp = true">?</button>
        <div class="map-legend ms-4">
          
          <div class="legend-item">
            <div class="legend-icon" style="background-color:#FF4500;"></div>
            <span>오르막</span>
          </div>
          
          <div class="legend-item">
            <div class="legend-icon" style="background-color:#32CD32;"></div>
            <span>평지</span>
          </div>
          
          <div class="legend-item">
            <div class="legend-icon" style="background-color:#1E90FF;"></div>
            <span>내리막</span>
          </div>
          
        </div>
      </div>
      <div v-else-if="showMap === 'photo'" id="photoMap" style="width: 100%; height: 750px; margin: 0px;"></div>
      
      <!-- 버튼 그룹 -->
    </div>
  </div>
  
  <!-- 드래그 패널 영역 -->
  <div
  class="drag-panel"
  :style="{ top: panelTop + 'px' }"
  @mouseenter="disableScroll"
  @mouseleave="enableScroll">
  
  <div class="drag-handle"></div>
  <div class="content">
    
      <!-- 슬라이드 토글 버튼 추가 -->
      <div class="toggle-switch" @click="toggleTestContent">
        <div :class="['toggle-thumb', { active: showTestContent }]"></div>
      </div>
  
      
      <!-- <h4 >{{ friend.name }}</h4>
      <p>{{ friend.location }}</p>
      <p>{{ friend.distance }} km</p> 
      {{ averageDifficultyText }} 난이도 변수-->
  
      <div v-if="!showTestContent" class="row">
  
        <div  id="courseinfo">
          <p class="image-course-name" id="courseName">{{ courseData?.courseName }} 코스</p>
          <div class="difficulty-text" id="courseAverage" style="margin-bottom: 5px;">
            난이도: {{ averageDifficultyText }} · 
            <i class="bi bi-star-fill image-rating-star" style="color: #ffd400;"></i> 
            3.5
          </div>
        </div>
  
        <canvas id="elevationChart"></canvas>
  
      </div>
        <!-- 메인 페이지 내용 -->
      <button id="showHelpButton" @click="showHelp = true">?</button>      
      <!-- 조건부 렌더링으로 콘텐츠 표시 -->
  
    <div class="row" style="margin: 0px; padding: 0px;" v-if="showTestContent">
      
               <!-- 날씨 : 시작 -->   
      <div class="weather-forecast" v-if="dailyWeather.length > 0">
  
        <div class="date-selector">
  
          <div v-for="(day, index) in dailyWeather.slice(0, 6)" :key="index" class="date-button-wrapper">
  
            <div class="day-label">{{ formatDayOfWeek(day.date) }}</div> <!-- 요일 표시 -->
  
            <button
              :class="['date-button', { active: selectedDateIndex === index }]"
              @click="selectDate(index)"
            >
              {{ formatDate(day.date) }}
            </button>
  
          </div>
        </div>

  <div class="forecast-details" v-if="selectedDay">
    <div class="d-flex">
  
      <div class="row">
        
        <div class="d-flex" style="margin-top: -1rem; margin-left: 0.4rem;">
          <p class="current-temperature">{{ Math.round(selectedDay.currentTemp - 273.15) }}°</p>
          <p class="weather-icon" v-html="getWeatherEmoji(selectedDay.weather[0].main)"></p>
        </div>
        
        
        <div class="row">
  
          <p class="description" >{{ translateWeatherDescription(selectedDay.weather[0].id) }}</p>
  
          <div class="temperature-range d-flex">
            <p class="min-temp">최저: {{ Math.round(selectedDay.minTemp - 273.15) }}°</p>
            <p class="max-temp">최고: {{ Math.round(selectedDay.maxTemp - 273.15) }}°</p>
          </div>
          
          <p class="forecast-date">오늘은 {{ formatDayOfWeek(selectedDay.date) }}요일 입니다 </p>
          
        </div>
        
      </div>
      
      <div class="row" style="margin-left: -14rem; margin-top: 3rem; max-height: 110px;">               
        <div class="rain-info d-flex">
          <div class="rain-icon" :style="{ '--rain-level': selectedDay.pop * 100 + '%' }"></div>
          <p class="rain-chance" style="margin-top: 1rem; margin-left: 1.5rem;">{{ Math.round(selectedDay.pop * 100) }}%</p>
        </div>
        
        <div class="sun-info" >
          <p id="sunrise"><img src="/images/sunrise.png" alt="sunrise" class="icon-small" >일출: {{ sunriseTimes[selectedDateIndex] }}</p>
          <p id="sunset"><img src="/images/sunset.png" alt="sunset" class="icon-small" >일몰: {{ sunsetTimes[selectedDateIndex] }}</p>
        </div>      
        
      </div>
  
    </div>
  </div>
  </div>
  </div>
  </div>
      
  
      <!-- 팝업 오버레이 -->
      <div v-if="showHelp" class="overlay" @click.self="closeHelp" >
        <div class="popup-content">
          <p id="popupTitle">고도 확인</p>
          <div class="popup-text">
  
            <div class="d-flex text">
              <img src="/images/로딩.gif" style="width: 80px; height: 80px;">
              <span>경사도에 손가락을 올려보세요.</span>
            </div>
  
            <div class="d-flex text">
              <img src="/images/로딩.gif" style="width: 80px; height: 80px;">
              <span>탭에서 정보를 확인해보세요. </span>
            </div>
  
            <div class="d-flex text">
              <img src="/images/로딩.gif" style="width: 80px; height: 80px;">
              <span>영상을 확인해보세요. </span>
            </div>
  
          </div>
            <img src="/images/ModalX.png" style="width: 20px; height: 20px;" @click="closeHelp" id="closepopup">
        </div>
      </div>
    </div>
  
    
      
    </template>
  
  <script setup>
  import { ref, onMounted, computed } from 'vue';
  import Chart from 'chart.js/auto';
  import axios from "axios";
  import { defineProps } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import {
    resolveTrailId,
    SLOPE_WINDOW_METERS,
    getEstimatedSlopeColor,
    fetchTrailGeoJson,
    fetchSlopeSections,
  } from '@/api/slopeSection.js';

  const router = useRouter(); // 라우터 객체
  const route = useRoute(); // 현재 경로 정보
  
  const showHelp = ref(false);  // 팝업 표시 여부
  
  /* global kakao */
  const map = ref(null);
  const routeCoordinates = ref([]);
  let chartInstance = null;
  const currentMarker = ref(null);
  
  const modalPolyline = ref(null);  // 모달 지도 경로
  
  
  const selectedDayIndex = ref(null);
  
  const showMap = ref('default');  // 기본 지도를 기본값으로 설정
  let photoMap = ref(null);
  
  const selectedFilter = ref('전체');


  // 지도 토글 함수
  const toggleMap = (type) => {
    showMap.value = type;
    initializeMap();

    if (type === 'default') {
    applyFilter('전체'); 
  }
  };
  
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
  
  // 요일 선택 관련 데이터
  // 요일 데이터 설정 함수
  
  
  // 현재 날짜를 기준으로 자동 선택
  function autoSelectToday() {
    selectedDayIndex.value = 0; // 오늘 날짜에 맞춰 첫 번째 버튼 선택
  }
  
  
  // 상태 정의
  const panelTop = ref(window.innerHeight - 270); // 드래그 패널 위치 (기본적으로 하단)
  
  
  const goToCourseInfoViewPage = (course) => {
    console.log("Navigating with course data:", course); 
    router.push({
      name: 'mobilecourseinfoview',
      query: {
        course: JSON.stringify(course),
      },
    });
  };
  
  // 버튼 클릭
  function applyFilter(filter) {
    selectedFilter.value = filter;
    
    // 기존 마커 지우기
    if (mapMarkers.length) {
      mapMarkers.forEach(marker => marker.setMap(null));
      mapMarkers = [];
    }
    
    // 전체버튼 클릭시 마커 띄우기
    if (selectedFilter.value === '전체') {
      loadMarkers("/data/헬기장spot.geojson", '/images/helipad.png');
      loadMarkers("/data/화장실.geojson", '/images/toilets.png');
      loadMarkers("/data/2023산악사고_인왕산2.geojson", '/images/danger.png');
    } else {
      // Otherwise, load only markers for the selected filter
      loadMarkers("/data/2023산악사고_인왕산2.geojson", '/images/danger.png');
    }
  }

  const props = defineProps({
  course: {
    type: String,
    required: true
  }
  
  });
  
  // props로 전달된 mountain 데이터를 JSON으로 파싱
  const courseData = ref(JSON.parse(props.course));
  
  // 지도 초기화
  function initializeMap() {
    if (showMap.value === 'default') {
        const script = document.createElement('script');
        script.onload = () => kakao.maps.load(() => {
          map.value = new kakao.maps.Map(document.getElementById('map'), {
            center: new kakao.maps.LatLng(37.66433293993584, 127.01160029114365),
            level: 5,
          });
          if (map.value) {
            loadGeoJSONFromServer(map.value);
            loadMarkers("/data/헬기장spot.geojson", '/images/helipad.png');
            loadMarkers("/data/화장실.geojson", '/images/toilets.png');
            loadMarkers("/data/2023산악사고_인왕산2.geojson", '/images/danger.png');
          }
        });
        script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false';
        document.head.appendChild(script);
    } else if (showMap.value === 'photo') { // 여기에
        const script = document.createElement('script');
        script.onload = () => kakao.maps.load(() => {
          photoMap.value = new kakao.maps.Map(document.getElementById('photoMap'), {
          center: new kakao.maps.LatLng(37.66433293993584, 127.01160029114365),
          level: 5,
          });
          if (photoMap.value) {
            loadGeoJSONFromServer(photoMap.value);
            loadMarkersToPhotoMap();
          }
        });
        script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false';
        document.head.appendChild(script);
      }
  }
  
  // Trail GeoJSON API 데이터 로드 함수 (Phase 12D -- Validated DB TrailFeature 기반, 기존 static
  // '/data/인왕산ele copy.geojson' 직접 fetch를 대체. 해당 static 파일 자체는 원본 provenance/
  // Legacy regression 용도로 계속 보존한다 -- docs/09-slope-section-analysis.md 참고.)
  async function loadGeoJSONFromServer(targetMap = map.value) {
    try {
      const geojsonData = await fetchTrailGeoJson();
      console.log('Trail GeoJSON data loaded:', geojsonData);
      processGeoJSON(geojsonData, targetMap);  // GeoJSON 데이터를 처리하여 경로를 그리는 함수 호출
    } catch (error) {
      console.error('Trail GeoJSON API 로드 중 에러 발생:', error);
    }
  }
  
  // 마커를 photoMap에 추가하는 함수
  function loadMarkersToPhotoMap() {
    if (!photoMap.value) {
      console.error("photoMap이 초기화되지 않았습니다.");
      return;
    }
  
    // comCourse 데이터에서 좌표 정보를 가져와 마커를 생성
    comCourse.value.forEach(course => {
      const lat = course.latitude;
      const lng = course.longitude;
  
      // 마커의 위치 설정
      const markerPosition = new kakao.maps.LatLng(lat, lng);

      const imageSrc = '/images/danger-icon.png';
      const imageSize = new kakao.maps.Size(35, 45);
      const imageOption = { offset: new kakao.maps.Point(12, 35) };
      const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imageOption);
  
      // 마커 생성
      const marker = new kakao.maps.Marker({
        position: markerPosition,
        map: photoMap.value,
        title: course.courseName, // 마커에 코스 이름 표시
        image: markerImage
      });
  
      // 이미지가 포함된 InfoWindow 콘텐츠 설정
      const infowindowPhotoMap = new kakao.maps.InfoWindow({
          content: `
    <div style="display: flex; flex-direction: column; align-items: center; width: 150px; height: auto; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); font-family: Arial, sans-serif; overflow: hidden; border: 1px solid #ddd; border-radius: 12px; z-index: 500;">
      <!-- 사진 영역 -->
      <div style="display: flex; justify-content: center; align-items: center; width: 100%; height: 150px; overflow: hidden; border-bottom: 2px solid #ced4da;">
        <img src="${course.communityUrl}" alt="사진" 
          style="
            width: 130px; 
            height: 120px; 
            object-fit: cover; 
            border: 1px solid black; 
            border-radius: 10px;
            box-sizing: border-box; /* 여백과 크기 조정 방지 */
            position: relative;
          ">
      </div>
      <!-- 텍스트 영역 -->
      <div style="text-align: center; padding: 10px; margin-top: -10px;">
        <p style="font-size: 14px; font-weight: bold; margin: 0; color: #333;">
          <img src="/images/window산.png" style="width: 16px; height: 16px;"/> ${course.communityTitle}
        </p>
        <p style="font-size: 12px; color: #666; margin: 5px 0 0;">- ${course.courseName} -</p>
      </div>
    </div>
  `,
          disableAutoPan: true, // 인포윈도우 표시로 인해 지도가 이동하지 않도록 설정
        });
  
      // 마커 클릭으로 infowindow 토글 방식
      kakao.maps.event.addListener(marker, 'click', () => {
      if (infowindowPhotoMap.getMap()) {
        // infowindow가 현재 열려 있는 경우 닫기
        infowindowPhotoMap.close();
      } else {
        // infowindow가 닫혀 있는 경우 열기
        infowindowPhotoMap.open(photoMap.value, marker);
      }
    });
    });
  }

  // 커뮤니티 코스 불러오기
  let comCourse = ref([]);

const requestCourse = async () => {
  try{
    const response = await axios.post('/api/comCourse',{
      courseName: courseData.value.courseName
    });
    comCourse.value = response.data;
  }catch(error){
    console.log(error)
  }
}

  
  let mapMarkers = []
  
  // 마커 추가
  async function loadMarkers(url, imageSrc) {
  try {
    // GeoJSON 데이터 로드
    const response = await fetch(url);
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const markerData = await response.json();

    // 필터링된 마커 데이터 가져오기
    const filteredMarkers = markerData.features.filter((feature) => {
      const markerType = feature.properties.type;
      const courseName = feature.properties.MNTN_NM2;
      return (
        courseName === courseData.value.courseName &&
        (selectedFilter.value === '전체' || markerType === selectedFilter.value || !markerType)
      );
    });

    // 필터링된 마커를 지도에 추가
    filteredMarkers.forEach((spot) => {
      const lat = spot.geometry.coordinates[1];
      const lng = spot.geometry.coordinates[0];
      const markerPosition = new kakao.maps.LatLng(lat, lng);

      // 마커 이미지 설정
      let finalImageSrc = imageSrc;
      switch (spot.properties.type) {
        case '실족':
          finalImageSrc = '/images/strumble.png';
          break;
        case '조난':
          finalImageSrc = '/images/distress.png';
          break;
        case '질환':
          finalImageSrc = '/images/disease.png';
          break;
      }

      // 마커 생성
      const imageSize = new kakao.maps.Size(35, 45);
      const imageOption = { offset: new kakao.maps.Point(12, 35) };
      const markerImage = new kakao.maps.MarkerImage(finalImageSrc, imageSize, imageOption);

      const marker = new kakao.maps.Marker({
        position: markerPosition,
        map: map.value,
        title: spot.properties.MNTN_NM,
        image: markerImage,
      });

      mapMarkers.push(marker); // 새 마커 배열에 추가

      // 인포윈도우 설정
      const infowindow = new kakao.maps.InfoWindow({
        content: `
          <div style="display: flex; flex-direction: column; align-items: center; width: 150px; height: auto; 
          box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); font-family: Arial, sans-serif; overflow: hidden; 
          border: 1px solid #ddd; border-radius: 12px; z-index: 500;">
            <!-- 사진 영역 -->
            <div style="display: flex; justify-content: center; align-items: center; width: 100%; height: 150px; 
            overflow: hidden; border-bottom: 2px solid #ced4da;">
              <img src="/images/${spot.properties.image}" alt="사진" 
              style="width: 130px; height: 120px; object-fit: cover; border: 1px solid black; border-radius: 10px;" />
            </div>
            <!-- 텍스트 영역 -->
            <div style="text-align: center; padding: 10px;">
              <p style="font-size: 14px; font-weight: bold; margin: 0; color: #333;">
                <img src="/images/window산.png" style="width: 16px; height: 16px;" /> ${spot.properties.MNTN_NM}
              </p>
              <p style="font-size: 12px; color: #666; margin: 5px 0;">- ${spot.properties.SAFE_SPOT2} -</p>
            </div>
          </div>
        `,
        disableAutoPan: true, // 인포윈도우 표시로 인해 지도가 이동하지 않도록 설정
      });

      // 마커 클릭 이벤트
      kakao.maps.event.addListener(marker, 'click', () => {
        if (infowindow.getMap()) {
          infowindow.close();
        } else {
          infowindow.open(map.value, marker);
        }
      });
    });
  } catch (error) {
    console.error(`${url} 파일 로드 중 에러 발생:`, error);
  }
}
  
  
  function processGeoJSON(geojsonData, targetMap) {
    let allCoordinates = [];
  
    geojsonData.features.forEach((feature) => {
      if (feature.properties.PMNTN_NM && feature.properties.PMNTN_NM.includes(courseData.value.courseName)) {
        let coordinates = [];
  
        // MultiLineString을 처리하기 위해 중첩 배열을 펼침
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
      addRouteLayer(allCoordinates, targetMap);
      drawElevationChart(allCoordinates);
      if (targetMap === map.value) {
        renderSlopeOverlay(targetMap); // Phase 12D: 20m SlopeSection Backend API 기반 경사 Overlay
      }
    } else {
      console.log('유효한 구간 데이터가 없습니다.');
    }
  }

  // 경로(Base Trail) 레이어 추가 함수 -- Phase 12D부터 경사 색상은 이 함수가 아니라
  // renderSlopeOverlay()가 별도 Overlay로 그린다 (Base + Overlay 구조, docs/09 참고). Desktop
  // (MountainDetailView.vue)과 동일하게 windowMeters=20 고정 -- Mobile이라고 더 큰 window를
  // 쓰지 않는다.
  function addRouteLayer(coordinates, targetMap) {
    if (!targetMap) {
      console.error("targetMap이 초기화되지 않았습니다.");
      return;
    }

    const linePath = coordinates.map((coord) => new kakao.maps.LatLng(coord.lat, coord.lng));

    // map.value에 경로를 그리는 경우
    if (targetMap === map.value) {
      drawBaseRoute(coordinates); // 기본(Base) 경로 그리기 -- 경사색은 renderSlopeOverlay가 그 위에 덧그림

    // photoMap.value에 경로를 그리는 경우 (경사 Overlay 없이 단색 경로)
    } else if (targetMap === photoMap.value) {
      modalPolyline.value = new kakao.maps.Polyline({
        path: linePath,
        strokeWeight: 5,
        strokeColor: '#00FF00',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
      });

      modalPolyline.value.setMap(photoMap.value); // 모달 지도에 경로 설정
    }

    // 지도 경계를 경로에 맞게 설정
    const bounds = new kakao.maps.LatLngBounds();
    coordinates.forEach(coord => bounds.extend(new kakao.maps.LatLng(coord.lat, coord.lng)));
    targetMap.setBounds(bounds); // 타겟 맵에 맞게 경계 조정
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

  // Phase 12D: 20m SlopeSection Backend API 기반 경사 Overlay -- Base Trail(drawBaseRoute)
  // 위에 덧그려진다. course 변경 시 이전 Overlay를 반드시 제거한 뒤 새로 그린다(중복 방지).
  // SlopeSection이 존재하지 않는 구간(docs/09 coverage 한계)은 Base Trail만 계속 보이며,
  // 이 함수는 그 구간에 임의로 slope=0을 채우지 않는다 -- Feature를 아예 그리지 않는다.
  let slopeOverlayPolylines = [];

  async function renderSlopeOverlay(targetMap) {
    slopeOverlayPolylines.forEach((polyline) => polyline.setMap(null));
    slopeOverlayPolylines = [];

    const trailId = resolveTrailId(courseData.value.courseName);
    if (!trailId) {
      console.error('SlopeSection Trail ID를 찾을 수 없는 코스명입니다:', courseData.value.courseName);
      return;
    }

    try {
      const slopeGeoJson = await fetchSlopeSections(trailId, SLOPE_WINDOW_METERS);
      slopeGeoJson.features.forEach((feature) => {
        const color = getEstimatedSlopeColor(feature.properties.estimatedSlopePercent);
        if (color == null) {
          // invalid/missing slope (not the same thing as a measured ~0% slope) -- skip this
          // Overlay Feature entirely; the Base Trail Layer already shows this geometry.
          return;
        }
        const path = feature.geometry.coordinates.map(([lng, lat]) => new kakao.maps.LatLng(lat, lng));
        const polyline = new kakao.maps.Polyline({
          path,
          strokeWeight: 5,
          strokeColor: color,
          strokeOpacity: 0.8,
          strokeStyle: 'solid',
        });
        polyline.setMap(targetMap);
        slopeOverlayPolylines.push(polyline);
      });
    } catch (error) {
      // SlopeSection API 실패는 경사 Overlay만 생략시킨다 -- 이미 그려진 Base Trail은 유지된다.
      console.error('SlopeSection API 로드 중 에러 발생 (Base Trail은 계속 표시됩니다):', error);
    }
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
  
  // Phase 12D: 경사(slope) 계산은 더 이상 Frontend 책임이 아니다 -- 기존
  // calculateHaversineDistance()/calculateSlope()/getColorBySlope()는 제거했다. 색상 매핑은
  // src/api/slopeSection.js의 getEstimatedSlopeColor()가, 경사 계산 자체는 Backend
  // SlopeSectionService가 담당한다 (docs/09-slope-section-analysis.md). deg2rad()는
  // drawElevationChart()의 거리 계산에서 계속 쓰이므로 유지한다.
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
  
  // 배경 이미지를 넣기 위한 커스텀 플러그인 정의
  const backgroundImagePlugin = {
      id: 'backgroundImagePlugin',
      beforeDraw: (chart) => {
        if (chart.config.options.backgroundImage) {
          const ctx = chart.ctx;
          const chartArea = chart.chartArea;
          const backgroundImage = new Image();
          backgroundImage.src = chart.config.options.backgroundImage;
  
          // 이미지가 로드된 후 캔버스에 그리기
          backgroundImage.onload = function () {
            ctx.save();
            ctx.globalAlpha = 0.4; // 불투명도 설정 (0.0: 완전히 투명, 1.0: 완전히 불투명)
            ctx.drawImage(backgroundImage, chartArea.left, chartArea.top, chartArea.right - chartArea.left, chartArea.bottom - chartArea.top);
            ctx.restore();
          };
        }
      },
    };
  
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
        backgroundImage: '/images/mountainbackground.png',
        scales: {
          x: {
            title: {
              display: true,
              text: '거리 (m)', // x축 제목 (미터 단위로 표시)
            },
            grid: {
              display : false,
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
            grid: {
              display : false,
            }
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
          legend: {
            display: false,
          }
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
      plugins: [backgroundImagePlugin],
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
  
  // 경로 상의 마커 강조 함수
  function highlightRouteOnMap(index, targetMap = map.value) {
    if (!targetMap) {
      console.error("지도 객체가 초기화되지 않았습니다. targetMap이 null입니다.");
      return;  // targetMap이 null이면 함수 실행 중지
    }
  
    if (index >= 0 && index < routeCoordinates.value.length) {
      const point = routeCoordinates.value[index];
  
      if (currentMarker.value) {
        currentMarker.value.setMap(null);
      }
  
      const imageSrc = '/images/running.png';
      const imagesSize = new kakao.maps.Size(35,45);
      const imageOption = { offset: new kakao.maps.Point(17,45) };
      const markerImage = new kakao.maps.MarkerImage(imageSrc,imagesSize,imageOption);
  
      currentMarker.value = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(point.lat, point.lng),
        image: markerImage,
      });
  
      currentMarker.value.setMap(targetMap);
      targetMap.panTo(new kakao.maps.LatLng(point.lat, point.lng));  // targetMap이 null이 아닐 때만 panTo 호출
    }
  }
  
  // 날씨
  const dailyWeather = ref([]);
  const sunriseTimes = ref([]);
  const sunsetTimes = ref([]);
  const selectedDateIndex = ref(0); // 선택된 날짜 인덱스
  
  // 선택된 날짜의 날씨 데이터
  const selectedDay = computed(() => dailyWeather.value[selectedDateIndex.value]);
  
  // 날짜 선택 함수
  const selectDate = (index) => {
    selectedDateIndex.value = index;
  };
  
  
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
  
  // 요일 변환 함수
  const formatDayOfWeek = (date) => {
    const formattedDate = new Date(date);
    return formattedDate.toLocaleDateString("ko-KR", { weekday: "short" });
  };
  
  // 날짜 포맷 변환 함수
  const formatDate = (date) => {
    const formattedDate = new Date(date);
    return `${formattedDate.getDate()}`;
  };
  
  // 날씨 이모티콘 반환 함수
  const getWeatherEmoji = (weatherMain) => {
    const weatherIcons = {
      Clear: '<img src="/images/로딩.gif" alt="Clear" style="width: 80px; height: 80px;">',
      Clouds: "☁️",
      Rain: "🌧️",
      Snow: "🌨️",
      Fog: "🌁",
      Thunderstorm: "⛈️",
      Drizzle: "🌦️",
      Mist: "🌫️",
      breeze: "༄",
      "Few Clouds": "🌥️",
      "freezing rain": "🌧️➜❄️",
    };
    return weatherIcons[weatherMain] || "🌥️";
  };
  
  // 날씨 ID를 한국어 설명으로 변환하는 함수
  const translateWeatherDescription = (weatherId) => {
    const weatherDescriptions = {
      200: "가벼운 비를 동반한 천둥구름",
      201: "비를 동반한 천둥구름",
      202: "폭우를 동반한 천둥구름",
      210: "약한 천둥구름",
      211: "천둥구름",
      212: "강한 천둥구름",
      221: "불규칙적 천둥구름",
      230: "약한 연무를 동반한 천둥구름",
      231: "연무를 동반한 천둥구름",
      232: "강한 안개비를 동반한 천둥구름",
      300: "가벼운 안개비",
      301: "안개비",
      302: "강한 안개비",
      310: "가벼운 적은비",
      311: "적은비",
      312: "강한 적은비",
      313: "소나기와 안개비",
      314: "강한 소나기와 안개비",
      321: "소나기",
      500: "약한 비",
      501: "중간 비",
      502: "강한 비",
      503: "매우 강한 비",
      504: "극심한 비",
      511: "우박",
      520: "약한 소나기 비",
      521: "소나기 비",
      522: "강한 소나기 비",
      531: "불규칙적 소나기 비",
      600: "가벼운 눈",
      601: "눈",
      602: "강한 눈",
      611: "진눈깨비",
      612: "소나기 진눈깨비",
      615: "약한 비와 눈",
      616: "비와 눈",
      620: "약한 소나기 눈",
      621: "소나기 눈",
      622: "강한 소나기 눈",
      701: "박무",
      711: "연기",
      721: "연무",
      731: "모래 먼지",
      741: "안개",
      751: "모래",
      761: "먼지",
      762: "화산재",
      771: "돌풍",
      781: "토네이도",
      800: "구름 한 점 없는 맑은 하늘",
      801: "약간의 구름이 낀 하늘",
      802: "드문드문 구름이 낀 하늘",
      803: "구름이 거의 없는 하늘",
      804: "구름으로 뒤덮인 흐린 하늘",
      900: "토네이도",
      901: "태풍",
      902: "허리케인",
      903: "한랭",
      904: "고온",
      905: "바람부는",
      906: "우박",
      951: "바람이 거의 없는",
      952: "약한 바람",
      953: "부드러운 바람",
      954: "중간 세기 바람",
      955: "신선한 바람",
      956: "센 바람",
      957: "돌풍에 가까운 센 바람",
      958: "돌풍",
      959: "심각한 돌풍",
      960: "폭풍",
      961: "강한 폭풍",
      962: "허리케인"
    };
    return weatherDescriptions[weatherId] || "알 수 없는 날씨";
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
  
  // 컴포넌트 마운트될 때 초기화 작업
  onMounted(() => {
    const queryMapType = route.query.mapType || 'default'; // 기본값은 'default'
    showMap.value = queryMapType
  initializeMap();
  fetchWeatherData(37.66433293993584, 127.01160029114365)
    .then(() => {
        autoSelectToday(); // 날씨 데이터를 불러온 후 자동으로 오늘 날짜 선택
      });
      requestCourse();
  });
  
  
  function gotomobilecourse(){
    window.location.href = '/mobilecourseview'
  }
  
  function closeHelp() {
    showHelp.value = false;  // 팝업 닫기
  }
  
  const showTestContent = ref(false); // 초기 상태를 false로 설정하여 canvas를 표시
  
  const toggleTestContent = () => {
    showTestContent.value = !showTestContent.value;
  
    // showTestContent가 false일 때 차트를 다시 그립니다.
    if (!showTestContent.value) {
      setTimeout(() => {
        drawElevationChart(routeCoordinates.value);
      }, 0); // DOM이 업데이트된 후 차트를 그리기 위해 약간의 딜레이 추가
    }
  };

  const isMenuOpen = ref(false);

  const toggleDropdown = () => {
    isMenuOpen.value = !isMenuOpen.value;

    const dropdownMenu = document.querySelector('.dropdown-menu');
    if (isMenuOpen.value) {
      dropdownMenu.classList.add('show');
    } else {
      dropdownMenu.classList.remove('show');
    }
  };

  
  </script>
  
  
  
  
  <style scoped>
  @font-face {
    font-family: 'TheJamsil5Bold';
    src: url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2') format('woff2');
    font-weight: 700;
    font-style: normal;
  }
  
  .toggle-switch {
    width: 50px;
    height: 24px;
    background-color: #ccc;
    border-radius: 12px;
    cursor: pointer;
    position: relative;
    transition: background-color 0.3s;
    margin-left: 17.8rem;
  }
  
  .toggle-switch .toggle-thumb {
    width: 22px;
    height: 22px;
    background-color: #fff;
    border-radius: 50%;
    position: absolute;
    top: 1px;
    left: 1px;
    transition: transform 0.3s;
  }
  
  .toggle-switch .toggle-thumb.active {
    transform: translateX(26px);
  }
  
  .toggle-switch.active {
    background-color: #4caf50;
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
  
  
  .ModalTitle{
    font-family: 'HakgyoansimBadasseugiTTF-L';
    font-weight: bold;
    font-style: normal;
    font-size: 30px;
  }
  
  #ModalContent{
    font-family: 'HakgyoansimBadasseugiTTF-L';
    font-weight: bolder;
    font-style: normal;
    font-size: 18px;
    margin-bottom: 1em;
  }
  
  .container {
  position: relative;
  height: 100vh;
  overflow: hidden;
  }
  
  /* 버튼 그룹 스타일 */
  .button-group {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  }
  
  .btn-group-vertical {
    position: absolute;
    font-size: 0.5rem !important;
    top: 100px;
    right: 0;
    z-index: 2;
    width: 85px;
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
  position: absolute;
  left: 0;
  right: 0;
  /* height: calc(150vh - 300px); */
  background-color: #ffffff;
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
  transition: top 0.3s ease-in-out;
  cursor: grab;
  overflow: hidden;
  z-index: 30; /* 드래그 패널을 지도 위에 오도록 설정 */
  box-shadow: 0 -2px 5px rgba(0, 0, 0, 0.3);
  margin-top: 0.1rem;
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
.dropdown-menu {
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
.dropdown-menu.show {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
  z-index: 30;
}

/* 드롭다운 아이템 버튼 스타일 */
.dropdown-item {
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
.dropdown-item:hover {
  transform: scale(1.1); /* 호버 시 버튼 확대 */
  background-color: #327C2B; /* 버튼 배경색 변경 */
  color: white; /* 글자 색 변경 */
  z-index: 30;
}

/* 버튼 등장 애니메이션 */
.dropdown-item {
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
  
  /* 전체 오버레이 */
  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999; /* z-index를 가장 높게 설정 */
    opacity: 0;
    animation: fadeIn 0.8s forwards; /* 천천히 나타나는 애니메이션 */
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

#elevationChart {
  width: auto;
  height: auto;
}
  </style>
  
  
  