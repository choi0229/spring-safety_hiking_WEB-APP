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
                  <button class="styled-button" @click="goToMainPage()">
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
                    <label class="col-form-label fw-bold">제목</label>
                  </div>
                  <div class="col-md-11">
                    <input class="form-control form-control-lg" type="text" v-model="titleInput"/>
                  </div>
                </div>
                <!--end::Title-->

                <!-- begin::타입 -->
                <div class="row align-items-center">
                  <div class="col-md-1 text-center">
                    <label for="category" class="col-form-label fw-bold">구분</label>  
                  </div>
                  <div class="col-auto">
                    <div class="form-group">
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
                  </div>
                </div>
                <!-- end::타입 -->

                <!-- begin::산이름 -->
                <div class="row align-items-center mb-3 mt-3">
                  <div class="col-md-1 text-center">
                    <label class="col-form-label fw-bold">산 이름</label>
                  </div>
                  <div class="col-auto">
                    <input type="text" id="groupName" class="form-control" v-model="mountainNameInput"/>
                  </div>
                </div>
                <!-- end::산이름 -->
            
                <!--begin::민원 지도 찍기-->
                <div class="">
                  <div id="map" style="width: 100%; height: 350px"></div>
                  <p><em>지도를 클릭해주세요!</em></p>
                  <div id="clickLatlng"></div>
                </div>
                <!--end::민원 지도 찍기-->
            
                <!-- 이미지 선택 START -->
                <div class="d-flex flex-column fv-row">
                  <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                    <span class="required">사진 등록</span>
                  </label>
                  <input
                    type="file"
                    id="image"
                    @change="onFileChange"
                    ref="image"
                  />
                </div>
                <!-- 이미지 선택 END -->

                <!--begin::설명-->
                <div class="mb-3 mt-3">
                  <label for="exampleFormControlTextarea1" class="form-label">위치, 시간, 내용을 자세히 입력해주세요.</label>
                  <textarea
                    class="form-control"
                    id="exampleFormControlTextarea1"
                    rows="7"
                    v-model="contentInput"></textarea>
                </div>
                <!--end::설명-->
                <!-- begin::등록버튼 -->
                <div class="d-flex justify-content-end">
                  <button
                    id="updateButton"
                    @click="insertComplaintButton()"
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
/* global kakao */
import { ref, onMounted } from "vue";
import router from "@/router/index.js";
import { insertComplaint } from "@/api/complaint.js";
import axios from 'axios';

// 민원글 저장 필요한 변수 생성
const titleInput = ref("");
let userIdInput = localStorage.getItem("userNickName");
const mountainNameInput = ref("");
const contentInput = ref("");
const typeInput = ref("");
let latitudeInput = 0;
let longitudeInput = 0;


// onMounted로 컴포넌트가 마운트된 후에 실행되도록 설정
onMounted(() => {
initializeMap();
});

function initializeMap() {
// Kakao 지도 API 스크립트를 로드
const script = document.createElement('script');
script.onload = () => kakao.maps.load(createMap); // 스크립트 로드 후 createMap 호출
script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=services'; // Kakao 지도 API
document.head.appendChild(script);

}

function createMap() {
const mapContainer = document.getElementById("map"); // 지도를 표시할 div 선택
const mapOption = {
    center: new kakao.maps.LatLng(37.660833, 126.993333), // 지도의 중심 좌표
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
async function insertComplaintButton() {
const imagePath = await uploadImage();

const complaintData = {
complaintTitle: titleInput.value,
complaintContent: contentInput.value,
complaintType: typeInput.value,
complaintImg: imagePath || '',
userId: userIdInput,
mountainName: mountainNameInput.value,
latitude: latitudeInput,
longitude: longitudeInput
};

try {
const response = await insertComplaint(complaintData);
console.log('서버 응답: ', response);

goToMainPage();
} catch (error) {
console.error(error);
}
}


function goToMainPage() {
router.replace({ path: "/complaintList" });
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
