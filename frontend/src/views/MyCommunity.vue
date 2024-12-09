<template>
  <div class="container">
    <!-- 모바일 헤더 -->
    <MobileHeaderView class="header" />
    <!-- 스위치 버튼 (안전 게시물/모든 게시물 보기) -->
     <div class="btn-group-container">
        <div class="btn-group" role="group" aria-label="Basic radio toggle button group">
          <input type="radio" class="btn-check" name="btnradio" id="btnradio1" autocomplete="off" @click="goToRecord()"/>
          <label class="btn btn-outline-success" for="btnradio1">운동</label>

          <input type="radio" class="btn-check" name="btnradio" id="btnradio2" autocomplete="off" @click="goToRecordImg()"/>
          <label class="btn btn-outline-success" for="btnradio2">기록</label>

          <input type="radio" class="btn-check" name="btnradio" id="btnradio3" autocomplete="off" checked />
          <label class="btn btn-outline-success" for="btnradio3">나의 커뮤니티</label>
        </div>
      </div>

     <!-- 카드 컨테이너 -->
      <div class="record-summary" style="margin-top: 60px; margin-bottom: 70px; margin-left: 7px;">
        <div class="image-container-wrapper">
          <div v-for="community in communityList" :key="community.communityPk" class="image-container2">
            <div class="scrollable-content">
              <div class="community-item">
                <span class="title-container">
                  <span class="title-text" >
                    <img src="/images/메모.png" alt="메모 아이콘" class="memo-icon" style="width: 16px; height: 16px; margin-left: 1px; margin-right: 2px;"/>
                    {{ community.communityTitle }}
                  </span>
                  <button @click="toggleMap(community)" class="custom-btn">
                    {{ community.showInfo ? "기록" : "경로" }}
                  </button>
                </span>
                <p class="location-text"> <img src="/images/기본.png" alt="" style="width: 13px; height: 13px;">서울 종로구 무악동</p>

                <div class="image-map-container">
                  <img
                    v-if="community.communityUrl"
                    :src="community.communityUrl"
                    alt="게시글 이미지"
                    class="img-fluid"
                    style="width: 500px; height: 225px; object-fit: cover"/>

                  <!-- 지도 표시 -->
                  <div
                    v-if="community.showMap"
                    :id="'map-' + community.communityPk"
                    class="map-overlay"
                    @click="goToMapDetail(community.lat, community.lng)"
                    style="cursor: pointer"
                  ></div>

                  <!-- 정보 표시 -->
                  <div v-if="community.showInfo" class="info-overlay">
                    <div class="overlay-content">
                      <span class="overlay-title">Time 33:00</span>
                      <span class="overlay-title">Distance 3.40km</span>
                      <span class="overlay-title">Pace 8'37"</span>
                    </div>
                  </div>
                </div>

                <div class="d-flex justify-content-between " style="border-bottom: solid 1px black; margin-bottom: 3px;">
                  <div class="d-flex ">
                    <div class="d-flex align-items-center me-2">
                      <span
                        class="like-icon"
                        @click="toggleLike(community.communityPk)"
                        :class="{ liked: community.liked }"
                        >{{ community.liked ? "💚" : "🤍" }}</span
                      >
                      <span class="like-count ms-1">{{ community.likeCount }}</span>
                    </div>
                    
                    <div class="d-flex align-items-center me-2">
                      <span class="comment-icon">💬</span>
                      <span class="comment-count ms-1">{{ community.commentCount }}</span>
                    </div>
                  </div>
                  <span class="community-date" style="margin-top: 3px; font-size: 14px;">{{ community.communityRegDate }}</span>
                </div>
                <div class="d-flex align-items-center ms-2" style="margin-bottom: 10px; margin-top: 5px;">
                  <img
                    v-if="community.profileImageUrl"
                    :src="community.profileImageUrl"
                    alt="프로필 사진"
                    class="profile-image me-2"/>
                  <span class="text-muted me-2" id="userNickName">{{community.userNickName}}</span
                  ><br />
                  <span class="card-text" id="cardContent">{{community.communityBody}}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
    </div>
  </div>
  <MobileFooterView3 class="footer"></MobileFooterView3>
</template>


<script setup>
import MobileHeaderView from "@/components/MobileHeaderView.vue";
import MobileFooterView3 from "@/components/MobileFooterView3.vue";
/* global kakao */
import { ref, onMounted, nextTick } from "vue";
import axios from "axios";
import router from "@/router/index.js";
//import HeaderView from '../components/HeaderView.vue'; // 상대 경로로 수정

// 지도 객체를 저장할 변수
//const map = ref(null);

const goToMapDetail = (lat, lng) => {
  router.push({ path: "/mapdetail", query: { lat, lng } });
};

// 카카오 맵 생성 함수
function createMap(containerId, lat, lng) {
  const container = document.getElementById(containerId);
  if (!container) {
    console.error(`Element with ID ${containerId} not found`);
    return;
  }
  const options = {
    center: new kakao.maps.LatLng(lat, lng),
    level: 5, // 지도 확대 수준
  };
  const map = new kakao.maps.Map(container, options);

  // 마커 생성
  const markerPosition = new kakao.maps.LatLng(lat, lng);
  const marker = new kakao.maps.Marker({
    position: markerPosition,
  });

  // 지도에 마커 표시
  marker.setMap(map);
}

// function goToCommunityDetail() {
//   router.replace({ path: '/communitydetail', query: {} });
// }

// 게시글 목록을 저장할 변수
const communityList = ref([]);

// API에서 게시글 목록을 가져오는 함수
onMounted(() => {
  const script = document.createElement("script");
  script.onload = () => {
    kakao.maps.load(() => {
      getList(); // 게시글 목록을 가져온 후 지도 생성
    });
  };
  script.src =
    "https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=clusterer,services";
  document.head.appendChild(script);
});

const getList = async () => {
  console.log("get 실행");
  try {
    const response = await axios.get("/api/communityList");
    communityList.value = response.data.map((community) => ({
      ...community,
      showMap: true, // 초기에는 지도 보이도록 설정
      showInfo: false, // 초기에는 정보는 숨김
      likeCount: community.likeCount || 0, // 좋아요 수 (없으면 0으로 기본값)
      commentCount: community.commentCount || 0, // 댓글 수 (없으면 0으로 기본값)
      lat: community.lat || community.latitude, // 위도 (서버에서 받아와야 함)
      lng: community.lng || community.longitude, // 경도 (서버에서 받아와야 함)
    }));
    console.log(response.data);
    // nextTick을 사용하여 DOM이 완전히 렌더링된 후에 지도 생성
    await nextTick(); // DOM 업데이트 후 실행
    communityList.value.forEach((community) => {
      const mapId = `map-${community.communityPk}`;
      createMap(mapId, community.lat, community.lng); // 좌표에 맞는 지도 생성
    });
  } catch (err) {
    console.log(err);
  }
};

// 정보와 지도를 번갈아 표시하는 함수
const toggleMap = async (community) => {
  community.showInfo = !community.showInfo; // 정보 표시 상태를 토글
  community.showMap = !community.showMap; // 지도 표시 상태를 토글

  // 만약 지도를 보여주는 경우
  if (community.showMap) {
    await nextTick(); // DOM 업데이트가 완료될 때까지 기다림
    const mapId = `map-${community.communityPk}`;
    createMap(mapId, community.lat, community.lng); // 지도 생성
  }
};

// 좋아요 토글 함수
const toggleLike = (communityPk) => {
  const post = communityList.value.find((c) => c.communityPk === communityPk);
  if (post) {
    post.liked = !post.liked; // 좋아요 상태를 반전

    // 좋아요 개수 업데이트
    post.liked ? post.likeCount++ : post.likeCount--;

    // 서버에 좋아요 상태를 업데이트 요청 (필요한 경우)
    // await axios.post(`/api/community/${communityPk}/like`, { liked: post.liked });
  }
};

function goToRecord() {
  router.push({ path: "/record" });
}

function goToRecordImg() {
  router.push({ path: "/recordImg" });
}
</script>

<style scoped>
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
  background: rgba(255, 255, 255, 0.8); /* 배경 색 */
  z-index: 2; /* 이미지 위에 보이게 */
}

.styled-button {
  border: 2px solid #4caf50; /* 테두리 추가 */
  border-radius: 5px; /* 모서리 둥글게 */
  padding: 8px 16px; /* 패딩 조절 */
  transition: background-color 0.3s, transform 0.2s; /* 효과 추가 */
}

#app {
  background-color: white; /* 전체 배경색을 설정 */
  min-height: 100vh; /* 뷰포트 높이를 최소한으로 설정 */
}

#map {
  height: 400px;
  width: 100%;
}

.app-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.container {
  display: flex;
  flex-direction: column;
  height: 100vh; /* 화면 전체 높이 확보 */
  overflow: hidden; /* 스크롤은 내부 컨텐츠에서만 작동 */
}

.record-summary {
  flex: 1; /* 남은 공간을 모두 차지 */
  margin-top: 60px; /* 헤더 높이만큼 여백 추가 */
  margin-bottom: 70px; /* 푸터 높이만큼 여백 추가 */
  overflow-y: auto; /* 수직 스크롤 가능 */
  box-sizing: border-box; /* 여백 포함 크기 계산 */
}

.record-summary::-webkit-scrollbar {
    width: 8px;
    background-color: transparent;
}

.record-summary::-webkit-scrollbar-thumb {
    background-color: transparent;
}

.image-container-wrapper {
  display: flex;
  flex-direction: column;
  gap: 10px;
}


.image-container2 {
  position: relative;
  width: 100%; /* Use full width of the parent container */
  background-color: white;
  margin: 0 auto; /* Center the card within the container */
  overflow: hidden;
}


/* 카드 본문 스타일 */
.card-body {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  width: 100%; /* Ensure the content spans the full width */
}

/* 이미지 스타일 */
.image-container2 img {
  width: 100%;
  height: 200px;
  object-fit: cover; /* 이미지 크기 조정 */
}


/* 추가된 스타일 */
.separator-line {
  border: none;
  border-top: 1px solid white; /* 구분선 스타일 */
  margin: 20px 0; /* 여백 설정 */
}

/* 웹킷 기반 브라우저용 스크롤바 숨기기 */
.scrollable-content::-webkit-scrollbar {
  width: 0; /* 스크롤바 너비를 0으로 설정 */
}



/* 지도 버튼 오버레이 */
.map-overlay {
  position: absolute;
  bottom: 20px;
  right: 10px;
  width: 70px;
  height: 70px;
  border: 2px solid white;
  border-radius: 10px;
  background-color: #4caf50;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  cursor: pointer;
  box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.2);
}

.date-overlay {
  position: absolute; /* 절대 위치 설정 */
  top: 10px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3); /* 그림자 효과 */
  bottom: 10px; /* 이미지의 아래쪽에서 10px 떨어지도록 설정 */
  left: 10px; /* 이미지의 왼쪽에서 10px 떨어지도록 설정 */
  color: white; /* 텍스트 색상 설정 (가독성을 위해) */
  background-color: rgba(0, 0, 0, 0); /* 배경색과 투명도 설정 */
  padding: 5px; /* 텍스트 주위에 패딩 추가 */
  border-radius: 5px; /* 모서리를 둥글게 설정 */
}

/* 추가된 스타일 */
.separator-line {
  border: none;
  border-top: 1px solid white;
  margin: 20px 0;
}

@font-face {
  font-family: "TheJamsil5Bold";
  src: url("https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2")
    format("woff2");
  font-weight: 700;
  font-style: normal;
}

h1,
h2,
h3,
.btn-group .btn,
p {
  /* 특정 요소에 폰트 적용 */
  font-family: "TheJamsil5Bold", sans-serif !important;
}

#cardContent {
  font-family: "TheJamsil4Bold";
  font-weight: 500;
  font-size: 14px;
}

#userNickName {
  font-family: "TheJamsil5Bold";
  font-weight: 600;
  font-size: 14px;
  margin-top: 0.2rem;
}

#communityRegDate {
  font-family: "TheJamsil4Bold";
  font-weight: 700;
  font-size: 15px;
}

.info-overlay {
  position: absolute; /* 이미지 위에 배치 */
  bottom: 0; /* 이미지의 아래쪽에 정렬 */
  left: 0; /* 이미지의 왼쪽에 정렬 */
  right: 0; /* 이미지의 오른쪽에 정렬 */
  display: flex; /* 가로로 나열 */
  justify-content: flex-end; /* 수평 방향으로 오른쪽 정렬 */
  align-items: flex-end; /* 세로 방향으로 아래쪽에 정렬 */
  padding: 10px; /* 패딩 추가 */
  background: rgba(255, 255, 255, 0); /* 배경을 완전히 투명하게 설정 */
  z-index: 2; /* 이미지 위에 보이게 */
}

.overlay-content {
  display: flex; /* 가로로 나열 */
  justify-content: space-around; /* 항목 간격을 균등하게 배치 */
  align-items: center; /* 수직 정렬 */
  gap: 20px; /* 아이템 간격 */
}

.overlay-title {
  margin: 0px; /* 마진 제거 */
  font-size: 17px; /* 폰트 크기 조절 */
  color: white; /* 글씨 색을 흰색으로 설정 */
  font-weight: bold; /* 글씨를 두껍게 설정 */
}

.card-group {
  display: flex; /* 가로로 나열 */
  width: 100%; /* 카드 그룹의 너비를 100%로 설정 */
}

.info-card {
  width: 150px; /* 카드의 너비 설정 */
  height: auto; /* 카드의 높이 설정 */
  margin: 0 5px; /* 카드 간격 */
  color: white;
  background-color: rgba(255, 255, 255, 0);
}

/* 카드 컨테이너 스타일 */
.card {
  background-color: #fff;
  margin-bottom: 10px; /* 카드 사이 간격 */
  border-radius: 10px; /* 카드 모서리 둥글게 */
  box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.1); /* 그림자 효과 */
  overflow: hidden;
}

/* 프로필 이미지 */
.profile-image {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
}

/* 스타일 정의 */

.location-text {
  font-size: 0.7em;
  font-weight: 100;
  margin: 0;
  color: #555;
  margin-bottom: 3px;
}

.header {
  position: fixed; /* 화면 상단에 고정 */
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000; /* 다른 요소 위에 표시 */
  background-color: white; /* 헤더 배경색 */
  border-bottom: 1px solid #ddd; /* 헤더 아래 구분선 */
  height: 60px; /* 헤더 높이 설정 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
}

.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 70px; /* 푸터 높이 */
  background-color: white;
  border-top: 1px solid #ddd;
  z-index: 1000; /* 다른 요소 위에 표시 */
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

.content {
  flex: 1;
  margin-top: 60px; /* 헤더 높이만큼 간격 추가 */
  margin-bottom: 70px; /* 풋터 높이만큼 간격 추가 */
  overflow-y: auto; /* 스크롤 가능 */
  padding: 10px; /* 내부 여백 */
  box-sizing: border-box; /* 여백을 포함한 박스 크기 계산 */
}

/* 스크롤바 스타일 */
.content::-webkit-scrollbar {
  width: 8px;
}

.content::-webkit-scrollbar-thumb {
  background-color: #ccc;
}

.content::-webkit-scrollbar-track {
  background-color: #f0f0f0;
}

.scrollable-content {
  max-width: 100%; /* Prevent overflow */
  padding: 0; /* Remove additional padding */
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

/* 이미지와 지도 컨테이너 스타일 */
.image-map-container {
  position: relative;
  width: 100%;

}

.image-map-container img {
  width: 100%;
  height: auto;
  object-fit: cover; 
  margin-bottom: 10px; 
  border: solid 1px #dedede; 
  border-radius: 10px;
}

.community-item {
  padding: 5px; 
  border-bottom: solid 1px #9e9e9e;
  width: 100%; 
  box-sizing: border-box; 
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
.title-container,
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

.title-container {
  display: flex;
  align-items: center; /* 수직 가운데 정렬 */
  justify-content: space-between; /* 텍스트와 버튼 사이에 균등한 간격 */
  width: 100%; /* 부모 요소에 맞게 확장 */
}

.title-text {
  display: inline-block; /* 버튼을 제외하고 텍스트에만 적용 */
  font-size: 18px;
  padding-right: 15px;
  white-space: nowrap; /* 텍스트 줄바꿈 방지 */
  overflow: hidden; /* 넘친 텍스트 숨기기 */
  text-overflow: ellipsis; /* 넘친 부분을 ...으로 표시 */
  max-width: calc(100% - 120px); /* 버튼의 너비와 간격만큼 제한 */
  vertical-align: middle;
}

.custom-btn {
  width: 100px;
  font-size: 14px;
  border-radius: 5px;
  border: solid 2px #3b8f3e;
  background-color: #ffffff;
  cursor: pointer;
  position: relative;
}

.custom-btn:hover {
  background-color: #3b8f3e;
  color: white;
}

.back-button-container {
  text-align: left;
  /* 버튼을 왼쪽에 정렬 */
  margin-top: 10px;
}

.btn-group-container {
  position: fixed;
  top: 13px; /* 헤더 위에 나타나도록 조정 */
  right: 10px; /* 오른쪽 끝에 배치 */
  z-index: 1100; /* 헤더 위에 나타나도록 설정 */
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
