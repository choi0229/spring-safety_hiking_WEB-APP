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

              <!-- Post Title and Author -->
              <div class="mb-3">
                <h1 class="text-gray-900 fs-3 fw-bold mb-2">
                  {{ complaintone.complaintTitle }}
                </h1>
                <hr class="separator-line2" />
                <span class="fw-bold text-muted fs-6 ps-1">
                  신고자 : {{ complaintone.userId }}
                </span>
                <hr class="separator-line" />
                <span class="fw-bold text-muted fs-6 ps-1">
                  신고 날짜 : {{ complaintone.createdAt }}
                </span>
                <hr class="separator-line" />
                <span class="fw-bold text-muted fs-6 ps-1">
                  신고 유형 : {{ complaintone.complaintType }}
                </span>
                <hr class="separator-line" />
                <span class="fw-bold text-muted fs-6 ps-1">
                  산이름 : {{ complaintone.mountainName }}
                </span>
              </div>

              <!-- Image Section -->
              <hr class="separator-line" />
              <div class="image-container mb-2 d-flex justify-content-center">
                <img
                  :src="complaintone.complaintImg"
                  alt="Complaint Image"
                  class="img-fluid"
                />
              </div>

              <!-- Post Content -->
              <hr class="separator-line" />
              <p class="fw-bold text-muted fs-6 ps-1">신고 내용</p>
              <div class="card mb-2" style="height: 150px">
                <div class="card-body p-2">
                  <p class="fs-6 text-gray-600">
                    {{ complaintone.complaintContent }}
                  </p>
                </div>
              </div>
              <hr class="separator-line mt-3" />
              <button class="styled-button2 me-2" @click="showUpdateModal()">수정</button>
              <button
                class="styled-button2"
                @click="deleteComplaint(complaintone.complaintNo)"
              >
                삭제
              </button>
            </div>
          </div>
          <!-- End Post Content -->
        </div>
      </div>
    </div>
  </div>

  <!-- //==== 민원 수정 모달 START ====// -->
  <div class="modal fade" id="complaint_update" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog mw-1000px" style="width: 800px">
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
              <h1 class="mb-4">신고 글 수정</h1>
              <!--end::Title-->
              <!--begin::Description-->
              <div class="text-muted fs-6">제목, 이미지, 위치, 내용을 수정하세요.</div>
              <!--end::Description-->
            </div>
            <!--end::제목-->

            <!--begin::이름 입력상자-->
            <div class="d-flex flex-column mb-8 fv-row mt-3">
              <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                <span class="required">제목</span>
              </label>
              <input
                type="text"
                class="form-control form-control-solid"
                name="target_title"
                v-model="titleInput"
              />
            </div>
            <!--end::이름 입력상자-->

            <!-- begin::타입 -->
            <label for="category" class="col-form-label fw-bold">구분</label>
            <div class="form-group mb-2">
              <select
                v-model="typeInput"
                id="number"
                name="number"
                class="form-control"
                style="cursor: pointer"
              >
                <option value="" disabled>유형 선택</option>
                <option value="파손">파손</option>
                <option value="낙석">낙석</option>
                <option value="화재">화재</option>
              </select>
            </div>
            <!-- end::타입 -->

            <!--begin::산이름 입력상자-->
            <div class="d-flex flex-column mb-8 fv-row">
              <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                <span class="required">산이름</span>
              </label>
              <input
                type="text"
                class="form-control form-control-solid"
                name="target_title"
                v-model="mountainNameInput"
              />
            </div>
            <!--end::산이름 입력상자-->

            <!--begin::민원 지도 찍기-->
            <div class="mb-3 mt-3">
              <div id="map" class="map-container"></div>
              <p><em>지도를 클릭해주세요!</em></p>
              <div id="clickLatlng"></div>
            </div>
            <!--end::민원 지도 찍기-->

            <!-- 이미지 선택 START -->
            <div class="d-flex flex-column mb-8 fv-row">
              <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                <span class="required">파일 선택</span>
              </label>
              <input type="file" id="image" @change="onFileChange" ref="image" />
            </div>
            <!-- 이미지 선택 END -->
            <!--begin::내용 입력상자-->
            <div class="d-flex flex-column mb-8 fv-row mt-2">
              <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                <span class="required">내용</span>
                <span
                  class="ms-1"
                  data-bs-toggle="tooltip"
                  title="내용을 입력하세요. 필수랍니다~"
                >
                  <i class="ki-duotone ki-information-5 text-gray-500 fs-6">
                    <span class="path1"></span>
                    <span class="path2"></span>
                    <span class="path3"></span>
                  </i>
                </span>
              </label>
              <textarea
                class="form-control form-control-solid"
                name="target_content"
                v-model="contentInput"
                rows="5"
              ></textarea>
            </div>
            <!--end::내용 입력상자-->
            <!--begin::하단버튼-->
            <div class="text-center mt-3">
              <button
                type="button"
                class="btn btn-success"
                @click="updateComplaint(complaintone.complaintNo)"
              >
                저장
              </button>
              <button type="reset" class="btn btn-light ms-3" @click="clearAll()">
                모두 지우기
              </button>
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
  <!-- //==== 민원 수정 모달 END ====// -->
</template>

<script setup>
/* global kakao */
import { ref } from "vue";
import { useComplaintStore } from "@/stores/complaint";
import { storeToRefs } from "pinia";
import router from "@/router/index.js";
import { deleteComplaintByNo, updateComplaintByNo } from "@/api/complaint";
import { Modal } from "bootstrap";
import axios from "axios";

// 민원글 수정에 필요한 변수 생성
const titleInput = ref("");
const mountainNameInput = ref("");
const contentInput = ref("");
const typeInput = ref("");
let latitudeInput = 0;
let longitudeInput = 0;

// 민원 한개 가져오는 스토어 사용
const complaintStore = useComplaintStore();
const { complaintone } = storeToRefs(complaintStore);

// 민원 목록 페이지로 이동
function goToMainPage() {
  router.replace({ path: "/complaintList" });
}

// 민원 삭제하기 버튼
function deleteComplaint(complaintNo) {
  if (confirm("정말 삭제하시겠습니까??") == true) {
    deleteComplaintByNo(complaintNo);
    goToMainPage();
  } else {
    return false;
  }
}

let updateComplaintModal;

// 민원 수정하기 버튼 - 모달 열기
function showUpdateModal() {
    window.scrollTo({
    top: 0,
    behavior: 'smooth'  // 부드럽게 스크롤하려면 'smooth', 즉시 이동하려면 'auto'
    });

  // 대화상자에 입력값 넣어주기
  titleInput.value = complaintone.value.complaintTitle;
  mountainNameInput.value = complaintone.value.mountainName;
  contentInput.value = complaintone.value.complaintContent;
  typeInput.value = complaintone.value.complaintType;

  // 대화상자 띄우기
  const elem = document.querySelector("#complaint_update");
  updateComplaintModal = new Modal(elem);
  updateComplaintModal.show();
  setTimeout(() => {
    initializeMap();
  }, 500);
}

function initializeMap() {
  // Kakao 지도 API 스크립트를 로드
  const script = document.createElement("script");
  script.onload = () => kakao.maps.load(createMap); // 스크립트 로드 후 createMap 호출
  script.src =
    "https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=services"; // Kakao 지도 API
  document.head.appendChild(script);
}

function createMap() {
  const mapContainer = document.getElementById("map"); // 지도를 표시할 div 선택
  const mapOption = {
    center: new kakao.maps.LatLng(
      complaintone.value.latitude,
      complaintone.value.longitude
    ), // 지도의 중심 좌표
    level: 8, // 지도 확대 레벨
  };

  // 지도 생성
  const map = new kakao.maps.Map(mapContainer, mapOption);

  // 지도를 클릭한 위치에 표출할 마커입니다
  var marker = new kakao.maps.Marker({
    // 지도 중심좌표에 마커를 생성합니다
    position: map.getCenter(),
  });
  // 지도에 마커를 표시합니다
  marker.setMap(map);

  // 지도에 클릭 이벤트를 등록합니다
  // 지도를 클릭하면 마지막 파라미터로 넘어온 함수를 호출합니다
  kakao.maps.event.addListener(map, "click", function (mouseEvent) {
    // 클릭한 위도, 경도 정보를 가져옵니다
    var latlng = mouseEvent.latLng;

    // 마커 위치를 클릭한 위치로 옮깁니다
    marker.setPosition(latlng);

    var message = "클릭한 위치의 위도는 " + latlng.getLat() + " 이고, ";
    message += "경도는 " + latlng.getLng() + " 입니다";

    latitudeInput = latlng.getLat();
    longitudeInput = latlng.getLng();

    var resultDiv = document.getElementById("clickLatlng");
    resultDiv.innerHTML = message;
  });
}

// 민원 수정하기 요청
async function updateComplaint(complaintNo) {
  let imagePath = await uploadImage();
  if (imagePath == null) {
    imagePath = complaintone.value.complaintImg;
  }

  console.log("수정할 글번호 : ", complaintNo);

  // 기존 좌표를 가져옵니다.
  const existingLatitude = complaintone.value.latitude;
  const existingLongitude = complaintone.value.longitude;

  // 위도와 경도를 업데이트합니다. 사용자가 클릭하지 않았다면 기존 좌표를 사용합니다.
  const latitude = latitudeInput !== 0 ? latitudeInput : existingLatitude;
  const longitude = longitudeInput !== 0 ? longitudeInput : existingLongitude;

  const data = {
    complaintNo: complaintNo,
    complaintTitle: titleInput.value,
    complaintContent: contentInput.value,
    complaintType: typeInput.value,
    mountainName: mountainNameInput.value,
    latitude: latitude,
    longitude: longitude,
    complaintImg: imagePath || "",
  };
  complaintone.value.complaintTitle = titleInput.value;
  complaintone.value.complaintContent = contentInput.value;
  complaintone.value.complaintType = typeInput.value;
  complaintone.value.mountainName = mountainNameInput.value;
  complaintone.value.latitude = latitudeInput;
  complaintone.value.longitude = longitudeInput;
  complaintone.value.complaintImg = imagePath;

  updateComplaintByNo(complaintNo, data);
  updateComplaintModal.hide();
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
  formData.append("file", selectedFile.value);

  try {
    const response = await axios.post("/api/complaint/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response.data; // 서버에서 반환한 이미지 경로
  } catch (error) {
    console.error("Error uploading image", error);
    return null;
  }
};
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

.image-container img {
  width: 700px;
  height: auto;
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
</style>
