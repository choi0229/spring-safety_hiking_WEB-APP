
<template>
  <div id="app">
    <div class="custom-header">
      <button class="back-button" @click="goBack">
        <img src="/images/뒤로가기.png" alt="뒤로가기" />
      </button>
      <!-- 제목 -->
      <span class="header-title">민원신고</span>
      <!-- 완료 버튼 -->
      <button class="done-button" @click="submitPost()">완료</button>
    </div>
  <div class="app-main">
      <!-- 뒤로가기 버튼 -->
    <!-- START:: 위에 제목과 버튼들 -->
    <div class="button-group-container" style="border-bottom: solid 1px #45a049">
      <div class="button-group" >
        <button :class="{ active: isRecent }" @click="getRecentList()"  ><img src="/images/나의민원.png" width="16" height="auto" style="margin-right: 7px; margin-bottom: 4px;">{{ btnname }}</button>
        <button :class="{ active: !isRecent && currentPage === 2 }" @click="goToMap()"><img src="/images/지도.png" width="16" height="auto" style="margin-right: 7px; margin-bottom: 4px;">지도보기</button>
        <button :class="{ active: currentPage === 3 }" @click="goToComplaintPage()"><img src="/images/신고.png" width="16" height="auto" style="margin-right: 7px; margin-bottom: 4px;">신고하기</button>
      </div>
    </div>
    <!-- END:: 위에 제목과 버튼들 -->
    <!-- 민원 목록 카드 -->
    <div class="card-container d-flex align-items-center justify-content-center mt-3 mb-4">
      <div class="col-12 col-md-9">
        <!-- 민원 목록 카드 -->
          <!-- 카드 내용 -->
          <div class="card-body" id="kt_contacts_list_body"  style="height: 520px;">
            <div class="container">
              <div class="row" style="flex-wrap: nowrap;">
                <div class="col-2 col-md-2 text-center">
                  <span class="text-muted fs-9" style="border-bottom: solid 1px #444; padding-bottom: 3px; display: inline-block;">번호</span>
                </div>
                <div class="col-5 text-center" style="margin-left: 0px;">
                  <span class="text-muted fs-9" style="border-bottom: solid 1px #444; padding-bottom: 3px; display: inline-block;">제목</span>
                </div>
              </div>
              <hr class="separator-line" />
            </div>
  
            <!-- 민원 목록 -->
            <div v-for="(item, index) in paginatedList" :key="item.complaintNo" class="post-container">
              <div class="post-content" @click="getComplaintByNo(item.complaintNo)">
                  <!-- 게시글 제목과 번호 -->
                  <div class="post-header">
                    <span class="post-number">{{ (currentPage - 1) * itemsPerPage + index + 1 }}</span>
                    <span class="separator"></span>
                    <span class="post-title">{{ item.complaintTitle }}</span>
                  </div>
                  <!-- 글쓴이 및 작성시간 -->
                  <div class="post-meta">
                    <span class="author">{{ userNickNames[item.userId] || '...' }}</span>
                    <span class="separator">|</span>
                    <span class="time">{{ formatDate(item.createdAt) }} | {{ formatTime(item.createdAt) }}</span>
                  </div>
                </div>

            </div>

          </div>
          <!-- START::페이지네이션 -->
          
          <!-- END::페이지네이션 -->  
      </div>
    </div>
  </div>
   <!-- 페이지네이션 고정 -->
   <div class="pagination-container">
  <nav aria-label="Pagination">
    <ul class="pagination">
      <li
        class="page-item"
        v-for="page in totalPages"
        :key="page"
        :class="{ active: page === currentPage, hover: page === currentPage }">
        <a class="page-link" @click.prevent="changePage(page)" >{{ page }} </a>
      </li>
    </ul>
  </nav>
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
  let btnname = ref('나의 민원');
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
      btnname = '나의 민원';
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

  function goBack() {
    window.location.href=`/mobilemainview`
  }

  onMounted(() => {
  currentPage.value = 1; 
});
  </script>
  
  <style scoped>
  #app {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 메인 콘텐츠 설정 */
.app-main {
  flex: 1; /* 남은 공간을 차지하도록 설정 */
  margin-top: 60px; /* 헤더 높이만큼 아래로 시작 */
  margin-bottom: 60px; /* 푸터 높이만큼 위로 끝 */
  overflow-y: auto; /* 수직 스크롤 허용 */
  background-color: #ffffff;

}

.card-item {
    display: flex;
    align-items: center;
    padding: 10px;
    border-bottom: 1px solid #ddd;
    cursor: pointer;
    transition: background-color 0.3s;
}

.card-item:hover {
    background-color: #f9f9f9;
}

.card-item .card-number,
.card-item .card-title,
.card-item .card-author,
.card-item .card-date {
    font-size: 0.9rem;
    color: #333;
    text-align: left;
}

.card-item .card-number {
    flex: 1;
    text-align: center;
}

.card-item .card-title {
    flex: 4;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.card-item .card-author {
    flex: 2;
    text-align: center;
}

.card-item .card-date {
    flex: 3;
    text-align: center;
}

/* 반응형 스타일 */
@media (max-width: 480px) {
    .header-title {
        font-size: 1rem;
    }

    .card-item {
        flex-direction: column;
        align-items: flex-start;
    }

    .card-item .card-number,
    .card-item .card-title,
    .card-item .card-author,
    .card-item .card-date {
        text-align: left;
        width: 100%;
    }
    .custom-header,
  .footer {
    height: 50px;
  }
  .app-main {
    margin-top: 50px;
    margin-bottom: 50px;
  }
}

.card-body {
  overflow-y: auto; /* 스크롤 설정 */
  padding: 10px;
  background-color: #f7f7f7;
  border: 1px solid #000000;
  top:-20px;
  position: relative;
}


.card-footer {
  position: absolute;
  bottom: 0; /* 게시판 하단에 고정 */
  width: 100%; /* 너비를 게시판에 맞게 */
  background-color: transparent; /* 배경 제거 */
  border: none; 
  padding: 10px 0; 
}


/* 게시글 스타일 */
.container.mb-2 {
  background-color: #f9f9f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 10px;
  padding: 15px 10px;
  display: flex;
  flex-direction: row;
  align-items: center;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.container.mb-2:hover {
  background-color: #fefefe;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15);
}

.row {
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: center;
}

.row > div {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.row .col-2 {
  text-align: center;
  font-size: 14px;
  font-weight: bold;
}

.row .col-5 {
  flex: 3;
  padding-left: 10px;
  font-size: 14px;
  color: #333;
  display: flex;
  align-items: center;
}

.row .col-5 span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row .col-2.text-center {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.row .col-3 {
  flex: 2;
  text-align: center;
  font-size: 12px;
  color: #888;
}

/* 텍스트 스타일 */
.row span {
  font-size: 14px;
  color: #666;
}

.row .text-muted {
  font-size: 14px;
  font-weight: bold;
  color: #444;
}

/* 구분선 */
hr.separator-line {
  margin: 8px 0;
  border: none;
  border-bottom: 1px solid #eee;
}

.button-group-container {
  margin-bottom: 0; /* 여백 제거 */
}

.button-group {
  display: flex;
  justify-content: space-around;
  background-color: transparent;
  padding-top: 9px;
}

.button-group button {
  flex: 1;
  font-size: 15px;
  font-weight: bold;
  color: #444;
  background-color: transparent;
  border: solid 1px #dddcdc;
  border-bottom: solid 1px #45a049;
  padding: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.button-group button:hover {
  background-color: #f9f9f9;
}


/* 통통 튀는 애니메이션 정의 */
@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px); /* 위로 살짝 튀는 효과 */
  }
}

/* 버튼 그룹과 페이지네이션을 분리 */
.pagination-container .active {
  background-color: transparent;
  color: #04663f;
  font-weight: bold;
}

/* 커스텀 헤더 */
.custom-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: white;
  padding: 0 10px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  z-index: 1000;
  font-family: 'TheJamsil5Bold', sans-serif !important;
}

/* 고정된 푸터 */
.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px; /* 푸터 높이 */
  background-color: white;
  border-top: 1px solid #ddd;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
}

/* 제목과 뒤로가기 버튼을 정렬하는 컨테이너 */
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px; /* 아래 여백 */
  padding: 10px; /* 내부 여백 */
  background-color: white;
  border: solid 1px #45a049;
}

/* 페이지 제목 스타일 */
.page-title {
  font-size: 20px;
  font-weight: bold;
  text-align: center;
  flex: 1; /* 제목이 남은 공간을 차지하도록 설정 */
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 5px;
  border-bottom: solid 1px black;
}

/* 헤더 제목 스타일 */
.header-title {
  flex: 1;
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  color: #333;
  font-family: 'TheJamsil5Bold', sans-serif !important;
}

/* 뒤로가기 버튼 */
button.back-button {
  border: none;
  cursor: pointer;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%; 
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.done-button {
  font-size: 14px;
  font-weight: bold;
  color: #45a049;
  background: none;
  border: none;
  cursor: pointer;
  margin-right: 10px;
}

/* 게시글 컨테이너 스타일 */
.post-container {
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  border: 1px solid #04663f;
  border-radius: 8px;
  margin-bottom: 10px;
  padding: 15px;
  cursor: pointer;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.post-container:hover {
  background-color: #f9f9f9;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

/* 게시글 헤더 스타일 */
.post-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  color: #333;
  margin-bottom: 5px;
  position: relative; /* 상대 위치 설정 */
}

/* 번호 스타일 */
.post-number {
  font-weight: bold;
  font-size: 20px; /* 번호 크기 증가 */
  color: #666;
  margin-right: 8px; /* 번호와 구분선 사이 간격 */
  position: relative; /* 상대 위치 */
  top: 10px; /* 번호를 제목 중앙과 맞춤 */
}

/* 구분선 스타일 */
.separator {
  width: 2px; /* 세로선의 두께 */
  height: 100%; /* 구분선 길이를 전체 높이로 설정 */
  background-color: #aaa; /* 구분선 색상 */
  margin: 0 8px; /* 구분선과 텍스트 간격 */
}

/* 제목 스타일 */
.post-title {
  flex: 1; /* 제목이 남은 공간을 채우도록 설정 */
  font-weight: bold;
  font-size: 16px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* 제목이 길면 ... 처리 */
  line-height: 24px; /* 제목 중앙 정렬을 위한 줄 높이 */
}


/* 메타 정보 스타일 */
.post-meta {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #666;
  margin-left: 37px;
}

.post-meta .author {
  font-weight: bold;
  color: #555;
}

.post-meta .separator {
  color: #aaa;
}

.post-meta .time {
  font-size: 13px;
  color: #888;
}

/* 페이지네이션 스타일 */
.pagination-container {
  position: fixed;
  bottom: 60px; /* Footer 바로 위에 위치 */
  left: 0;
  width: 100%;
  background-color: white; /* 배경색 */
  text-align: center;
  z-index: 1000; /* 위로 올리기 */
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1); /* 위쪽 그림자 */
}

.pagination {
  display: flex;
  justify-content: center;
}

.pagination .page-item {
  list-style: none;
  margin: 0 5px;
}

.pagination .page-link {
  background-color: transparent;
  border: none;
  color: black;
  font-weight: bold;
  font-size: 14px;
  padding: 6px 12px;
  cursor: pointer;
  text-decoration: none; 
  border-bottom: 2px solid rgb(160, 177, 156); 
  transition: all 0.3s ease; 
  border-radius: 0; 
}

.pagination .page-item.active .page-link,
.pagination .page-item.hover .page-link {
  color: #04663f; /* Green text color */
  border-bottom: 2px solid #04663f; /* Green underline */
  font-size: 16px; /* Larger font size */
  font-weight: bold;
}

.pagination .page-link:hover {
  color: #04663f;
  border-bottom: 2px solid #04663f;
  font-size: 16px;
}


</style>