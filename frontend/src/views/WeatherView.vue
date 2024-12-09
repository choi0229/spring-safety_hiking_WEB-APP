<template>
  <div class="review-form">
    <h2>리뷰 작성</h2>

    <!-- 코스 선택 -->
    <label for="course">코스 선택:</label>
    <select v-model="selectedCourse" id="course">
      <option value="" disabled>코스를 선택하세요</option>
      <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
        {{ course.courseName }}
      </option>
    </select>

    <!-- 별점 -->
    <div class="rating">
      <label>별점:</label>
      <div class="stars">
        <i v-for="star in 5" 
           :key="star" 
           :class="star <= rating ? 'bi bi-star-fill' : 'bi bi-star' "
           @click="setRating(star)">
        </i>
      </div>
    </div>

    <!-- 난이도 선택 -->
    <label for="difficulty">난이도:</label>
    <select v-model="difficulty" id="difficulty">
      <option value="" disabled>난이도를 선택하세요</option>
      <option value="쉬움">쉬움</option>
      <option value="보통">보통</option>
      <option value="어려움">어려움</option>
    </select>

    <!-- 리뷰 내용 -->
    <textarea v-model="reviewContent" placeholder="후기 내용을 입력하세요"></textarea>

    <!-- 사진 업로드 -->
    <label for="photos">사진 업로드:</label>
    <input type="file" multiple @change="onFileChange" />

    <!-- 제출 버튼 -->
    <button @click="submitReview">리뷰 작성</button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();

const selectedCourse = ref('');
const rating = ref(0);
const difficulty = ref('');
const reviewContent = ref('');
const photos = ref([]);
const courses = ref([]);

// 실제 로그인된 유저 아이디
const userId = "123"; 

const setRating = (star) => {
rating.value = star;
};

const onFileChange = (event) => {
photos.value = Array.from(event.target.files);
console.log(photos.value);
};

// 필수 항목 작성 여부 확인
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

// 리뷰 작성 요청 함수
const submitReview = async () => {
if (!validateForm()) {
  return; // 입력값이 부족하면 제출하지 않음
}

const review = {
  courseId: selectedCourse.value,
  userId: userId, 
  rating: rating.value,
  difficulty: difficulty.value,
  reviewContent: reviewContent.value,
};

console.log("리뷰 데이터:", review); 
console.log("사진 데이터:", photos.value); 

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
  router.push('/review');
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
</script>

<style scoped>
.review-form {
display: flex;
flex-direction: column;
gap: 10px;
max-width: 600px;
margin: 0 auto;
padding: 20px;
border: 1px solid #ddd;
border-radius: 8px;
background-color: #f9f9f9;
}
.stars {
display: flex;
cursor: pointer;
}
.bi-star-fill, .bi-star {
font-size: 24px;
color: #ffc107;
}
</style>
