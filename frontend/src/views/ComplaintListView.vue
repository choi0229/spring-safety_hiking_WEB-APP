<template>
  <div id="app">
  
  <div class="app-main">
    <!-- START:: 위에 제목과 버튼들 -->
    <div class="container" style="height: 100px;">
        <div class="row" style="height: 65px;">
          <div class="col-8 d-flex justify-content-center">
            <span class="fs-1 m-2 fw-bold">위험신고 목록</span>
          </div>
          <div class="col-4 d-flex justify-content-center">
            <button class="styled-button" @click="goToMap()">지도보기</button>
          </div>
        </div> 
        <div class="row" style="height: 45px;">
          <div class="col-6 d-flex justify-content-center">
            <button class="styled-button" @click="getRecentList()">{{btnname}}</button>
          </div>
          <div class="col-6 d-flex justify-content-center">
            <button class="styled-button" @click="goToComplaintPage()">신고하기</button>
          </div>
        </div> 
    </div>
    <!-- END:: 위에 제목과 버튼들 -->
    <!-- 민원 목록 카드 -->
    <div class="card-container d-flex align-items-center justify-content-center mt-3 mb-4">
      <div class="col-12 col-md-9">
        <!-- 민원 목록 카드 -->
        <div class="card card-flush" id="kt_contacts_list">
          <!-- 카드 내용 -->
          <div class="card-body pt-3 pb-4" id="kt_contacts_list_body"  style="height: 500px;">
            <div class="container">
              <div class="row" style="flex-wrap: nowrap;">
                <div class="col-2 col-md-2 text-center">
                  <span class="text-muted fs-9">번호</span>
                </div>
                <div class="col-5 col-md-5 text-center">
                  <span style="white-space: nowrap" class="text-muted fs-9">제목</span>
                </div>
                <div class="col-2 col-md-2 text-center">
                  <span class="text-muted fs-9">글쓴이</span>
                </div>
                <div class="col-3 col-md-3 text-center">
                  <span class="text-muted fs-9">작성시간</span>
                </div>
              </div>
              <hr class="separator-line" />
            </div>
  
            <!-- 민원 목록 -->
            <div v-for="(item, index) in paginatedList" :key="item.complaintNo" class="container mb-2">
              <div class="row" style="flex-wrap: nowrap;" @click="getComplaintByNo(item.complaintNo)" >
                <div class="col-2 col-md-2 text-center">
                  <span class="text-muted smaller-text">{{ (currentPage - 1) * itemsPerPage + index + 1 }}</span>
                </div>
                <div class="col-5 col-md-5">
                  <span class="smaller-text" style="white-space: nowrap">{{ item.complaintTitle }}</span>
                </div>
                <div class="col-2 col-md-2 text-center">
                  <span class="text-muted smaller-text">{{ userNickNames[item.userId] || '...' }}</span>
                </div>
                <div class="col-3 col-md-3 text-center">
                  <span class="text-muted smaller-text">
                    <span>{{ formatDate(item.createdAt) }}</span>
                    <br />
                    <span>{{ formatTime(item.createdAt) }}</span>
                  </span>
                </div>
              </div>
              <hr class="separator-line" />
            </div>
          </div>
          <!-- START::페이지네이션 -->
          <div class="card-footer d-flex justify-content-center pb-3">
            <!-- Pagination -->
            <nav aria-label="Pagination">
              <ul class="pagination">
                <li
                  class="page-item"
                  v-for="page in totalPages"
                  :key="page"
                  :class="{ active: page === currentPage }"
                >
                  <a class="page-link" href="#" @click.prevent="changePage(page)">{{ page }}</a>
                </li>
              </ul>
            </nav>
          </div>
          <!-- END::페이지네이션 -->
        </div>
      </div>
    </div>
  </div>
  
  <MobileFooterView4></MobileFooterView4>
  </div>
  </template>
  
  
  <script setup>
  import MobileFooterView4 from "@/components/MobileFooterView4.vue";
  import { ref, onMounted, computed } from "vue";
  import { useComplaintListStore, useComplaintStore, useRecentComplaintListStore, useMyComplaintListStore } from "@/stores/complaint";
  import { getUserById } from "@/api/complaint";
  import router from "@/router/index.js";
  import { storeToRefs } from 'pinia';
  
  // 스토어 가져와서 리스트 받아오기
  const complaintListStore = useComplaintListStore();
  const recentcomplaintListStore = useRecentComplaintListStore();
  const myComplaintListStore = useMyComplaintListStore();
  
  // 상태 변수 추가
  const isRecent = ref(false);
  let btnname = ref('나의글');
  let loginUserId = localStorage.getItem("userId");

  const userNickNames = ref({});
  const currentPage = ref(1);
  const itemsPerPage = 7;

  // onMounted로 컴포넌트가 마운트된 후에 실행되도록 설정
  onMounted(async () => {

    await complaintListStore.fetchComplaintList();
    //window.location.reload();
    await recentcomplaintListStore.fetchRecentComplaintList();
    await myComplaintListStore.fetchMyComplaintList(loginUserId);
    // sessionStorage에서 새로고침 여부를 확인
    const hasRefreshed = sessionStorage.getItem('hasRefreshed');
  
    if (!hasRefreshed) {
      sessionStorage.setItem('hasRefreshed', 'true'); // 새로고침 플래그 설정
      window.location.reload(); // 페이지 새로고침
    } else {
      sessionStorage.removeItem('hasRefreshed'); // 플래그 초기화
    } // Vue가 마운트될 때 지도를 초기화

    const uniqueUserIds = [...new Set(displayedList.value.map(item => item.userId))];
    await Promise.all(uniqueUserIds.map(async (userId) => {
      const userInfo = await getUserById(userId);
      userNickNames.value[userId] = userInfo.userNickName;
    }));

  });
 
  // eslint-disable-next-line no-unused-vars
  const { complaintlist } = storeToRefs(complaintListStore);
  const { recentcomplaintlist } = storeToRefs(recentcomplaintListStore);
  const { mycomplaintlist } = storeToRefs(myComplaintListStore);
  
  // 표시할 리스트를 computed로 정의
  const displayedList = computed(() => isRecent.value ? mycomplaintlist.value : recentcomplaintlist.value);

  const paginatedList = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage;
    const end = start + itemsPerPage;
    return displayedList.value.slice(start, end);
  });

  const totalPages = computed(() => Math.ceil(displayedList.value.length / itemsPerPage));

  function changePage(page) {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page;
    }
  }

  function getRecentList() {
    isRecent.value = !isRecent.value;
    if(isRecent.value) {
      btnname = '전체글';
    }else{
      btnname = '나의글';
    }
  }
  // Helper method to format the date (YYYY-MM-DD)
  function formatDate(datetime) {
    return datetime.split(' ')[0]; // Returns the date part
  }
  
  // Helper method to format the time (HH:MM:SS)
  function formatTime(datetime) {
    return datetime.split(' ')[1]; // Returns the time part
  }
  
  // 번호에 따른 민원 글 데이터 불러오기
  const complaintStore = useComplaintStore();
  // eslint-disable-next-line no-unused-vars
  const complaintone = storeToRefs(complaintStore);
  
  async function getComplaintByNo(key) {
    await complaintStore.fetchComplaintone(key);
    sessionStorage.setItem("complaintNo", key);
    router.replace({path: '/complaintInfoMobile'});
  }
 
  function goToComplaintPage() {
    router.replace({ path: "/complaintMobile" });
  }

  function goToMap() {
    router.replace({ path: "/clMapMobile"});
  }

  </script>
  
  <style scoped>
  #app {
      height: 100vh;
      width: 100vw;
      overflow: hidden;
    }
  
  
   /* 메인 콘텐츠 설정 */
   .app-main {
    height: 100vh;
    width: 100vw;
      overflow-y: scroll;
      background-color: #f5f5f5;
    }
  
  .fs-9 {
    font-size: 0.6rem !important; /* 폰트 크기 정의 및 우선 적용 */
  }
  
  hr.separator-line {
    border: 1px solid #ccc;
    margin: 10px 0;
    width: 100%;
  }
  
  .row {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    justify-content: space-between;
  }
  
  .smaller-text {
    font-size: 0.75rem;
    white-space: nowrap;
  }
  
  .card-container {
    padding-left: 10px;
    padding-right: 10px;
  }

  .styled-button {
  background: linear-gradient(#55995e, #3d7935);
  color: white;
  padding: 8px 16px;
  border: none;
  border-radius: 25px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
  

  /* 추가된 작은 화면에 대한 반응형 스타일 */
  @media (max-width: 360px) {
    .smaller-text {
      font-size: 0.65rem;
      white-space: normal; /* 텍스트 잘림 방지 */
    }
  
    .card-title {
      font-size: 1.2rem; /* 카드 제목 크기 줄임 */
    }
  
    .btn {
      padding: 5px 10px;
      font-size: 0.8rem;
    }
  
    .container {
      padding-left: 5px;
      padding-right: 5px;
    }
  
    #map {
      height: 200px; /* 지도 크기 줄임 */
    }
  
    .card-body {
      padding: 10px; /* 카드 내용에 더 작은 패딩 적용 */
    }
  
    hr.separator-line {
      margin: 5px 0; /* 구분선 마진 축소 */
    }
  
    .col-5, .col-2, .col-3 {
      text-align: left; /* 텍스트 정렬을 좌측으로 조정 */
    }
  
    .card-container {
      padding-left: 5px;
      padding-right: 5px;
    }
  }

  /* 기본 페이지 링크 색상 */
  .pagination .page-link {
    color: black;
  }

  /* 선택된(활성) 페이지 색상 변경 */
  .pagination .page-item.active .page-link {
    background-color: #04663f; /* success 색상 */
    border-color: #04663f; /* success 색상 */
    color: white; /* 텍스트 색상 */
  }
  </style>
  