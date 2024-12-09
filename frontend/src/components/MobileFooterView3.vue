<template>
  <div id="mobileFooter">
    <!-- 코스 아이콘 -->
    <div class="footer-category" @click="gotoMobileCourse()" style="margin-right: 10px;">
      <img src="/images/코스2.png" alt="코스 아이콘" style="filter: grayscale(100%) brightness(0%);">
      <p style="">코스</p>
    </div>

    <!-- 등산 아이콘 -->
    <div class="footer-category" @click="gotoMobileRecoding()" style="margin-right: 10px;">
      <img src="/images/HikingIcon.gif" alt="등산 아이콘" style="filter: hue-rotate(280deg); width: 45px; height: 45px;">
      <p style="">등산</p>
    </div>

    <!-- 홈 아이콘 -->
    <div class="footer-category home" @click="gotoHome()">
      <img src="/images/홈.png" alt="홈 아이콘" style="filter: grayscale(100%) brightness(0%);">
      <p style="">홈</p>
    </div>

    <!-- 민원 신고 아이콘 -->
    <div class="footer-category" @click="gotoMobileComplaint()" style="margin-left: 10px;">
      <img src="/images/위험신고.png" alt="민원 신고 아이콘" style="filter: grayscale(100%) brightness(0%);">
      <p style="">민원신고</p>
    </div>

    <!-- 커뮤니티 아이콘 -->
    <div class="footer-category" @click="gotoMobileCommunity()" style="margin-left: 10px;">
      <img src="/images/커뮤니티.png" alt="커뮤니티 아이콘" style="filter: grayscale(100%) brightness(0%);">
      <div style="margin: 0;  font-size: 13px; line-height: 1.2; text-align: center;">커뮤니티</div>
    </div>
  </div>
</template>

<script setup>

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
  height: 70px; /* 푸터 높이 */
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #ffffff;
  display: flex;
  justify-content: space-between; /* 간격 균등 배치 */
  align-items: center;
  padding: 0 5px; /* 좌우 여백 */
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.footer-category {
  display: flex;
  flex-direction: column; /* 아이콘과 텍스트 세로 배치 */
  align-items: center; /* 수평 중앙 정렬 */
  text-align: center;
  font-family: 'TheJamsil4Bold';
  font-weight: 500;
  cursor: pointer;
  flex: 1; /* 각 버튼의 동일한 크기 */
}

.footer-category img {
  width: 30px; /* 아이콘 크기 증가 */
  height: 30px;
  margin-bottom: 5px; /* 아이콘과 텍스트 간 간격 */
}

.footer-category p {
  margin: 0; /* 텍스트 간 여백 제거 */
  font-size: 13px;
  line-height: 1.2;
  text-align: center;
}

</style>
