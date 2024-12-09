<template>
  <div class="container">
    <!-- 모바일 헤더 -->
    <MobileHeaderView class="header" />
    <!-- 스위치 버튼 (안전 게시물/모든 게시물 보기) -->
    <div class="switch-container">
      <label class="switch">
        <input type="checkbox" v-model="communityState" @change="toggleCommunityState" />
        <span class="slider">
          <span class="switch-text">{{ communityState ? "안전" : "게시판" }}</span>
        </span>
      </label>
    </div>

    <div class="content">
      <div class="col-md-12">
        <!-- 글쓰기 버튼 -->
        <div class="write-button-container fixed-button-container">
          <button class="btn btn-lg custom-btn2" @click="goToWritePost()">
            <img src="/images/글쓰기.png" width="35px" height="auto" />
          </button>
        </div>

        <!-- 게시물 카드 리스트 -->
        <div v-for="community in communityList" :key="community.communityPk" class="card shadow-sm">
          <div class="scrollable-content">
            <div class="community-item">
              <div class="d-flex align-items-center" style="padding: 6px;">
                <img src="/images/profile.png" alt="프로필 사진" class="profile-image" />
                <p class="text-muted" id="userNickName" style="margin: 2px;">{{ community.userNickName }}</p>
              </div>

              <div class="image-map-container">
                <img
                  v-if="community.communityUrl"
                  :src="community.communityUrl"
                  alt="게시글 이미지"
                  class="img-fluid"
                  style="width: 500px; height: 225px; object-fit: cover;"
                  @click="goToCommunityDetail(community)"
                />
                <!-- 지도 표시 -->
                <div
                  v-if="community.showMap"
                  :id="'map-' + community.communityPk"
                  class="map-overlay"
                  @click="goToMapDetail(community.courseName)"
                  style="cursor: pointer;"
                ></div>
              </div>

              <div class="d-flex align-items-center" style="border-bottom: solid 1px black;">
                <div class="d-flex justify-content-between align-items-center" id="my-container">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="19"
                    height="19"
                    fill="currentColor"
                    style="margin-left: 15px;"
                    class="bi bi-heart like-icon"
                    @click="Like(community.communityPk)"
                    :class="{ liked: community.liked }"
                    viewBox="0 0 16 16"
                  >
                    <path
                      d="m8 2.748-.717-.737C5.6.281 2.514.878 1.4 3.053c-.523 1.023-.641 2.5.314 4.385.92 1.815 2.834 3.989 
                      6.286 6.357 3.452-2.368 5.365-4.542 6.286-6.357.955-1.886.838-3.362.314-4.385C13.486.878 10.4.28 8.717 
                      2.01zM8 15C-7.333 4.868 3.279-3.04 7.824 1.143q.09.083.176.171a3 3 0 0 1 .176-.17C12.72-3.042 23.333 4.867 8 15"
                      :class="{ 'text-danger': community.liked }"
                    />
                  </svg>
                  <span class="like-count ms-2" style="margin-right: 5px;">{{ community.likes }}</span>
                  <span class="comment-icon ms-2">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="19"
                      height="19"
                      fill="currentColor"
                      class="bi bi-chat-left-dots"
                      viewBox="0 0 16 16"
                    >
                      <path
                        d="M14 1a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H4.414A2 2 0 0 0 3 11.586l-2 2V2a1 1 0 0 1 1-1zM2 0a2 2 0 0 0-2 2v12.793a.5.5 0 0 0 .854.353l2.853-2.853A1 1 0 0 1 4.414 12H14a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2z"
                      />
                      <path
                        d="M5 6a1 1 0 1 1-2 0 1 1 0 0 1 2 0m4 0a1 1 0 1 1-2 0 1 1 0 0 1 2 0m4 0a1 1 0 1 1-2 0 1 1 0 0 1 2 0"
                      />
                    </svg>
                  </span>
                  <span class="comment-count ms-2">{{ community.commentCount }}</span>
                  <span
                    ><img
                      src="/images/공유.png"
                      style="width: 18px; height: 18px; margin-left: 10px; margin-bottom: 1px"
                  /></span>
                  <span class="date-overlay" id="communityRegDate">{{ community.communityRegDate }}</span>
                </div>
              </div>

              <div style="margin-bottom: 15px; padding: 5px;">
                <span id="userNickName2"><img src="/images/등산객.png" width="16px;">{{ community.userNickName }}</span>
                <div>
                  <!-- 텍스트 영역 -->
                  <div class="card-text" :class="{ expanded: community.expanded }" id="cardContent">
                    {{ community.communityBody }}
                  </div>
                  <!-- '...' 버튼 -->
                  <button
                    v-if="!community.expanded && community.communityBody.length > 50"
                    @click="toggleExpand(community)"
                    class="show-more-btn"
                  >
                    더 보기
                  </button>
                  <!-- '닫기' 버튼 -->
                  <button
                    v-if="community.expanded"
                    @click="toggleExpand(community)"
                    class="show-more-btn"
                  >
                    닫기
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <MobileFooterView5 class="footer"></MobileFooterView5>
  </div>
</template>


<script setup>
/* global kakao */
import MobileHeaderView from '@/components/MobileHeaderView.vue';
import MobileFooterView5 from "@/components/MobileFooterView5.vue";
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

const showMap = ref('photo'); // 기본 상태를 'default'로 설정

const toggleCommunityState = () => {
  if (communityState.value) {
    getSafeList();
  } else {
    getAllList();
  }
};

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
  background-color: white; /* 헤더 배경색 */
  border-bottom: 1px solid #ddd; /* 헤더 아래 구분선 */
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

.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* 전체 화면 높이 */
  overflow: visible;
  margin: 0; /* 외부 여백 제거 */
  padding: 0; /* 내부 여백 제거 */
}


/* 컨텐츠 영역 스타일 */
.content {
  flex: 1;
  overflow-y: auto;
  margin-top: 65px; /* 헤더 아래 여백 (기존 60px에서 70px로 추가 조정) */
  padding-bottom: 70px; /* 푸터 공간 확보 */
}

.card {
  margin: 2px auto; /* 카드 간격 */
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); /* 카드 그림자 */
  background-color: #fff; /* 카드 배경색 */
  overflow: hidden;
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

.scrollable-content {
  max-height: 300%;
  /* 자식 요소의 최대 높이 설정 */
  overflow-y: auto;
  /* 세로 스크롤 가능 */
  background: transparent;
  position: relative;
  /* 그림자 효과가 부모에 영향을 받지 않도록 절대 위치 설정 */
  z-index: 10;

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
  height: auto;
  object-fit: cover;
  margin-bottom: 10px; /* 이미지와 텍스트 사이 간격 */
  border: solid 1px #dedede;
}

.map-overlay {
  position: absolute;
  /* 절대 위치 지정 */
  bottom: 15px;
  right: 5px;
  /* 컨테이너의 오른쪽에 정렬 */
  width: 80px;
  /* 너비를 원하는 크기로 설정 */
  height: 80px;
  /* 높이를 원하는 크기로 설정 */
  border: 2px solid #dedede;
  /* 테두리 설정 */
  border-radius: 15px;
  /* 테두리 모서리를 둥글게 설정 */
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
  padding: 2px;
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
  flex: 0 0 15%;
  /* userNickName이 25% 너비를 차지하도록 설정 */
  font-size: 16px;
  font-weight: bold;
  word-wrap: break-word;
  /* 긴 단어가 있을 경우 줄바꿈 */
}

#userNickName2 {
  font-family: 'TheJamsil5Bold';
  margin-left: 2;
  flex: 0 0 15%;
  /* userNickName이 25% 너비를 차지하도록 설정 */
  font-size: 14px;
  font-weight: bold;
  margin-left: 5px;
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

/* 프로필 이미지 스타일 */
.profile-image {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid black; /* 테두리 */
  margin-right: 10px;
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
  bottom: 100px; /* 화면 아래쪽 여백 */
  right: 20px; /* 화면 오른쪽 여백 */
  z-index: 1000; /* 다른 요소 위에 표시 */
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px; /* 버튼과 다른 요소 간의 간격 */
}

/* 안전 게시물/모든 게시물 보기 버튼 */
.fixed-button-container .btn {
  background-color: #ffffff;
  border: solid 1px #4caf50; /* 테두리 */
  border-radius: 20px; /* 둥근 버튼 */
  color: #4caf50; /* 텍스트 색상 */
  font-size: 12px; /* 폰트 크기 */
  padding: 5px 10px; /* 패딩 */
  box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.2); /* 버튼 그림자 */
  transition: background-color 0.2s ease-in-out, color 0.2s ease-in-out; /* 애니메이션 효과 */
}

.fixed-button-container .btn:hover {
  background-color: #4caf50; /* hover 시 배경색 */
  color: #ffffff; /* hover 시 텍스트 색상 */
}

/* 글쓰기 버튼 */
.custom-btn2 {
  background-color: #4caf50; /* 글쓰기 버튼의 배경색 */
  color: white; /* 텍스트 색상 */
  border: solid 1px #4caf50; /* 테두리 */
  border-radius: 50%; /* 둥근 모양 */
  width: 55px; /* 버튼 크기 */
  height: 55px; /* 버튼 크기 */
  padding: 10px; /* 내부 여백 */
  box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.3); /* 그림자 효과 */
  display: flex;
  justify-content: center;
  align-items: center;
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out; /* 애니메이션 효과 */
  cursor: pointer;
}

.custom-btn2:hover {
  transform: scale(1.1); /* 버튼 크기 확대 효과 */
  box-shadow: 0px 6px 8px rgba(0, 0, 0, 0.5); /* hover 시 그림자 강조 */
}

/* 글쓰기 아이콘 내부 이미지 */
.custom-btn2 img {
  width: 40px; /* 아이콘 크기 조정 */
  height: auto;
  left: 1px;
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

#my-container{
  margin-bottom: 5px;
  margin-right: 15px;
}


.card-text {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-wrap: break-word;
  overflow-wrap: break-word;
  white-space: normal;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.card-text.expanded {
  display: block;
  white-space: normal;
  overflow: visible;
}

.show-more-btn {
  background: none;
  font-weight: bold;
  border: none;
  color: #9e9e9e;
  cursor: pointer;
  font-size: 12px;
  margin-top: 2px;
  margin-left: 3px;
}

.show-more-btn:hover {
  text-decoration: underline;
}

.date-overlay{
  margin-left:140px;
  font-family: 'TheJamsil4Bold';
  font-size: 14px;
}

/* 스위치 창 스타일 */
.switch-container {
  position: fixed;
  top: 25px;
  left: 160px;
  z-index: 1100;
  display: flex;
  align-items: center;
  border: solid 1px black;
  border-radius: 50px;
}

.switch {
  position: relative;
  display: inline-block;
  width: 60px; /* 전체 스위치 너비 */
  height: 24px; /* 전체 스위치 높이 */
}

.switch input {
  opacity: 0; /* 숨김 */
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc; /* OFF 상태 배경 */
  border-radius: 50px; /* 둥근 모서리 */
  transition: background-color 0.4s; /* 전환 효과 */
  display: flex;
  justify-content: center;
  align-items: center;
}

.switch-text {
  font-size: 12px;
  font-weight: bold;
  color: black; /* 기본: 검정 */
  text-transform: uppercase;
  transition: color 0.4s; /* 텍스트 색상 전환 효과 */
}

input:checked + .slider {
  background-color: #4caf50; /* ON 상태 배경 */
}

input:checked + .slider .switch-text {
  color: white; /* 안전일 때: 흰색 */
}

input:not(:checked) + .slider {
  background-color: #fefefe; /* OFF 상태 배경 */
}

input:not(:checked) + .slider .switch-text {
  color: black; /* 기본일 때: 검정 */
}



</style>