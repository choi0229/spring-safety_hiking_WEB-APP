<template>
  <HeaderView style="z-index: 4;width: 1920px;"></HeaderView>

  <div class="dashboard">

    <aside class="sidebar">
      <div class="d-flex flex-column">
        <div>
          <!-- 헤더 섹션 -->
          <div class="header">
            <button @click="goBack" class="back-button"><img src="/images/moveback_icon.png" alt="뒤로가기 아이콘" width="15"
                height="15"></button>


          </div>

          <!-- 코스 이미지 및 기본 정보 -->
          <div class="course-info">
            <div class="image-container">
              <img v-if="courseData?.courseImage" :src="courseData.courseImage" alt="Mountain Image"
                class="course-image" />
              <div class="image-overlay">
                <p class="image-course-name">{{ courseData?.courseName }}코스</p>
                <p class="image-course-location">{{ courseData?.courseLocation }}<span id="courseTitle">{{
                  courseData?.mountainName || '코스 정보' }}</span></p>
                <p class="difficulty-text"> 난이도: {{ averageDifficultyText }} · <i
                    class="bi bi-star-fill image-rating-star"></i> {{ averageRating }}({{ reviewCount }})</p>
              </div>
            </div>
            <div class="course-details">
              <p>{{ courseData?.courseContent }}</p>
            </div>
          </div>

          <div class="button-group">
            <div class="btn custom-btn primary-btn" @click="showCoursePreviewModal">
              <img src="/images/미리보기.png">
            <p type="button" @click="showCoursePreviewModal">코스 미리보기</p>
            </div>

            <div class="btn custom-btn secondary-btn" @click="goToCourse3DPage()">
              <img src="/images/산3D.png">
              <p type="button" >3D 보기</p>
            </div>
            
            <div class="btn custom-btn tertiary-btn">
              <img src="/images/비교하기.png">
              <p type="button">코스 비교하기</p>
            </div>
            
          </div>

          <!-- 사고 요약 섹션 -->
          <div class="accident-summary">
            <div class="carousel-container">
              <div class="card-carousel" ref="accidentCarouselRef">
                <div class="card">
                  <div class="card-overlay">
                    <img src="/images/strumble.png" class="card-img-top"/>
                    <span class="accident-item-content1">실족 : {{ strumbleCount }}
                      <div class="image-grid">
                        <div v-for="(strumble, index) in strumbles" :key="strumble" class="image-item">
                          <img
                            :src="'/images/' + strumble"
                            class="clickable-image"
                            @click="openInfoWindowForImage(index, '실족')"
                          />
                        </div>
                      </div>
                    </span>
                  </div>
                </div>

                <div class="card">
                  <div class="card-overlay">
                    <img src="/images/distress.png" class="card-img-top" /> 
                    <span class="accident-item-content1">조난 : {{ distressCount }}
                      <div class="image-grid">
                        <div v-for="(distress, index) in distresses" :key="distress" class="image-item">
                          <img
                            :src="'/images/' + distress"
                            class="clickable-image"
                            @click="openInfoWindowForImage(index, '조난')"
                          />
                        </div>
                      </div>
                    </span>
                    <span class="accident-item-content2"></span>
                  </div>
                </div>

                <div class="card">
                  <div class="card-overlay">
                    <img src="/images/disease.png" class="card-img-top" />
                    <span class="accident-item-content1">질환 : {{ diseaseCount }}
                      <div class="image-grid">
                        <div v-for="(disease, index) in diseases" :key="disease" class="image-item">
                          <img
                            :src="'/images/' + disease"
                            class="clickable-image"
                            @click="openInfoWindowForImage(index, '질환')"
                          />
                        </div>
                      </div>
                    </span>
                    <span class="accident-item-content2"></span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 날씨 : 시작 -->
          <div class="weather-forecast" v-if="dailyWeather.length > 0">

            <div class="date-selector">

              <div v-for="(day, index) in dailyWeather.slice(0, 6)" :key="index" class="date-button-wrapper">

                <div class="day-label">{{ formatDayOfWeek(day.date) }}</div> <!-- 요일 표시 -->

                <button :class="['date-button', { active: selectedDateIndex === index }]" @click="selectDate(index)">
                  {{ formatDate(day.date) }}
                </button>

              </div>
            </div>

            <div class="forecast-details" v-if="selectedDay">
              <div class="d-flex">

                <div class="row" id="forecast1">

                  <div class="d-flex" style="margin-top: -1rem; margin-left: 0.3rem; width:200px; height:100px">
                    <p class="current-temperature">{{ Math.round(selectedDay.currentTemp - 273.15) }}°</p>
                    <p class="weather-icon" v-html="getWeatherEmoji(selectedDay.weather[0].main)"></p>
                  </div>


                  <div class="row">

                    <p class="description">{{ translateWeatherDescription(selectedDay.weather[0].id) }}</p>

                    <div class="temperature-range d-flex">
                      <p class="min-temp">최저: {{ Math.round(selectedDay.minTemp - 273.15) }}°</p>
                      <p class="max-temp">최고: {{ Math.round(selectedDay.maxTemp - 273.15) }}°</p>
                    </div>

                  </div>

                </div>




                <div class="rainSun-info">
                  <div class="rain-info d-flex">
                    <div class="rain-icon" :style="{ '--rain-level': selectedDay.pop * 100 + '%' }"></div>
                    <p class="rain-chance" style="margin-top: 1rem; margin-left: 1.5rem;">{{
                      Math.round(selectedDay.pop * 100) }}%</p>
                  </div>

                  <div class="sun-info">
                    <p id="sunrise"><img src="/images/sunrise.png" alt="sunrise" class="icon-small">일출: {{
                      sunriseTimes[selectedDateIndex] }}</p>
                    <p id="sunset"><img src="/images/sunset.png" alt="sunset" class="icon-small">일몰: {{
                      sunsetTimes[selectedDateIndex] }}</p>
                  </div>

                </div>

              </div>
            </div>
          </div>
        </div>

        <div class="summary-section">
          <div class="rating">
            <p class="average-rating-text">{{ averageRating }}</p>
            <div class="stars">
              <i v-for="star in fullStars" :class="'bi bi-star-fill'" :key="star"></i>
              <i v-if="halfStar" class="bi bi-star-half"></i>
              <i v-for="star in emptyStars" class="bi bi-star" :key="star"></i>
            </div>
            <p class="review-count-text">{{ reviewCount }}개의 리뷰</p>
          </div>

          <div class="rating-breakdown">
            <div v-for="(count, star) in ratingDistribution" :key="star" class="rating-bar">
              <span>{{ star }}점</span>
              <i class="bi bi-star-fill n-star"></i>
              <div class="progress-bar">
                <div class="progress" :style="{ width: (count / reviewCount) * 100 + '%' }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 리뷰 작성 버튼 -->


        <!-- 리뷰 작성 모달 -->
        <div v-if="showModal" class="overlay" @click.self="showModal = false">
          <div class="review-form">
            <button class="close-button" @click="showModal = false">X</button>
            <h2>리뷰 작성</h2>

            <!-- 코스 선택 -->
            <label for="course" class="modal-label">코스 선택</label>
            <select v-model="selectedCourse" id="course" class="input-field">
              <option value="" disabled>코스를 선택하세요</option>
              <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
                {{ course.courseName }}
              </option>
            </select>

            <!-- 별점 -->
            <div class="rating">
              <label class="modal-label">별점</label>
              <div class="stars">
                <i v-for="star in 5" :key="star"
                  :class="star <= rating ? 'bi bi-star-fill n-star' : 'bi bi-star n-star'" @click="setRating(star)">
                </i>
              </div>
            </div>

            <!-- 난이도 선택 -->
            <label for="difficulty" class="modal-label">난이도</label>
            <select v-model="difficulty" id="difficulty" class="input-field">
              <option value="" disabled>난이도를 선택하세요</option>
              <option value="쉬움">쉬움</option>
              <option value="보통">보통</option>
              <option value="어려움">어려움</option>
            </select>

            <!-- 리뷰 내용 -->
            <label class="modal-label">리뷰 내용</label>
            <textarea v-model="reviewContent" placeholder="후기 내용을 입력하세요" class="input-field textarea"></textarea>

            <!-- 사진 업로드 -->
            <label for="photos" class="modal-label">사진 업로드</label>
            <input type="file" multiple @change="onFileChange" class="file-input" />

            <!-- 제출 버튼 -->
            <button @click="submitReview" class="submit-button">리뷰 작성</button>
          </div>
        </div>

        <!-- 리뷰 페이지 -->
        <div class="review-page">
          <div class="content-wrapper">
            <div class="review-search">
          
            </div>
            <div>
              <button class="review-button" @click="showModal = true"><img src="/images/write.png"
                  style="width: 32px; height:32px; justify-content: center; margin-bottom: 0.1rem;"></button>
            </div>

            <div class="reviews-list">
              <div v-for="review in visibleReviews" :key="review.reviewId" class="review-item">
                <div class="review-header">
                  <img class="profile-image" :src="review.profileImage || '/images/main1.jpg'" alt="Profile" />
                  <div class="user-info">
                    <h3>{{ review?.userId || 'Unknown User' }}</h3>
                    <div class="stars-inline" v-if="review?.rating !== undefined">
                      <i v-for="star in 5" :class="{
                        'bi bi-star-fill': review?.rating && star <= Math.floor(review.rating),
                        'bi bi-star-half': review?.rating && star === Math.ceil(review.rating) && review.rating % 1 >= 0.5,
                        'bi bi-star': !review?.rating || star > Math.ceil(review.rating)
                      }" :key="star"></i>
                    </div>

                    <span class="review-date">{{ formatDate(review?.date) }}</span>
                  </div>
                  <!-- 로그인한 사용자와 리뷰 작성자가 동일할 때만 수정 및 삭제 버튼 표시 -->
                  <template v-if="review.userId === loggedInUserId">
                    <button @click="editReview(review)" class="edit-button">수정</button>
                    <button @click="confirmDelete(review.reviewId)" class="delete-button">X</button>
                  </template>
                </div>
                <p class="review-content">{{ review?.reviewContent || '내용 없음' }}</p>
                <div class="review-photos">
                  <img v-for="photo in review.photos" :src="photo" :key="photo" alt="리뷰 사진" class="review-photo" />
                </div>
                <span class="difficulty">난이도: {{ review?.difficulty || '정보 없음' }}</span>
              </div>
            </div>
            <button v-if="canShowMore" @click="loadMoreReviews" class="load-more-button">더 보기</button>
          </div>
        </div>

        <!-- 리뷰 수정 모달 -->
        <div v-if="editModal && currentReview" class="overlay" @click.self="editModal = false">
          <div class="review-form">
            <button class="close-button" @click="editModal = false">X</button>
            <h2>리뷰 수정</h2>

            <!-- 코스 선택 (수정 모달에서는 비활성화) -->
            <label class="modal-label">코스 선택</label>
            <select v-model="currentReview.courseId" class="input-field" disabled>
              <option :value="currentReview.courseId">{{ courseData2.courseName }} 코스</option>
              <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
                {{ course.courseName }}
              </option>
            </select>

            <!-- 별점 -->
            <label class="modal-label">별점</label>
            <div class="stars">
              <i v-for="star in 5" :key="star"
                :class="star <= currentReview.rating ? 'bi bi-star-fill n-star' : 'bi bi-star n-star'"
                @click="currentReview.rating = star"></i>
            </div>

            <!-- 난이도 선택 -->
            <label class="modal-label">난이도</label>
            <select v-model="currentReview.difficulty" class="input-field">
              <option value="쉬움">쉬움</option>
              <option value="보통">보통</option>
              <option value="어려움">어려움</option>
            </select>

            <!-- 리뷰 내용 -->
            <label class="modal-label">리뷰 내용</label>
            <textarea v-model="currentReview.reviewContent" placeholder="후기 내용을 입력하세요"
              class="input-field textarea"></textarea>

            <!-- 저장 버튼 -->
            <button @click="saveReview" class="submit-button">저장</button>
          </div>
        </div>



        <!-- 삭제 확인 모달 -->
        <div v-if="deleteModal" class="overlay" @click.self="deleteModal = false">
          <div class="review-form">
            <button class="close-button" @click="deleteModal = false">X</button>
            <h2>리뷰 삭제</h2>
            <p>정말 삭제하시겠습니까?</p>
            <div class="modal-actions">
              <button @click="deleteReview" class="submit-button confirm-button">예</button>
              <button @click="deleteModal = false" class="submit-button cancel-button">아니오</button>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <main class="main">
      <!-- 세로 버튼 그룹 : 시작 -->
      <!-- 실족, 조난, 질환 버튼 -->
      <div class="toggle-switch-container2" style="z-index: 10;"> <!-- 기존 z-index를 낮춰 설정 -->
        <div class="toggle-switch2" @click="toggleMap('default')" :class="{ active: showMap === 'default' }">
          <img src="/images/기본.png" alt="" style="width: 20px; height: auto; margin-right: 5px;"> 기본
        </div>
        <div class="toggle-switch2" @click="toggleMap('photo')" :class="{ active: showMap === 'photo' }">
          <img src="/images/사진.png" alt="" style="width: 20px; height: auto; margin-right: 5px;"> 사진
        </div>
        <div class="toggle-switch2" @click="applyFilter('실족')" :class="{ active: selectedFilter === '실족' }">
          <img src="/images/strumble.png" alt="" style="width: 20px; height: auto; margin-right: 5px;"> 실족
        </div>
        <div class="toggle-switch2" @click="applyFilter('조난')" :class="{ active: selectedFilter === '조난' }">
          <img src="/images/distress.png" alt="" style="width: 20px; height: auto; margin-right: 5px;"> 조난
        </div>
        <div class="toggle-switch2" @click="applyFilter('질환')" :class="{ active: selectedFilter === '질환' }">
          <img src="/images/disease.png" alt="" style="width: 20px; height: auto; margin-right: 5px;"> 질환
        </div>
      </div>

  <div v-if="showMap === 'default'" id="map" ref="map-container" class="map-container">
        
        <!-- 메인 페이지 내용 -->
      <button id="showHelpButton" @click="showHelp = true">?</button>
        <div class="map-legend ms-4">
          
          <div class="legend-item">
            <div class="legend-icon" style="background-color:#FF4500;"></div>
            <span>오르막</span>
          </div>
          
          <div class="legend-item">
            <div class="legend-icon" style="background-color:#32CD32;"></div>
            <span>평지</span>
          </div>
          
          <div class="legend-item">
            <div class="legend-icon" style="background-color:#1E90FF;"></div>
            <span>내리막</span>
          </div>
          
        </div>
      </div>
      <div v-else-if="showMap === 'photo'" id="photoMap" class="map-container"></div>

      <div id="grapth">
        <div class="chart1-section">
          <canvas id="elevationChart" style="height: 180px; width: 1000px;"></canvas>
        </div>
      </div>
    </main>
  </div>

 <!-- 코스 미리보기 모달 -->
 <div class="modal fade" id="kt_modal_course_preview" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered mw-1200px">
    <div class="modal-content rounded">

      <div class="modal-header compact-header">
        <h1 class="modal-title">코스 미리보기</h1>
        <button class="btn btn-sm btn-icon btn-active-color-primary" data-bs-dismiss="modal">
          <i class="bi bi-x-circle"></i>
        </button>
      </div>

      <div class="modal-body scroll-y px-10 px-lg-15 pt-0 pb-15">
        <div class="mb-13 text-center">
          <div class="text-muted fw-semibold fs-5">동영상과 경로를 동시에 미리 확인 할 수 있습니다.</div>
        </div>

        <!-- 지도와 비디오 미리보기 영역 -->
        <div class="video-container">
          <!-- 지도 -->
          <div id="modalMap"></div>

          <!-- 비디오 -->
          <video
            ref="videoRef"
            controls
            autoplay
            muted
            @timeupdate="syncCourseWithVideo"
          >
            <source src="/videos/HJcourse.mp4" type="video/mp4" />
            브라우저가 비디오 태그를 지원하지 않습니다.
          </video>
        </div>
      </div>
    </div>
  </div>
</div>

</template>

<script setup>
import HeaderView from '@/components/HeaderView.vue';
import { onMounted, ref, computed } from 'vue';
import Chart from 'chart.js/auto';
import axios from "axios";
import { defineProps } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { nextTick } from 'vue';
import { Modal } from 'bootstrap';

const props = defineProps({
  course: {
    type: String,
    required: true
  }

});

// props로 전달된 mountain 데이터를 JSON으로 파싱
const courseData = ref(JSON.parse(props.course));

// mountainData가 제대로 파싱되었는지 확인
console.log("Mountain Data:", courseData.value);


/* global kakao */
const map = ref(null);
const routeCoordinates = ref([]);
let chartInstance = null;
const currentMarker = ref(null);
const modalPolyline = ref(null);  // 모달 지도 경로
const showMap = ref('default');  // 기본 지도를 기본값으로 설정
  let photoMap = ref(null);
const distressCount = ref(0);
const strumbleCount = ref(0);
const diseaseCount = ref(0);

const videoRef = ref(null);
const modalMap = ref(null);

onMounted(() => {
  const queryMapType = route.query.mapType || 'default'; // 기본값은 'default'
    showMap.value = queryMapType

  initializeMap();
  // mountainData로부터 lat, lon을 사용해 날씨 정보를 가져옴
  
  fetchReviewsAndRatings();
  fetchSummary();

  fetchWeatherData(37.590870, 126.958077);
  requestCourse()

});

function showCoursePreviewModal() {
  console.log('showCoursePreviewModal 호출됨');
  const elem = document.querySelector('#kt_modal_course_preview');
  const coursePreviewModal = new Modal(elem); // Bootstrap Modal 객체 생성
  coursePreviewModal.show(); // 모달 표시

  coursePreviewModal._element.addEventListener('shown.bs.modal', () => {
    initializeModalMap(); // 모달 지도 초기화
    kakao.maps.event.trigger(modalMap.value, 'resize'); // 지도 크기 재조정
    modalMap.value.setCenter(new kakao.maps.LatLng(37.66433293993584, 127.01160029114365));
    console.log('모달 지도 리사이즈 완료');
  });
}

// 모달 지도 초기화
function initializeModalMap() {
  const modalMapElement = document.getElementById('modalMap');
  if (!modalMapElement) {
    console.error('modalMap 요소를 찾을 수 없습니다.');
    return;
  }

  modalMap.value = new kakao.maps.Map(modalMapElement, {
    center: new kakao.maps.LatLng(37.66433293993584, 127.01160029114365),
    level: 5,
  });

  loadGeoJSONFromServer('/data/인왕산ele copy.geojson', modalMap.value);

  if (routeCoordinates.value.length > 0) {
    const groupedCoordinates = groupCoordinates(routeCoordinates.value, 5);
    addRouteLayer(groupedCoordinates, modalMap.value); // 모달 지도에 경로 추가
  }
}

const selectedFilter = ref('전체');

let mapMarkers = []

 // 버튼 클릭
 function applyFilter(filter) {
    selectedFilter.value = filter;
    
    // 기존 마커 지우기
    if (mapMarkers.length) {
      mapMarkers.forEach(marker => marker.setMap(null));
      mapMarkers = [];
    }
    
    // 전체버튼 클릭시 마커 띄우기
    if (selectedFilter.value === '전체') {
      loadMarkers("/data/헬기장spot.geojson", '/images/helipad.png');
      loadMarkers("/data/화장실.geojson", '/images/toilets.png');
      loadDangerMarkers("/data/2023산악사고_인왕산2.geojson", '/images/danger.png');
    } else {
      // Otherwise, load only markers for the selected filter
      loadDangerMarkers("/data/2023산악사고_인왕산2.geojson", '/images/danger.png');
    }
  }

  // JSON 데이터를 사용하여 마커 추가
  async function loadMarkers(url, imageSrc) {
  try {
    // GeoJSON 데이터 로드
    const response = await fetch(url);
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const markerData = await response.json();

    // 필터링된 마커 데이터 가져오기
    const filteredMarkers = markerData.features.filter((feature) => {
      const courseName = feature.properties.MNTN_NM2;
      return (
        courseName === courseData.value.courseName);
    });

    // 필터링된 마커를 지도에 추가
    filteredMarkers.forEach((spot) => {
      const lat = spot.geometry.coordinates[1];
      const lng = spot.geometry.coordinates[0];
      const markerPosition = new kakao.maps.LatLng(lat, lng);

      // 마커 생성
      const imageSize = new kakao.maps.Size(35, 45);
      const imageOption = { offset: new kakao.maps.Point(12, 35) };
      const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imageOption);

      const marker = new kakao.maps.Marker({
        position: markerPosition,
        map: map.value,
        title: spot.properties.MNTN_NM,
        image: markerImage,
      });

      mapMarkers.push(marker); // 새 마커 배열에 추가

      // 인포윈도우 설정
      const infowindow = new kakao.maps.InfoWindow({
        content: `
          <div style="display: flex; flex-direction: column; align-items: center; width: 150px; height: auto; 
          box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); font-family: Arial, sans-serif; overflow: hidden; 
          border: 1px solid #ddd; border-radius: 12px; z-index: 500;">
            <!-- 사진 영역 -->
            <div style="display: flex; justify-content: center; align-items: center; width: 100%; height: 150px; 
            overflow: hidden; border-bottom: 2px solid #ced4da;">
              <img src="/images/${spot.properties.image}" alt="사진" 
              style="width: 130px; height: 120px; object-fit: cover; border: 1px solid black; border-radius: 10px;" />
            </div>
            <!-- 텍스트 영역 -->
            <div style="text-align: center; padding: 10px;">
              <p style="font-size: 14px; font-weight: bold; margin: 0; color: #333;">
                <img src="/images/window산.png" style="width: 16px; height: 16px;" /> ${spot.properties.MNTN_NM}
              </p>
              <p style="font-size: 12px; color: #666; margin: 5px 0;">- ${spot.properties.SAFE_SPOT2} -</p>
            </div>
          </div>
        `,
        disableAutoPan: true, // 인포윈도우 표시로 인해 지도가 이동하지 않도록 설정
      });

      // 마커 클릭 이벤트
      kakao.maps.event.addListener(marker, 'click', () => {
        if (infowindow.getMap()) {
          infowindow.close();
        } else {
          infowindow.open(map.value, marker);
        }
      });
    });
  } catch (error) {
    console.error(`${url} 파일 로드 중 에러 발생:`, error);
  }
}

const distresses = ref([]);
const strumbles = ref([]);
const diseases = ref([]);


function openInfoWindowForImage(index, type = '실족') {
  // 이미지와 타입에 해당하는 마커 찾기
  const target = markers.value.find(
    (item) => item.type === type && item.image === (type === '실족' ? strumbles.value[index] :
                                                   type === '조난' ? distresses.value[index] :
                                                   diseases.value[index])
  );

  if (target) {
    // 이전에 열린 인포윈도우가 있다면 닫기
    if (currentMarker.value && currentMarker.value.infowindow) {
      currentMarker.value.infowindow.close();
    }

    // 해당 마커의 인포윈도우 열기
    target.infowindow.open(map.value, target.marker);

    // 현재 열려 있는 마커 저장
    currentMarker.value = target;
  } else {
    console.error('해당 이미지를 찾을 수 없습니다.');
  }
}


const markers = ref([]); // 모든 마커와 관련 데이터를 저장

async function loadDangerMarkers(url, imageSrc) {
  try {
    if(selectedFilter.value == '전체'){
      strumbles.value = [];
      distresses.value = [];
      diseases.value = [];
      strumbleCount.value = 0;
      distressCount.value = 0;
      diseaseCount.value = 0;
    } else if(selectedFilter.value == '실족'){
      strumbles.value = [];
      strumbleCount.value = 0;
    } else if(selectedFilter.value == '조난'){
      distresses.value = [];
      distressCount.value = 0;
    } else if(selectedFilter.value == '질환'){
      diseases.value = [];
      diseaseCount.value = 0;
    }    


    const response = await fetch(url);
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const markerData = await response.json();

    const filteredMarkers = markerData.features.filter((feature) => {
      const markerType = feature.properties.type;
      const courseName = feature.properties.MNTN_NM2;
      return courseName === courseData.value.courseName &&
             (selectedFilter.value === '전체' || markerType === selectedFilter.value || !markerType);
    });

    filteredMarkers.forEach((spot) => {
      const lat = spot.geometry.coordinates[1];
      const lng = spot.geometry.coordinates[0];
      const markerPosition = new kakao.maps.LatLng(lat, lng);

      if (spot.properties.type === '실족') {
        strumbleCount.value += 1;
        strumbles.value.push(spot.properties.image);
      } else if (spot.properties.type === '조난') {
        distressCount.value += 1;
        distresses.value.push(spot.properties.image);
      } else if (spot.properties.type === '질환') {
        diseaseCount.value += 1;
        diseases.value.push(spot.properties.image);
      }

      // 마커 이미지 설정
      switch (spot.properties.type) {
          case '실족':
            imageSrc = '/images/strumble.png';
            break;
          case '조난':
            imageSrc = '/images/distress.png';
            break;
          case '질환':
            imageSrc = '/images/disease.png';
            break;
        }

      const imageSize = new kakao.maps.Size(35, 45);
      const imageOption = { offset: new kakao.maps.Point(12, 35) };
      const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imageOption);

      const marker = new kakao.maps.Marker({
        position: markerPosition,
        map: map.value,
        title: spot.properties.MNTN_NM,
        image: markerImage,
      });

      // 인포윈도우 설정
      const infowindow = new kakao.maps.InfoWindow({
          content: `
    <div style="display: flex; flex-direction: column; align-items: center; width: 150px; height: auto; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); font-family: Arial, sans-serif; overflow: hidden; border: 1px solid #ddd; border-radius: 12px; z-index: 500;">
      <!-- 사진 영역 -->
      <div style="display: flex; justify-content: center; align-items: center; width: 100%; height: 150px; overflow: hidden; border-bottom: 2px solid #ced4da;">
        <img src="/images/${spot.properties.image}" alt="사진" 
          style="
            width: 130px; 
            height: 120px; 
            object-fit: cover; 
            border: 1px solid black; 
            border-radius: 10px;
            box-sizing: border-box; /* 여백과 크기 조정 방지 */
            position: relative;
          ">
      </div>
      <!-- 텍스트 영역 -->
      <div style="text-align: center; padding: 10px; margin-top: -10px;">
        <p style="font-size: 14px; font-weight: bold; margin: 0; color: #333;">
          <img src="/images/window산.png" style="width: 16px; height: 16px;"/> ${spot.properties.MNTN_NM}
        </p>
        <p style="font-size: 12px; color: #666; margin: 5px 0 0;">- ${spot.properties.SAFE_SPOT2} -</p>
      </div>
    </div>
  `,
          disableAutoPan: true, // 인포윈도우 표시로 인해 지도가 이동하지 않도록 설정
});

      // 마커와 인포윈도우, 추가 데이터를 markers 배열에 저장
      markers.value.push({
        type: spot.properties.type,
        image: spot.properties.image,
        marker,
        infowindow,
      });

      // 마커 클릭 시 인포윈도우 열기
      kakao.maps.event.addListener(marker, 'click', () => {
        infowindow.open(map.value, marker);
      });
      mapMarkers.push(marker); // 마커를 배열에 저장
    });
  } catch (error) {
    console.error(`${url} 파일 로드 중 에러 발생:`, error);
  }
}


// 비디오 재생 시간에 따른 경로 동기화
function syncCourseWithVideo(event) {
  if (!modalMap.value || !routeCoordinates.value.length) return;

  const progress = event.target.currentTime / event.target.duration;
  const index = Math.floor(progress * routeCoordinates.value.length);

  if (index >= 0 && index < routeCoordinates.value.length) {
    const point = routeCoordinates.value[index];

    if (currentMarker.value) currentMarker.value.setMap(null);

    currentMarker.value = new kakao.maps.Marker({
      position: new kakao.maps.LatLng(point.lat, point.lng),
      map: modalMap.value,
    });

    modalMap.value.panTo(new kakao.maps.LatLng(point.lat, point.lng));
  }
}

// 지도 초기화
function initializeMap() {
    if (showMap.value === 'default') {
        const script = document.createElement('script');
        script.onload = () => kakao.maps.load(() => {
          map.value = new kakao.maps.Map(document.getElementById('map'), {
            center: new kakao.maps.LatLng(37.66433293993584, 127.01160029114365),
            level: 5,
          });
          if (map.value) {
            loadGeoJSONFromServer('/data/인왕산ele copy.geojson');
            loadMarkers("/data/헬기장spot.geojson", '/images/helipad.png');
            loadMarkers("/data/화장실.geojson", '/images/toilets.png');
            loadDangerMarkers("/data/2023산악사고_인왕산2.geojson", '/images/danger.png');
          }
        });
        script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false';
        document.head.appendChild(script);
    } else if (showMap.value === 'photo') { // 여기에
        const script = document.createElement('script');
        script.onload = () => kakao.maps.load(() => {
          photoMap.value = new kakao.maps.Map(document.getElementById('photoMap'), {
          center: new kakao.maps.LatLng(37.66433293993584, 127.01160029114365),
          level: 5,
          });
          if (photoMap.value) {
            loadGeoJSONFromServer('/data/인왕산ele copy.geojson', photoMap.value);
            loadMarkersToPhotoMap();
          }
        });
        script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false';
        document.head.appendChild(script);
      }
  }

   // 지도 토글 함수
   const toggleMap = (type) => {
    showMap.value = type;
    initializeMap();
  };

// GeoJSON 데이터 로드 함수
async function loadGeoJSONFromServer(url, targetMap = map.value) {
    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      
      const geojsonData = await response.json();
      console.log('GeoJSON data loaded:', geojsonData);  // GeoJSON 데이터를 콘솔에 출력해 확인
      processGeoJSON(geojsonData, targetMap);  // GeoJSON 데이터를 처리하여 경로를 그리는 함수 호출
    } catch (error) {
      console.error('GeoJSON 파일 로드 중 에러 발생:', error);
    }
  }

  // 커뮤니티 코스 불러오기
  let comCourse = ref([]);

const requestCourse = async () => {
  try{
    console.log(courseData.value.courseName)
    const response = await axios.post('/api/comCourse',{
      courseName: courseData.value.courseName
    });
    comCourse.value = response.data;
    console.log(comCourse.value)
  }catch(error){
    console.log('에러발생! '+error)
  }
}


  // 마커를 photoMap에 추가하는 함수
  function loadMarkersToPhotoMap() {
    if (!photoMap.value) {
      console.error("photoMap이 초기화되지 않았습니다.");
      return;
    }
  
    // comCourse 데이터에서 좌표 정보를 가져와 마커를 생성
    comCourse.value.forEach(course => {
      const lat = course.latitude;
      const lng = course.longitude;
  
      // 마커의 위치 설정
      const markerPosition = new kakao.maps.LatLng(lat, lng);
  
      const imageSrc = '/images/danger-icon.png';
      const imageSize = new kakao.maps.Size(35, 45);
      const imageOption = { offset: new kakao.maps.Point(12, 35) };
      const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imageOption);

      const marker = new kakao.maps.Marker({
        position: markerPosition,
        map: photoMap.value,
        title:  course.courseName, // 마커에 코스 이름 표시
        image: markerImage,
      });
  
      const infowindowPhotoMap = new kakao.maps.InfoWindow({
          content: `
    <div style="display: flex; flex-direction: column; align-items: center; width: 150px; height: auto; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); font-family: Arial, sans-serif; overflow: hidden; border: 1px solid #ddd; border-radius: 12px; z-index: 500;">
      <!-- 사진 영역 -->
      <div style="display: flex; justify-content: center; align-items: center; width: 100%; height: 150px; overflow: hidden; border-bottom: 2px solid #ced4da;">
        <img src="${course.communityUrl}" alt="사진" 
          style="
            width: 130px; 
            height: 120px; 
            object-fit: cover; 
            border: 1px solid black; 
            border-radius: 10px;
            box-sizing: border-box; /* 여백과 크기 조정 방지 */
            position: relative;
          ">
      </div>
      <!-- 텍스트 영역 -->
      <div style="text-align: center; padding: 10px; margin-top: -10px;">
        <p style="font-size: 14px; font-weight: bold; margin: 0; color: #333;">
          <img src="/images/window산.png" style="width: 16px; height: 16px;"/> ${course.communityTitle}
        </p>
        <p style="font-size: 12px; color: #666; margin: 5px 0 0;">- ${course.courseName} -</p>
      </div>
    </div>
  `,
          disableAutoPan: true, // 인포윈도우 표시로 인해 지도가 이동하지 않도록 설정
        });
  
      // 마커 클릭으로 infowindow 토글 방식
      kakao.maps.event.addListener(marker, 'click', () => {
      if (infowindowPhotoMap.getMap()) {
        // infowindow가 현재 열려 있는 경우 닫기
        infowindowPhotoMap.close();
      } else {
        // infowindow가 닫혀 있는 경우 열기
        infowindowPhotoMap.open(photoMap.value, marker);
      }
    });
    });
  }



  function processGeoJSON(geojsonData, targetMap) {
    let allCoordinates = [];
  
    geojsonData.features.forEach((feature) => {
      if (feature.properties.PMNTN_NM && feature.properties.PMNTN_NM.includes(courseData.value.courseName)) {
        let coordinates = [];
  
        // MultiLineString을 처리하기 위해 중첩 배열을 펼침
        if (feature.geometry.type === 'MultiLineString') {
          feature.geometry.coordinates.forEach(line => {
            coordinates = coordinates.concat(line.map((coord) => ({
              lng: coord[0],
              lat: coord[1],
              elevation: feature.properties.DN || 0
            })));
          });
        } else if (feature.geometry.type === 'LineString') {
          coordinates = feature.geometry.coordinates.map((coord) => ({
            lng: coord[0],
            lat: coord[1],
            elevation: feature.properties.DN || 0
          }));
        }
  
        routeCoordinates.value = allCoordinates;  // 경로 좌표를 할당
        allCoordinates = allCoordinates.concat(coordinates);
      }
    });
  
    if (allCoordinates.length > 0) {
      const groupedCoordinates = groupCoordinates(allCoordinates, 5); // 5개씩 그룹화
      addRouteLayer(groupedCoordinates, targetMap);
      drawElevationChart(allCoordinates);
    } else {
      console.log('유효한 구간 데이터가 없습니다.');
    }
  }
  
  // 좌표 그룹화 함수
  function groupCoordinates(coordinates, groupSize) {
    const groups = [];
    for (let i = 0; i < coordinates.length; i += groupSize) {
      groups.push(coordinates.slice(i, i + groupSize));
    }
    return groups;
  }

// 경로 레이어 추가 함수
function addRouteLayer(groupedCoordinates, targetMap) {
    if (!targetMap) {
      console.error("targetMap이 초기화되지 않았습니다.");
      return;
    }
  
    // 중첩 배열을 평평하게 만든 flatCoordinates 생성
    const flatCoordinates = groupedCoordinates.flat();
  
    // 지도에 그릴 경로 생성
    const linePath = flatCoordinates.map((coord) => new kakao.maps.LatLng(coord.lat, coord.lng));
  
    // map.value에 경로를 그리는 경우
    if (targetMap === map.value) {
      drawBaseRoute(flatCoordinates); // 기본 경로 그리기
  
      groupedCoordinates.forEach((group) => {
        if (group.length > 1) {
          const startPoint = group[0];
          const endPoint = group[group.length - 1];
          const slope = calculateSlope(startPoint, endPoint);
          const color = getColorBySlope(slope);
  
          // 각 그룹에 대한 경로 생성
          const groupLinePath = group.map(coord => new kakao.maps.LatLng(coord.lat, coord.lng));
  
          const polyline = new kakao.maps.Polyline({
            path: groupLinePath,
            strokeWeight: 5,
            strokeColor: color,
            strokeOpacity: 0.8,
            strokeStyle: 'solid'
          });
  
          polyline.setMap(map.value); // 경로를 map에 그리기
        }
      });
  
    // modalMap.value에 경로를 그리는 경우
    } else if (targetMap === photoMap.value) {
      console.log('groupedCoordinates:', groupedCoordinates);
  
      modalPolyline.value = new kakao.maps.Polyline({
        path: linePath, // 평평하게 만든 경로 사용
        strokeWeight: 5,
        strokeColor: '#00FF00',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
      });
  
      modalPolyline.value.setMap(photoMap.value); // 모달 지도에 경로 설정
    } else if (targetMap === modalMap.value) {
      console.log('groupedCoordinates:', groupedCoordinates);
  
      modalPolyline.value = new kakao.maps.Polyline({
        path: linePath, // 평평하게 만든 경로 사용
        strokeWeight: 5,
        strokeColor: '#00FF00',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
      });
  
      modalPolyline.value.setMap(modalMap.value); // 모달 지도에 경로 설정
    }
  
    // 지도 경계를 경로에 맞게 설정
    const bounds = new kakao.maps.LatLngBounds();
    flatCoordinates.forEach(coord => bounds.extend(new kakao.maps.LatLng(coord.lat, coord.lng)));
    targetMap.setBounds(bounds); // 타겟 맵에 맞게 경계 조정
  }

  // 새로운 함수: 기본 녹색 경로를 그리는 함수
  function drawBaseRoute(coordinates) {
    const linePath = coordinates.map(coord => new kakao.maps.LatLng(coord.lat, coord.lng));
  
    const basePolyline = new kakao.maps.Polyline({
      path: linePath,
      strokeWeight: 5,
      strokeColor: '#32CD32', // 기본 경로 색상 (녹색)
      strokeOpacity: 1, // 기본 경로는 약간 투명하게 설정
      strokeStyle: 'solid'
    });
  
    basePolyline.setMap(map.value);
  }

function calculateDistance(index, coord, coordinates) {
  if (index === 0) return 0; // 첫 번째 좌표의 거리는 0

  const prevCoord = coordinates[index - 1];

  const R = 6371; // 지구 반지름 (km)
  const dLat = deg2rad(coord[1] - prevCoord[1]);
  const dLng = deg2rad(coord[0] - prevCoord[0]);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(deg2rad(prevCoord[1])) * Math.cos(deg2rad(coord[1])) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // 거리 반환
}

function deg2rad(deg) {
  return deg * (Math.PI / 180);
}

function calculateCumulativeDistances(coordinates) {
    let cumulativeDistances = [];
    let totalDistance = 0;
  
    for (let i = 0; i < coordinates.length; i++) {
      if (i > 0) {
        totalDistance += calculateDistance(i, coordinates[i], coordinates);
      }
      cumulativeDistances.push(totalDistance);
    }
  
    return cumulativeDistances;
  }

  // 경사도에 따른 색상 결정 함수
  function getColorBySlope(slope) {
    if (slope > 30) return '#FF4500'; // 급경사 (빨강)
    if (slope < -15) return '#1E90FF'; // 급한 내리막 (파랑)
    return '#32CD32'; // 평지에 가까움 (초록)
  }

  function calculateHaversineDistance(coord1, coord2) {
    const R = 6371e3; // 지구 반지름 (미터 단위)
    const lat1 = deg2rad(coord1.lat);
    const lat2 = deg2rad(coord2.lat);
    const deltaLat = deg2rad(coord2.lat - coord1.lat);
    const deltaLon = deg2rad(coord2.lng - coord1.lng);
  
    const a =
      Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
      Math.cos(lat1) * Math.cos(lat2) *
      Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
  
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  
    // 두 지점 간의 거리 반환 (미터 단위)
    return R * c;
  }

  function calculateSlope(start, end) {
    // 하버사인 공식을 사용하여 두 지점 간의 수평 거리 (지구 곡률 반영)
    const horizontalDistance = calculateHaversineDistance(
      { lat: start.lat, lng: start.lng },
      { lat: end.lat, lng: end.lng }
    );
  
    // 고도 차이 계산
    const elevationChange = end.elevation - start.elevation;
  
    // 피타고라스 정리를 사용해 두 지점 간의 대각선 거리 계산
    const diagonalDistance = Math.sqrt(
      Math.pow(horizontalDistance, 2) + Math.pow(elevationChange, 2)
    );
  
    // 수평 거리가 너무 짧으면 경사도 계산을 무시하고 기본 경사도 0으로 설정
    if (horizontalDistance < 1) { // 예시: 수평 거리가 10m 미만일 경우
      return 0; // 경사도 0으로 간주
    }
  
    // 경사도 = 고도 차이 / 대각선 거리 * 100
    return (elevationChange / diagonalDistance) * 100;
  }

function drawElevationChart(data) {
    const ctx = document.getElementById('elevationChart').getContext('2d');
  
    // 누적 거리 계산 (기존의 총 거리를 계산)
    const distances = calculateCumulativeDistances(data.map(point => [point.lng, point.lat]));
    const totalDistance = distances[distances.length - 1]; // 총 거리
    const scaleFactor = 645 / totalDistance; // 645m에 맞추기 위한 스케일링 비율
  
    // 각 누적 거리를 스케일링
    const scaledDistances = distances.map(distance => distance * scaleFactor);
    const elevations = data.map(point => point.elevation); // y축: 고도
  
  // 배경 이미지를 넣기 위한 커스텀 플러그인 정의
  const backgroundImagePlugin = {
      id: 'backgroundImagePlugin',
      beforeDraw: (chart) => {
        if (chart.config.options.backgroundImage) {
          const ctx = chart.ctx;
          const chartArea = chart.chartArea;
          const backgroundImage = new Image();
          backgroundImage.src = chart.config.options.backgroundImage;
  
          // 이미지가 로드된 후 캔버스에 그리기
          backgroundImage.onload = function () {
            ctx.save();
            ctx.globalAlpha = 0.4; // 불투명도 설정 (0.0: 완전히 투명, 1.0: 완전히 불투명)
            ctx.drawImage(backgroundImage, chartArea.left, chartArea.top, chartArea.right - chartArea.left, chartArea.bottom - chartArea.top);
            ctx.restore();
          };
        }
      },
    };
    if(chartInstance){
      return 
    }
  
    chartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: scaledDistances, // x축: 스케일링된 누적 거리
        datasets: [
          {
            label: '고도 (m)',
            data: elevations,  // y축: 고도
            borderColor: 'rgba(75, 192, 192, 1)',
            borderWidth: 2,
            fill: false,
          },
        ],
      },
      options: {
        backgroundImage: '/images/mountainbackground.png',
        scales: {
          x: {
            title: {
              display: true,
              text: '거리 (m)', // x축 제목 (미터 단위로 표시)
            },
            grid: {
              display : false,
            },
            ticks: {
              autoSkip: true, // 자동으로 레이블을 생략함
              maxTicksLimit: 10, // 최대 레이블 개수를 10개로 제한
              maxRotation: 0, // 레이블을 비스듬히 하지 않음
              minRotation: 0, // 레이블을 수평으로 유지
              callback: function(value) {
                return value.toFixed(2) + ' m'; // 원하는 형식으로 레이블 포맷
              }
            }
          },
          y: {
            title: {
              display: true,
              text: '고도 (m)', // y축 제목
            },
            grid: {
              display : false,
            }
          },
        },
        interaction: {
          mode: 'index',
          intersect: false,
        },
        plugins: {
          tooltip: {
            enabled:false,
          },
          legend: {
            display: false,
          }
        },
        onClick: (event, elements) => {
          if (elements.length > 0) {
            const index = elements[0].index;
            highlightRouteOnMap(index); // 클릭 시 지도에서 경로 강조
          }
        },
        onHover: (event, elements) => {
          if (elements.length > 0) {
            const index = elements[0].index;
            highlightRouteOnMap(index); // 마우스 오버 시 경로 강조
          }
        },
      },
      plugins: [backgroundImagePlugin],
    });
  }

// 경로 상의 마커 강조 함수
function highlightRouteOnMap(index, targetMap = map.value) {
    if (!targetMap) {
      console.error("지도 객체가 초기화되지 않았습니다. targetMap이 null입니다.");
      return;  // targetMap이 null이면 함수 실행 중지
    }
  
    if (index >= 0 && index < routeCoordinates.value.length) {
      const point = routeCoordinates.value[index];
  
      if (currentMarker.value) {
        currentMarker.value.setMap(null);
      }
  
      const imageSrc = '/images/running.png';
      const imagesSize = new kakao.maps.Size(35,45);
      const imageOption = { offset: new kakao.maps.Point(17,45) };
      const markerImage = new kakao.maps.MarkerImage(imageSrc,imagesSize,imageOption);
  
      currentMarker.value = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(point.lat, point.lng),
        image: markerImage,
      });
  
      currentMarker.value.setMap(targetMap);
      targetMap.panTo(new kakao.maps.LatLng(point.lat, point.lng));  // targetMap이 null이 아닐 때만 panTo 호출
    }
  }

// 날씨
const dailyWeather = ref([]);
const sunriseTimes = ref([]);
const sunsetTimes = ref([]);
const selectedDateIndex = ref(0); // 선택된 날짜 인덱스

// 선택된 날짜의 날씨 데이터
const selectedDay = computed(() => dailyWeather.value[selectedDateIndex.value]);

// 날짜 선택 함수
const selectDate = (index) => {
  selectedDateIndex.value = index;
};


// 날씨 정보 조회
const fetchWeatherData = async (latitude, longitude) => {
  try {
    const apiKey = "5c48577c775896e979e7bcc3b225b730";
    const response = await axios.get(
      `https://api.openweathermap.org/data/2.5/forecast?lat=${latitude}&lon=${longitude}&appid=${apiKey}`
    );

    console.log(response.data); // 응답 확인용 콘솔 로그

    const groupedByDay = {};
    response.data.list.forEach(item => {
      const date = item.dt_txt.split(" ")[0];
      if (!groupedByDay[date]) groupedByDay[date] = [];
      groupedByDay[date].push(item);
    });

    const selectedWeather = [];
    Object.keys(groupedByDay).forEach(date => {
      const { currentTemp, minTemp, maxTemp } = calculateMinMaxTemp(groupedByDay[date]);
      const daySummary = {
        date,
        currentTemp,
        minTemp,
        maxTemp,
        ...groupedByDay[date][0],
      };
      selectedWeather.push(daySummary);
    });

    dailyWeather.value = selectedWeather;

    const sunriseSunsetPromises = dailyWeather.value.map(day =>
      axios.get(
        `https://api.sunrise-sunset.org/json?lat=${latitude}&lng=${longitude}&date=${day.date}&formatted=0`
      )
    );

    const sunriseSunsetResults = await Promise.all(sunriseSunsetPromises);
    sunriseTimes.value = sunriseSunsetResults.map(result => formatTime(result.data.results.sunrise));
    sunsetTimes.value = sunriseSunsetResults.map(result => formatTime(result.data.results.sunset));

  } catch (error) {
    console.error("Error fetching weather data:", error);
  }
};

// 요일 변환 함수
const formatDayOfWeek = (date) => {
  const formattedDate = new Date(date);
  return formattedDate.toLocaleDateString("ko-KR", { weekday: "short" });
};

// 날짜 포맷 변환 함수
const formatDate = (date) => {
  const formattedDate = new Date(date);
  return `${formattedDate.getDate()}`;
};

// 날씨 이모티콘 반환 함수
const getWeatherEmoji = (weatherMain) => {
  const weatherIcons = {
    Clear: "☀️",
    Clouds: "☁️",
    Rain: "🌧️",
    Snow: "🌨️",
    Fog: "🌁",
    Thunderstorm: "⛈️",
    Drizzle: "🌦️",
    Mist: "🌫️",
    breeze: "༄",
    "Few Clouds": "🌥️",
    "freezing rain": "🌧️➜❄️",
  };
  return weatherIcons[weatherMain] || "🌥️";
};

// 날씨 ID를 한국어 설명으로 변환하는 함수
const translateWeatherDescription = (weatherId) => {
  const weatherDescriptions = {
    200: "가벼운 비를 동반한 천둥구름",
    201: "비를 동반한 천둥구름",
    202: "폭우를 동반한 천둥구름",
    210: "약한 천둥구름",
    211: "천둥구름",
    212: "강한 천둥구름",
    221: "불규칙적 천둥구름",
    230: "약한 연무를 동반한 천둥구름",
    231: "연무를 동반한 천둥구름",
    232: "강한 안개비를 동반한 천둥구름",
    300: "가벼운 안개비",
    301: "안개비",
    302: "강한 안개비",
    310: "가벼운 적은비",
    311: "적은비",
    312: "강한 적은비",
    313: "소나기와 안개비",
    314: "강한 소나기와 안개비",
    321: "소나기",
    500: "약한 비",
    501: "중간 비",
    502: "강한 비",
    503: "매우 강한 비",
    504: "극심한 비",
    511: "우박",
    520: "약한 소나기 비",
    521: "소나기 비",
    522: "강한 소나기 비",
    531: "불규칙적 소나기 비",
    600: "가벼운 눈",
    601: "눈",
    602: "강한 눈",
    611: "진눈깨비",
    612: "소나기 진눈깨비",
    615: "약한 비와 눈",
    616: "비와 눈",
    620: "약한 소나기 눈",
    621: "소나기 눈",
    622: "강한 소나기 눈",
    701: "박무",
    711: "연기",
    721: "연무",
    731: "모래 먼지",
    741: "안개",
    751: "모래",
    761: "먼지",
    762: "화산재",
    771: "돌풍",
    781: "토네이도",
    800: "구름 한 점 없는 맑은 하늘",
    801: "약간의 구름이 낀 하늘",
    802: "드문드문 구름이 낀 하늘",
    803: "구름이 거의 없는 하늘",
    804: "구름으로 뒤덮인 흐린 하늘",
    900: "토네이도",
    901: "태풍",
    902: "허리케인",
    903: "한랭",
    904: "고온",
    905: "바람부는",
    906: "우박",
    951: "바람이 거의 없는",
    952: "약한 바람",
    953: "부드러운 바람",
    954: "중간 세기 바람",
    955: "신선한 바람",
    956: "센 바람",
    957: "돌풍에 가까운 센 바람",
    958: "돌풍",
    959: "심각한 돌풍",
    960: "폭풍",
    961: "강한 폭풍",
    962: "허리케인"
  };
  return weatherDescriptions[weatherId] || "알 수 없는 날씨";
};

const calculateMinMaxTemp = (dayData) => {
  const temps = dayData.map(item => item.main.temp);
  const currentTemp = dayData[0].main.temp;
  const minTemp = Math.min(...temps);
  const maxTemp = Math.max(...temps);
  return { currentTemp, minTemp, maxTemp };
};

// 시간 포맷 변환 함수
const formatTime = (time) => {
  const date = new Date(time);
  return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", timeZone: "Asia/Seoul" });
};


//------------------------------------- 여기는 리뷰

const router = useRouter();
const route = useRoute();
const goBack = () => router.back();

const showModal = ref(false); // 리뷰 작성 모달
const editModal = ref(false); // 리뷰 수정 모달
const deleteModal = ref(false); // 리뷰 삭제 확인 모달

const currentReview = ref({
  courseId: null,
  reviewId: null,
  userId: '',
  rating: 0,
  difficulty: '',
  reviewContent: ''
});
const selectedReviewId = ref(null); // 삭제할 리뷰 ID
const loggedInUserId = ref(localStorage.getItem('userId') || '');

// 전달된 query 데이터 파싱
const courseData2 = ref({});
const selectedCourse = ref('');
const rating = ref(0);
const difficulty = ref('');
const reviewContent = ref('');
const photos = ref([]);
const courses = ref([]);

// 리뷰 수정 모달 열기
const editReview = async (review) => {
  console.log("리뷰 수정 모달 열기:", review);
  currentReview.value = { ...review };
  await nextTick(); // 렌더링 강제 트리거
  editModal.value = true; // 모달 열기
  console.log("currentReview 상태:", currentReview.value);
  console.log("editModal 상태:", editModal.value);
};


// 리뷰 삭제 확인 모달 열기
const confirmDelete = (reviewId) => {
  console.log("리뷰 삭제 요청됨, ID:", reviewId); // 삭제할 리뷰 ID 확인
  if (reviewId && reviewId !== 0) {
    selectedReviewId.value = reviewId;
    console.log("선택된 리뷰 ID:", selectedReviewId.value); // 선택된 ID 확인
    deleteModal.value = true;
  } else {
    console.warn("유효하지 않은 리뷰 ID입니다:", reviewId);
  }
};


// 리뷰 수정 저장
const saveReview = async () => {
  console.log("리뷰 저장 실행, currentReview:", currentReview.value);
  if (!currentReview.value || !currentReview.value.reviewId) {
    console.error("유효하지 않은 리뷰 ID");
    return;
  }
  try {
    // 반응형 객체가 아닌 일반 객체로 변환하여 서버로 전송
    const reviewData = {
      reviewId: currentReview.value.reviewId,
      userId: loggedInUserId.value,  // 필요시 .value 추가
      rating: currentReview.value.rating,
      difficulty: currentReview.value.difficulty,
      reviewContent: currentReview.value.reviewContent,
    };

    // 서버에 수정 요청 보내기
    await axios.put(`/api/reviews/edit/${currentReview.value.reviewId}`, reviewData);

    alert('리뷰가 성공적으로 수정되었습니다.');
    editModal.value = false; // 모달 닫기
    fetchReviewsAndRatings(); // 리뷰와 평점 데이터 갱신
  } catch (error) {
    console.error('리뷰 수정 오류:', error);
    alert("리뷰 수정에 실패했습니다.");
  }
};



// 리뷰 삭제
const deleteReview = async () => {
  try {
    await axios.delete(`/api/reviews/delete/${selectedReviewId.value}`, {
      params: { userId: localStorage.getItem('userId') },
    });
    alert('리뷰가 삭제되었습니다.');
    deleteModal.value = false;
    fetchReviewsAndRatings();
  } catch (error) {
    console.error('리뷰 삭제 오류:', error);
  }
};

try {
  courseData.value = route.query.course ? JSON.parse(route.query.course) : {};
} catch (e) {
  console.error("Failed to parse course data:", e);
  courseData.value = {}; // 파싱 오류 시 빈 객체로 초기화
}

console.log("Loaded course data:", courseData.value);


const difficultyMapping = {
  "쉬움": 1,
  "보통": 2,
  "어려움": 3,
};

const averageDifficultyText = computed(() => {
  if (reviews.value.length === 0) return "정보 없음";
  const totalDifficulty = reviews.value.reduce((sum, review) => {
    return sum + (difficultyMapping[review.difficulty] || 0);
  }, 0);
  const averageDifficulty = totalDifficulty / reviews.value.length;

  // 평균 난이도
  if (averageDifficulty <= 1.5) {
    return "쉬움";
  } else if (averageDifficulty <= 2.5) {
    return "보통";
  } else {
    return "어려움";
  }
});


const courseId = computed(() => courseData.value.courseId);

const averageRating = ref(0);
const reviewSummary = ref('');
const reviews = ref([]);
const ratingDistribution = ref({});
const reviewCount = ref(0);
const searchQuery = ref('');
const reviewsToShow = ref(3);

const setRating = (star) => {
  rating.value = star;
};

const onFileChange = (event) => {
  photos.value = Array.from(event.target.files);
};

const submitReview = async () => {
  if (!validateForm()) {
    return; // 입력값이 부족하면 제출x
  }

  const review = {
    courseId: selectedCourse.value,
    userId: localStorage.getItem('userId'),
    rating: rating.value,
    difficulty: difficulty.value,
    reviewContent: reviewContent.value,
  };

  const formData = new FormData();
  formData.append('review', new Blob([JSON.stringify(review)], { type: 'application/json' }));

  photos.value.forEach((photo, index) => {
    formData.append(`photos[${index}]`, photo);
  });

  try {
    await axios.post('/api/reviews/create', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    alert('리뷰가 성공적으로 작성되었습니다!');
    // 모달 닫기
    showModal.value = false;

    // 리뷰와 평점 데이터 다시 가져오기
    fetchReviewsAndRatings();
  } catch (error) {
    console.error('리뷰 작성 오류:', error);
  }
};


const validateForm = () => {
  if (!selectedCourse.value) {
    alert('코스를 선택하세요.');
    return false;
  }
  if (rating.value === 0) {
    alert('별점을 선택하세요.');
    return false;
  }
  if (!difficulty.value) {
    alert('난이도를 선택하세요.');
    return false;
  }
  if (!reviewContent.value) {
    alert('리뷰 내용을 입력하세요.');
    return false;
  }
  return true;
};

// 리뷰와 평점 데이터 갱신
const fetchReviewsAndRatings = async () => {
  if (!courseId.value) return;
  try {
    const response = await axios.get(`/api/course/${courseId.value}/reviews`);
    console.log("로드된 리뷰 데이터:", response.data.reviews); // 데이터 확인
    reviews.value = response.data.reviews.map(review => ({
      ...review,
      reviewId: review.reviewId || review.id || null
    }));

    ratingDistribution.value = response.data.ratingDistribution || {};
    reviewCount.value = response.data.reviewCount || 0;

    if (response.data.averageRating) {
      averageRating.value = response.data.averageRating.toFixed(1);
    }
  } catch (error) {
    console.error('리뷰 데이터를 가져오는 데 실패했습니다:', error);
  }
};



const fetchSummary = async () => {
  if (!courseId.value) return;
  try {
    const response = await axios.get(`/api/course/${courseId.value}/summarize`);
    reviewSummary.value = response.data.summary || '요약된 내용 없음';
  } catch (error) {
    console.error('리뷰 요약 데이터를 가져오는 데 실패했습니다:', error);
  }
};

const fullStars = computed(() => Math.floor(averageRating.value));
const halfStar = computed(() => averageRating.value % 1 >= 0.5);
const emptyStars = computed(() => 5 - fullStars.value - (halfStar.value ? 1 : 0));

const filteredReviews = computed(() => {
  return reviews.value.map(review => ({
    ...review,
    rating: review.rating ?? 0, // rating이 없으면 기본값 0 설정
  })).filter(review => {
    const reviewContent = review?.reviewContent || '';
    const userId = review?.userId || '';
    return reviewContent.toLowerCase().includes(searchQuery.value.toLowerCase()) || userId.toLowerCase().includes(searchQuery.value.toLowerCase());
  });
});


const visibleReviews = computed(() => {
  return (reviews.value || [])
    .map(review => ({
      ...review,
      rating: review?.rating ?? 0, // rating이 없을 때 기본값 0 설정
      difficulty: review?.difficulty ?? '정보 없음', // 난이도 기본값 설정
      reviewContent: review?.reviewContent ?? '내용 없음' // 리뷰 내용 기본값 설정
    }))
    .filter(review => {
      const reviewContent = review?.reviewContent || '';
      const userId = review?.userId || '';
      return reviewContent.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
        userId.toLowerCase().includes(searchQuery.value.toLowerCase());
    })
    .slice(0, reviewsToShow.value);
});

const loadMoreReviews = () => {
  if (reviewsToShow.value < filteredReviews.value.length) {
    reviewsToShow.value += 3;
  }
};

const canShowMore = computed(() => {
  return reviewsToShow.value < filteredReviews.value.length;
});

function goToCourse3DPage() {
    router.push({ name: '3d', query: { course: JSON.stringify(courseData.value) }});
  }


</script>

<style scoped>
*{
  overflow: hidden;
}

  .btn-group-vertical {
    position: absolute;
    font-size: 0.5rem !important;
    top: 100px;
    right: 0;
    z-index: 2;
    width: 85px;
    margin-right: 2rem;
    margin-top: 3rem;
  }

.dashboard {
  display: grid;
  background-color: white;
  grid-template-columns: 410px 1fr;
  grid-template-rows: auto 1fr auto;
  height: 100vh;
  gap: 10px;
  overflow: hidden;
}

.sidebar {
  background-color: white;
  justify-content: center;
  width: 410px;
  max-height: 100vh; /* Limit sidebar height to viewport height */
  overflow: auto; /* Hide overflow content */
  overflow-x: hidden;
}

.main {
  display: flex;
  flex-direction: column; /* Stack elements vertically */
  gap: 10px; /* Space between map and chart */
}

.map-container {
  height: 60vh; /* Adjust to fit your layout */
  width: 100%;
  border-radius: 10px;
}

#grapth {
  padding: 10px;
  background-color: #f1f1f1;
  width: 100%;
}

.chart1-section {
  text-align: center;
}


.weather-forecast {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 100%;
  margin-top: 5rem;
  margin-bottom: 3rem;
  }

.date-selector {
  display: flex;
  justify-content: space-around;
  width: 100%;
  margin-bottom: 10px;
}

.date-button-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .day-label {
    font-family: 'TheJamsil4Bold';
    font-size: 12px;
    color: #333;
    margin-bottom: 5px;
  }

  .date-button {
    width: 30px;
    height: 30px;
    border-radius: 50%; /* 버튼을 원형으로 만듦 */
    background-color: #ffffff; /* 기본 색상 */
    border: none;
    font-size: 14px;
    font-family: 'TheJamsil4Bold';
    transition: background-color 0.3s;
  }

.forecast-header {
  text-align: center;
  margin-bottom: 20px;
}

.forecast-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr); /* 5일 날씨에 맞게 5개의 칸으로 */
  gap: 5px;
  width: 100%; /* 부모 요소의 너비를 꽉 채움 */
}

.forecast-item {
  text-align: center;
  padding: 5px;
  border: 1px solid #ccc;
  border-radius: 10px;
  background-color: #f7f7f7; /* 배경 색상 추가로 가독성 향상 */
  box-shadow: 0px 2px 10px rgba(0, 0, 0, 0.1); /* 가벼운 그림자 추가 */
}

.forecast-info {
  margin-top: 10px;
}

.current-temperature {
    font-family: 'TheJamsil4Bold';
    font-weight: 500;
    font-size: 70px;
    font-weight: bold;
    color: #333; /* 텍스트 색상을 진하게 */
  }

  .weather-icon {
    font-size: 50px; /* 날씨 이모티콘 크기를 키움 */
    margin-top: 1rem;
  }

  .description {
    font-family: 'TheJamsil4Bold';
  font-size: 14px;
  font-weight: 400;
  color: #333;
  text-align: left;
  width: 180px;
  margin-left: 0.5rem;
  }

  .temperature-range {
  display: flex;
  margin-left: 0.5rem;
  margin-top: -0.6rem;
  }

  .min-temp{
    font-family: 'TheJamsil4Bold';
    width: 80px;
    height: 50px;
    font-weight: 300;
    font-size: 14px;
    color:rgb(80, 80, 80);
    margin-right: -1rem;

  }

  .max-temp {
    font-family: 'TheJamsil4Bold';
    width: 80px;
    height: 50px;
    font-weight: 300;
    font-size: 14px;
    color:rgb(80, 80, 80);
  }

  .sun-info{
    font-family: 'TheJamsil3Bold';
  font-weight: 400;
  font-size: 16px;
  color: rgb(130, 130, 130);
  margin-top: 0.5rem;
  text-align: center;
  }

  .icon-small {
  width: 15px;
  height: 15px;
  margin-right: 5px;
  opacity: 0.7;
}

  .rain-info {
    display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.4rem;
  }

  .rain-icon {
  width: 20px;
  height: 20px;
  background-color: rgba(173, 216, 230, 0.5);
  border-radius: 50%;
  position: relative;
  overflow: hidden;
  }

  .rain-icon::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: var(--rain-level, 0);
  background-color: rgb(78, 130, 182);
  }

  .rain-chance {
    font-family: 'TheJamsil5Bold';
    font-weight: 400;
    font-size: 14px;
  }

  #sunrise, #sunset {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: rgb(80, 80, 80);
  margin: 0.5rem 0;
}

    .date-button.active {
    background-color: #415d37; /* 선택된 버튼의 색상 */
    color: #fff; /* 선택된 버튼의 글자 색상 */
  }
/* 댓글 */

.course-preview-page {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.back-button {
  background-color: #ffffff;
  color: #a52d2d;
  border: none;
  padding: 8px;
  width: 50px;
  height: 50px;
  font-size: 18px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin-bottom: 10px;
}
.review-button {
  background-color: #28a745;
  color: white;
  border: none;
  font-weight: bold;
  border-radius: 50%;
  cursor: pointer;
  width: 40px;
  height: 40px;
  transition: background-color 0.3s ease;
  box-shadow: 0 3px 7px rgba(0, 0, 0, 0.5);
  margin-top: 2rem;
  margin-left: 19.9rem;
}
.review-button:hover {
  background-color: #36672f;
}
/* 모달 스타일 */
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex !important;
  visibility: visible !important;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.review-form {
  background: white;
  padding: 30px;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
  position: relative;
  text-align: center;
}

.close-button {
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.close-button:hover {
  color: #999;
}

.modal-label {
  font-size: 1rem;
  font-weight: bold;
  margin-top: 15px;
  display: block;
  text-align: left;
  color: #444;
}

.input-field {
  width: 100%;
  padding: 10px;
  margin-top: 10px;
  border-radius: 5px;
  border: 1px solid #ddd;
  font-size: 14px;
}

.textarea {
  height: 100px;
  resize: none;
}

.file-input {
  width: 100%;
  margin-top: 10px;
  padding: 10px;
  font-size: 14px;
}

.rating {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 10px;
}

.stars {
  display: flex;
  gap: 5px;
  color: #ffc107;
}

.n-star {
  font-size: 1.5rem;
  cursor: pointer;
}

.submit-button {
  background-color: #28a745;
  color: white;
  border: none;
  padding: 15px;
  font-size: 16px;
  font-weight: bold;
  border-radius: 25px;
  cursor: pointer;
  width: 100%;
  margin-top: 20px;
  transition: background-color 0.3s ease;
}

.submit-button:hover {
  background-color: #218838;
}

.load-more-button {
  background-color: #28a745;
  color: white;
  border: none;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: bold;
  border-radius: 25px;
  cursor: pointer;
  display: block;
  margin: 20px auto;
  transition: background-color 0.3s ease;
}
.load-more-button:hover {
  background-color: #218838;
}
.course-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
  text-align: center;
}
.course-image {
  width: 100%;
  max-width: 600px;
  height: auto;
  object-fit: cover;
}
.course-details {
  font-family: 'TheJamsil4Bold';
  font-size: 15px;
  font-weight:300;
  color: #555;
  padding-top: 15px;
  line-height: 1.7;
  margin-top: 0.5rem;
  margin: 0 1.5rem;
}

.accident-summary {
  text-align: center;
  background: #f9f9f9;
  padding: 20px;
  margin-bottom: 20px;
}

#reviewTitle{
  font-family: 'TheJamsil4Bold';
  font-size: 20px;
  font-weight: 700;
}

.rating-number {
  font-size: 2rem;
  color: #4caf50;
}

.review-page {
  background: #f9f9f9;
  padding: 15px;
  border-radius: 10px;
  
}

.review-search input {
  width: 100%;
  padding: 10px;
  border-radius: 5px;
  border: 1px solid #ddd;
}

.reviews-list {
  margin-top: 20px;
}

.review-item {
position: relative;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
  padding-bottom: 15px;
}

.review-header {
  display: flex;
  gap: 10px;
  align-items: center;
}

.profile-image {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.user-info h3 {
  font-size: 1rem;
  margin: 0;
}

.stars-inline i {
  color: #4caf50;
}

.review-photos img {
  width: 100%;
  height: auto;
  max-height: 200px;
  margin-top: 10px;
  border-radius: 5px;
  object-fit: cover;
}

.summary-section {
  background-color: #ffffff;
  padding: 20px;
  display: flex;
  align-items: center;
  width: 100%;
  margin-bottom: 20px;
}

.rating h1 {
  font-size: 3rem;
  color: #4caf50;
  margin-bottom: 10px;
}

.stars {
  display: flex;
  gap: 5px;
  color: #4caf50;
}

.review-count-text {
  font-size: 1rem;
  color: #666;
  margin-top: 10px;
}

.rating-breakdown {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.rating-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-bar span {
  width: 30px;
  text-align: right;
  font-weight: bold;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background-color: #ddd;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.progress {
  height: 100%;
  background-color: #4caf50;
  border-radius: 4px;
}
.image-container {
  position: relative;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
}


.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-start;
  padding: 10px;
  background: rgba(0, 0, 0, 0.1); /* 어두운 배경 추가 */

}

#courseTitle{
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 400;
  margin-top: 0.4rem;
  margin-left: 0.3rem;
}

.image-course-name{
    font-family: 'TheJamsil';
    font-weight: 500;
    color: white;
    font-size: 30px;
    margin: 5px 3px -6px;
}


.image-course-location{
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 400;
  margin: 5px 3px -5px;
}

.difficulty-text{
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 400;
  margin: 5px 3px 1px;
}
.image-rating-star {
 color: #50df4b;
}
.n-star{
    color: #4caf50; 
}
.edit-button {
  position: absolute;
  right: 10px; /* 우측 여백 */
  bottom: 10px; /* 하단 여백 */
  background: none;
  border: none;
  color: #4caf50;
  cursor: pointer;
  font-size: 0.9rem;
}

.delete-button {
  position: absolute;
  right: 10px; /* 우측 여백 */
  top: 10px; /* 상단 여백 */
  background: none;
  border: none;
  color: #4caf50;
  cursor: pointer;
  font-size: 0.9rem;
}

.delete-confirm-modal {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 300px;
  text-align: center;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.2);
}

.confirm-button {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 10px 20px;
  margin-right: 10px;
  cursor: pointer;
}

.cancel-button {
  background-color: #6c757d;
  color: white;
  border: none;
  padding: 10px 20px;
  cursor: pointer;
}

.review-content{
  margin-top: 0.5rem;
  font-family: 'TheJamsil4Bold';
  font-size: 15px;
  font-weight:300;
  line-height:1.7;
}

.average-rating-text{
  font-family: 'TheJamsil5Bold';
  font-size: 50px;
  font-weight:700;
  color: #4caf50;
}

.carousel-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-direction: column;
}

.card-carousel::-webkit-scrollbar {
  display: none; /* 스크롤바 가리기 */
}

.card-carousel {
  display: row;
  gap: 1.4rem;
  overflow-x: auto;
  overflow-y: hidden;
  scroll-behavior: smooth;
  width: 100%; /* 전체 폭을 차지 */
}

.card {
  position: relative;
  width:353px;
  height: 70px;
  background-color: #fff;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0; /* 카드가 줄어들지 않고 고정된 크기로 유지 */
  margin-top: 1rem;
}

.card-img-top {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border: none;
  margin-right: 15px;
  margin-left: 10px;
}

.card-overlay {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px; /* 이미지와 텍스트 사이의 간격 */
}

.accident-item-content1 {
  display: flex;
  align-items: center;
  font-family: 'TheJamsil5Bold', sans-serif; /* 고급스럽고 깔끔한 글꼴 */
  font-size: 16px; /* 크기를 약간 키워 가독성 향상 */
  font-weight: 500; /* 텍스트를 더 두껍게 */
  color: #333; /* 텍스트 색상을 고급스러운 짙은 회색으로 */
  margin-left: 2rem;
  gap: 10px; /* 이미지와 텍스트 사이의 간격 */
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1); /* 부드러운 그림자 효과 */
  letter-spacing: 0.5px; /* 약간의 글자 간격 추가 */
  transition: color 0.3s ease, transform 0.3s ease; /* 마우스 호버 효과를 부드럽게 */
}

.accident-item-content1:hover {
  color: #4CAF50; /* 호버 시 텍스트 색상을 초록색으로 변경 */
  transform: scale(1.02); /* 약간 확대 효과 */
}

.toggle-switch-container2 {
  position: fixed;
  z-index: 10;
  display: flex;
  justify-content: start;
  gap: 10px;
  margin-top: 3rem;
  margin-left: 5rem;
  width: 50%;
  height: 50px;
}

.toggle-switch2 {
  width: 100px;
  height: 45px;
  background-color: #f5f5f5; /* 기본 배경색을 연한 회색으로 설정 */
  border-radius: 20px; /* 더 부드러운 모서리를 위해 둥글게 처리 */
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-family: 'TheJamsil4Bold';
  font-size: 16px;
  font-weight: 700;
  color: #555; /* 텍스트 색상을 부드러운 회색으로 설정 */
  transition: all 0.3s ease; /* 전환 효과 추가 */
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1); /* 가벼운 그림자 효과 */
  border: 1px solid #ddd; /* 테두리 추가 */
}

.toggle-switch2 img {
  width: 40px; /* 아이콘 크기 조정 */
  height: auto;
  margin-right: 8px; /* 텍스트와 아이콘 사이 간격 */
  transition: transform 0.3s ease; /* 클릭 시 부드러운 전환 효과 */
}

.toggle-switch2:hover {
  background-color: #e0e0e0; /* 호버 시 배경색 변화 */
  color: #333; /* 텍스트 색상 진하게 변경 */
}

.toggle-switch2.active {
  background-color: #4CAF50; /* 활성화된 버튼 배경을 초록색으로 설정 */
  color: #ffffff; /* 활성화된 텍스트 색상을 흰색으로 설정 */
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); /* 활성화된 버튼에 더 강한 그림자 */
}

.toggle-switch2.active img {
  transform: scale(1.1); /* 활성화된 아이콘 살짝 확대 */
}

.toggle-switch-container2 {
  flex-wrap: wrap; /* 버튼이 많아지면 줄바꿈 가능 */
  gap: 15px; /* 버튼 사이의 간격 */
}

  .forecast-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  width: 100%;
  padding: 10px;
}

  #forecast1{
    width: 60%;
  }

  .rainSun-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 40%; /* forecast1과 균형을 맞추기 위해 필요 시 조정 */
  text-align: center;
  margin-top: 0.5rem;
  margin-left: 3rem;
}

.DangerImage {
  display: flex;
  gap: 5px; /* 이미지 사이 간격 */
}

#img-storage {
  display: flex;
  align-items: center;
  justify-content: center;
}

#DangerImage1,
#DangerImage2,
#DangerImage3 {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 5px;
}

.button-group {
  display: flex;
  justify-content: space-around;
  margin: 20px 0;
}

.btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  width: 100px; /* 버튼 너비 조정 */
  height: 120px; /* 버튼 높이 조정 */
  text-align: center;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn img {
  width: 50px; /* 이미지 너비 */
  height: 50px; /* 이미지 높이 */
  margin-bottom: 10px; /* 이미지와 텍스트 간 간격 */
  object-fit: contain; /* 이미지가 깨지지 않도록 조정 */
}

.btn p {
  margin: 0;
  font-size: 12px; /* 텍스트 크기 */
}

.btn:hover {
  transform: scale(1.05); /* 호버 시 약간 확대 효과 */
  box-shadow: 0 6px 10px rgba(0, 0, 0, 0.15);
  color:white;
  background-color:#218838
}
.custom-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2);
}

.custom-btn:active {
  transform: translateY(0);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

/* 이미지 그리드 */
.image-grid {
  display: flex; /* Flexbox로 정렬 */
  flex-wrap: nowrap; 
  gap: 10px; /* 이미지 간 간격 */
  overflow-x: auto; /* 이미지가 넘칠 경우 가로 스크롤 */
  overflow: hidden;
  padding: 5px 0;
  justify-content: start; /* 좌측 정렬 (또는 center, space-between 등 필요에 따라 변경 가능) */
}

/* 개별 이미지 스타일 */
.image-item {
  width: 60px; /* 이미지 컨테이너 크기 */
  height: 60px;
  overflow: hidden; /* 이미지가 크기를 벗어나지 않도록 숨김 */
  border-radius: 8px; /* 이미지의 모서리를 둥글게 */
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); /* 가벼운 그림자 효과 */
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.image-item:hover {
  transform: scale(1.1); /* 호버 시 살짝 확대 */
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15); /* 호버 시 그림자 강조 */
}

/* 클릭 가능한 이미지 */
.clickable-image {
  width: 100%; /* 이미지가 컨테이너에 꽉 차게 */
  height: 100%;
  object-fit: cover; /* 이미지 비율 유지하며 꽉 채우기 */
  cursor: pointer; /* 클릭 가능한 커서 */
  border-radius: 8px;
}

/* 범례 스타일 */
.map-legend {
    position: absolute;
    top: 0;
    right: 0;
    margin-top: 35.5em;
    margin-right: 2em;
    background-color: #faf7f0; /* 반투명 배경 */
    padding: 5px;
    border-radius: 10px;
    font-size: 12px;
    z-index: 10; /* 지도 위에 표시되도록 설정 */
  }

  .legend-item {
    display: flex;
    align-items: center;
    margin-bottom: 2px;
  }

  .legend-icon {
    width: 10px;
    height: 10px;
    margin-right: 5px;
    margin-left: 5px;
    border-radius: 10px;
  }

  /* 모달 전반적인 스타일 */
.modal-dialog {
  max-width: 1400px; /* 모달 너비 확장 */
}

.modal-content {
  background: #f9f9f9;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
}

.modal-header {
  background: #28a745; /* 초록색 배경 */
  padding: 10px; /* 상하 여백 줄이기 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px; /* 헤더 높이 축소 */
}

.modal-header .modal-title {
  font-size: 2rem; /* 글씨 크기 축소 */
  font-weight: bold;
  color: white; /* 헤더 텍스트 색상 */
  margin: 0; /* 좌측 정렬 */
}

.modal-header .btn {
  width: 30px; /* 버튼 크기 축소 */
  height: 30px;
  color: white;
  background: transparent;
  border: none;
}

.modal-header .btn:hover {
  background: rgba(0, 0, 0, 0.1); /* 닫기 버튼 호버 효과 */
}

.modal-header .btn i {
  font-size: 2rem; /* 아이콘 크기 확대 */
  color: white; 
}

.modal-body {
  padding: 20px;
  background: #ffffff;
  border-radius: 16px;
}

/* 지도와 비디오 컨테이너 */
.video-container {
  display: flex;
  gap: 10px; /* 지도와 비디오 사이 간격 줄이기 */
  align-items: center;
}

#modalMap {
  width: 60%; /* 지도 넓이 증가 */
  height: 700px; /* 지도 높이 증가 */
  border-radius: 8px;
  border: 1px solid #ddd;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.video-container video {
  width: 38%; /* 비디오 넓이 조정 */
  height: 700px; /* 비디오 높이 증가 */
  border: 1px solid #ddd;
  border-radius: 8px;
  object-fit: cover; /* 여백 없이 비디오 맞춤 */
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}
</style>
