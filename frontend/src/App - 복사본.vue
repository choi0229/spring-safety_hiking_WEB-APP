<template>
  <div id="app">
    <router-view /> <!-- 페이지 컨텐츠가 렌더링되는 자리 -->
    <div v-if="isLoading" class="loading-overlay">
      <img src="/images/로딩.gif" alt="로딩 중..." class="loading-gif" />
      <p>페이지 로딩 중...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const isLoading = ref(false);
const router = useRouter();

onMounted(() => {
  // 라우터 이동 시작 시 로딩 창 표시
  router.beforeEach((to, from, next) => {
    isLoading.value = true;
    next();
  });

  // 라우터 이동 완료 시 로딩 창 숨기기
  router.afterEach(() => {
    setTimeout(() => {
      isLoading.value = false;
    }, 500); // 0.5초 후 로딩 숨김 (필요에 따라 조정 가능)
  });
});
</script>

<style scoped>
/* 로딩 오버레이 스타일 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(255, 255, 255, 1); /* 반투명 배경 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

/* 로딩 GIF 스타일 */
.loading-gif {
  width: 80px; /* GIF 크기 */
  height: 80px; /* 필요에 따라 조정 가능 */
}

/* 로딩 텍스트 스타일 */
.loading-overlay p {
  font-family: 'TheJamsil4Bold', sans-serif;
  font-size: 14px;
  font-weight: 400;
  color: #333;
  margin-top: 10px;
}
</style>
