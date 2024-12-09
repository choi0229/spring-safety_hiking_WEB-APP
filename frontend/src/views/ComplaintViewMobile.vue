<template>
  
  <!--begin::Post card-->
  <div class="card">
    <!--begin::Body-->
    <div class="card-body p-lg-20 pb-lg-0">
      <!--begin::Layout-->
      <div class="d-flex flex-column flex-xl-row">
        <!--begin::Content-->
        <div class="flex-lg-row-fluid me-xl-15">
          <!--begin::Post content-->
          <div class="mb-17">
            <!--begin::Wrapper-->
            <div class="mb-2">
              <!--begin::Info-->
              <div class="d-flex justify-content-between align-items-center mb-4">
                <button class="styled-button" @click="goToMainPage()">
                  뒤로가기
                </button>
              </div>
              <!--end::Info-->
              
              <!--begin::Title-->
              <div class="row align-items-center mb-2">
                <div class="col-md-2">
                  <label class="col-form-label fw-bold">제목</label>
                </div>
                <div class="col-md-10">
                  <input class="form-control form-control-lg" type="text" v-model="titleInput"/>
                </div>
              </div>
              <!--end::Title-->

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
                  <option value="야생동물">야생동물</option>
                </select>
              </div>
              <!-- end::타입 -->

              <!--begin::작성자
              <div class="row align-items-center">
                <div class="col-md-2">
                  <label class="col-form-label fw-bold">작성자</label>
                </div>
                <div class="col-md-10">
                  <input class="form-control form-control-lg" type="text" v-model="userIdInput"/>
                </div>
              </div>
              end::작성자-->
            </div>
            <!--end::Wrapper-->

            <!--begin::산 이름-->
            <div class="row align-items-center mb-3">
              <div class="col-md-2">
                <label class="col-form-label fw-bold">산 이름</label>
              </div>
              <div class="col-md-10">
                <input type="text" id="groupName" class="form-control" v-model="mountainNameInput"/>
              </div>
            </div>
            <!--end::산 이름-->

            <button type="button" @click="openSearchModal" class="mb-2">장소 검색</button>

            <!--begin::민원 지도 찍기-->
            <div class="mb-3">
              <div id="map" class="map-container"></div>
              <p><em>지도를 클릭해주세요!</em></p>
              <div id="clickLatlng"></div>
            </div>
            <!--end::민원 지도 찍기-->

            <!--begin::사진 등록-->
            <div class="d-flex flex-column mb-3 fv-row">
              <label class="d-flex align-items-center fs-6 fw-semibold mb-2">
                <span class="required">사진 등록</span>
              </label>
              <input
                type="file"
                id="image"
                @change="onFileChange"
                ref="image"
                accept="image/*"
              />
            </div>
            <!--end::사진 등록-->

            <!--begin::Description-->
            <div class="mb-4">
              <label for="exampleFormControlTextarea1" class="form-label">위치, 시간, 내용을 자세히 입력해주세요.</label>
              <textarea
                class="form-control"
                id="exampleFormControlTextarea1"
                rows="6"
                v-model="contentInput"></textarea>
            </div>
            <!--end::Description-->

            <!--begin::등록하기 버튼-->
            <div class="d-flex justify-content-end">
              <button
                id="updateButton"
                @click="insertComplaintBtn()"
                class="btn btn-success fs-6 p-2"
              >
                등록하기
              </button>
            </div>
            <!--end::등록하기 버튼-->

          </div>
          <!--end::Post content-->
        </div>
        <!--end::Content-->
      </div>
      <!--end::Layout-->
    </div>
    <!--end::Body-->
  </div>
  <!--end::Post card-->
<!-- 장소 검색 모달 -->
<div v-if="isModalOpen" class="modal">
      <div class="modal-content">
        <span class="close" @click="closeSearchModal">&times;</span>
        <input class="mt-4" v-model="keywordInput" placeholder="장소 검색" />
        <button @click="searchPlaces" class="searchButton">검색하기</button>

        <ul v-if="searchResults.length > 0">
          <li v-for="(place, index) in searchResults" :key="index" @click="selectPlace(place)">
            {{ place.place_name }} ({{ place.address_name }})
          </li>
        </ul>
      </div>
    </div>
</template>


<script setup>
/* global kakao */
import { ref, onMounted } from "vue";
import router from "@/router/index.js";
import { insertComplaint } from "@/api/complaint.js";
import axios from 'axios';

// 위치 상태 관리
const latitude = ref(37.6584);
const longitude = ref(126.9775);
const altitude = ref(null);
const speed = ref(null);
const bearing = ref(null);
const time = ref("");

// 민원글 저장 필요한 변수 생성
const titleInput = ref("");
let userIdInput = localStorage.getItem("userId");
let userNickNameInput = localStorage.getItem("userNickName");
const mountainNameInput = ref("");
const contentInput = ref("");
const typeInput = ref("");
let latitudeInput = latitude.value;
let longitudeInput = longitude.value;

// 모달 상태를 제어하는 변수
const isModalOpen = ref(false);
const keywordInput = ref("");
const searchResults = ref([]);

// Kakao Maps가 로드된 상태를 추적하는 변수
let isKakaoMapLoaded = ref(false);

// 마커 변수 추가
let marker = null;

onMounted(() => {
    initializeMap();
    fetchLocation();
    setInterval(fetchLocation, 5000); // 3초마다 위치 데이터 가져오기
});

function initializeMap() {
    const script = document.createElement('script');
    script.onload = () => {
        kakao.maps.load(() => {
            createMap();
            isKakaoMapLoaded.value = true; // Kakao Maps 로드 완료
        });
    };
    script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=services'; 
    document.head.appendChild(script);
}

let map; // 전역으로 맵 객체를 선언합니다

function createMap() {
    const mapContainer = document.getElementById("map");
    const mapOption = {
        center: new kakao.maps.LatLng(latitude.value, longitude.value),
        level: 5,
    };

    // 맵 객체를 생성하고 전역 변수에 할당합니다.
    map = new kakao.maps.Map(mapContainer, mapOption);

    // 마커 초기화
    marker = new kakao.maps.Marker({
        position: map.getCenter(),
    });
    marker.setMap(map);

    kakao.maps.event.addListener(map, "click", function (mouseEvent) {
        const latlng = mouseEvent.latLng;
        // 클릭한 위치에 마커를 이동
        marker.setPosition(latlng);
        latitudeInput = latlng.getLat();
        longitudeInput = latlng.getLng();
        document.getElementById("clickLatlng").innerHTML = `위도: ${latlng.getLat()}, 경도: ${latlng.getLng()}`;
    });
    //console.log("위도",latitudeInput,"경도",longitudeInput);
}

// 서버에서 위치 정보들을 가져오는 함수
async function fetchLocation() {
    try {
        const response = await axios.get('/api/recordlocation');
        console.log("Received response:", response.data); // 응답 데이터 확인
        latitude.value = response.data.latitude;
        longitude.value = response.data.longitude;
        altitude.value = response.data.altitude;
        speed.value = response.data.speed;
        bearing.value = response.data.bearing;
        time.value = response.data.time;

    } catch (error) {
        console.error("Error fetching location:", error);
    }
}

// ==== 장소 검색 관련 함수 ==== //
function openSearchModal() {
    isModalOpen.value = true; 
}

function closeSearchModal() {
    isModalOpen.value = false;
}

function searchPlaces() {
    const keyword = keywordInput.value.trim();
    if (!keyword) {
        alert('키워드를 입력해주세요!');
        return;
    }

    if (!isKakaoMapLoaded.value) {
        alert('Kakao Maps가 아직 로드되지 않았습니다.');
        return;
    }

    const ps = new kakao.maps.services.Places();
    ps.keywordSearch(keyword, placesSearchCB);
}

function placesSearchCB(data, status) {
    if (status === kakao.maps.services.Status.OK) {
        searchResults.value = data; 
    } else {
        alert('검색 결과가 없습니다.');
    }
}

// 선택한 장소로 메인 맵에 마커를 표시하는 함수
function selectPlace(place) {
    latitudeInput = place.y;
    longitudeInput = place.x;

    // 기존 맵의 중심을 선택한 장소로 이동
    const centerPosition = new kakao.maps.LatLng(place.y, place.x);
    marker.setPosition(centerPosition); // 마커의 위치를 업데이트
    map.setCenter(centerPosition); // 맵의 중심을 선택한 장소로 이동

    document.getElementById("clickLatlng").innerHTML = `위도: ${place.y}, 경도: ${place.x}`; // 위도 경도 표시
    closeSearchModal(); // 모달을 닫습니다
}

// ==== 이미지 관련 코드 ====
const selectedFile = ref(null);

const onFileChange = (event) => {
  selectedFile.value = event.target.files[0];
};

const uploadImage = async () => {
  if (!selectedFile.value) {
    return null;
  }
  const formData = new FormData();
  formData.append('file', selectedFile.value);

  try {
    const response = await axios.post('/api/complaint/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  } catch (error) {
    console.error('Error uploading image', error);
    return null;
  }
};

// 민원 작성 함수
async function insertComplaintButton() {
  const imagePath = await uploadImage();
  const complaintData = {
    complaintTitle: titleInput.value,
    complaintContent: contentInput.value,
    complaintType: typeInput.value,
    complaintImg: imagePath || '',
    userId: userIdInput,
    userNickName: userNickNameInput,
    mountainName: mountainNameInput.value,
    latitude: latitudeInput,
    longitude: longitudeInput
  };

  try {
    const response = await insertComplaint(complaintData);
    console.log('서버 응답: ', response);
  } catch (error) {
    console.error('서버 요청 중 오류 발생: ', error);
  }
}

function insertComplaintBtn() {
  insertComplaintButton();
  goToMainPage();
}

function goToMainPage() {
  console.log("목록페이지로 이동")
  router.replace({ path: "/complaintListMobile" });
}
</script>

<style scoped>

.card {
  margin: 10px;
}

/* Map container styling */
.map-container {
  width: 100%;
  height: 400px;
}

/* Button styling */
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

/* Input and textarea styles */
input.form-control, textarea.form-control {
  font-size: 16px;
  padding: 10px;
  width: 100%;
}

/* Margin bottom for spacing between elements */
.mb-4, .mb-5 {
  margin-bottom: 1.5rem;
}


/* Media queries for 360x740 */
@media (max-width: 360px) {
  .card {
    padding: 15px;
  }

  /* Title and input fields */
  label.col-form-label.fw-bold {
    font-size: 14px;
  }

  input.form-control, textarea.form-control {
    font-size: 14px;
    padding: 8px;
  }

  /* Buttons */
  .styled-button {
    padding: 8px 15px;
    font-size: 14px;
  }

  /* Map container */
  .map-container {
    height: 250px;
  }

  /* Adjust margins for smaller screens */
  .mb-4, .mb-5 {
    margin-bottom: 1rem;
  }
}

/* 모달 스타일 */
.modal {
  display: block;
  position: fixed;
  z-index: 1000;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  overflow: auto;
  background-color: rgba(0, 0, 0, 0.4);
}

.modal-content {
  position: relative;
  background-color: white;
  margin: 15% auto;
  padding: 20px;
  border: 1px solid #888;
  width: 80%;
}

.close {
  position: absolute;
  top: 1px;
  right: 10px;
  color: #aaa;
  font-size: 28px;
  font-weight: bold;
}

.close:hover, .close:focus {
  color: black;
  text-decoration: none;
  cursor: pointer;
}

.searchButton{
    background-color: #04663f !important;
}
</style>