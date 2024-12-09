<template>
  <div class="container">
    <!-- 모바일 헤더 -->
    <HeaderView class="header" />
    <div class="row content">
      <div v-if="communityState">
        <button class="btn btn-success btn-sm mb-3 mt-3 ms-3" @click="getAllList()">모든 게시물 보기</button>
      </div>
      <div v-else>
        <button class="btn btn-success btn-sm mb-3 mt-3 ms-3" @click="getSafeList()">안전 게시물 보기</button>
      </div>

      <div class="col-md-12">
        <!-- 글쓰기 버튼 추가 -->
        <div class="write-button-container fixed-button-container mt-3 mx-4">
          <button class="btn btn-lg custom-btn2" @click="goToWritePost()">
            <svg xmlns="http://www.w3.org/2000/svg" width="35" height="35" fill="currentColor"
              class="bi bi-plus-circle me-2" viewBox="0 0 16 16" style="margin-left: -12px; margin-top: -5px;">
              <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16" />
              <path
                d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4" />
            </svg>
          </button>
        </div>

        <div v-for="community in communityList" :key="community.communityPk" class="card shadow-sm">
          <div class="scrollable-content">
            <div class="community-item">
              <div class="d-flex align-items-center">
                <img src="/images/profile.png" alt="프로필 사진" class="profile-image me-2" />
                <p class="text-muted mt-4" id="userNickName">{{ community.userNickName }}</p>
              </div>

              <div class="image-map-container mb-2">
                <img v-if="community.communityUrl" :src="community.communityUrl" alt="게시글 이미지" class="img-fluid"
                  style="width: 500px; height: 225px; object-fit: cover;" @click="goToCommunityDetail(community)" />
                <div class="date-overlay" id="communityRegDate">{{ community.communityRegDate }}</div>


                <!-- 지도 표시 -->
                <div v-if="community.showMap" :id="'map-' + community.communityPk" class="map-overlay"
                  @click="goToMapDetail(community.courseName)" style="cursor: pointer;"></div>
              </div>

              <div class="d-flex justify-content-between align-items-center ms-2" id="my-container">
                <div class="d-flex align-items-center">
                  <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" fill="currentColor"
                    class="bi bi-heart like-icon" @click="Like(community.communityPk)"
                    :class="{ liked: community.liked }" viewBox="0 0 16 16">
                    <path
                      d="m8 2.748-.717-.737C5.6.281 2.514.878 1.4 3.053c-.523 1.023-.641 2.5.314 4.385.92 1.815 2.834 3.989 
                      6.286 6.357 3.452-2.368 5.365-4.542 6.286-6.357.955-1.886.838-3.362.314-4.385C13.486.878 10.4.28 8.717 
                      2.01zM8 15C-7.333 4.868 3.279-3.04 7.824 1.143q.09.083.176.171a3 3 0 0 1 .176-.17C12.72-3.042 23.333 4.867 8 15"
                      :class="{ 'text-danger': community.liked }" />
                  </svg>

                  <span class="like-count ms-1">{{ community.likes }}</span>

                  <span class="comment-icon ms-2">
                    <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" fill="currentColor"
                      class="bi bi-chat-left-dots" viewBox="0 0 16 16">
                      <path
                        d="M14 1a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H4.414A2 2 0 0 0 3 11.586l-2 2V2a1 1 0 0 1 1-1zM2 0a2 2 0 0 0-2 2v12.793a.5.5 0 0 0 .854.353l2.853-2.853A1 1 0 0 1 4.414 12H14a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2z" />
                      <path
                        d="M5 6a1 1 0 1 1-2 0 1 1 0 0 1 2 0m4 0a1 1 0 1 1-2 0 1 1 0 0 1 2 0m4 0a1 1 0 1 1-2 0 1 1 0 0 1 2 0" />
                    </svg>
                  </span>
                  <span class="comment-count ms-2">{{ community.commentCount }}</span>
                </div>
              </div>

              <div class="d-flex ms-2">
                <span class="text-muted me-2" id="userNickName">{{ community.userNickName }}</span>
                <span class="card-text" id="cardContent">{{ community.communityBody }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      
    </div>
    
  </div>

  
</template>

<script setup>
/* global kakao */
import HeaderView from '@/components/HeaderView.vue';
import { ref, computed, onMounted, nextTick } from 'vue';
import axios from 'axios';
import router from '@/router/index.js';

// API에서 게시글 목록을 가져오는 함수
const communityList = ref([]);

// 커뮤니티별 지도를 저장할 객체
const communityMap = ref({});

// 검색어를 저장할 변수
const searchQuery = ref('');

// PMNTN_NM별 데이터를 저장하는 객체
const courseData = ref({});

const course = ref([]);

const communityState = ref(false);

const showMap = ref('photo');

const getAllList = async () => {
  try {
    await getList(); // 모든 게시물 로드
    communityState.value = false;

    await nextTick(); // DOM 업데이트 후 실행
    communityList.value.forEach(community => {
      drawCourseOnMap(community.courseName, community.communityPk, community.lat, community.lng);
    });
  } catch (error) {
    console.error(error);
  }
};

const getSafeList = async () => {
  try {
    const response = await axios.post('/api/safeList');
    communityList.value = response.data.map(community => ({
      ...community,
      showMap: true,
      likes: community.likes || 0,
      commentCount: community.commentCount || 0,
      courseName: community.courseName,
      lat: community.lat || community.latitude,
      lng: community.lng || community.longitude
    }));

    await nextTick(); // DOM 업데이트 후 실행
    communityList.value.forEach(community => {
      drawCourseOnMap(community.courseName, community.communityPk, community.lat, community.lng);
    });
    communityState.value = true;
  } catch (error) {
    console.error("Error during safe list loading:", error);
  }
};



// 검색 결과를 필터링하는 computed
const filteredCommunityList = computed(() => {
  return communityList.value.filter(community => {
    return community.communityBody.includes(searchQuery.value) ||
      community.userNickName.includes(searchQuery.value);  // 제목이나 내용에서 검색어를 포함하는 게시글만 표시
  });
});


// 글쓰기 페이지로 이동하는 함수
const goToWritePost = () => {
  router.push({ path: '/writePost' }); // 글쓰기 페이지 경로로 변경
};

// 상세 페이지로 이동하는 함수 추가
const goToCommunityDetail = (community) => {
  router.push({ path: `/communityDetailView`, query: { community: JSON.stringify(community) } });
};


function createMap(containerId, startLat, startLng) {
  const container = document.getElementById(containerId);
  if (!container) {
    console.error(`Element with ID ${containerId} not found`);
    return;
  }

  const options = {
    center: new kakao.maps.LatLng(startLat, startLng),
    level: 6,
  };

  // 지도 객체 생성
  const map = new kakao.maps.Map(container, options);

  // 생성된 지도를 communityMap 객체에 저장
  communityMap.value[containerId] = map;

  return map;
}

// 카드 클릭 시 호출되는 함수
const goToMapDetail = async (courseName) => {
  try {
    // 좋아요 기능이 필요하다면 별도의 함수로 호출
    const response = await axios.post('/api/course/byName', {
      courseName: courseName
    });

    course.value = response.data;

    // 페이지 이동
    router.push({
      name: 'mobilemountaindetailview',
      query: {
        course: JSON.stringify(course.value),
        mapType: showMap.value, // 현재 지도 상태 전달
      },
    });
  } catch (error) {
    console.error("Error during like or navigation:", error);
  }
};


// 처음에 모든 GeoJSON 데이터를 불러와 PMNTN_NM을 키로 하는 객체로 저장
const loadCourseData = async () => {
  try {
    const response = await axios.get('/data/인왕산ele copy.geojson');
    const geoJsonData = response.data;

    geoJsonData.features.forEach(feature => {
      const courseName = feature.properties.PMNTN_NM;
      const coordinates = feature.geometry.coordinates
        .reduce((acc, lineString) => acc.concat(lineString.map(coord => ({ lat: coord[1], lng: coord[0] }))), []);

      // 누적 방식으로 각 코스의 좌표를 추가
      if (!courseData.value[courseName]) {
        courseData.value[courseName] = [];
      }
      courseData.value[courseName].push(...coordinates);
    });

    console.log("Course Data Loaded:", courseData.value);
  } catch (error) {
    console.error("GeoJSON 데이터를 불러오는 중 오류 발생:", error);
  }
};

const drawCourseOnMap = (courseName, communityPk, lat, lng) => {
  const mapId = `map-${communityPk}`;
  resetMap(mapId); // 지도 초기화

  // courseData의 키 값 중에서 courseName을 포함하는 키를 찾음
  const matchingCourseKey = Object.keys(courseData.value).find(key =>
    key.includes(courseName)
  );

  if (!matchingCourseKey) {
    console.error(`Coordinates not found for course: ${courseName}`);
    return;
  }

  const coordinates = courseData.value[matchingCourseKey];
  const linePath = coordinates.map(coord => new kakao.maps.LatLng(coord.lat, coord.lng));

  let map = communityMap.value[mapId];
  if (!map) {
    map = createMap(mapId, lat, lng);
  }

  if (map) {
    const polyline = new kakao.maps.Polyline({
      path: linePath,
      strokeWeight: 5,
      strokeColor: '#00FF00',
      strokeOpacity: 0.8,
      strokeStyle: 'solid',
    });
    polyline.setMap(map); // 새로운 경로 추가

    console.log(`Polyline set for course: ${matchingCourseKey}`, polyline.getPath());
  } else {
    console.error(`Map not found for map ID: ${mapId}`);
  }
};


function resetMap(containerId) {
  const map = communityMap.value[containerId];
  if (map) {
    // 지도 상의 모든 경로 제거
    map.removeOverlayMapTypeId(kakao.maps.MapTypeId.POLYLINE);
    communityMap.value[containerId] = null;
  }
}


onMounted(async () => {
  const script = document.createElement('script');
  script.onload = async () => {
    kakao.maps.load(async () => {
      await loadCourseData();  // GeoJSON 데이터를 한 번만 로드
      await getList();  // `communityList` 채운 후
      await nextTick();  // DOM 업데이트 후 drawCourseOnMap 호출

      communityList.value.forEach(community => {
        drawCourseOnMap(community.courseName, community.communityPk, community.lat, community.lng);
      });
    });
  };
  script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false';
  document.head.appendChild(script);
});

const getList = async () => {
  try {
    const response = await axios.get('/api/communityList');
    communityList.value = response.data.map(community => ({
      ...community,
      showMap: true,
      likes: community.likes || 0,
      commentCount: community.commentCount || 0,  // 댓글 수 초기화
      courseName: community.courseName,
      lat: community.lat || community.latitude,
      lng: community.lng || community.longitude
    }));

    // 게시글 목록을 필터링된 목록에 복사
    filteredCommunityList.value = communityList.value;

    // 게시글을 communityRegDate 기준으로 내림차순 정렬 (날짜와 시간 모두 고려)
    communityList.value.sort((a, b) => new Date(b.communityRegDate) - new Date(a.communityRegDate));

    await nextTick();  // DOM 업데이트 후 실행

    communityList.value.forEach(community => {
      const mapId = `map-${community.communityPk}`;
      createMap(mapId, community.lat, community.lng); // 좌표에 맞는 지도 생성
    });
    console.log(communityList.value);
  } catch (err) {
    console.error(err);
  }
};


const Like = async (communityPk) => {
  try {
    const userId = localStorage.getItem('userId');  // 로그인된 사용자 ID 가져오기
    if (!userId) {
      console.log("User not logged in");
      return;
    }

    // 서버로 좋아요 상태 변경 요청
    const response = await axios.post('/api/like', {
      userId: userId,
      communityPk: communityPk
    });

    communityList.value = response.data.map(community => ({
      ...community,
      showMap: true,
      likes: community.likes || 0,
      commentCount: community.commentCount || 0,  // 댓글 수 초기화
      lat: community.lat || community.latitude,
      lng: community.lng || community.longitude
    }));


  } catch (error) {
    console.log("Error liking the post:", error);
  }
};
</script>


<style scoped>


/* 고정 헤더 스타일 */
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

/* 고정 푸터 스타일 */
.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.info-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  /* 배경 색 */
  z-index: 2;
  /* 이미지 위에 보이게 */
}

.styled-button {
  border: 2px solid #4CAF50;
  /* 테두리 추가 */
  border-radius: 5px;
  /* 모서리 둥글게 */
  padding: 8px 16px;
  /* 패딩 조절 */
  transition: background-color 0.3s, transform 0.2s;
  /* 효과 추가 */
}

#map {
  height: 400px;
  width: 100%;
}

body {
  background: linear-gradient(to bottom, #fafafa, #ffffff); /* 밝은 그라데이션 */
}

.container {
  background: #ffffff; /* 배경을 흰색으로 */
  max-width: 900px; /* 중앙 정렬 */
  margin: auto; /* 화면 중앙에 위치 */
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05); /* 가벼운 그림자 */
  border-radius: 10px; /* 부드러운 모서리 */
  padding: 20px;
}

.main-content {
  width: 70%; /* 메인 컨텐츠 넓이 */
  padding-right: 20px;
}

.sidebar {
  width: 25%; /* 사이드바 넓이 */
  background-color: #fff;
  border-radius: 15px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  font-family: 'Roboto', sans-serif;
}

.sidebar-item {
  margin-bottom: 20px;
}

.sidebar-item h5 {
  font-size: 18px;
  font-weight: bold;
  color: #262626;
  margin-bottom: 10px;
}

.sidebar-item ul {
  list-style-type: none;
  padding: 0;
}

.sidebar-item ul li {
  margin-bottom: 8px;
  font-size: 14px;
  color: #555;
}

.sidebar-item ul li:hover {
  text-decoration: underline;
  cursor: pointer;
}

.content {
  flex: 1;
  /* 남은 공간을 채우도록 설정 */
  overflow-y: auto;
  /* 세로 스크롤 활성화 */
  padding-top: 70px;
  /* 헤더 공간 */
  padding-bottom: 70px;
  /* 푸터 공간 */
  margin-top: 25%;
}

.card {
  transition: transform 0.2s;
  /* 카드에 호버 효과 */
  background-color: white;
  /* 카드 배경색 */
  min-height: 350px;
  /* 카드의 최소 높이 설정 */
  border-radius: 20px;
  /* 카드 테두리 동그랗게 설정 */
  border: none;
  /* 카드의 테두리 제거 */
  max-width: 700px;
  /* 카드의 최대 가로 크기 설정 */
  margin: 0 auto;
  /* 카드를 중앙 정렬 */
  display: flex;
  flex-direction: column;
  word-wrap: break-word;
  /* 단어가 길 때 자동으로 줄바꿈 */
  overflow: visible;
  /* 자식 요소의 그림자가 잘리지 않도록 설정 */
}

.card-body {
  flex-grow: 1;
  /* 카드 안의 내용이 남은 공간을 채우도록 설정 */
  display: flex;
  flex-direction: column;
  /* 카드 내용도 세로로 배치 */
  overflow: hidden;
  /* 내용이 넘쳐도 스크롤이 생기지 않도록 설정 */
}


.like-icon:hover {
  color: #FF5252; /* 빨간색 */
  transform: scale(1.2); /* 살짝 확대 */
  transition: 0.2s ease-in-out;
}

.comment-icon:hover {
  color: #007BFF; /* 파란색 */
  transform: scale(1.2);
  transition: 0.2s ease-in-out;
}



.scrollable-content {
  max-height: 300%;
  /* 자식 요소의 최대 높이 설정 */
  overflow-y: auto;
  /* 세로 스크롤 가능 */
  padding: 10px;
  /* 내부 여백 */
  background: transparent;
  position: relative;
  /* 그림자 효과가 부모에 영향을 받지 않도록 절대 위치 설정 */
  z-index: 10;
  /* 다른 요소보다 위에 위치하도록 설정 */
  margin: 13px;
  border-radius: 15px;
}

/* 추가된 스타일 */
.separator-line {
  border: none;
  border-top: 1px solid white;
  /* 구분선 스타일 */
  margin: 20px 0;
  /* 여백 설정 */
}

/* 웹킷 기반 브라우저용 스크롤바 숨기기 */
.scrollable-content::-webkit-scrollbar {
  width: 0;
  /* 스크롤바 너비를 0으로 설정 */
}

/* Firefox에서 스크롤바 숨기기 */
.scrollable-content {
  scrollbar-width: none;
  /* Firefox에서 스크롤바 숨기기 */
}

/* 이미지와 지도 컨테이너 스타일 */
.image-map-container {
  position: relative;
  width: 100%;

}

.image-map-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* 이미지 비율 유지하며 크기를 조절 */
  border-radius: 15px;
}

.map-overlay {
  position: absolute;
  /* 절대 위치 지정 */
  bottom: 0;
  /* 컨테이너의 아래쪽에 정렬 */
  margin-bottom: 2.5%;
  margin-right: 2.5%;
  right: 0;
  /* 컨테이너의 오른쪽에 정렬 */
  width: 80px;
  /* 너비를 원하는 크기로 설정 */
  height: 80px;
  /* 높이를 원하는 크기로 설정 */
  border: 2px solid white;
  /* 테두리 설정 */
  border-radius: 15px;
  /* 테두리 모서리를 둥글게 설정 */
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.date-overlay {
  position: absolute;
  /* 절대 위치 설정 */
  top: 2px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
  /* 그림자 효과 */
  bottom: 10px;
  /* 이미지의 아래쪽에서 10px 떨어지도록 설정 */
  left: 10px;
  /* 이미지의 왼쪽에서 10px 떨어지도록 설정 */
  color: white;
  /* 텍스트 색상 설정 (가독성을 위해) */
  background-color: rgba(0, 0, 0, 0);
  /* 배경색과 투명도 설정 */
  padding: 5px;
  /* 텍스트 주위에 패딩 추가 */
  border-radius: 5px;
  /* 모서리를 둥글게 설정 */
}

/* 추가된 스타일 */
.separator-line {
  border: none;
  border-top: 1px solid white;
  margin: 20px 0;
}

@font-face {
  font-family: 'TheJamsil5Bold';
  src: url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2') format('woff2');
  font-weight: 700;
  font-style: normal;
}

h1,
h2,
h3,
p {
  /* 특정 요소에 폰트 적용 */
  font-family: 'TheJamsil5Bold', sans-serif !important;
}

#cardContent {
  font-family: 'TheJamsil4Bold';
  font-weight: 500;
  font-size: 14px;
}

#userNickName {
  font-family: 'TheJamsil5Bold';
  font-weight: 600;
  font-size: 14px;
  margin-left: 2;
  /* 왼쪽 정렬 */
  text-align: left;
  /* 왼쪽 정렬 */
  width: 500px;
  /* 고정 너비 설정 */
  min-width: 50px;
  /* 최소 너비 설정 */
}

#communityRegDate {
  font-family: 'TheJamsil4Bold';
  font-weight: 700;
  font-size: 15px;
}

.info-overlay {
  position: absolute;
  /* 이미지 위에 배치 */
  bottom: 0;
  /* 이미지의 아래쪽에 정렬 */
  left: 0;
  /* 이미지의 왼쪽에 정렬 */
  right: 0;
  /* 이미지의 오른쪽에 정렬 */
  display: flex;
  /* 가로로 나열 */
  justify-content: flex-end;
  /* 수평 방향으로 오른쪽 정렬 */
  align-items: flex-end;
  /* 세로 방향으로 아래쪽에 정렬 */
  padding: 10px;
  /* 패딩 추가 */
  background: rgba(255, 255, 255, 0);
  /* 배경을 완전히 투명하게 설정 */
  z-index: 2;
  /* 이미지 위에 보이게 */
}

.overlay-content {
  display: flex;
  /* 가로로 나열 */
  justify-content: space-around;
  /* 항목 간격을 균등하게 배치 */
  align-items: center;
  /* 수직 정렬 */
  gap: 20px;
  /* 아이템 간격 */
}

.overlay-title {
  margin: 0px;
  /* 마진 제거 */
  font-size: 17px;
  /* 폰트 크기 조절 */
  color: white;
  /* 글씨 색을 흰색으로 설정 */
  font-weight: bold;
  /* 글씨를 두껍게 설정 */
}

.card-group {
  display: flex;
  /* 가로로 나열 */
  width: 100%;
  /* 카드 그룹의 너비를 100%로 설정 */
}

.info-card {
  width: 150px;
  /* 카드의 너비 설정 */
  height: auto;
  /* 카드의 높이 설정 */
  margin: 0 5px;
  /* 카드 간격 */
  color: white;
  background-color: rgba(255, 255, 255, 0);
}

.custom-btn {
  padding: 4px 7px;
  /* 버튼의 상하/좌우 패딩 조정 */
  font-size: 10px;
  /* 버튼의 글씨 크기 조정 */
  border-radius: 20px;
}

.profile-image {
  width: 40px;
  /* 원하는 너비 */
  height: 40px;
  /* 원하는 높이 */
  border-radius: 50%;
  /* 동그란 형태로 만들기 */
  object-fit: cover;
  /* 이미지 비율 유지 */
}


.back-button-container {
  text-align: left;
  /* 버튼을 왼쪽에 정렬 */
  margin-top: 10px;
}

/* 스타일 정의 */
.location-text {
  font-size: 0.70em;
  /* 폰트 크기를 줄입니다 */
  font-weight: 100;
  /* 얇은 글씨 */
  margin: 0;
  padding: 0;
  text-decoration: underline;
  /* 밑줄 추가 */
  color: #555;
  /* 텍스트 색상 조정 (필요 시 수정 가능) */
  text-align: left;
  /* 왼쪽 정렬 */
  margin-left: 2px;
}

.fixed-button-container {
  position: fixed;
  bottom: 100px;
  /* 원하는 위치에 맞게 조정 */
  right: 20px;
  /* 원하는 위치에 맞게 조정 */
  z-index: 9999;
  /* 가능한 높은 값으로 설정 */
}

.custom-btn2 {
  background-color: #FF5E57; /* 강렬한 레드/오렌지 */
  color: white;
  border: none;
  border-radius: 50%; /* 원형 버튼 */
  width: 60px;
  height: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 1.5rem;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); /* 버튼 그림자 */
  transition: transform 0.2s;
}

.custom-btn2:hover {
  transform: scale(1.1); /* 약간 확대 */
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.3); /* 그림자 강조 */
}

#my-container .d-flex.align-items-center {
  height: 30px;
  /* 원하는 높이로 조정 */
  padding: 0;
  /* 패딩 제거 */
  margin: 0;
  /* 마진 제거 */
}

#my-container {
  padding: 0;
  /* 패딩 제거 */
  height: 35px;
  /* 원하는 높이로 조정 */
}

#userNickName {
  flex: 0 0 15%;
  /* userNickName이 25% 너비를 차지하도록 설정 */
  font-size: 16px;
  font-weight: bold;
  word-wrap: break-word;
  /* 긴 단어가 있을 경우 줄바꿈 */
}

#cardContent {
  flex: 1;
  /* 나머지 공간을 모두 차지 */
  font-size: 14px;
  color: #333;
  word-wrap: break-word;
  /* 긴 텍스트가 있을 경우 줄바꿈 */
  text-align: left;
  /* 텍스트를 왼쪽 정렬 */
}

/* 컨텐츠 영역 스타일 */
.content {
  flex: 1;
  overflow-y: auto;
  padding-top: 0px;
  /* 게시글 내용과 검색창 간 간격 */
  padding-bottom: 70px;
  /* 푸터 공간 확보 */
}
</style>