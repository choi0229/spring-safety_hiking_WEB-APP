<template>
  <HeaderView style="position: fixed; z-index: 4;width: 1920px;"></HeaderView>

  <!-- 메인 타이틀과 이미지 컨테이너 -->
  <div id="main-nav">
    <p id="mainTitle1" class="main-title">Let ' s find , 마루</p>
    <p id="mainTitle2">적당한 설명</p>
    <img id="mainImage1" class="main-image" src="/images/main2.jpg">
  </div>

  <!-- 검색기능 -->
  <div id="mainInput1" class="input-group">
    <input type="text" class="form-control rounded-pill" placeholder="🔭 산 이름이나 위치를 검색하세요..." aria-label="Recipient's username" aria-describedby="button-addon2" @focus="expandContainer($event)"
      v-model="searchQuery" @input="filterMountains">
  </div>

  <div id="inforTitle" class="d-flex flex-wrap custom-gab2">
    <div>
      <p id="helpTitle">등산 도우미</p>
      <p id="helpTitleContent1">등산에 도움이 되는 서비스를</p>
      <p id="helpTitleContent2">한곳에 모아봤어요.</p>
    </div>

    <div class="card" style="width: 10em; justify-content: center; align-items: center;">
      <img src="/images/메인4.jpg" style="object-fit: cover; width: 100%; height: 100%;" alt="">
      <p style="position: absolute; color: white; font-weight: bold; margin-top: 6.5em; margin-right: 3.5em;">대중교통</p>
    </div>
    <!-- 다른 카드들도 동일한 방식으로 유지 -->
  </div>

  <div style="margin-top: 10em; margin-left: 18.2em;">
    <span style="font-size: 25px; font-weight: lighter;">근처의 지역 인기 등산로</span><span style="font-size: 25px;"> 서울</span>
  </div>

  <!-- 등산로 코스 카드 -->
  <div class="d-flex row row-cols-1 row-cols-md-6 custom-gab" ref="cardsContainer">
    <div class="col card-wrapper" v-for="course in filteredCourses" :key="course.courseId">
      <div class="card mountainCourseCard" @click="goToMountainDetail(course)">
        <img :src="''+course.courseImage" id="mountainImage" class="card-img-top" alt="Mountain Image">
        <div class="card-body">
          <p id="mountain_name">{{ course.mountainName }}</p>
          <p id="mountain_name">{{ course.courseName }} 코스</p>
          <p id="mountain_location">{{ course.courseLocation }}</p>
          <p class="card-text" id="mountain_content">{{ course.courseContent }}</p>
        </div>
      </div>
    </div>
  </div>

  <div id="subTitle1">
    <p></p>
  </div>
</template>

<script setup>
import HeaderView from '@/components/HeaderView.vue';
import { ref, nextTick, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';


// Vue Router 사용 설정
const router = useRouter();

// 산 목록 관련 상태 및 함수들
const courses = ref([]); // 전체 산 목록 데이터
const searchQuery = ref(""); // 검색어 입력 값
const filteredCourses = ref([]); // 필터링된 산 목록

// 산 목록 데이터를 불러오는 함수
const fetchCourses = async () => {
  try {
    const response = await axios.get('/api/course');
    courses.value = response.data; // 전체 산 목록 저장
    filteredCourses.value = response.data; // 초기 필터링 목록 설정
  } catch (error) {
    console.error('산 목록을 불러오는 중 오류 발생:', error);
  }
};


// 검색어에 따라 산 목록을 필터링하는 함수
const filterMountains = () => {
  const query = searchQuery.value.toLowerCase();
  filteredCourses.value = courses.value.filter((course) => {
    return (
      course.courseName.toLowerCase().includes(query) ||
      course.courseLocation.toLowerCase().includes(query)
    );
  });
};

// 카드 클릭 시 호출되는 함수
const goToMountainDetail = (course) => {
  router.push({
    name: 'mountaindetail',
    query: {
      course: JSON.stringify(course), 
    },
  });
};

// 이미지 컨테이너의 높이를 변경하는 함수
const cardsContainer = ref(null);

const expandContainer = async (event) => {
  event.stopPropagation(); // 검색창 클릭 시 이벤트 전파 방지

  // 페이지 상단으로 이동
  window.scrollTo({
    top: 0,
    behavior: 'smooth' // 부드럽게 스크롤
  });

  // 스크롤이 완료될 때까지 대기한 후 실행
  setTimeout(async () => {
    // 스크롤 비활성화
    document.body.style.overflow = 'hidden';

    const mainTitle = document.getElementById('mainTitle1');
    const mainNav = document.getElementById('main-nav');
    const mainTitle2 = document.getElementById('mainTitle2');

    // 타이틀 숨기기
    mainTitle.style.opacity = 0;

    // 컨테이너 높이 증가
    mainNav.style.height = '800px';

    // 새로운 텍스트 보이기
    mainTitle2.style.opacity = 1;

    // nextTick을 사용하여 DOM이 업데이트된 후 카드에 접근
    await nextTick();

    // 카드들 천천히 사라지게 하기
    const cards = cardsContainer.value?.querySelectorAll('.card-wrapper');
    if (cards) {
      cards.forEach((card, index) => {
        setTimeout(() => {
          card.style.opacity = 0;
        }, index * 1); // 각 카드가 약간씩 지연되어 사라지도록 설정
      });
    }
  }, 100); // 스크롤이 부드럽게 완료될 시간을 고려하여 약간의 지연 설정 (100ms)
};

// 화면 아무 곳이나 클릭 시 초기 상태로 되돌리는 함수
const resetContainer = async (event) => {
  const inputElement = document.querySelector('#mainInput1 input');
  const imageElement = document.getElementById('mainImage1');
  if (event.target === inputElement || event.target === imageElement) {
    return; // 검색창 또는 이미지 클릭 시에는 컨테이너 초기화 방지
  }

  // 스크롤 활성화
  document.body.style.overflow = 'auto';

  const mainTitle = document.getElementById('mainTitle1');
  const mainNav = document.getElementById('main-nav');
  const mainTitle2 = document.getElementById('mainTitle2');

  // 타이틀 다시 보이기
  mainTitle.style.opacity = 1;

  // 컨테이너 높이 원래대로 줄이기
  mainNav.style.height = '630px';

  // 새로운 텍스트 숨기기
  mainTitle2.style.opacity = 0;

  // nextTick을 사용하여 DOM이 업데이트된 후 카드에 접근
  await nextTick();

  // 카드들 천천히 나타나게 하기
  const cards = cardsContainer.value?.querySelectorAll('.card-wrapper');
  if (cards) {
    cards.forEach((card, index) => {
      setTimeout(() => {
        card.style.opacity = 1;
      }, index * 230); // 각 카드가 약간씩 지연되어 나타나도록 설정
    });
  }
};

// 전역 클릭 이벤트 추가 및 제거
onMounted(() => {
  fetchCourses();
  document.addEventListener('click', resetContainer);
});

onUnmounted(() => {
  document.removeEventListener('click', resetContainer);
});
</script>

<style scoped>
/* 스타일 관련 부분은 사용자 정의로 필요 시 추가하세요. */
</style>
