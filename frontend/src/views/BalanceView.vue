<template>
    <div id="app">
        <MobileHeaderView></MobileHeaderView>
        <div class="app-main">
            <button @click="goBack(courseData)" class="back-button"><img src="/images/뒤로가기.png" alt="돋보기 아이콘" width="25"
                height="25"></button>
            <div class="comparison-container mb-5">
                <!--begin::Card-->
									<div class="card">
										<!--begin::Card header-->
										<div class="card-header border-0 pt-6">
											<!--begin::Card title-->
											<div class="card-title">
												<!--begin::Search-->
												<div class="d-flex align-items-center position-relative my-1">
													<i class="ki-duotone ki-magnifier fs-3 position-absolute ms-5">
														<span class="path1"></span>
														<span class="path2"></span>
													</i>
													<input type="text" data-kt-user-table-filter="search" class="form-control form-control-solid w-250px ps-13" placeholder="등산코스 검색하기" />
												</div>
												<!--end::Search-->
											</div>
											<!--begin::Card title-->
                                            <div class="d-flex justify-content-end">
                                                <button class="btn btn-success btn-sm" @click="goToCompare()">비교하기</button>
                                            </div>
										</div>
										<!--end::Card header-->
										<!--begin::Card body-->
										<div class="card-body py-4">
											<!--begin::Table-->
											<table class="table align-middle table-row-dashed fs-6 gy-5" id="kt_table_users">
												<thead>
													<tr class="text-start text-muted fw-bold fs-7 text-uppercase gs-0">
														<th class="w-10px pe-2">
															<div class="form-check form-check-sm form-check-custom form-check-solid me-3">
																<input class="form-check-input" type="checkbox" v-model="selectAll" @click="toggleSelectAll" />
															</div>
														</th>
														<th class="min-w-125px">등산코스</th>
													</tr>
												</thead>
												<tbody class="text-gray-600 fw-semibold">
													<tr v-for="(item, index) in courses" :key="item.courseId">
                                                        <td>
                                                            <div class="form-check form-check-sm form-check-custom form-check-solid">
                                                                <input class="form-check-input" type="checkbox" v-model="checkedItems[index]" @change="handleCheckboxChange(item.courseId, index)" />
                                                            </div>
                                                        </td>
                                                        <td class="d-flex align-items-center">
                                                            <div class="d-flex">
                                                                <span class="d-flex justify-content-start me-4">{{ item.mountainName }}</span>
                                                                <span>{{ item.courseName }}코스</span>
                                                            </div>
                                                        </td>
                                                    </tr>
												</tbody>
											</table>
											<!--end::Table-->
										</div>
										<!--end::Card body-->
									</div>
									<!--end::Card-->
            </div>
        </div>
        <MobileFooterView></MobileFooterView>
    </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import MobileFooterView from "@/components/MobileFooterView2.vue";
import MobileHeaderView from "@/components/MobileHeaderView.vue";
import axios from 'axios';
import { useRouter, useRoute } from 'vue-router';


const courses = ref([]); // 전체 산 목록 데이터
const selectedCourseIds = ref([]); // 선택된 courseId 목록

const router = useRouter();
const route = useRoute();

// props로 전달된 mountain 데이터를 JSON으로 파싱
const courseData = ref(JSON.parse(route.query.course));

const selectAll = ref(false);
const checkedItems = reactive([]);

onMounted(() => {
  fetchCourses();
});

// 산 목록 데이터를 불러오는 함수
const fetchCourses = async () => {
try {
  const response = await axios.get('/api/course');
  courses.value = response.data; // 전체 산 목록 저장
} catch (error) {
  console.error('산 목록을 불러오는 중 오류 발생:', error);
}
};

// 코스 비교 페이지로 넘어가기
function goToCompare() {
    router.push({path: '/compareCourse'});
}

// 전체 선택 토글
function toggleSelectAll() {
    if (selectAll.value) {
        // 전체 체크박스가 해제될 때 모든 항목을 선택 해제
        courses.value.forEach((_, index) => {
            checkedItems[index] = false;
        });
        selectedCourseIds.value = []; // 선택된 courseId 목록 초기화
    }
    selectAll.value = false; // 전체 체크박스 상태 업데이트
    updateSelectedCourseIds(); // sessionStorage 업데이트
}

// 개별 체크박스 변경 시 호출
function handleCheckboxChange(courseId, index) {
    if (checkedItems[index]) {
        if (selectedCourseIds.value.length < 2) {
            selectedCourseIds.value.push(courseId); // 선택된 courseId 저장
        } else {
            checkedItems[index] = false; // 선택 취소
            alert('최대 2개까지만 선택할 수 있습니다.');
            return;
        }
    } else {
        selectedCourseIds.value = selectedCourseIds.value.filter(id => id !== courseId); // 선택 해제
    }

    // 선택된 항목이 2개일 경우 전체 체크박스를 자동으로 선택
    selectAll.value = selectedCourseIds.value.length === 2;

    updateSelectedCourseIds(); // sessionStorage 업데이트
}

// 선택된 courseId 목록을 sessionStorage에 저장하는 함수
function updateSelectedCourseIds() {
    sessionStorage.setItem('selectedCourseIds', JSON.stringify(selectedCourseIds.value));
}

const goBack = (course) => {
router.push({
  name: 'mobilemountaindetailview',
  query: {
    course: JSON.stringify(course), 
  },
});
};

</script>

<style scoped>
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
    justify-content: center;
    padding: 1rem;
}

.comparison-container {
    width: 100%;
    max-width: 600px;
    text-align: center;
    overflow-y: auto; /* 스크롤 가능하게 설정 */
    max-height: 80vh; /* 최대 높이 설정 */
}

/* 스크롤바 및 스크롤 트랙을 투명하게 설정 */
.comparison-container::-webkit-scrollbar,
.comparison-container::-webkit-scrollbar-thumb,
.comparison-container::-webkit-scrollbar-track {
    background-color: transparent;
    width: 8px; /* 스크롤바 너비 */
}

.map-container {
    display: flex;
    justify-content: space-around;
    margin-bottom: 1rem;
    height: 400px;
    align-items: center;
}

.map {
    width: 45%;
    height: 400px;
    background-color: #f0f0f0;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #ccc;
}

.map-card {
    position: relative;
    width: 48%;
    height: 400px;
    background-color: #f0f0f0;
    display: flex;
    flex-direction: column; /* 세로로 쌓이게 설정 */
    align-items: center;
    border: 1px solid #ccc;
    box-sizing: border-box;
    overflow: hidden;
}

.map-card-title {
    width: 100%;
    text-align: center;
    background-color: rgba(17, 134, 13, 0.384);
    color: white;
    padding: 4px 8px;
    font-size: 14px;
    font-weight: bold;
}

.map-card img {
    max-width: 100%;
    height: auto;
    object-fit: cover;
    flex: 1; /* 남은 공간을 채우도록 설정 */
}

.comparison-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 1rem;
}

.comparison-table th,
.comparison-table td {
    border: 1px solid #ddd;
    padding: 0.5rem;
    text-align: center;
}

.comparison-table th {
    background-color: #f5f5f5;
    font-weight: bold;
}

.comparison-table td:first-child {
    font-weight: bold;
    background-color: #fafafa;
}

.back-button {
  position: absolute;
  top: 140px;
  left: 30px;
  background-color: transparent;
  color: rgb(12, 12, 12);
  border: none;
  padding: 6px;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  z-index: 10;
  cursor: pointer;
}
</style>
