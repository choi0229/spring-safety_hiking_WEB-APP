<template>
  <HeaderView></HeaderView>
  <div id="app">

    <div class="d-flex">
      <div id="sideBar">
        <div class="d-flex flex-column">
          <div>
                <!-- 헤더 섹션 -->
                <div class="header">
                  <button @click="goBack" class="back-button"><img src="/images/moveback_icon.png"  alt="뒤로가기 아이콘" width="15" height="15"></button>

                </div>
                
                <!-- 코스 이미지 및 기본 정보 -->
                <div class="course-info">
                  <div class="image-container">
                    <img v-if="courseData?.courseImage" :src="courseData.courseImage" alt="Mountain Image" class="course-image" />
                    <div class="image-overlay">
                      <p class="image-course-name">{{ courseData?.courseName }}코스</p>
                      <p class="image-course-location">{{ courseData?.courseLocation }}<span id="courseTitle">{{ courseData?.mountainName || '코스 정보' }}</span></p>
                      <p class="difficulty-text"> 난이도: {{ averageDifficultyText }}  · <i class="bi bi-star-fill image-rating-star"></i> {{ averageRating }}({{ reviewCount }})</p>      
                  </div>
                </div>
                <div class="course-details">
                  <p>{{ courseData?.courseContent }}</p>
                </div>
              </div>

                <!-- 사고 요약 섹션 -->
                <div class="accident-summary">
                  <div class="carousel-container">
                    <div class="card-carousel" ref="accidentCarouselRef">
                      <div class="card-hidden" v-for="(item, index) in accidentItems" :key="index">
                        <img :src="item.image" :alt="item.title" class="card-img-top" />
                        <div class="card-overlay">
                          <p class="accident-item-content1">{{ item.content1 }}</p>
                          <p class="accident-item-content2">{{ item.content2 }}</p>
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

      <button
        :class="['date-button', { active: selectedDateIndex === index }]"
        @click="selectDate(index)"
      >
        {{ formatDate(day.date) }}
      </button>

    </div>
  </div>

<div class="forecast-details" v-if="selectedDay">
<div class="d-flex">

<div class="row">
  
  <div class="d-flex" style="margin-top: -1rem; margin-left: 5.5rem;">
    <p class="current-temperature">{{ Math.round(selectedDay.currentTemp - 273.15) }}°</p>
    <p class="weather-icon" v-html="getWeatherEmoji(selectedDay.weather[0].main)"></p>
  </div>
  
  
  <div class="row">

    <p class="description" >{{ translateWeatherDescription(selectedDay.weather[0].id) }}</p>

    <div class="temperature-range d-flex">
      <p class="min-temp">최저: {{ Math.round(selectedDay.minTemp - 273.15) }}°</p>
      <p class="max-temp">최고: {{ Math.round(selectedDay.maxTemp - 273.15) }}°</p>
    </div>
    

    
    
  </div>
  
</div>




<div class="rainSun-info">               
  <div class="rain-info d-flex">
    <div class="rain-icon" :style="{ '--rain-level': selectedDay.pop * 100 + '%' }"></div>
    <p class="rain-chance" style="margin-top: 1rem; margin-left: 1.5rem;">{{ Math.round(selectedDay.pop * 100) }}%</p>
  </div>
  
  <div class="sun-info" >
    <p id="sunrise"><img src="/images/sunrise.png" alt="sunrise" class="icon-small" >일출: {{ sunriseTimes[selectedDateIndex] }}</p>
    <p id="sunset"><img src="/images/sunset.png" alt="sunset" class="icon-small" >일몰: {{ sunsetTimes[selectedDateIndex] }}</p>
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
                          <i v-for="star in 5" 
                          :key="star" 
                          :class="star <= rating ? 'bi bi-star-fill n-star' : 'bi bi-star n-star' "
                          @click="setRating(star)">
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
                    <input type="text" placeholder="리뷰 검색" v-model="searchQuery" />
                  </div>
                  <div>
                  <button class="review-button" @click="showModal = true"><img src="/images/write.png" style="width: 32px; height:32px; justify-content: center; margin-bottom: 0.1rem;"></button>
                </div>

                  <div class="reviews-list">
                    <div v-for="review in visibleReviews" :key="review.reviewId" class="review-item">
                      <div class="review-header">
                        <img class="profile-image" :src="review.profileImage || '/images/main1.jpg'" alt="Profile" />
                        <div class="user-info">
                          <h3>{{ review?.userId || 'Unknown User' }}</h3>
                          <div class="stars-inline" v-if="review?.rating !== undefined">
                              <i v-for="star in 5" 
                                  :class="{
                                      'bi bi-star-fill': review?.rating && star <= Math.floor(review.rating),
                                      'bi bi-star-half': review?.rating && star === Math.ceil(review.rating) && review.rating % 1 >= 0.5,
                                      'bi bi-star': !review?.rating || star > Math.ceil(review.rating)
                                  }"
                                  :key="star"></i>
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
                      <i v-for="star in 5"
                          :key="star"
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
                      <textarea v-model="currentReview.reviewContent" placeholder="후기 내용을 입력하세요" class="input-field textarea"></textarea>

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





      
      </div>     
      <div class="row" style="margin: 0px; padding: 0px;">
        <div id="map"></div>
        <canvas id="elevationChart" height="50px"></canvas>
      </div>
    </div>

  </div>



  <!-- 코스 미리보기 모달 -->
  <div class="modal fade" id="kt_modal_course_preview" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered mw-850px">
        <div class="modal-content rounded">
          <div class="modal-header pb-0 border-0 justify-content-end">
            <div class="btn btn-sm btn-icon btn-active-color-primary" data-bs-dismiss="modal">
              <i class="ki-duotone ki-cross fs-1" style="color: black;">X</i>
            </div>
          </div>

          <div class="modal-body scroll-y px-10 px-lg-15 pt-0 pb-15">
            <div class="mb-13 text-center">
              <h1 class="mb-3">코스 미리보기</h1>
              <div class="text-muted fw-semibold fs-5">동영상과 경로를 나란히 볼 수 있습니다.</div>
            </div>

            <!-- 지도와 비디오 미리보기 영역 -->
            <div class="d-flex flex-row mb-8">
              <!-- 지도 -->
              <div id="modalMap" style="width: 50%; height: 400px; margin-right: 10px;"></div>

              <!-- 비디오 -->
              <video
                ref="videoRef"
                width="50%"
                controls
                @timeupdate="syncCourseWithVideo"
              >
                <source v-id="courseData" :src= "'' + courseData.courseVideo" alt="" type="video/mp4" />
                브라우저가 비디오 태그를 지원하지 않습니다.
              </video>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 모달 : 끝-->
</template>

<script setup>
import HeaderView from '@/components/HeaderView.vue';
import { onMounted, ref, computed } from 'vue';
import Chart from 'chart.js/auto';
import axios from "axios";
import { defineProps } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { nextTick } from 'vue';

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
const polyline = ref(null);
const modalMap = ref(null);
const modalPolyline = ref(null);  // 모달 지도 경로
const videoRef = ref(null);  // 비디오 요소 참조

onMounted(() => {
  initializeMap();
  // mountainData로부터 lat, lon을 사용해 날씨 정보를 가져옴

  fetchWeatherData(37.590870, 126.958077);
     
});

function initializeMap() {
  // Kakao Maps API 스크립트 로드
  const script = document.createElement('script');
  script.onload = () => kakao.maps.load(() => {
    map.value = new kakao.maps.Map(document.getElementById('map'), {
      center: new kakao.maps.LatLng(37.66433293993584, 127.01160029114365),
      level: 5
    });
    kakao.maps.event.addListener(map.value, 'resize', () => {
            map.value.relayout();
        });
        kakao.maps.event.trigger(map.value, 'resize');


    loadGeoJSONFromServer('/data/인왕산ele copy.geojson');

    // 마커 추가 JSON 데이터 로드
    loadMarkers("/data/위험지역spot.geojson", '/images/danger.png'); // 마커 데이터 경로
    loadMarkers("/data/헬기장spot.geojson" , '/images/helipad.png');
    loadMarkers("/data/벤치spot.geojson", '/images/bench.png'); // 마커 데이터 경로
    loadMarkers("/data/2023산악사고_인왕산.geojson", '/images/danger.png'); // 마커 데이터 경로
  });
  script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=333bda7da18df138fb4d9b3e5cf351c4&autoload=false';
  document.head.appendChild(script);

}



// 비디오 시간과 경로 동기화 함수 (지도 초기화 이후에만 실행)
function syncCourseWithVideo(event) {
  if (!modalMap.value) {
    console.error("지도 객체가 초기화되지 않았습니다. modalMap이 null입니다.");
    return; // 지도 초기화 전에 동기화를 시도하면 중지
  }

  const currentTime = event.target.currentTime;  // 현재 재생 시간
  const totalDuration = event.target.duration;   // 전체 비디오 길이

  const progress = currentTime / totalDuration;  // 현재 재생 시간 비율
  const currentRouteIndex = Math.floor(progress * routeCoordinates.value.length);  // 경로 인덱스 계산

  highlightRouteOnMap(currentRouteIndex, modalMap.value);  // 모달 지도에서 커서 이동
}
// GeoJSON 데이터 로드 함수
async function loadGeoJSONFromServer(url, targetMap = map.value) {
  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const geojsonData = await response.json();
    processGeoJSON(geojsonData, targetMap);
  } catch (error) {
    console.error('GeoJSON 파일 로드 중 에러 발생:', error);
  }
}

// JSON 데이터를 사용하여 마커 추가
async function loadMarkers(url, imageSrc) {
  try {
    const response = await fetch(url); // JSON 파일에서 마커 정보를 가져오기
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const markerData = await response.json();
    console.log(`${url} 마커 데이터 로드 성공:`, markerData);

    // MNTN_NM이 "북한산", "도봉산", "수리산" 인 항목만 필터링
    const filteredMarkers = markerData.features.filter((feature) => {
      mountainName.value = feature.properties.MNTN_NM;

      return mountainName.value == '인왕산';
    });

    const imageSize = new kakao.maps.Size(35,45);
    const imageOpation = { offset: new kakao.maps.Point(12, 35)};
    const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imageOpation);

    // GeoJSON 데이터의 features 배열에서 마커 추가
    filteredMarkers.forEach((spot) => {
      const lat = spot.geometry.coordinates[1]; // 위도
      const lng = spot.geometry.coordinates[0]; // 경도
      const markerPosition = new kakao.maps.LatLng(lat, lng); // 마커 좌표

      // 마커 생성
      const marker = new kakao.maps.Marker({
        position: markerPosition, // 마커 위치
        map: map.value, // 마커를 표시할 지도 객체
        title: spot.properties.MNTN_NM, // 마커 제목
        image: markerImage,
      });

      // 마커에 대한 정보창 추가
      const infowindow = new kakao.maps.InfoWindow({
        content: `<div style="padding:5px;">${spot.properties.MNTN_NM}<br>${spot.properties.SAFE_SPOT2}</div>`, // 정보창 내용
      });

      // 마커에 마우스오버 이벤트 등록
      kakao.maps.event.addListener(marker, 'mouseover', () => infowindow.open(map.value, marker));
      kakao.maps.event.addListener(marker, 'mouseout', () => infowindow.close());
    });
  } catch (error) {
    console.error(`${url} 파일 로드 중 에러 발생:`, error);
  }
}

function processGeoJSON(geojsonData, targetMap) {
  let allCoordinates = [];

  geojsonData.features.forEach((feature) => {
    if (feature.properties.PMNTN_NM && feature.properties.PMNTN_NM.includes(courseData.value.courseName)) {
      let coordinates = [];

      // MultiLineString을 처리하기 위해 중첩 배열을 펼침
      if (feature.geometry.type === 'MultiLineString') {
        feature.geometry.coordinates.forEach(line => {
          coordinates = coordinates.concat(line.map((coord, index) => ({
            lng: coord[0],
            lat: coord[1],
            elevation: feature.properties.DN || 0,
            distance: calculateDistance(index, coord, line)
          })));
        });
      } else if (feature.geometry.type === 'LineString') {
        coordinates = feature.geometry.coordinates.map((coord, index) => ({
          lng: coord[0],
          lat: coord[1],
          elevation: feature.properties.DN || 0,
          distance: calculateDistance(index, coord, feature.geometry.coordinates)
        }));
      }

      allCoordinates = allCoordinates.concat(coordinates);
      addRouteLayer(coordinates, targetMap);
    }
  });

  if (allCoordinates.length > 0) {
    routeCoordinates.value = allCoordinates;
    drawElevationChart(allCoordinates);
  } else {
    console.log('유효한 구간 데이터가 없습니다.');
  }
}

// 경로 레이어 추가 함수
function addRouteLayer(coordinates, targetMap) {
  const linePath = coordinates.map((coord) => new kakao.maps.LatLng(coord.lat, coord.lng));

  if (targetMap === map.value) {
    polyline.value = new kakao.maps.Polyline({
      path: linePath,
      strokeWeight: 5,
      strokeColor: '#FF0000',
      strokeOpacity: 0.8,
      strokeStyle: 'solid',
    });
    polyline.value.setMap(map.value);
  } else if (targetMap === modalMap.value) {
    modalPolyline.value = new kakao.maps.Polyline({
      path: linePath,
      strokeWeight: 5,
      strokeColor: '#00FF00',
      strokeOpacity: 0.8,
      strokeStyle: 'solid',
    });
    modalPolyline.value.setMap(modalMap.value);
  }

  const bounds = new kakao.maps.LatLngBounds();
  coordinates.forEach((coord) => bounds.extend(new kakao.maps.LatLng(coord.lat, coord.lng)));
  targetMap.setBounds(bounds);
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

function drawElevationChart(data) {
  const ctx = document.getElementById('elevationChart').getContext('2d');

  // x축을 누적 거리로 설정하고, y축을 고도로 설정
  const distances = data.map(point => point.distance); // x축: 경로의 누적 거리
  const elevations = data.map(point => point.elevation); // y축: 고도

  // 기존 차트 인스턴스가 있으면 먼저 삭제 (에러 방지)
  if (chartInstance) {
    chartInstance.destroy(); // 기존 차트 제거
    chartInstance = null;
  }

  // 차트 새로 생성
  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels: distances, // x축: 누적 거리
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
      scales: {
        x: {
          title: {
            display: true,
            text: '거리 (m)', // x축 제목 (미터 단위로 표시)
          },
          ticks: {
            autoSkip: true, // 자동으로 레이블을 생략함
            maxTicksLimit: 10, // 최대 레이블 개수를 10개로 제한
            maxRotation: 0, // 최대 회전각
            minRotation: 0, // 최소 회전각
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
        },
      },
      interaction: {
        mode: 'index',
        intersect: false,
      },
      plugins: {
        tooltip: {
          callbacks: {
            label: function (context) {
              const index = context.dataIndex;
              highlightRouteOnMap(index); // 해당 인덱스를 지도에서 강조
              return `고도: ${context.parsed.y}m, 거리: ${context.parsed.x.toFixed(2)}m`;
            },
          },
        },
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

    currentMarker.value = new kakao.maps.Marker({
      position: new kakao.maps.LatLng(point.lat, point.lng),
    });

    currentMarker.value.setMap(targetMap);
    targetMap.panTo(new kakao.maps.LatLng(point.lat, point.lng));  // targetMap이 null이 아닐 때만 panTo 호출
  }
}

  // 날씨
  const mountainName = ref("");
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

async function requestCourses() {
  try {
    const response = await axios.get('/api/course');
    courses.value = response.data;
  } catch (error) {
    console.error('코스 목록 불러오기 오류:', error);
  }
}


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

onMounted(() => {
  fetchReviewsAndRatings();
  fetchSummary();
  requestCourses();
});

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

const accidentItems = [
  { image: 'images/solution1.png', content1: '알람.', content2: '긴급, 비상 서비스'},
  { image: 'images/solution2.png', content1: 'SOS 헬프콜', content2: '긴급, 비상 서비스'},
  { image: 'images/solution3.png', content1: '음성안내', content2: '위험지역 안내'},
  { image: 'images/solution4.png', content1: '민원신고', content2: '낙석, 파손 등 민원처리'},
];


</script>

<style scoped>

#sideBar{
    background-color: white;
    z-index: 2;
    width: 410px;
    height: 100vh; /* 높이 제한 설정 */
    overflow-y: auto;
    overflow-x: hidden;
}

#map {
  height: 600px;
  width: 100px; 
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
  margin-top: -1.5rem;
  margin-left: 5.5rem;
  font-family: 'TheJamsil4Bold';
  font-weight: 300;
  font-size: 14px;
  color: #333; /* 텍스트 색상을 진하게 */
  }

  .temperature-range {
  margin-top: -0.7rem;
  margin-left: 0.45rem;
  }

  .min-temp{
    font-family: 'TheJamsil4Bold';
    width: 80px;
    font-weight: 300;
    font-size: 14px;
    color:rgb(80, 80, 80);
    margin-top: 0.1rem;
    margin-left: 5rem;
    margin-right: 1rem;

  }

  .max-temp {
    font-family: 'TheJamsil4Bold';
    width: 80px;
    font-weight: 300;
    font-size: 14px;
    color:rgb(80, 80, 80);
    margin-top: 0.1rem;
    margin-left: 1rem;

  }

  .sun-info{
    font-family: 'TheJamsil3Bold';
    font-weight: 400;
    font-size: 16px;
    color:rgb(130, 130, 130);
    margin-top: 8.77rem;
    margin-left: 1rem;
  }

  .icon-small {
  width: 15px;
  height: 15px;
  vertical-align: middle;
  margin-right: 5px; /* 아이콘과 텍스트 간의 간격 추가 */
  opacity: 0.7;
  margin-top: -0.3rem;
  }

  .rain-info {
  display: flex;
  align-items: center;
  height: 10px;
  margin-top: 2.5rem;
  margin-right: 9rem;
  margin-left: 3.1rem;
  margin-bottom: -5rem;
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

  #sunrise{
    margin-top: -2.5rem;
    margin-bottom: -0.1rem;
    margin-left: 2.2rem;
    font-family: 'TheJamsil4Bold';
    font-weight: 300;
    font-size: 13px;
  }

  #sunset{
    margin-left: 2.2rem;
    margin-top: -3rem;
    margin-bottom: -0.5rem;
    font-family: 'TheJamsil4Bold';
    font-weight: 300;
    font-size: 13px;
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
    color: white;
    font-family: 'TheJamsil4Bold';
    font-size: 20px;
    font-weight: 500;
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
  width:347px;
  height: 70px;
  background-color: #fff;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0; /* 카드가 줄어들지 않고 고정된 크기로 유지 */
  margin-top: 1rem;
}

.card-img-top {
  position: absolute;
  top: 0;
  right: 0;
  width: 50px;
  height: 50px;
  margin: 0.5rem;
  object-fit: cover;
}

.card-overlay {
  position: absolute;
  left: 0;
  width: 100%;
  padding: 0.8rem;
  text-align: left;
}

.accident-item-content1 {
  color: #000;
  font-family: 'TheJamsil5Bold';
  font-size: 14px;
  font-weight: 500;
}

.accident-item-content2 {
  color: #000000;
  font-weight: 100;
  font-size: 16px;
  margin-top: -1rem;
}

</style>
