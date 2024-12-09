<template>
  <div id="mobileFooter">
    <!-- 코스 아이콘 -->
    <div class="footer-category" @click="gotoMobileCourse()" style="margin-right: 10px;">
      <img 
        :src="isCoursePage ? '/images/코스.gif' : '/images/코스2.png'" 
        :style="isCoursePage ? gifStyle : defaultStyle"/>
      <p style="font-size: 13px; margin-top: 1rem;">코스</p>
    </div>

    <!-- 운동 아이콘 -->
    <div class="footer-category" @click="gotoMobileRecoding()" style="margin-right: 10px;">
      <img 
        :src="isFitnessPage ? '/images/HikingIcon.gif' : '/images/HikingIcon.png'" 
        :style="isFitnessPage ? gifStyle : defaultStyle" />
      <p style="font-size: 13px; margin-top: 1rem;">등산</p>
    </div>

    <!-- 홈 아이콘 -->
    <div class="footer-category home" @click="gotoHome()" style="">
      <img 
        :src="isHomePage ? '/images/홈.gif' : '/images/홈.png'" 
        :style="isHomePage ? gifStyle : defaultStyle"
      />
      <p style="font-size: 13px; margin-top: -5px;">홈</p>
    </div>

    <!-- 위험신고 아이콘 -->
    <div class="footer-category" @click="gotoMobileComplaint()" style="margin-left: 10px;">
      <img 
        :src="isComplaintPage ? '/images/위험신고.gif' : '/images/위험신고.png'" 
        :style="isComplaintPage ? gifStyle : defaultStyle"
      />
      <p style="font-size: 13px; margin-top: 1rem;">민원신고</p>
    </div>

    <!-- 커뮤니티 아이콘 -->
    <div class="footer-category" @click="gotoMobileCommunity()" style="margin-left: 10px;">
      <img 
        :src="isCommunityPage ? '/images/커뮤니티.gif' : '/images/커뮤니티.png'" 
        :style="isCommunityPage ? gifStyle : defaultStyle"
      />
      <p style="font-size: 13px; margin-top: 1rem;">커뮤니티</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

// 각 아이콘의 경로 상태를 ref로 설정
const isCoursePage = ref(false);
const isFitnessPage = ref(false);
const isHomePage = ref(false);
const isComplaintPage = ref(false);
const isCommunityPage = ref(false);

// 경로 확인하여 상태 업데이트
onMounted(() => {
  isCoursePage.value = route.path === '/MobileCourseView';
  isFitnessPage.value = route.path === '/MobileFitnessView';
  isHomePage.value = route.path === '/mobilemainview';
  isComplaintPage.value = route.path === '/complaintListMobile';
  isCommunityPage.value = route.path === '/communityListMobile';
});

// 페이지 이동 함수들

function gotoHome() {
  window.location.href = '/mobilemainview';
}

function gotoMobileCourse() {
  window.location.href = '/mobilecourseview';
}

function gotoMobileCommunity() {
  window.location.href = '/mobilecommunity';
}

function gotoMobileComplaint() {
  window.location.href = '/complaintListMobile';
}

function gotoMobileRecoding() {
  window.location.href = '/record';
}

// 기본 스타일과 GIF 스타일을 개별적으로 설정
const defaultStyle = {
  width: '30px',
  height: '30px',
  filter: 'grayscale(100%) brightness(0%)',
  marginBottom: '-0.7rem'
};

const gifStyle = {
  width: '40px',
  height: '40px',
  filter: 'hue-rotate(280deg)'
};
</script>

<style scoped>
@font-face {
  font-family: 'TheJamsil5Bold';
  src: url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2') format('woff2');
  font-weight: 700;
  font-style: normal;
}

h1,
h2,
h3,
div,
.btn-group .btn,
p {
  /* 특정 요소에 폰트 적용 */
  font-family: "TheJamsil5Bold", sans-serif !important;
}

#mobileFooter {
  position: fixed;
  height: 70px;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #ffffff;
  display: flex;
  justify-content: space-evenly; /* 모든 버튼 간격 동일 */
  align-items: center; /* 수직 중앙 정렬 */
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  padding: 0 5px;
}

.footer-category {
  display: flex;
  flex-direction: column; /* 아이콘과 텍스트를 세로로 배치 */
  align-items: center; /* 수평 중앙 정렬 */
  text-align: center;
  font-family: 'TheJamsil4Bold';
  font-weight: 500;
  cursor: pointer;
  flex: 1; /* 모든 버튼 동일 크기 */
}

.footer-category img {
  width: 33px; /* 아이콘 크기 증가 */
  height: 33px; /* 아이콘 크기 증가 */
  margin-bottom: 8px; /* 아이콘과 텍스트 간 간격 증가 */
}

.footer-category p {
  margin: 0; /* 텍스트 간 여백 제거 */
  font-size: 13px;
  line-height: 1.2; /* 텍스트 줄 간격 */
  position: relative;
  top: 4px; /* 텍스트를 아래로 이동하여 "홈" 높이 기준에 맞춤 */
}

.home p {
  top: 0; /* "홈" 텍스트는 기본 높이로 설정 */
}

</style>
