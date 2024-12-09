<template>
    <div id="app">
      <MobileHeaderView class="header" />
      <div class="btn-group-container">
        <div class="btn-group" role="group" aria-label="Basic radio toggle button group">
          <input type="radio" class="btn-check" name="btnradio" id="btnradio1" autocomplete="off" @click="goToRecord()" />
          <label class="btn btn-outline-success" for="btnradio1">운동</label>
  
          <input type="radio" class="btn-check" name="btnradio" id="btnradio2" autocomplete="off" checked />
          <label class="btn btn-outline-success" for="btnradio2">기록</label>
  
          <input type="radio" class="btn-check" name="btnradio" id="btnradio3" autocomplete="off" @click="goToMyCom()" />
          <label class="btn btn-outline-success" for="btnradio3">나의 커뮤니티</label>
        </div>
      </div>
      <div class="app-main">
        <div class="container">
          <!-- 기록 요약 -->
          <div class="record-summary mt-5">
            <div class="image-container-wrapper mb-5">
              <div v-for="(item, index) in pathList" :key="item.pathId" class="image-container2">
                <img :src="item.pathImg" alt="Saved Run Image" />
                <i class="bi bi-cloud-upload icon-top-right" @click="showUploadMessage(index)"></i>
                <div class="overlay">
                  <div class="row stats">
                    <div class="col-4 stat-item justify-content-start">
                      <i class="bi bi-stopwatch"></i> {{ item.totalTime }}
                    </div>
                    <div class="col-4 stat-item justify-content-center">
                      <i class="icon-distance"></i> {{ item.totalDistance }} KM
                    </div>
                    <div class="col-4 stat-item justify-content-end">
                      <i class="bi bi-speedometer"></i> {{ item.avgSpeed }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <!-- 업로드 확인 메시지 -->
          <div v-if="showMessage" class="upload-message">
            <p>등산 기록이 업로드되었습니다!</p>
            <button class="btn btn-primary" @click="confirmUpload">확인</button>
          </div>
          <MobileFooterView3 />
        </div>
      </div>
    </div>
  </template>

<script setup>
import MobileFooterView3 from "@/components/MobileFooterView3.vue";
import MobileHeaderView from "@/components/MobileHeaderView.vue";
import router from '@/router';
import { onMounted } from 'vue';
import { usePathList } from '../stores/complaint';
import { storeToRefs } from 'pinia';

// 등산 기록 리스트 가져오기
const pathListStore = usePathList();
// onMounted로 컴포넌트가 마운트된 후에 실행되도록 설정
onMounted(async () => {
    await pathListStore.fetchPathList();
});
 
const { pathList } = storeToRefs(pathListStore);



function goToRecord(){
    router.push({path:'/record'});
}

function goToMyCom(){
    router.push({path:'/myCommunity'});
}

</script>

<style scoped>
h1,
h2,
h3,
.btn-group .btn,
p {
  /* 특정 요소에 폰트 적용 */
  font-family: "TheJamsil5Bold", sans-serif !important;
}

#app {
    height: 100vh;
    width: 100vw;
    overflow: hidden;
    display: flex;
    flex-direction: column;
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
    height: 100%;
    position: relative;
}


.record-summary {
    flex: 1;
    flex-direction: column;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: transparent transparent;
}

.image-container-wrapper {
    display: flex;
    justify-content: center; /* 가운데 정렬 */
    flex-wrap: wrap;
    gap: 20px; /* 이미지 간 간격 */
}

.image-container2 {
  position: relative;
  width: 320px;
  height: 320px;
  border-radius: 30px;
  overflow: hidden;
}

.image-container2 img {
  width: 320px;
  height: 320px;
  object-fit: cover;
  filter: brightness(0.93);
}

.icon-top-right {
    position: absolute;
    top: 10px !important;
    right: 20px;
    font-size: 24px;
    color: white;
    cursor: pointer;
}

.overlay {
  position: absolute;
  top: 90%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 19px;
  font-weight: 600;
  pointer-events: none;
}

.stats {
    display: flex;
    width: 310px;
}

.stat-item {
    display: flex;
    align-items: center;
    font-size: 18px;
}

.stat-item i {
  font-size: 18px;
  margin-right: 4px;
}

.record-summary::-webkit-scrollbar {
    width: 8px;
    background-color: transparent;
}

.record-summary::-webkit-scrollbar-thumb {
    background-color: transparent;
}

.img {
    max-width: 100%;
    margin-bottom: 20px;
    border-radius: 10%;
    object-fit: contain;
}

@media (max-width: 360px) {
    .btn-group .btn {
        font-size: 12px;
        padding: 2px 4px;
    }
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

/* 헤더 수정 */
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background-color: white; /* 헤더 배경색 */
  border-bottom: 1px solid #ddd; /* 헤더 아래 구분선 */
  height: 60px; /* 헤더 높이 설정 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
}
</style>