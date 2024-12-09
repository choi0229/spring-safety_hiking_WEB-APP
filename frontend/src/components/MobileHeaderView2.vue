<template>
  <header class="d-flex justify-content-between align-items-center p-2 MobileHeader" style="background-color: white;">
    <!-- 왼쪽 로고 및 브랜드명 -->
    <div class="d-flex align-items-center" @click="gotoHome()" style="margin-left: 0.5rem;">
      <img src="/images/icon2.png" alt="산 로고" class="" width="25" height="15" />
      <span class="brand-name">안전하산</span>
    </div>

    <!-- 오른쪽 드롭다운 메뉴 -->
    <div class="dropdown">
      <a role="button" @click="toggleDropdown" data-bs-toggle="dropdown" aria-expanded="false" style="margin-right: -0.4rem;">
        <img src="/images/hamburgerIcon.png" alt="돋보기 아이콘" width="50" height="50">
      </a>
      <ul class="dropdown-menu">
        <li><a class="dropdown-item" href="#">로그인</a></li>
        <li><a class="dropdown-item" href="#">커뮤니티</a></li>
        <li><a class="dropdown-item" href="#">멘토신청</a></li>
      </ul>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue';

const showDropdown = ref(false);

function toggleDropdown() {
  showDropdown.value = !showDropdown.value;
}

function gotoHome() {
  window.location.href = '/mobilemainview';
}
</script>

<style scoped>
@font-face {
  font-family: 'TheJamsil5Bold';
  src: url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2') format('woff2');
  font-weight: 700;
  font-style: normal;
}

.MobileHeader {
  box-shadow: 0 1px 10px rgba(0, 0, 0, 0.3);
}

/* 브랜드 이름 스타일 */
.brand-name {
  font-family: 'TheJamsil3Bold';
  font-weight: 700;
  font-size: 17px;
}

/* 드롭다운 애니메이션 스타일 */
.dropdown-menu {
  opacity: 0;
  transform: translateY(-10px); /* 드롭다운이 살짝 위에서 내려오도록 설정 */
  transition: opacity 0.3s ease, transform 0.3s ease; /* 스르륵 효과 */
}

.dropdown-menu.show {
  opacity: 1;
  transform: translateY(0); /* 애니메이션 완료 시 원래 위치 */
}

/* 설명 및 본문 스타일 */
.description {
  font-weight: 300;
  font-size: 13px;
}
</style>
