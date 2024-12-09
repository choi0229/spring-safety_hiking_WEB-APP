
<template>
  <HeaderView style="z-index: 4;width: 100%;"></HeaderView>

  <div class="image-container mb-2">
    <img src="/images/mount3.JPG" alt="Mountains" class="background-img">
    <h2 class="align-to-card">등산로 위험 신고 목록</h2>
  </div>


  <span style="margin-left: 240px;">총 : {{ totalList.length }}건</span>
  <span style="margin-left: 20px;">현재 : {{ currentPage }}/{{ totalPages }}페이지</span>

  <!-- 오른쪽 아래 공지사항 카드 START -->
  <div class="d-flex justify-content-center">
    <div class="h-300px d-flex align-items-center justify-content-center mt-4 mb-4" style="width: 2000px;">
      <!--begin::Search-->
      <div class="col-9">
        <!--begin::Contacts-->
        <div class="card card-flush" id="kt_contacts_list">
          <!--begin::Header-->
          <div class="card-header d-flex flex-column align-items-center  pb-2">
            <div class="row w-100">
              <div class="col d-flex justify-content-start">
                <button class="btn btn-success btn-sm fw-bold" @click="getRecentList()">{{btnname}}</button>
              </div>
              <div class="col d-flex justify-content-end">
                <button type="button" class="btn btn-success btn-sm fw-bold" @click="goToHeatmapPage">히트맵</button>
              </div>
            </div>
          </div>
          <!--end::Header-->

          <!--begin::Separator-->
          <div class="separator separator-dashed m-1"></div>
          <!--end::Separator-->

          <!--begin::Card body-->
          <div class="card-body pt-3 pb-4" id="kt_contacts_list_body">
            <div class="container">
              <div class="row" >
                  <div class="col-1 text-center">
                    <span class="text-muted fs-7" >번호</span>
                  </div>
                  <div class="col-1 text-center">
                  </div>
                  <div class="col-5 text-center">
                    <span class="" style="white-space: nowrap">제목</span>
                  </div>
                  <div class="col-3 text-center">
                    <span class="" style="white-space: nowrap">담당</span>
                  </div>
                  <div class="col-2 text-center">
                    <span class="text-muted fs-7">신고일</span>
                  </div>
                  <hr />
                </div>
            </div>
            <!--begin::List-->
            <!--begin::User-->
            <div v-for="(item, index) in displayedList" :key="item.complaintNo" class="container mb-2">
              <!--begin::Details-->
              <div class="row">
                <div class="col-1 text-center">
                  <!-- 글 번호 계산 -->
                  <span class="text-muted fs-7">{{ totalList.length - (currentPage - 1) * itemsPerPage - index }}</span>
                </div>
                <div class="col-1 d-flex justify-content-center">
                  <span class="rounded-box" :style="item.complaintState === '미처리' ? { color: 'red', borderColor: 'red' } : {}">{{ item.complaintState }}</span>
                </div>
                <div class="col-5">
                  <span class="ms-3" style="white-space: nowrap" @click="getComplaintByNo(item.complaintNo)">{{ item.complaintTitle }}</span>
                </div>
                <div class="col-3 d-flex justify-content-center">
                  <span class="ms-3" style="white-space: nowrap">{{ item.institution }}</span>
                </div>
                <div class="col-2 text-center">
                  <span class="text-muted fs-7">{{ item.createdAt.split(' ')[0] }}</span>
                </div>
                <hr />
              </div>
              <!--end::Details-->
            </div>

            <!--end::User-->

            <!--end::List-->
          </div>
          <!--end::Card body-->
          <!-- START:: pagination-->
          <div class="card-footer d-flex justify-content-center" style="height: 55px;">
            <nav aria-label="Page navigation example">
              <ul class="pagination">
                <!-- 이전 페이지 -->
                <li class="page-item" :class="{ disabled: currentPage === 1 }">
                  <button class="page-link" @click="changePage(currentPage - 1)" aria-label="Previous">
                    <span aria-hidden="true">&laquo;</span>
                  </button>
                </li>
                
                <!-- 페이지 번호 -->
                <li v-for="page in totalPages" :key="page" class="page-item" :class="{ active: currentPage === page }">
                  <button class="page-link" @click="changePage(page)">{{ page }}</button>
                </li>

                <!-- 다음 페이지 -->
                <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                  <button class="page-link" @click="changePage(currentPage + 1)" aria-label="Next">
                    <span aria-hidden="true">&raquo;</span>
                  </button>
                </li>
              </ul>
            </nav>
          </div>
          <!-- END:: pagination-->
        </div>
        <!--end::Contacts-->
      </div>
      <!--end::Search-->
    </div>
  </div>
  <!-- 오른쪽 아래 공지사항 카드 END -->


</template>

<script setup>
import HeaderView from '@/components/HeaderView.vue';
import { ref, onMounted, computed } from "vue";
import { useComplaintListStore, useComplaintStore, useRecentComplaintListStore, useManagingComplaintListStore } from "@/stores/complaint"
import router from "@/router/index.js";
import { storeToRefs } from 'pinia';

// 상태 변수 추가
const isRecent = ref(false);
let btnname = ref('담당신고보기');

// 스토어 가져와서 리스트 받아오기
const complaintListStore = useComplaintListStore();
const recentcomplaintListStore = useRecentComplaintListStore();
const managercomplaintListStore = useManagingComplaintListStore();

// onMounted로 컴포넌트가 마운트된 후에 실행되도록 설정
onMounted(async () => {
  await init();
});

let institution = localStorage.getItem("userInstitution")
console.log("로그인한사람담당과:", institution);

async function init() {
  await complaintListStore.fetchComplaintList();
  await recentcomplaintListStore.fetchRecentComplaintList();
  await managercomplaintListStore.fetchManagingComplaintList(institution);
  
}
// eslint-disable-next-line no-unused-vars
const { complaintlist } = storeToRefs(complaintListStore);
const { recentcomplaintlist } = storeToRefs(recentcomplaintListStore);
const { managingcomplaintlist } = storeToRefs(managercomplaintListStore);

// 표시할 리스트를 computed로 정의
const totalList  = computed(() => isRecent.value ? managingcomplaintlist.value : recentcomplaintlist.value);

function getRecentList() {
  isRecent.value = !isRecent.value;
  if(isRecent.value) {
    btnname = '전체보기';
  }else{
    btnname = '담당신고보기';
  }
}

// 번호에 따른 민원 글 데이터 불러오기
const complaintStore = useComplaintStore();
// eslint-disable-next-line no-unused-vars
const complaintone = storeToRefs(complaintStore);

async function getComplaintByNo(key) {
  await complaintStore.fetchComplaintone(key);
  sessionStorage.setItem("complaintNo", key);
  router.replace({path: '/manageInfo'});
}

function goToHeatmapPage() {
  router.replace({ path: "/heatmap" });
}

// 페이지네이션 변수
const currentPage = ref(1);
const itemsPerPage = 10;

// 페이지에 따른 데이터 계산
const displayedList = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  return totalList.value.slice(start, end);
});

// 총 페이지 수 계산
const totalPages = computed(() => Math.ceil(totalList.value.length / itemsPerPage));

// 페이지 변경 함수
function changePage(page) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
}
</script>

<style scoped>
.image-container {
  position: relative; /* 자식 요소의 기준이 되는 컨테이너 */
  width: 100%; /* 컨테이너 가로 폭 */
  max-width: 1200px; /* 이미지 최대 가로 폭 (필요시 설정) */
  height: 350px;
}

.background-img {
  width: 1903px; /* 이미지 가로 폭 */
  height: 330px;
  display: block; /* 불필요한 여백 제거 */
}

.align-to-card {
  position: absolute; /* 부모 컨테이너를 기준으로 위치 */
  bottom: 40px; /* 이미지 아래에서 20px 위로 배치 */
  left: 230px; /* 이미지 왼쪽에서 20px 떨어지도록 배치 */
  font-size: 40px; /* 텍스트 크기 */
  font-weight: 700;
  color: white; /* 텍스트 색상 */
  text-shadow: 2px 2px 5px rgba(0, 0, 0, 0.7); /* 텍스트에 그림자 효과 */
  font-family: 'Roboto', sans-serif;
}

.rounded-box {
  display: inline-block;
  padding: 1px 4px;
  width: 100px;
  border: 2px solid;
  border-radius: 10px;
  text-align: center;
}

hr {
    border: 1px solid #ccc; /* 구분선 색과 두께 */
    margin: 10px 0; /* 구분선 위아래 간격 */
  }

/* 범례 스타일 */
.map-legend {
  position: absolute;
  top: 10px;
  left: 10px;
  background-color: rgba(255, 255, 255, 0.8); /* 반투명 배경 */
  padding: 10px;
  border-radius: 5px;
  font-size: 14px;
  z-index: 2; /* Ensure the legend is on top of the map */
}

.legend-item {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.legend-icon {
  width: 24px;
  height: 24px;
  margin-right: 8px;
}

.legend-item:last-child {
  margin-bottom: 0;
}

/* 선택된(활성) 페이지 색상 변경 */
.pagination .page-item.active .page-link {
  background-color: #04663f; /* success 색상 */
  border-color: #04663f; /* success 색상 */
  color: white; /* 텍스트 색상 */
}
</style>
