<template>
    <div id="app">
        <div class="app-main">
            <!-- 지도 영역 -->
            <div id="map-container">
                <div id="map"></div>
            </div>
                
            <div class="review-search" style="top: 30px;" @click="openSearchModal">
                <div class="search-container">
                <input type="text" placeholder="검색" />
                <button class="search-button">
                    <img src="/images/돋보기white.png" alt="Search" />
                </button>
                </div>
            </div>


      <button id="back-button" @click="goToListPage">
      <img src="/images/뒤로가기.png" alt="뒤로가기">
      </button>
    </div>

    <div class="toggle-switch-container2" style="z-index: 10;"> <!-- 기존 z-index를 낮춰 설정 -->
        <div class="toggle-switch2">
          <img src="/images/파손.png" alt="" style="width: 15px; height: auto; margin-right: 5px;"> 파손
        </div>
        <div class="toggle-switch2" >
          <img src="/images/낙석.png" alt="" style="width: 15px; height: auto; margin-right: 5px;"> 낙석
        </div>
        <div class="toggle-switch2" style=" font-size: 12px; width: 100px;">
          <img src="/images/야생동물.png" alt="" style="width: 15px; height: auto; margin-right: 5px;"> 야생동물
        </div>
      </div>
            
    
        <!-- 하단 푸터 -->
        <MobileFooterView4></MobileFooterView4>

        <!-- 장소 검색 모달 -->
        <div v-if="isModalOpen" class="modal">
            <div class="modal-content">
                <span class="close" @click="closeSearchModal">&times;</span>
                <input v-model="keywordInput" placeholder="장소 검색" class="mt-5" />
                <button class="searchButton" @click="searchPlaces">검색하기</button>

                <ul v-if="searchResults.length > 0">
                    <li v-for="(place, index) in searchResults" :key="index" @click="selectPlace(place)">
                        {{ place.place_name }} ({{ place.address_name }})
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>
    
    
<script setup>
import MobileFooterView4 from "@/components/MobileFooterView4.vue";
/* global kakao */
import { ref, onMounted } from "vue";
import { useComplaintListStore, useComplaintStore } from "@/stores/complaint"
import router from "@/router/index.js";
import { storeToRefs } from 'pinia';

// 스토어 가져와서 리스트 받아오기
const complaintListStore = useComplaintListStore();

// onMounted로 컴포넌트가 마운트된 후에 실행되도록 설정
onMounted(async () => {
    await complaintListStore.fetchComplaintList();
    // sessionStorage에서 새로고침 여부를 확인
    const hasRefreshed = sessionStorage.getItem('hasRefreshed');

    if (!hasRefreshed) {
    sessionStorage.setItem('hasRefreshed', 'true'); // 새로고침 플래그 설정
    window.location.reload(); // 페이지 새로고침
    } else {
    sessionStorage.removeItem('hasRefreshed'); // 플래그 초기화
    }
    initializeMap(); // Vue가 마운트될 때 지도를 초기화
});

const { complaintlist } = storeToRefs(complaintListStore);

function initializeMap() {
    // Kakao 지도 API 스크립트를 동적으로 로드
    const script = document.createElement('script');
    script.onload = () => {
        kakao.maps.load(() => {
            createMap();
            isKakaoMapLoaded.value = true; // Kakao Maps 로드 완료
        });
    };
    script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false&libraries=clusterer,services'; // autoload=false로 설정
    document.head.appendChild(script);
}

let map;

function createMap() {
    const mapContainer = document.getElementById('map'); // 지도를 표시할 div 선택
    const mapOption = {
        center: new kakao.maps.LatLng(37.660833, 126.993333), // 지도의 중심 좌표
        level: 9, // 지도 확대 레벨
    };

    // 지도 생성
    map = new kakao.maps.Map(mapContainer, mapOption);

    // 마커 클러스터러를 생성합니다 
    var clusterer = new kakao.maps.MarkerClusterer({
        map: map, // 마커들을 클러스터로 관리하고 표시할 지도 객체 
        averageCenter: true, // 클러스터에 포함된 마커들의 평균 위치를 클러스터 마커 위치로 설정 
        minLevel: 6 // 클러스터 할 최소 지도 레벨 
    });

    // 마커 이미지의 주소 (각 유형별 이미지)
    const markerImages = {
        낙석: "/images/낙석.png", // 낙석 마커 이미지
        야생동물: "/images/야생동물.png", // 화재 마커 이미지
        파손: "/images/파손.png", // 파손 마커 이미지
    };

    // 마커를 표시할 위치와 타이틀 정보
    const positions = complaintlist.value.map(complaint => ({
        title: complaint.mountainName,
        latlng: new kakao.maps.LatLng(complaint.latitude, complaint.longitude),
        type: complaint.complaintType,
        complaintNo: complaint.complaintNo
    }));

    //console.log("배열 : " + positions); // 위치 배열이 제대로 생성되었는지 확인

    // goToComplaintInfo 함수 정의
    function goToComplaintInfo(complaintNo) {
        sessionStorage.setItem("complaintNo", complaintNo);
        router.push({ path: "/complaintInfoMobile" });
    }

    // 각 위치에 마커 생성
    const markers = positions.map((position) => {
        const imageSrc = markerImages[position.type] || markerImages['낙석']; // 기본 이미지로 낙석을 사용

        const imageSize = new kakao.maps.Size(25, 30); // 마커 이미지 크기
        const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize); // 마커 이미지 생성

        // 마커 생성
        const marker = new kakao.maps.Marker({
            position: position.latlng,
            title: position.title,
            image: markerImage,
        });

        // 인포윈도우의 내용에 아이콘 버튼 추가
        const infowindowContent = document.createElement('div');
        infowindowContent.style.padding = '2px';
        infowindowContent.style.cursor = 'pointer';
        infowindowContent.style.width = '180px';
        infowindowContent.style.textAlign = 'center'; // 가운데 정렬
        infowindowContent.innerHTML = `
            ${position.type} - ${position.title}
            <i class="bi bi-caret-right-fill" style="font-size: 1em; color: black; cursor: pointer; margin-left: 1px;"></i>
        `;
    
        // 아이콘 클릭 시 goToComplaintInfo 실행
        infowindowContent.querySelector('.bi-caret-right-fill').addEventListener('click', () => {
            goToComplaintInfo(position.complaintNo);
        });


        const infowindow = new kakao.maps.InfoWindow({
            content: infowindowContent,
            zIndex: 10
        });


        // 인포윈도우 상태를 추적하는 변수
        let openInfowindow = null;

        // 마우스 클릭 이벤트 등록
        kakao.maps.event.addListener(marker, 'click', function() {
            if (openInfowindow !== infowindow) {
                // 현재 열려 있는 인포윈도우가 있다면 닫기
                if (openInfowindow) {
                    openInfowindow.close(); // 열려 있는 인포윈도우 닫기
                }
                // 클릭한 마커의 인포윈도우 열기
                infowindow.open(map, marker);
                openInfowindow = infowindow; // 현재 열린 인포윈도우를 업데이트
            } else {
                // 클릭한 인포윈도우가 이미 열려 있다면 닫기
                infowindow.close();
                openInfowindow = null; // 열린 인포윈도우를 초기화
            }
        });

        //console.log(position.title);
        // 인포윈도우를 마커에 즉시 표시
        //infowindow.open(map, marker);
        return marker;

    });

    // 클러스터러에 마커들을 추가합니다
    clusterer.addMarkers(markers);

    // 줌 레벨 변경 이벤트 리스너 추가
    kakao.maps.event.addListener(map, 'zoom_changed', function() {
        if (map.getLevel() <= 6) {
            clusterer.clear();
            markers.forEach(function(marker) {
                marker.setMap(map);
            });
        } else {
            markers.forEach(function(marker) {
                marker.setMap(null);
            });
            clusterer.addMarkers(markers);
        }
    });
}

// 번호에 따른 민원 글 데이터 불러오기
const complaintStore = useComplaintStore();
// eslint-disable-next-line no-unused-vars
const complaintone = storeToRefs(complaintStore);

// // 마커누르면 번호받아서 저장하고 세부페이지로 이동시킴
// async function getComplaintByNo(key) {
//   await complaintStore.fetchComplaintone(key);
//   sessionStorage.setItem("complaintNo", key);
//   router.replace({path: '/complaintInfoMobile'});
// }

// 모달 상태를 제어하는 변수
const isModalOpen = ref(false);
const keywordInput = ref("");
const searchResults = ref([]);

// Kakao Maps가 로드된 상태를 추적하는 변수
let isKakaoMapLoaded = ref(false);

// ==== 장소 검색 관련 함수 ==== //
function openSearchModal() {
    isModalOpen.value = true; 
    console.log("모달열림" + isModalOpen.value);
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

    // 기존 맵의 중심을 선택한 장소로 이동
    const centerPosition = new kakao.maps.LatLng(place.y, place.x);
    map.setCenter(centerPosition); // 맵의 중심을 선택한 장소로 이동
    map.setLevel(7);
    closeSearchModal(); // 모달을 닫습니다
}


// 리스트페이지로 이동하기
function goToListPage() {
    router.replace({ path: "/complaintListMobile" });
}
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
    position: relative;
    height: 100vh;
    width: 100vw;
    background-color: #f5f5f5;
    overflow: hidden;
}

/* 지도 컨테이너 */
#map-container {
    position: relative;
    width: 100%;
    height: 100%;
}

#map {
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0;
    left: 0;
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

.search-modal {
    position: absolute;
    top: 20px;
    left: 50%;
    transform: translateX(-50%);
    width: 300px;
    background-color: #fff;
    padding: 10px;
    border-radius: 5px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    z-index: 10;
    display: flex;
    align-items: center;
    margin-left: 25px;
}

.search-modal input {
    width: 100%;
    padding: 8px;
    border: 1px solid #ccc;
    border-radius: 5px;
    font-size: 14px;
}


.carousel-inner {
  display: flex;
  width: 300px;
  align-items: center;
  margin-top: 28em;
  overflow: hidden;
}

.carousel-image {
    display: block; 
  width: 100%;
  height: auto; 
  max-width: 200px; 
  object-fit: cover; 
  margin: 0 5px;
  border-radius: 8px; 
}

/* 범례 스타일 */
.map-legend {
    position: absolute;
    bottom: 20px;
    left: 20px;
    background-color: rgba(255, 255, 255, 0.8);
    padding: 10px;
    border-radius: 5px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    z-index: 10;
}

.legend-item {
    display: flex;
    align-items: center;
    margin-bottom: 5px;
}

.legend-icon {
    width: 20px;
    height: 20px;
    margin-right: 5px;
}


/* 뒤로가기 버튼 */
button.back-button {
    position: absolute;
    top: 20px;
    left: 20px;
    z-index: 10;
    background-color: white;
    border-radius: 5px;
    padding: 10px;
    border: none;
    cursor: pointer;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

button.back-button img {
    width: 20px;
    height: 20px;
}

/* 모달 스타일 */
.modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 20;
}

.modal-content {
    background-color: white;
    padding: 20px;
    border-radius: 10px;
    width: 80%;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.modal-content input {
    width: 100%;
    padding: 10px;
    margin-bottom: 10px;
    border: 1px solid #ccc;
    border-radius: 5px;
}

.searchButton {
    display: block;
    width: 100%;
    background-color: #04663f;
    color: white;
    padding: 10px;
    border: none;
    border-radius: 5px;
    font-size: 16px;
    cursor: pointer;
}

.searchButton:hover {
    background-color: #034f2c;
}

.close {
    position: absolute;
    top: 10px;
    right: 10px;
    font-size: 18px;
    cursor: pointer;
    color: #333;
}

/* 반응형 스타일 */
@media (max-width: 480px) {
    .search-modal {
        width: 80%;
    }

    .map-legend {
        bottom: 10px;
        left: 10px;
        font-size: 12px;
    }

    .legend-icon {
        width: 15px;
        height: 15px;
    }

    button.back-button {
        top: 30px;
        left: 10px;
    }
}
</style>  