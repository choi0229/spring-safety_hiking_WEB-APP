<template>
  <HeaderView></HeaderView>

  <div class="main-content">
    <img src="/images/커뮤1.jpg" alt="Main Image" class="main-image" />
    <div class="text-overlay">
      <p class="large-text">Welcome to Our Store</p>
      <p class="small-text">Find what you need in your neighborhood</p>
    </div>
  </div>

  <div>
    <p style="font-size: 30px; font-weight: bold; margin-top: 2em; margin-left: 4em; ">이런 멘토는 어때요?</p>
  </div>

  <!-- 동그란 프로필 이미지 리스트 -->
  <div class="profile-container" @mouseover="pauseScroll" @mouseout="resumeScroll" style="margin-top: 3em;">
    <div class="profile-wrapper" ref="profileWrapper">
      <div
        v-for="(profile, index) in infiniteProfiles"
        :key="index"
        class="profile-item"
        @click="goToLink(profile.id)"
      >
        <img :src="profile.img" alt="Profile Image" class="profile-image" />
      </div>
    </div>
 </div>

 <div class="container d-flex flex-column align-items-center" style="margin-top: 4.5em;">

  <div>
    <p style="font-weight: bold; font-size: 30px;">다양한 Mentor 분들을 만나 볼 수 있어요!</p>
  </div>

  <!-- 버튼 목록 -->
  <div class="button-group">
    <button
      v-for="(hashtag, index) in hashtags"
      :key="index"
      :class="['category-btn', { active: activeHashtag === hashtag }]"
      @click="selectHashtag(hashtag)"
    >
      {{ hashtag }}
    </button>
  </div>

 <!-- 카드 목록 -->
 <div class="d-flex flex-wrap row-cols-1 row-cols-md-3 custom-gap">
    <div
      v-for="(card, index) in Cards"
      :key="index"
      class="card mb-3 mentorCard"
      style="width: 25rem; height: 190px; border: none;"
    >
      <div class="d-flex g-0" id="mentorCardDetail" style="margin-bottom: 1.5em;">
        <div>
          <img :src="card.img" class="" alt="Card Image" style="width: 90px; height: 90px; border-radius: 50%; object-fit: cover;" />
        </div>
        <div>
          <div class="card-body">
            <h5 class="card-title" style="font-weight: bold; font-size: 25px; margin-top: -0.3em;">{{ card.title }}</h5>
            <p class="card-text" style="font-size: 15px; opacity: 0.7;">{{ card.contentDetail }}</p>
            <smal style="font-weight: bold; font-size: 12px; opacity: 0.5;">{{ card.contentHashtags }}</smal>
          </div>
          <div class="row">
            <smal style="font-weight: bold; font-size: 12px; opacity: 0.8;">{{counts[card.contentId]||0}}/{{card.totalPerson}}</smal>
            <button @click="handleParticipation(card.contentId)" class="btn btn-secondary btn-sm">참여</button>
          </div>
        </div>
      </div>
    </div>
  </div>
  </div>

    <!-- 글쓰기 버튼 -->
    <div class="d-flex justify-content-end" style="margin: 20px;">
    <button @click="handleWriteClick" class="btn btn-primary">글쓰기</button>
  </div>

    <!-- 로그아웃 버튼 -->
    <div v-if="isLoggedIn" class="d-flex justify-content-end" style="margin: 20px;">
    <button @click="logout" class="btn btn-danger">로그아웃</button>
  </div>



</template>

<script setup>
import { ref,onMounted } from 'vue';
import HeaderView from '@/components/HeaderView.vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const profiles = [
  { id: 1, img: '/images/메인4.jpg', url: 'https://example1.com' },
  { id: 2, img: '/images/메인5.jpg', url: 'https://example2.com' },
  { id: 3, img: '/images/메인6.jpg', url: 'https://example3.com' },
  { id: 4, img: '/images/커뮤1.jpg', url: 'https://example3.com' },
  { id: 5, img: '/images/메인4.jpg', url: 'https://example1.com' },
  { id: 6, img: '/images/메인5.jpg', url: 'https://example2.com' },
  { id: 7, img: '/images/메인6.jpg', url: 'https://example3.com' },
  { id: 8, img: '/images/커뮤1.jpg', url: 'https://example3.com' },
  { id: 9, img: '/images/메인4.jpg', url: 'https://example1.com' },
  { id: 10, img: '/images/메인5.jpg', url: 'https://example2.com' },
  { id: 11, img: '/images/메인6.jpg', url: 'https://example3.com' },
  { id: 12, img: '/images/커뮤1.jpg', url: 'https://example3.com' },


  // 다른 프로필들
];

// 프로필 배열을 여러 번 복사하여 무한 루프처럼 보이게 함
const infiniteProfiles = [...profiles, ...profiles, ...profiles];

const profileWrapper = ref(null);

const pauseScroll = () => {
  profileWrapper.value.style.animationPlayState = 'paused';
};

const resumeScroll = () => {
  profileWrapper.value.style.animationPlayState = 'running';
};

const goToLink = (id) => {
  const profile = profiles.find((p) => p.id === id);
  if (profile && profile.url) {
    window.location.href = profile.url;
  }
};

const hashtags = ['#전체', '#등린이', '#운동', '#카페', '#헬스', '#필라테스'];
const activeHashtag = ref('#전체');


const Cards =ref([]);
const counts = ref({});

const fetchPosts = async () => {
  try {
    const response = await axios.get('/api/mentordetail/list');
    Cards.value = await response.data;
  } catch (error) {
    console.error('Error fetching posts:', error);
  }
};

const selectHashtag = (hashtag) => {
  activeHashtag.value = hashtag;
};

// 사용자 로그인 상태 (예시)
const isLoggedIn = ref(false);
const router = useRouter();

onMounted(() => {
  fetchPosts();
  requestCount();
  const userId = localStorage.getItem('userId');
  console.log("userId from localStorage:", userId);
  if (userId) {
    isLoggedIn.value = true;
  }
});

const handleWriteClick = () => {
  if (!isLoggedIn.value) {
    // 로그인하지 않았을 때 경고창 표시
    alert('로그인이 필요한 서비스입니다.');
    // 확인 버튼을 누르면 로그인 페이지로 이동
    router.push('/weblogin');
  } else {
    // 로그인한 상태에서 글쓰기 페이지로 이동
    router.push('/mentorposting');
  }
};

const logout = () => {
  // localStorage에서 로그인 정보 삭제
  localStorage.removeItem('userId');
  localStorage.removeItem('userNickName');
  localStorage.removeItem('userGender');
  
  // 로그아웃 후 로그인 페이지로 이동
  isLoggedIn.value = false;
  router.push('/weblogin');
};

const requestCount = async () => {
  try {
    const response = await axios.get('/api/mentordetail/count');
    const countMap = {};  // 빈 객체 생성
    response.data.forEach(item => {
      countMap[item.contentId] = item.countNum;  // contentId를 키로, countNum을 값으로 저장
    });
    counts.value = countMap;  // counts에 저장
  } catch (error) {
    console.error('Error fetching counts:', error);
  }
};

// 참여 버튼 클릭 이벤트
const handleParticipation = async (contentId) => {
  if (!isLoggedIn.value) {
    alert('로그인 후 참여가 가능합니다.');
    router.push('/weblogin');
  } else {
    try {
      await axios.post(`/api/mentordetail/participate/${contentId}`);
      // 참여 후 카운트 갱신
      requestCount();
    } catch (error) {
      console.error('Error participating:', error);
    }
  }
};

</script>

<style scoped>
.main-content {
  position: relative;
  text-align: center;
}

.main-image {
  width: 100%;
  height: 500px;
  object-fit: cover;
  opacity: 0.4;
}

.text-overlay {
  position: absolute;
  top: 250px;
  left: 50%;
  transform: translateX(-50%);
  color: #333;
}

.large-text {
  font-size: 2.5em;
  font-weight: bold;
}

.small-text {
  font-size: 1.2em;
  color: #777;
}

/* 프로필 이미지 리스트 */
.profile-container {
  margin-top: 50px;
  overflow: hidden;
  width: 100%;
}

.profile-wrapper {
  display: flex;
  animation: scroll-left 60s linear infinite; /* 스크롤 애니메이션 */
}

.profile-item {
  margin-right: 40px;
  opacity: 1;
  cursor: pointer; /* 커서를 포인터로 변경 */
}

.profile-image {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
}

/* 스크롤 애니메이션 */
@keyframes scroll-left {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}

.button-group {
  display: flex;
  gap: 10px;
  margin: 20px;
}

.category-btn {
  padding: 10px 20px;
  border-radius: 20px;
  background-color: #f0f0f0;
  border: none;
  cursor: pointer;
}

.category-btn.active {
  background-color: #333;
  color: #fff;
}
.card-header {
  padding: 0; /* 이미지와 카드 간격 제거 */
  margin-bottom: 10px;
}

.no-gap {
  padding: 0; /* 카드 사이의 공백을 제거 */
}

.d-flex {
  gap: 20px; /* 카드 사이의 공백을 추가 */
}

.mentorCard{
padding: 0.3em;
margin-left: 1em;
}

.mentorCard:hover{
  cursor:pointer;
  background-color: #f0f0f0;
}

.card-text{
    display: -webkit-box;   
    -webkit-box-orient: vertical;
    overflow: hidden;
    -webkit-line-clamp: 3; /* 제한할 텍스트의 줄 수 */
}

#mentorCardDetail{
  margin: 1em;
}



button {
  margin-top: 10px;
}
</style>
