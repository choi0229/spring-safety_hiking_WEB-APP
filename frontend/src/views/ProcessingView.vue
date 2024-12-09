<template>
  <!--begin::Content container-->
  <div id="kt_app_content_container" class="app-container container-xxl" style="margin-top: 10px; margin-bottom: 10px;">
    <!--begin::Post card-->
    <div class="card">
      <!--begin::Body-->
      <div class="card-body p-lg-20 pb-lg-0">
          <!--begin::Content-->
          <div class="flex-lg-row-fluid me-xl-15">
            <!--begin::Post content-->
            <div class="mb-17">
              <!--begin::Info-->
              <div class="d-flex flex-wrap mb-2">
                <!--begin::Item-->
                <div>
                  <button class="styled-button" @click="goToManageInfoPage()">
                    뒤로가기
                  </button>
                </div>
                <!--end::Item-->
              </div>
              <!--end::Info-->

              <!-- ==== start 카드 내용 ==== -->
              <div class="card p-3 mt-4 mb-3" style="background-color: #21252908;">
                <!--begin::Title-->
                <div class="row align-items-center mb-3 mt-3">
                  <div class="col-md-1 text-center">
                    <label class="col-form-label fw-bold">처리내용</label>
                  </div>
                  <div class="col-md-11">
                    <input class="form-control form-control-lg" type="text" v-model="contentInput"/>
                  </div>
                </div>
                <!--end::Title-->

                <!-- begin::산이름 -->
                <div class="row align-items-center mb-3 mt-3">
                  <div class="col-md-1 text-center">
                    <label class="col-form-label fw-bold">처리기관</label>
                  </div>
                  <div class="col-auto">
                    <input type="text" id="groupName" class="form-control" v-model="processorInput"/>
                  </div>
                </div>
                <!-- end::산이름 -->
            
                <!-- 이미지 선택 START -->
                <div class="d-flex flex-column fv-row">
                  <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                    <span class="required">처리 후 사진 등록</span>
                  </label>
                  <input
                    type="file"
                    id="image"
                    @change="onFileChange"
                    ref="image"
                  />
                </div>
                <!-- 이미지 선택 END -->

                <!-- begin::등록버튼 -->
                <div class="d-flex justify-content-end">
                  <button
                    id="updateButton"
                    @click.prevent="insertProcessingBtn()"
                    class="btn btn-success fs-6 p-2"
                  >
                  등록하기
                  </button>
                </div>
                <!-- end::등록버튼 -->
              </div>
              <!-- ==== end 카드 내용 ==== -->
            </div>
          </div>
          <!--end::Post content-->
        </div>
        <!--end::Content-->
      <!--end::Layout-->
    </div>
    <!--end::Body-->
  </div>
  <!--end::Post card-->
<!--end::Content container-->
</template>

<script setup>
import { ref, onMounted } from "vue";
import router from "@/router/index.js";
import { insertProcessing } from "@/api/complaint.js";
import axios from 'axios';

// 민원처리정보 저장 필요한 변수 생성
const contentInput = ref("");
const processorInput = ref(localStorage.getItem('userInstitution'));

// onMounted로 컴포넌트가 마운트된 후에 실행되도록 설정
onMounted(() => {
  window.scroll({
    top: 0,
    behavior: 'smooth'
  });
});

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

// 민원 작성 함수 - 서버 요청
async function insertProcessingButton() {
  const imagePath = await uploadImage();
  console.log("Image Path:", imagePath);
  if (!imagePath) {
    console.error("Image upload failed");
    return; // 업로드 실패 시 함수 종료
  }

  const processingData = {
    processingContent: contentInput.value,
    processor: processorInput.value,
    processingComplaintNo: sessionStorage.getItem("complaintNo"),
    processingImg: imagePath || ''
  };
  try {
    const response = await insertProcessing(processingData);
    //console.log("Insert Processing Response:", response);

    // 응답 객체 전체 확인 (디버깅용)
    console.log("Full Response Object:", response);

  } catch (error) {
    console.error("Error in insertProcessingButton:", error);
  }
}

function insertProcessingBtn() {
  insertProcessingButton();
  goToMainPage();

}

// eslint-disable-next-line no-unused-vars
function goToMainPage() {
  console.log("Navigating to /manageList");
  router.push({path: '/manageList'});
}

function goToManageInfoPage() {
  router.replace({path: '/manageInfo'});
}
</script>

<style scoped>
.styled-button {
background: linear-gradient(#d3d3d3, #a9a9a9);
color: white;
padding: 10px 20px;
border: none;
border-radius: 25px;
font-size: 16px;
font-weight: bold;
cursor: pointer;
box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
transition: all 0.3s ease;
}
</style>