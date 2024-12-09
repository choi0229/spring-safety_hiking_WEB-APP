<template>
  <div class="container">
    <!-- Post Card -->
    <div class="card">
      <div class="card-body p-lg-5 pb-lg-0">
        <div class="d-flex flex-column flex-md-row">
          <!-- Post Content -->
          <div class="flex-md-grow-1 me-md-4">
            <div class="mb-2">
              <div class="d-flex flex-wrap mb-3 align-items-center">
                <!-- Back Button -->
                <button class="styled-button" @click="goToMainPage()">뒤로가기</button>
              </div>

              <div class="mb-3 mt-4">
                <h1 class="text-gray-900 fs-4 mb-3">
                  위험신고내용
                </h1>
                <!-- START :: 위험신고내용 -->
                <div class="container table-container">
                  <!-- 첫 번째 줄: 제목 -->
                  <div class="row">
                    <div class="col-2 table-header">
                      제목
                    </div>
                    <div class="col-10 table-cell">
                      {{ complaintone.complaintTitle }}
                    </div>
                  </div>
                  
                  <!-- 두 번째 줄: 신고일시 / 신고유형 -->
                  <div class="row">
                    <div class="col-2 table-header">
                      신고일시
                    </div>
                    <div class="col-4 table-cell">
                      {{ complaintone.createdAt }}
                    </div>
                    <div class="col-2 table-header">
                      신고유형
                    </div>
                    <div class="col-4 table-cell">
                      {{ complaintone.complaintType }}
                    </div>
                  </div>
                  
                  <!-- 세 번째 줄: 산이름 -->
                  <div class="row">
                    <div class="col-2 table-header">
                      산이름
                    </div>
                    <div class="col-10 table-cell">
                      {{ complaintone.mountainName }}
                    </div>
                  </div>
                  
                  <!-- 네 번째 줄: 내용 -->
                  <div class="row lastrow mb-2">
                    <div class="col-2 table-header">
                      내용
                    </div>
                    <div class="col-10 table-cell">
                      {{ complaintone.complaintContent }}
                    </div>
                  </div>
                </div>
                <!-- END :: 위험신고내용 -->
              </div>

              <div class="mb-5 mt-5">
                <h1 class="text-gray-900 fs-4 mb-3">
                  처리결과
                </h1>
                <!-- START :: 처리결과내용 -->
                <div class="container table-container">
                  <!-- 첫 번째 줄: 처리내용 -->
                  <div class="row">
                    <div class="col-2 table-header">
                      처리내용
                    </div>
                    <div class="col-10 table-cell">
                      {{ processingone?.processingContent ? processingone.processingContent : '처리전입니다.' }}
                    </div>
                  </div>
                  
                  <!-- 두 번째 줄: 처리기관 -->
                  <div class="row lastrow mb-2">
                    <div class="col-2 table-header">
                      처리기관
                    </div>
                    <div class="col-10 table-cell">
                      {{ processingone?.processor ? processingone.processor : '처리전입니다.'}}
                    </div>
                  </div>
                </div>
                <!-- END :: 처리결과내용 -->
              </div>

              <!-- Image Section -->
              <div class="d-flex justify-content-center align-items-center">
                <div class="col-5 image-container mb-2 d-flex justify-content-center position-relative">
                  <img
                    :src="complaintone.complaintImg"
                    alt="Complaint Image"
                    class="img-fluid"
                  />
                  <!-- 전 삼각형 -->
                  <div class="triangle">
                    <span class="triangle-text">전</span>
                  </div>
                </div>
                <div class="col-1 ms-1">
                  <i class="bi bi-chevron-double-right fs-1 ms-4" style="color: gray;"></i>
                </div>
                <div class="col-5 image-container mb-2 d-flex justify-content-center">
                  <img
                    :src="processingone?.processingImg ? processingone.processingImg : '/images/normal.png'"
                    alt="Processing Image"
                    class="img-fluid"
                  />
                  <!-- 후 삼각형 -->
                  <div class="triangle2">
                    <span class="triangle-text">후</span>
                  </div>
                </div>
              </div>
              
              <hr class="separator-line mt-5 mb-4" />

              <!-- Post Content -->
              <div class="mb-4">
                <button v-if="!processingone?.processingContent" class="styled-button2 me-2" @click="goToProcessing()">
                처리결과등록하기</button>
                <button v-else class="styled-button2 me-2" @click="showUpdateModal()">
                처리결과수정하기
                </button>
              </div>
              
            </div>
          </div>
          <!-- End Post Content -->
        </div>
      </div>
    </div>
  </div>

  <!-- //==== 민원처리정보 수정 모달 START ====// -->
  <div class="modal fade" id="processing_update" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded">
        <div class="modal-header pb-0 border-0 justify-content-end">
          <!--begin::닫기 아이콘-->
          <div class="" data-bs-dismiss="modal">
            <button class="btn btn-sm btn-success">닫기</button>
          </div>
          <!--end::닫기 아이콘-->
        </div>
        <div class="modal-body scroll-y px-10 px-lg-15 pt-0 pb-15">
          <form id="kt_modal_new_target_form" class="form">
            <!--begin::제목-->
            <div class="mb-8 text-center">
              <!--begin::Title-->
              <h1 class="mb-4">처리결과 수정</h1>
              <!--end::Title-->
              <!--begin::Description-->
              <div class="text-muted fs-6">내용, 결과, 이미지를 수정하세요.</div>
              <!--end::Description-->
            </div>
            <!--end::제목-->

            <!--begin::처리내용 입력상자-->
            <div class="d-flex flex-column mb-8 fv-row mt-3">
              <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                <span class="required">처리내용</span>
              </label>
              <input type="text" class="form-control form-control-solid" name="target_content" v-model="contentInput" />
            </div>
            <!--end::처리내용 입력상자-->

            <!--begin::처리기관 입력상자-->
            <div class="d-flex flex-column mb-8 fv-row">
              <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                <span class="required">처리기관</span>
              </label>
              <input type="text" class="form-control form-control-solid" name="target_processor" v-model="processorInput" />
            </div>
            <!--end::처리기관 입력상자-->

            <!-- 이미지 선택 START -->
            <div class="d-flex flex-column mb-8 fv-row">
              <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                <span class="required">처리 후 파일 선택</span>
              </label>
              <input type="file" id="image" @change="onFileChange" ref="image" />
            </div>
            <!-- 이미지 선택 END -->

            <!--begin::하단버튼-->
            <div class="text-center mt-3">
              <button type="button" class="btn btn-success" @click.prevent="updateProcessing(processingone.processingComplaintNo, $event)">저장</button>
              <button type="reset" class="btn btn-light ms-3" @click="clearAll()">모두 지우기</button>
            </div>
            <!--end::하단버튼-->
          </form>
          <!--end:Form-->
        </div>
        <!--end::Modal body-->
      </div>
      <!--end::Modal content-->
    </div>
  </div>
  <!-- //==== 민원처리정보 수정 모달 END ====// -->

</template>

<script setup>
import { ref, onMounted } from "vue";
import router from "@/router/index.js";
import { getComplaintByNo, getProcessingByNo, updateProcessingByNo } from "@/api/complaint";
import { Modal } from 'bootstrap';
import axios from 'axios';

let complaintNo = 0;

// onMounted로 컴포넌트가 마운트된 후에 실행되도록 설정
onMounted(async () => {
  complaintNo = sessionStorage.getItem("complaintNo");
  await init(complaintNo);
});

// 민원, 민원처리정보 한개씩 가져옴
let complaintone = ref({});
let processingone = ref({});
let userInstitution = '';

async function init(complaintNo) {
  try {
    // 비동기로 민원글 정보를 가져옴
    const response = await getComplaintByNo(complaintNo);
    complaintone.value = response;
    console.log("민원 가져온거", complaintone.value);
    const response2 = await getProcessingByNo(complaintNo);
    processingone.value = response2;
    console.log("민원처리정보 가져온거", processingone.value);
    userInstitution = localStorage.getItem('userInstitution');
  } catch (error) {
    console.error("민원글을 가져오는 중 오류 발생:", error);
  }
}

let updateProcessingModal;

// 처리결과정보 수정에 필요한 변수 생성
const processorInput = ref("");
const contentInput = ref("");

// 처리결과정보 수정하기 버튼 - 모달 열기
function showUpdateModal() {

  window.scrollTo({
    top: 0,
    behavior: 'smooth'  // 부드럽게 스크롤하려면 'smooth', 즉시 이동하려면 'auto'
    });

  // 대화상자에 입력값 넣어주기
  contentInput.value = processingone.value.processingContent;
  processorInput.value = processingone.value.processor;

  // 대화상자 띄우기
  const elem = document.querySelector('#processing_update');
  updateProcessingModal = new Modal(elem);
  updateProcessingModal.show();
}


// 민원 수정하기 요청
async function updateProcessing(processingComplaintNo, event) {

  event.preventDefault();
  console.log("Update processing triggered");

  let imagePath = await uploadImage();
  if(imagePath == null) {
    imagePath = processingone.value.processingImg;
  }

  const data = {
    processingContent: contentInput.value,
    processor: processorInput.value,
    processingComplaintNo: processingComplaintNo,
    processingImg: imagePath || ''
  }
  processingone.value.processingContent = contentInput.value;
  processingone.value.processor = processorInput.value;
  processingone.value.processingImg = imagePath;

  updateProcessingByNo(processingComplaintNo, data);
  updateProcessingModal.hide();

  //router.replace({path:'/complaintListMobile'});

}

// ==== 이미지 관련 코드 ==== //
// 이미지 주소로 넘기기
const selectedFile = ref(null);

const onFileChange = (event) => {
  selectedFile.value = event.target.files[0];
};

const uploadImage = async () => {
  if (!selectedFile.value) {
    return null; // 이미지가 없으면 null 반환
  }

  const formData = new FormData();
  formData.append('file', selectedFile.value);

  try {
    const response = await axios.post(
      '/api/complaint/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data; // 서버에서 반환한 이미지 경로
  } catch (error) {
    console.error('Error uploading image', error);
    return null;
  }
};


// 민원 목록 페이지로 이동
function goToMainPage() {
  router.replace({ path: "/manageList" });
}

// 민원처리정보 등록 페이지로 이동
function goToProcessing() {
  console.log('실행')
  if(userInstitution.trim().toLowerCase() !== complaintone.value.institution.trim().toLowerCase()) {
    alert('담당 업무만 처리가능합니다!');
    return;
  }
  router.replace({ path: "/processing"});
}



</script>

<style scoped>
.map-container {
  width: 100%;
  height: 300px; /* 지도의 높이를 명확하게 지정하세요 */
  border: 1px solid #ccc;
}

hr.separator-line {
  border: 1px solid #ccc;
  margin: 10px 0;
  width: 100%;
}

.card {
  cursor: pointer;
  margin-top: 10px;
  margin-bottom: 10px;
}

.image-container {
  width: 500px;
  height: 350px;
  position: relative;
  overflow: hidden; /* 이미지가 틀을 넘어가면 잘리게 */
}

.fixed-size-image {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 이미지 비율 유지하면서 틀에 맞추기 */
}


img {
  display: block;
  width: 100%; /* 이미지가 컨테이너에 맞게 사이즈 조정 */
  height: auto; /* 원본 비율을 유지 */
}

.styled-button {
  background: linear-gradient(#d3d3d3, #a9a9a9);
  color: white;
  padding: 8px 16px;
  border: none;
  border-radius: 25px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}

.styled-button2 {
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

.styled-button:hover {
  background: linear-gradient(#a9a9a9, #808080);
}

/* 표 선 관련 스타일 */
.table-container {
  border-collapse: collapse;
  border-top: 3px solid black; /* 전체 표 맨 위에만 진한 선 */
}

.table-header {
  background-color: #f0f0f0; /* 회색 배경 */
  padding: 10px;
  padding-left: 20px;
  text-align: left;
}

.table-cell {
  padding: 10px;
  padding-left: 20px;
}

.container .row {
  border-bottom: 1px solid lightgray; /* 줄 사이 연한 선 */
}

.row:last-child {
  border-bottom: 1px solid black; /* 마지막 줄 기본 선 */
}

.triangle {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  border-right: 55px solid transparent; /* 삼각형의 크기 및 투명한 왼쪽 */
  border-top: 55px solid gray; /* 삼각형의 색상 및 크기 */
  z-index: 1; /* 삼각형이 이미지 위에 나오도록 설정 */
}

.triangle2 {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  border-right: 55px solid transparent; /* 삼각형의 크기 및 투명한 왼쪽 */
  border-top: 55px solid blue; /* 삼각형의 색상 및 크기 */
  z-index: 1; /* 삼각형이 이미지 위에 나오도록 설정 */
}

.triangle-text {
  position: absolute;
  bottom: 26px; /* 텍스트의 세로 위치 */
  left: 10px; /* 텍스트의 가로 위치 */
  color: white;
  font-size: 14px;
  font-weight: bold;
  z-index: 2; /* 텍스트가 삼각형 위에 나오도록 설정 */
}
</style>