<template>
  <div class="container">
    <div class="course-main">
      <!-- 뒤로가기 버튼 -->
      <button id="back-button" @click="goBack">
        <img src="/images/뒤로가기.png" alt="뒤로가기" />
      </button>

      <!-- 코스 정보 섹션 -->
      <div class="course-info">
        <div class="image-container">
          <img v-if="courseData?.courseImage" :src="courseData.courseImage" alt="Mountain Image" class="course-image" />
        </div>
        <div class="text-info">
          <div style="border-bottom: solid 1px #6c757d;">
            <span id="courseTitle" style="color: black; margin-right: 5px;">{{ courseData?.mountainName || '코스 정보' }} </span>
            <span class="image-course-name" style="font-weight: bold;">· {{ courseData?.courseName }}코스</span>
          </div>
          <p class="image-course-location">
            <img src="/images/기본.png" style="width: 16px; height: auto;">
            {{ courseData?.courseLocation }}
          </p>
          <p class="difficulty-text">
            <i class="bi bi-star-fill image-rating-star"></i> {{ averageRating }} · 난이도 {{ averageDifficultyText }} · 방문자 리뷰 300
          </p>
        </div>
      </div>

      <div class="button-container no-align">
        <button class="styled-button">
          <img src="/images/길.png" alt="">코스
        </button>
        <button class="styled-button">
          <img src="/images/3D_2.png" alt="">3D 코스
        </button>
        <button class="styled-button">
          <img src="/images/비디오.png" alt="">영상
        </button>
      </div>

      <div class="course-details" style="font-weight: bold;">
      <div class="mountain-info">
        <div class="mountain-info-item">
          <img src="/images/window산.png" alt="산 정보" class="mountain-info-icon" />
          <span class="mountain-info-title">{{ courseData?.mountainName || '코스 정보' }}은?</span>
        </div>
        <div class="mountain-info-content">
          <span class="mountain-info-description">{{ courseData?.courseContent || '코스 정보가 없습니다.' }}</span>
        </div>
      </div>
      <div class="info-container">
        <div class="info-item">
          <img src="/images/danger.png" alt="위험구간" class="info-icon" />
          <span class="info-title" style="font-weight: bold">위험구간</span>
          <span class="info-value">4곳</span>
        </div>
        <div class="info-item">
          <img src="/images/toilets.png" alt="화장실" class="info-icon" />
          <span class="info-title" style="font-weight: bold">화장실</span>
          <span class="info-value">2곳</span>
        </div>
      </div>

      <div class="info-container" style="font-weight: bold">
        <div class="info-item">
          <span class="info-title" style="font-weight: bold">난이도</span>
          <span class="info-value difficulty" :class="getDifficultyClass(averageDifficultyText)">{{ averageDifficultyText }}</span>
        </div>
        <div class="info-item">
          <span class="info-title" style="font-weight: bold">소요시간</span>
          <span class="info-value">{{courseData?.duration}}</span>
        </div>
        <div class="info-item">
          <span class="info-title" style="font-weight: bold">코스길이</span>
          <span class="info-value">{{courseData?.distance}}</span>
        </div>
        <div class="info-item">
          <span class="info-title" style="font-weight: bold">고도</span>
          <span class="info-value">600m</span>
        </div>
      </div>
    </div>
      
      <!-- 요약 및 별점 분포 -->
      <div class="summary-section">
        <!-- 제목 섹션 -->
        <div class="rating-header">
          <span id="courseTitle" style="color: black; margin-right: 5px;">
            {{ courseData?.mountainName || '코스 정보' }}
          </span>
          <span class="image-course-name" style="font-weight: bold;">· {{ courseData?.courseName }}코스</span>
        </div>

        <!-- 별점 평균 및 분포 섹션 -->
        <div class="rating-content">
          <!-- 별점 평균 섹션 -->
          <div class="rating">
            <div class="average-rating-text">{{ averageRating }}/5</div>
            <div class="stars">
              <div style="top: 5px;">
                <i v-for="star in fullStars" :class="'bi bi-star-fill'" :key="star"></i>
                <i v-if="halfStar" class="bi bi-star-half"></i>
                <i v-for="star in emptyStars" class="bi bi-star" :key="star"></i>
              </div>
            </div>
            <p class="review-count-text">최근리뷰 {{ reviewCount }}</p>
          </div>

          <!-- 별점 분포 섹션 -->
          <div class="rating-breakdown">
            <div v-for="(count, star) in ratingDistribution" :key="star" class="rating-bar">
              <span>{{ star }}점</span>
              <div class="progress-bar">
                <div class="progress" :style="{ width: (count / reviewCount) * 100 + '%' }"></div>
              </div>
              <span>({{ count || 0 }})</span>
            </div>
          </div>
        </div>
      </div>


        <!-- 리뷰 요약 -->
        <div class="review-summary">
          <p id="reviewTitle" style="font-weight: bold;">리뷰 한 줄 요약</p>
          <p class="review-ai-summary">{{ reviewSummary || 'AI가 리뷰를 요약중입니다.' }}</p>
          <small style="font-size: 10px;">(이 요약은 AI가 트레일 리뷰를 토대로 생성한 것이므로 항상 완벽하지는 않습니다.)</small>
        </div>

      <!-- 리뷰 작성 버튼 -->
      <button class="write-review-button" @click="showModal = true">리뷰 쓰기</button>

      <!-- 리뷰 페이지 (항상 표시) -->
      <div class="review-page">
        <div class="content-wrapper">
          <div class="review-search">
           
          </div>
          <div class="tag-buttons">
            <button class="btn btn-secondary btn-sm btn-custom">#쉬운</button>
            <button class="btn btn-secondary btn-sm btn-custom">#어려운</button>
            <button class="btn btn-secondary btn-sm btn-custom">#재미</button>
            <button class="btn btn-secondary btn-sm btn-custom">#코스</button>
            <button class="btn btn-secondary btn-sm btn-custom" style="width: 45px;">···</button>
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
              <span class="difficulty" :class="getDifficultyClass(review?.difficulty)">난이도: {{ review?.difficulty || '정보 없음' }}</span>
            </div>
          </div>
          <button 
            v-if="canShowMore" 
            @click="loadMoreReviews" 
            class="load-more-button">
            더 보기
            <img src="/images/더보기.png" alt="화살표 아이콘">
          </button>
        </div>
      </div>
    </div>

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

    <!-- 리뷰 수정 모달 -->
    <div v-if="editModal && currentReview" class="overlay" @click.self="editModal = false">
      <div class="review-form">
        <button class="close-button" @click="editModal = false">X</button>
        <h2>리뷰 수정</h2>

        <!-- 코스 선택 (수정 모달에서는 비활성화) -->
        <label class="modal-label">코스 선택</label>
        <select v-model="currentReview.courseId" class="input-field" disabled>
          <option :value="currentReview.courseId">{{ courseData.courseName }} 코스</option>
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
</template>


<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
import { useRoute } from 'vue-router';
import { useRouter } from "vue-router";
import { nextTick } from 'vue';

const route = useRoute();
const router = useRouter();

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
const courseData = ref({});
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

onMounted(async () => {
try {
  const response = await axios.get('/api/course');
  courses.value = response.data;
} catch (error) {
  console.error('코스 목록 불러오기 오류:', error);
}
});

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

const formatDate = (dateString) => {
const date = new Date(dateString);
return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
};

const goBack = () => router.back();

const getDifficultyClass = (difficulty) => {
  switch (difficulty) {
    case '쉬움':
      return 'difficulty-easy';
    case '보통':
      return 'difficulty-medium';
    case '어려움':
      return 'difficulty-hard';
    default:
      return 'difficulty-unknown'; // 기본값
  }
};
</script>

<style scoped>
/* 전체 레이아웃 설정 */
.container {
  height: 100vh; /* 전체 화면 높이 */
  overflow: hidden; /* 스크롤은 course-main에만 적용 */
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

/* 스크롤 가능한 영역 */
.course-main {
  flex: 1; /* 남은 공간을 모두 채우기 */
  overflow-y: auto; /* 세로 스크롤 활성화 */
  margin: 0;
  padding: 0;
}

.review-button {
background-color: #28a745;
color: white;
border: none;
padding: 15px;
margin-bottom: 20px;
font-size: 16px;
font-weight: bold;
border-radius: 25px;
cursor: pointer;
width: 100%;
transition: background-color 0.3s ease;
box-shadow: 0 3px 7px rgba(0, 0, 0, 0.5);
}

.review-button:hover {
background-color: #218838;
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
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  justify-content: center; 
  font-size: 14px; 
  font-weight: 500; 
  color: #6c757d; 
  background: none; 
  border: none; 
  cursor: pointer; 
  position: relative; 
  transition: color 0.3s ease;
  left: 140px;
}

.load-more-button:hover {
  color: #218838; 
}

.load-more-button img {
  width: 16px; 
  height: 16px;
  transition: transform 0.3s ease; 
}

.load-more-button:hover img {
  transform: translateY(4px); 
}

/* 버튼 아래 가로선 */
.load-more-button::after {
  content: "";
  display: block;
  width: 100%; 
  height: 1px; 
  background-color: #e0e0e0; 
  transition: background-color 0.3s ease; 
}

.load-more-button:hover::after {
  background-color: #218838; 
}

.course-info {
display: flex;
flex-direction: column;
align-items: center;
text-align: center;
}

.course-details {
font-family: 'TheJamsil4Bold';
font-size: 15px;
font-weight:300;
color: #555;
padding-top: 3px;
line-height: 1.7;
margin-top: 0.5rem;
border-top: solid 1px #ececec;
border-bottom: solid 1px #ececec;
}

.review-summary {
  text-align: center;
  background: white;
  padding: 20px;
  margin: 0 auto; /* 수평 중앙 정렬 */
  width: 300px;
  margin-top: 6px;
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
background: white;
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
border-bottom: 1px solid #e0e0e0;
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
color: #218838;
}

.review-photos img {
width: 100%;
height: auto;
max-height: 200px;
margin-top: 10px;
border-radius: 5px;
object-fit: cover;
}

/* 요약 섹션 (summary-section) */
.summary-section {
  background: white;
  border: 1px solid #e0e0e0;
  padding: 20px;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
}


.average-rating-text {
  font-size: 32px;
  font-weight: 700;
  color: black;
}

.review-count-text {
  font-size: 14px;
  color: #777;
}

/* 별점 스타일 */
.stars {
  display: flex;
  gap: 5px;
  color: #218838;
}

#reviewTitle {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.review-ai-summary {
  font-size: 14px;
  color: #555;
  margin-top: 5px;
}

.review-summary small {
  font-size: 12px;
  color: #999;
  display: block;
  margin-top: 10px;
}

/* 제목 섹션 스타일 */
.rating-header {
  border-bottom: 1px solid #6c757d;
  padding-bottom: 10px;
  margin-bottom: 15px; /* 아래 섹션과 간격 */
}

/* 별점 평균 및 분포 섹션 스타일 */
.rating-content {
  display: flex; /* 나란히 배치 */
  justify-content: space-between; /* 두 섹션 간격 */
  align-items: flex-start; /* 세로 정렬 기준 상단 */
  gap: 20px; /* 간격 추가 */
}

/* 별점 평균 섹션 */
.rating-container {
  flex: 1; /* 두 섹션이 동일한 공간을 차지하도록 */
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

/* 별점 평균 섹션 */
.rating {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  position: relative;
  top: 20px;
  left: 10px;
}

.rating-breakdown {
  flex: 2;
  display: flex;
  flex-direction: column;
}

.rating-bar {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

/* 별점 막대 스타일 */
.rating-bar span {
  width: 50px; /* 점수 텍스트 폭을 조금 넓게 조정 */
  text-align: right;
  font-size: 14px;
  font-weight: 500;
  color: #555;
}

.progress-bar {
  flex: 1; /* 남은 공간을 최대한 채우기 */
  background: #e0e0e0;
  border-radius: 4px;
  margin-left: 10px;
  height: 10px; /* 막대 높이를 약간 높게 조정 */
  overflow: hidden;
}

.progress {
  background: #218838;
  height: 100%;
  transition: width 0.3s ease;
}

.write-review-button {
  display: block; 
  margin: 20px auto; 
  width: 90%; 
  height: auto;
  background-color: #ffffff;
  color: black;
  font-size: 16px;
  font-weight: 700;
  text-align: center;
  border: solid 1px #218838;
  padding: 12px;
  cursor: pointer;
  transition: background-color 0.3s ease;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.2);
}

.write-review-button:hover {
  background-color: #388e3c;
  color: white;
}

.image-container {
position: relative;
width: 100%;
border: solid 1px #6c757d;
max-width: 600px;
margin: 0 auto;
}

.course-image {
width: 100%;
height: 200px;
object-fit: cover;
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
}

#courseTitle{
font-family: 'TheJamsil4Bold';
font-size: 25px;
font-weight: 700;
margin-top: 0.4rem;
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


/* 뒤로가기 버튼 */
#back-button {
  position: fixed;
  top: 10px;
  left: 10px;
  z-index: 100;
  border: none;
  cursor: pointer;
  background-color: white;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%; 
  box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.3); 
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.text-info {
  margin-top: 10px; 
  text-align: left; 
  width: 90%; 

}

.image-course-name {
  font-family: 'TheJamsil4Bold';
  font-size: 20px;
  font-weight: 500;
  margin: 5px 0;
  color: #333; /* 어두운 텍스트 */
}

.image-course-location {
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 400;
  margin: 5px 0;
  color: #555; 
}

.difficulty-text {
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 400;
  margin: 5px 0;
  color: #555; 
}

.image-rating-star {
  color: #4caf50; 
}

/* button-container 섹션: align-items 설정 제거 */
.button-container.no-align {
  display: flex;
  justify-content: space-around; /* 버튼 간격 균등 분배 */
  align-items: flex-start; /* 세로 정렬 제거 */
  padding: 10px 0;
  background-color: white;
  border-top: 1px solid #e0e0e0;
}

.styled-button {
  display: flex;
  flex-direction: column; /* 아이콘과 텍스트 세로 정렬 */
  align-items: center;
  justify-content: center;
  text-align: center;
  width: 70px; /* 버튼 크기 고정 */
  height: 70px;
  background-color: transparent; /* 배경 투명 */
  border: none; /* 경계선 제거 */
  cursor: pointer;
  color: #555; /* 기본 텍스트 색상 */
  font-size: 12px; /* 텍스트 크기 */
  font-weight: 500; /* 텍스트 두께 */
  transition: color 0.3s ease, transform 0.2s ease;
}

.styled-button img {
  width: 24px; /* 아이콘 크기 */
  height: 24px;
  margin-bottom: 5px; /* 아이콘과 텍스트 간격 */
}

.styled-button:hover {
  color: #218838; /* 호버 시 텍스트 색상 */
  transform: scale(1.1); /* 확대 효과 */
}

/* 전체 정보 섹션 컨테이너 */
.info-container {
  display: flex;
  justify-content: space-between; 
  align-items: center;
  border: 1px solid #e0e0e0; 
  background-color: #fff; 
  padding: 10px 15px; 
  margin-top: 3px;
}

/* 각 정보 아이템 스타일 */
.info-item {
  text-align: center; 
  flex: 1; 
}

/* 아이콘 스타일 */
.info-icon {
  width: 24px;
  height: 24px;
  margin-bottom: 5px; 
}

/* 제목 텍스트 스타일 */
.info-title {
  display: block; 
  font-size: 14px;
  font-weight: 500;
  color: #888888; 
  margin-bottom: 5px;
}

/* 값 텍스트 스타일 */
.info-value {
  font-size: 16px;
  font-weight: 700;
  color: #333; 
}

/* 쉬움 */
.difficulty-easy {
  background-color: #4caf50; /* 초록색 */
  color: white;
  padding: 5px 10px;
  border-radius: 12px;
  font-size: 14px;
  display: inline-block;
  border: solid 1px #28a745;
}

/* 보통 */
.difficulty-medium {
  background-color: #ffc107; /* 노란색 */
  color: white;
  padding: 5px 10px;
  border-radius: 12px;
  font-size: 14px;
  display: inline-block;
  border: solid 1px #fac422;
}

/* 어려움 */
.difficulty-hard {
  background-color: #f12719; /* 빨간색 */
  color: white;
  padding: 5px 10px;
  border-radius: 12px;
  font-size: 14px;
  display: inline-block;
}

/* 기본값 */
.difficulty-unknown {
  background-color: #bdbdbd; /* 회색 */
  color: white;
  padding: 5px 10px;
  border-radius: 12px;
  font-size: 14px;
  display: inline-block;
}

/* 산 정보 컨테이너 */
.mountain-info {
  display: flex;
  flex-direction: column; 
  align-items: center;
  border: 1px solid #e0e0e0; 
  background-color: #fff; 
  padding: 20px 15px; 
}

/* 산 정보 아이템 (아이콘과 제목) */
.mountain-info-item {
  text-align: center;
  margin-bottom: 10px;
}

/* 산 정보 아이콘 스타일 */
.mountain-info-icon {
  width: 24px;
  height: 24px;
  margin-bottom: 5px; 
}

/* 산 정보 제목 텍스트 스타일 */
.mountain-info-title {
  display: block; 
  font-size: 16px;
  font-weight: 700;
  color: #333; 
  margin-bottom: 10px; 
  border-bottom: solid 1px #218838;
}

/* 산 정보 설명 컨텐츠 */
.mountain-info-content {
  text-align: center;
}

/* 산 정보 설명 텍스트 스타일 */
.mountain-info-description {
  font-size: 14px;
  font-weight: 400;
  color: #555; 
  line-height: 1.5; 
}

/* 태그 버튼 설정 */
.tag-buttons {
  display: flex;
  gap: 5px;
  padding:5px;
  justify-content: center;
  background-color: #fafafa;
}

/* 버튼 스타일 */
.btn-custom {
  background-color: #ffffff;
  border-radius: 10px;
  border-color: transparent;
  border: solid 1px #327C2B;
  color: black;
  font-size: 14px; /* 글자 크기 */
  padding: 1px 8px; /* 높이와 좌우 여백 조정 */
}

.btn-custom:hover {
  transform: scale(1.05);
  background-color: #218838;
  cursor: pointer;
  color: white;
}

</style>