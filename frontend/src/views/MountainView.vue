<template>
  <div class="review-summary">
    <div class="rating-summary">
      <h2>산와봤니 방문객들은 이렇게 말합니다.</h2>
      <div class="average-rating">
        <div class="rating-display">
          <!-- 평균 별점 -->
          <span class="rating-number">{{ averageRating }}</span>
          <span class="rating-star">
            <i class="bi bi-star-fill"></i>
          </span>
        </div>
        <p class="rating-text">평균 별점</p>
      </div>
    </div>

    <div class="review-ai-summary">
      <p>{{ reviewSummary }}</p>
      <small>이 요약은 AI가 트레일 리뷰를 토대로 생성한 것이므로 항상 완벽하지는 않습니다.</small>
    </div>
  </div>

  <div class="review-page">
    <div class="content-wrapper">
      <div class="summary-section">
        <div class="rating">
          <h1>{{ averageRating }}</h1>
          <div class="stars">
            <i v-for="star in fullStars" :class="'bi bi-star-fill'" :key="star"></i>
            <i v-if="halfStar" class="bi bi-star-half"></i>
            <i v-for="star in emptyStars" class="bi bi-star" :key="star"></i>
          </div>
          <p>{{ reviewCount }}개의 리뷰</p>
        </div>

        <div class="rating-breakdown">
          <div v-for="(count, star) in ratingDistribution" :key="star" class="rating-bar">
            <span>{{ star }}점</span>
            <i class="bi bi-star-fill"></i>
            <div class="progress-bar">
              <div class="progress" :style="{ width: (count / reviewCount) * 100 + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="reviews-section">
        <div class="review-search">
          <input type="text" placeholder="리뷰 검색" v-model="searchQuery" />
        </div>

        <!-- 리뷰 목록 -->
        <div class="reviews-list-wrapper">
          <div class="reviews-list" ref="reviewsList">

            <!-- 리뷰 항목 -->
            <div class="card review-card">
              <div v-for="(review, index) in visibleReviews" :key="index" class="review-item">
                <div class="review-content-wrapper">
                  <!-- 리뷰 헤더 -->
                  <div class="review-header">
                    <img class="profile-image" :src="review.profileImage || '/images/main1.jpg'" alt="Profile Image">
                    <div class="user-info">
                      <h3>{{ review?.userId || 'Unknown User' }}</h3>
                      <div class="stars-inline">
                        <i v-for="star in 5"
                           :class="{
                             'bi bi-star-fill': star <= Math.floor(review.rating),
                             'bi bi-star-half': star === Math.ceil(review.rating) && review.rating % 1 >= 0.5,
                             'bi bi-star': star > Math.ceil(review.rating)
                           }"
                           :key="star"></i>
                      </div>
                      <span class="review-date">{{ formatDate(review?.date) || '날짜 없음' }}</span>
                      
                    </div>
                  </div>

                  <!-- 리뷰 내용 -->
                  <p class="review-content">{{ review?.reviewContent  || '내용 없음' }}</p>

                  <!-- 리뷰 사진 -->
                  <div class="review-photos">
                    <img v-for="photo in review?.photos" :src="photo" :key="photo" alt="리뷰 사진" class="review-photo">
                  </div>

                  <span class="difficulty">난이도: {{ review?.difficulty || '정보 없음' }}</span>

                </div>
              </div>
            </div>

            <!-- '더 보기' 버튼 -->
            <button v-if="canShowMore" @click="loadMoreReviews">
              더 보기
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

const averageRating = ref(0);
const reviewSummary = ref('');
const reviews = ref([]);
const ratingDistribution = ref({});
const reviewCount = ref(0);
const searchQuery = ref('');
const reviewsToShow = ref(3); // 처음에 보여줄 리뷰 개수

const fetchReviewsAndRatings = async () => {
  try {
    const response = await axios.get('/api/course/1/reviews');
    console.log(response.data.reviews);
    reviews.value = response.data.reviews || [];
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
  try {
    const response = await axios.get(`/api/course/1/summarize`);
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
  return reviews.value.filter(review => {
    const reviewContent  = review?.reviewContent  || '';
    const userId = review?.userId || '';
    return reviewContent .toLowerCase().includes(searchQuery.value.toLowerCase()) || userId.toLowerCase().includes(searchQuery.value.toLowerCase());
  });
});

// 처음 3개의 리뷰를 표시하고 '더 보기' 클릭 시 3개씩 더 로드
const visibleReviews = computed(() => {
  return filteredReviews.value.slice(0, reviewsToShow.value);
});

const loadMoreReviews = () => {
  if (reviewsToShow.value + 3 <= filteredReviews.value.length) {
    reviewsToShow.value += 3; // 3개씩 추가로 보이게
  } else {
    reviewsToShow.value = filteredReviews.value.length; // 더 이상 없으면 전체 보여주기
  }
};

// '더 보기' 버튼을 언제까지 보여줄지 설정
const canShowMore = computed(() => {
  return reviewsToShow.value < filteredReviews.value.length;
});

const formatDate = (dateString) => {
  const date = new Date(dateString);
  const kstOffset = 9 * 60;
  const localDate = new Date(date.getTime() + kstOffset * 60 * 1000);
  const year = localDate.getFullYear();
  const month = (localDate.getMonth() + 1).toString().padStart(2, '0');
  const day = localDate.getDate().toString().padStart(2, '0');
  const hours = localDate.getHours().toString().padStart(2, '0');
  const minutes = localDate.getMinutes().toString().padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}`;
};
</script>


<style scoped>
.review-summary {
  text-align: center;
  margin-bottom: 20px;
}
.average-rating {
  font-size: 24px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 5px;
}
.stars {
  display: flex;
  align-items: center;
  gap: 2px;
}
.stars-inline {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.bi-star, .bi-star-fill, .bi-star-half {
  font-size: 20px; /* 별 사이즈 조정 */
}
.bi-star-fill, .bi-star-half {
  color: #4caf50;
}
.review-ai-summary {
  font-size: 16px;
  margin-top: 10px;
  padding-left: 20px;
  padding-right: 20px;
  word-wrap: break-word; /* 긴 문장이 한 줄에서 벗어나도록 처리 */
}
.review-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px;
  font-family: 'Noto Sans KR', sans-serif;
}
.content-wrapper {
  display: flex;
  justify-content: space-between;
  gap: 40px;
  max-width: 1200px;
  margin: 0 auto;
}
.summary-section {
  width: 35%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
}
.rating {
  text-align: center;
}
.rating h1 {
  font-size: 4rem;
  color: #4caf50;
}
.rating-breakdown {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.rating-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rating-bar span {
  width: 30px;
  text-align: right;
}
.progress-bar {
  width: 100px;
  height: 10px;
  background-color: #ddd;
  position: relative;
}
.progress {
  height: 100%;
  background-color: #4caf50;
}
.review-search input {
  padding: 10px;
  width: 100%;
  border: 1px solid #ddd;
  border-radius: 5px;
  margin-bottom: 20px;
}
.reviews-list-wrapper {
  max-height: 400px;
  overflow-y: auto;
  overflow-x: hidden;
}
.reviews-list {
  max-width: 100%;
}
.review-item {
  border-bottom: 1px solid #ddd;
  padding-bottom: 10px;
  word-wrap: break-word;
}
.review-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.profile-image {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}
.review-date {
  font-size: 17px;
  color: gray;
}
.difficulty {
  font-size: 14px;
  color: #777;
}
.review-photos {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}
.review-photo {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 5px;
}
button {
  padding: 10px 20px;
  border: none;
  background-color: #4caf50;
  color: white;
  font-size: 16px;
  cursor: pointer;
  border-radius: 5px;
  margin-top: 20px;
}
button:hover {
  background-color: #45a049;
}
.review-content-wrapper {
  margin: 1em;
}
.review-card {
  width: 1000px;
}
</style>
