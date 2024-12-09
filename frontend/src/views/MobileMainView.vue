<template>
  <div class="main-container">
    <!-- 공통 헤더 -->
    <HeaderView v-if="isDesktop" class="header" />
    <MobileHeaderView v-else class="header" />

    <!-- PC 버전 -->
    <div v-if="isDesktop" class="main-view">
      <!-- 첫 번째 섹션: "안전등산" -->
      <section class="ipad-section">
        <div class="text-section">
          <h1 id="ipad-title">안전등산</h1>
          <div id="ipad-Mainsubtitle">
            <p id="ipad-subtitle1">나, 너, 우리 모두가</p>
            <p id="ipad-subtitle2">안전한 산행을 하는 그날까지.</p>
          </div>
        </div>
        <div class="video-section">
          <video autoplay loop muted class="ipad-video">
            <source src="videos/hiking.mp4" type="video/mp4" />
          </video>
        </div>
      </section>

      <!-- 카드 섹션 -->
      <section class="scroll-effect-section">
        <p id="ipad-explore-title" ref="titleRef" class="hidden">알면 알수록, 안전하산.</p>
        <div class="carousel-container">
          <div class="card-carousel" ref="cardCarouselRef">
            <div
              class="card hidden"
              v-for="(course, index) in courses"
              :key="index"
              :ref="setCardRef"
              @click="goToMountainDetail(course)"
            >
              <img :src="course.courseImage" :alt="course.courseName" class="card-img-top" />
              <div class="card-overlay">
                <p class="card-title">{{ course.mountainName }}</p>
                <p class="card-content">{{ course.courseName }}</p>
                <p class="card-content2">{{ course.courseContent }}</p>
              </div>
            </div>
          </div>
          <div class="carousel-controls">
            <button class="carousel-control prev" @click="scrollLeft">＜</button>
            <button class="carousel-control next" @click="scrollRight">＞</button>
          </div>
        </div>
      </section>

      <!-- 라인업 섹션 -->
    <section class="scroll-effect-section lineup-section">
      <p id="lineup-title" ref="lineupTitleRef" class="hidden">함께하면 좋은 이유.</p>
      <div class="carousel-container">
        <div class="card-carousel2" ref="lineupCarouselRef">
          <div class="card2 hidden" v-for="(item, index) in lineupItems" :key="index" :ref="setLineupRef">
            <img :src="item.image" :alt="item.title" class="card-img-top2" />
            <div class="card-overlay2">
              <p class="lineup-item-content1">{{ item.content1 }}</p>
              <p class="lineup-item-content2">{{ item.content2 }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="carousel-container">
        <div class="card-carousel3" ref="lineupCarouselRef">
          <div class="card2 hidden" v-for="(item, index) in lineupItems2" :key="index" :ref="setSecondLineupRef">
            <img :src="item.image" :alt="item.title" class="card-img-top2" />
            <div class="card-overlay3">
              <p class="lineup-item-content1">{{ item.content1 }}</p>
              <p class="lineup-item-content2">{{ item.content2 }}</p>
            </div>
          </div>
        </div>
      </div>
      
    </section>

    <!-- 솔루션 섹션 -->
    <section class="solution-section">
      <p id="solution-title" ref="solutionTitleRef" class="hidden">그래서 안전하산은,</p>
      <div class="carousel-container">
        <div class="card-carousel4" ref="solutionCarouselRef">
          <div class="card3 hidden" v-for="(item, index) in solutionItems" :key="index" :ref="setSolutionRef">
            <img :src="item.image" :alt="item.title" class="solution-image" />
            <div class="card-overlay4">
              <p class="solution-item-content1">{{ item.content1 }}</p>
              <p class="solution-item-content2">{{ item.content2 }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

      <!-- 연간 분석 섹션 -->
      <section class="annual-analysis-section" ref="annualAnalysisRef">
        <h2 class="section-title">등산중 사고 원인</h2>
        <p class="section-description">
          산행 도중 사고의 선두주자로서 신뢰성 있는 정보와 서비스를 제공합니다.
        </p>
        <div class="stats-container">
          <div class="stat-item">
            <span class="count-up">{{ Math.floor(growthRate) }}</span>
            <p class="stat-title">실족 및 추락</p>
            <p class="stat-desc">소방청 연간 산악사고 자료중 일부 (2023년 말 기준)</p>
          </div>
          <div class="stat-item">
            <span class="count-up">{{ Math.floor(countries) }}</span>
            <p class="stat-title">길 잃음</p>
            <p class="stat-desc">소방청 연간 산악사고 자료중 일부 (2023년 말 기준)</p>
          </div>
          <div class="stat-item">
            <span class="count-up" >{{ Math.floor(research) }}</span>
            <p class="stat-title">개인질환</p>
            <p class="stat-desc">소방청 연간 산악사고 자료중 일부 (2023년 말 기준)</p>
          </div>
        </div>
      </section>
    </div>

    <!-- 모바일 버전 -->
    <div v-else class="content">
      <!-- 추천 코스 카드 -->
      <div class="card-container">
        <div class="main-card" @click="gotoMobileCourse">
          <img src="/images/climbing.png" alt="추천 코스 이미지" class="CourseCard" />
          <div>
            <p id="CourseText">오늘의 등산로</p>
            <p id="CourseText2">등산로 보기</p>
          </div>
        </div>
        <div class="side-cards">
          <div class="side-card" @click="gotoMobileComplaint">
            <img src="/images/complaint.png" alt="민원신청 이미지" class="ComplaintCard" />
            <div>
              <p id="ComplaintText">민원신청</p>
              <p id="ComplaintText2">불편,개선사항 접수</p>
            </div>
          </div>
          <div class="side-card" @click="gotoMobileHelpcall">
            <img src="/images/emergency.png" alt="긴급 신고 이미지" class="EmergencyCard" />
            <div>
              <p id="EmergencyText">긴급</p>
              <p id="EmergencyText2">SOS 신고</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 추가 기능 카드 -->
      <div class="additional-cards-container">
        <div class="Fitness-card" @click="gotoMobileRecording">
          <img src="/images/health.png" alt="등산 기록 이미지" class="Fitness-card-image" />
          <div>
            <p id="FitnessText">등산</p>
            <p id="FitnessText2">나만의</p>
            <p id="FitnessText3">등산기록</p>
          </div>
        </div>
        <div class="Community-card" @click="gotoMobileCommunity">
          <img src="/images/community.png" alt="커뮤니티 이미지" class="Community-card-image" />
          <div>
            <p id="CommunityText">커뮤니티</p>
            <p id="CommunityText2">#오등완</p>
            <p id="CommunityText3">#등린이</p>
          </div>
        </div>
      </div>

      <div class="profile-section">
        <p id="profile-text">어서와 등산은 처음이지?😚</p><p id="profile-text2">요즘 뜨는 등산멘토</p>
        <div class="profile-list">
          <div class="profile-circle">
            <img src="/images/프로필1.jpg" alt="프로필 사진" class="profile-image">
          </div>
          <div class="profile-circle">
            <img src="/images/프로필2.jpg" alt="프로필 사진" class="profile-image">
          </div>
          <div class="profile-circle">
            <img src="/images/프로필3.jpg" alt="프로필 사진" class="profile-image">
          </div>
          <div class="profile-circle">
            <img src="/images/프로필4.jpg" alt="프로필 사진" class="profile-image">
          </div>
          <div class="profile-circle">
            <img src="/images/프로필5.jpg" alt="프로필 사진" class="profile-image">
          </div>
          <div class="profile-circle">
            <img src="/images/프로필2.jpg" alt="프로필 사진" class="profile-image">
          </div>
        </div>
      </div>

      <!-- 핫 게시물 -->
      <div class="hot-posts-section">
        <p id="hot-post-text">지금 가장 핫한 게시물 🔥</p>
        <div class="hot-posts-list">
          <div class="hot-post-card" v-for="i in 3" :key="i">
            <img src="/images/climbing.png" alt="게시물 이미지" class="hot-post-image" />
            <div class="hot-post-text">게시물 제목</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 공통 푸터 -->
    <MobileFooterView v-if="!isDesktop" />
  </div>
</template>

<script setup>
import HeaderView from "@/components/HeaderView.vue";
import MobileHeaderView from "@/components/MobileHeaderView.vue";
import MobileFooterView from "@/components/MobileFooterView.vue";
import { ref, onMounted, onUnmounted, nextTick } from "vue";
import axios from "axios";
import { useRouter } from "vue-router";

// Vue Router 설정
const router = useRouter();

// 반응형 상태
const isDesktop = ref(window.innerWidth > 1024);

// PC 전용 상태 및 로직
const titleRef = ref(null);
const cardRefs = ref([]);
const lineupRefs = ref([]);
const secondLineupRefs = ref([]);
const lineupTitleRef = ref(null);

const solutionTitleRef = ref(null);
const solutionRefs = ref([]);
const courses = ref([]); // 전체 산 목록 데이터

// 숫자 값을 저장할 ref 변수
const growthRate = ref(0);
const countries = ref(0);
const research = ref(0);

// 목표 숫자
const targetGrowthRate = 3186; // 예: 실족 및 추락
const targetCountries = 2842;  // 예: 길 잃음
const targetResearch = 1075;   // 예: 개인질환

// 섹션 참조
const annualAnalysisRef = ref(null);

// IntersectionObserver 상태
let hasStartedCounting = false;

// IntersectionObserver를 설정하여 섹션이 화면에 보일 때 카운트 업 시작
const startCountOnView = () => {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting && !hasStartedCounting) {
          hasStartedCounting = true; // 중복 실행 방지
          countUp(targetGrowthRate, growthRate);
          countUp(targetCountries, countries);
          countUp(targetResearch, research);
          observer.disconnect(); // 한 번 실행 후 관찰 중지
        }
      });
    },
    { threshold: 0.9 } // 50% 이상 섹션이 보일 때 트리거
  );

  if (annualAnalysisRef.value) {
    observer.observe(annualAnalysisRef.value);
  }
};

// 숫자 증가 함수
const countUp = (target, currentRef, duration = 2000) => {
  const increment = target / (duration / 16); // 프레임당 증가량 (16ms: 약 60fps)
  const interval = setInterval(() => {
    if (currentRef.value >= target) {
      currentRef.value = target; // 목표값으로 정확히 설정
      clearInterval(interval); // 인터벌 종료
    } else {
      currentRef.value += increment; // 값 증가
    }
  }, 16);
};

const setCardRef = (el) => {
  if (el) cardRefs.value.push(el);
};

const setLineupRef = (el) => {
  if (el) lineupRefs.value.push(el);
};

const setSecondLineupRef = (el) => {
  if (el) secondLineupRefs.value.push(el);
};

const setSolutionRef = (el) => {
  if (el) solutionRefs.value.push(el);
};

// IntersectionObserver 설정
const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        if (entry.target === titleRef.value || entry.target === lineupTitleRef.value) {
          // 타이틀 관련 애니메이션
          entry.target.classList.add("visible");
        } else if (entry.target === annualAnalysisRef.value) {
          // 연간 분석 섹션 카운트 업 실행
          if (!hasStartedCounting) {
            hasStartedCounting = true; // 중복 실행 방지
            countUp(targetGrowthRate, growthRate);
            countUp(targetCountries, countries);
            countUp(targetResearch, research);
          }
        } else {
          // 카드 섹션 애니메이션
          const cardIndex = cardRefs.value.indexOf(entry.target);
          if (cardIndex !== -1) {
            // 카드 섹션 애니메이션
            setTimeout(() => entry.target.classList.add("visible"), cardIndex * 200);
          }

          // 라인업 섹션 애니메이션
          const lineupIndex = lineupRefs.value.indexOf(entry.target);
          if (lineupIndex !== -1) {
            setTimeout(() => entry.target.classList.add("visible"), lineupIndex * 200);
          }

          // 라인업 두 번째 카드 애니메이션
          const secondLineupIndex = secondLineupRefs.value.indexOf(entry.target);
          if (secondLineupIndex !== -1) {
            setTimeout(() => entry.target.classList.add("visible"), secondLineupIndex * 200);
          }
        }

        // 관찰 중지
        observer.unobserve(entry.target);
      }
    });
  },
  { threshold: 0.5 } // 섹션이 50% 보일 때 트리거
);


// 솔루션 섹션 IntersectionObserver
const solutionObserver = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("visible");
        solutionObserver.unobserve(entry.target);
      }
    });
  },
  { threshold: 0.5 }
);



// 모바일 전용 상태 및 로직
const userNickName = ref("");
const userGender = ref("");
const profileImage = ref("");

const requestNotificationPermission = async () => {
  if (window.Android) {
    let isTokenRequested = false;
    let permissionGranted = false;
    let token = "";

    window.Android.onNotificationPermissionGranted = (data) => {
      try {
        const parsedData = JSON.parse(data);
        token = parsedData.token || "";
        permissionGranted = parsedData.permissionGranted || false;

        if (permissionGranted && token && !isTokenRequested) {
          isTokenRequested = true;
          console.log("FCM 토큰:", token);
          saveTokenAndPermission(token, permissionGranted).then(() => {
            window.location.href = "/helpcall";
          });
        } else {
          console.error("알림 권한이 거부되었거나 FCM 토큰이 없습니다.");
        }
      } catch (error) {
        console.error("FCM 토큰 수신 중 오류:", error);
      }
    };

    window.Android.fetchFCMToken();
  }
  window.location.href = "/helpcall"; // 임시용 버튼
};

const saveTokenAndPermission = async (token) => {
  const userId = localStorage.getItem("userId");
  try {
    const response = await axios.post("/api/saveNotificationData", {
      userId: userId,
      token: token,
    });

    if (response.status === 200) {
      return true;
    } else {
      return false;
    }
  } catch (error) {
    return false;
  }
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


const cardCarouselRef = ref(null);

// 좌측으로 스크롤 이동
const scrollLeft = () => {
  if (cardCarouselRef.value) {
    const currentScroll = cardCarouselRef.value.scrollLeft;
    cardCarouselRef.value.scrollTo({
      left: currentScroll - 350,
      behavior: 'smooth'
    });
  }
};

// 우측으로 스크롤 이동
const scrollRight = () => {
  if (cardCarouselRef.value) {
    const currentScroll = cardCarouselRef.value.scrollLeft;
    cardCarouselRef.value.scrollTo({
      left: currentScroll + 350,
      behavior: 'smooth'
    });
  }
};

// 페이지 이동 함수
const gotoMobileCourse = () => (window.location.href = "/mobilecourseview");
const gotoMobileComplaint = () => (window.location.href = "/complaintListMobile");
const gotoMobileHelpcall = () => {
  window.location.href = "/helpcall";
  requestNotificationPermission();
};
const gotoMobileRecording = () => (window.location.href = "/record");
const gotoMobileCommunity = () => (window.location.href = "/mobilecommunity");

onMounted(async () => {
  // 페이지 로드 시 최상단으로 이동
  window.scrollTo(0, 0);
  startCountOnView();

  // 모바일 사용자 정보 로드
  userNickName.value = localStorage.getItem("userNickName");
  userGender.value = localStorage.getItem("userGender");

  const userId = localStorage.getItem("userId");
  const storedProfileImage = localStorage.getItem(`profileImage_${userId}`);
  if (storedProfileImage) {
    profileImage.value = storedProfileImage;
  }

  // nextTick을 사용하여 DOM 렌더링 완료 후 작업
  await nextTick();

  // PC 버전 로직
  if (isDesktop.value) {
    // API 요청으로 카드 데이터 불러오기
    await requestCourses();

    // Observer를 통한 요소 관찰
    if (titleRef.value) observer.observe(titleRef.value);
    if (lineupTitleRef.value) observer.observe(lineupTitleRef.value);
    if (annualAnalysisRef.value) observer.observe(annualAnalysisRef.value);

    // 카드 섹션 관찰
    cardRefs.value.forEach((card) => observer.observe(card));

    // 라인업 섹션 관찰
    lineupRefs.value.forEach((lineupCard) => observer.observe(lineupCard));
    secondLineupRefs.value.forEach((secondLineupCard) => observer.observe(secondLineupCard));

    // 솔루션 섹션 관찰
    if (solutionTitleRef.value) solutionObserver.observe(solutionTitleRef.value);
    solutionRefs.value.forEach((card, index) => {
      setTimeout(() => solutionObserver.observe(card), index * 100);
    });

    // 스크롤 이벤트 추가
    window.addEventListener("scroll", handleScroll);
  }

  // 반응형 상태 업데이트 함수
  const updateLayout = () => {
    isDesktop.value = window.innerWidth > 1024;
  };

  // 초기 반응형 상태 업데이트 호출
  updateLayout();

  // 반응형 상태와 스크롤 이벤트 리스너 추가
  window.addEventListener("resize", updateLayout);

  // 컴포넌트 언마운트 시 이벤트 제거
  onUnmounted(() => {
    window.removeEventListener("resize", updateLayout);
    window.removeEventListener("scroll", handleScroll);
  });
});

// PC 전용 스크롤 이벤트
const handleScroll = () => {
  // 비디오 섹션 및 비디오 요소를 ref로 관리
  const videoSection = document.querySelector(".video-section");
  const video = document.querySelector(".ipad-video");

  if (videoSection && video) {
    if (window.scrollY > 150) {
      videoSection.classList.add("scrolled");
    } else {
      videoSection.classList.remove("scrolled");
      video.style.transform = "scale(1)";
    }
  } else {
    console.warn("videoSection 또는 video 요소를 찾을 수 없습니다.");
  }
};

const lineupItems = [
  { image: 'images/interview1.png', content1: '처음가보는 산에 관한 정보를 얻기 위해 수많은 블로그 글을 찾아봐야해요', content2: '32세,박태호 / 주말마다 등산'},
  { image: 'images/interview4.png', content1: '정비가 잘 되어있지 않은곳은 사고가 발생할 가능성이 많고, 각 등산로에 대한 위험요소를 알기 어려워요.', content2: '46세,김강산 / 2주에 한번 등산'},
  { image: 'images/interview3.png', content1: '타 등산 앱 서비스를 사용해 봤는데 기능이 너무 많고, 복잡해서 혼란스러웠어요.', content2: '28세,이진호 / 한달에 3번 등산'},
];

const lineupItems2 = [
  { image: 'images/interview2.png', content1: '오후에 야등하려고 했는데, 여자 혼자라 너무 위험할 것 같아요.', content2: '26세,신민아 / 1주에 한번 등산'},
  { image: 'images/interview5.png', content1: '내 활동량을 기록해 줄 수 이는 수단이 있었으면 좋겠어요.', content2: '39세,윤여진 / 주말마다 등산'},
  { image: 'images/interview6.png', content1: '처음가보는 산에 관한 정보를 얻기 위해 수많은 블로그 글을 찾아봐야해요', content2: '25세,고경표 / 1주에 두번 등산'},
];

const solutionItems = [
  { image: 'images/solution1.png', content1: '알람.', content2: '긴급, 비상 서비스'},
  { image: 'images/solution2.png', content1: 'SOS 헬프콜', content2: '긴급, 비상 서비스'},
  { image: 'images/solution3.png', content1: '음성안내', content2: '위험지역 안내'},
  { image: 'images/solution4.png', content1: '민원신고', content2: '낙석, 파손 등 민원처리'},
];

// 데이터 요청 함수
const requestCourses = async () => {
  try {
    const response = await axios.get("/api/course");
    courses.value = response.data;
  } catch (error) {
    console.error("산 목록을 불러오는 중 오류 발생:", error);
  }
};
</script>

<style scoped>
@font-face {
  font-family: 'TheJamsil5Bold';
  src: url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2') format('woff2');
  font-weight: 700;
  font-style: normal;
}

@media (min-width: 1024px){

  *::-webkit-scrollbar{
    display: none; /* 스크롤바 가리기 */
  }

/* 스크롤바 숨기기 (크롬, 사파리 등 Webkit 기반 브라우저) */
.card-carousel::-webkit-scrollbar {
  display: none; /* 스크롤바 가리기 */
}

.card-carousel2::-webkit-scrollbar {
  display: none; /* 스크롤바 가리기 */
}

.card-carousel3::-webkit-scrollbar {
  display: none; /* 스크롤바 가리기 */
}

.card-carousel4::-webkit-scrollbar {
  display: none; /* 스크롤바 가리기 */
}

.main-container::-webkit-scrollbar {
  display: none;
}

/* 전체 레이아웃 설정 */
.main-view {
  font-family: TheJamsil;
  font-weight: 100; 
}

.main-view {
  max-width: 1900px;      /* 최대 너비 */
  max-height: 920px;      /* 최대 높이 */
  min-width: 1840px;      /* 최소 너비 */
  min-height: 900px;      /* 최소 높이 */
  width: 100%;
  height: 100%;
  margin: 0 auto;         /* 가운데 정렬 */
}

/* ipad-section 스타일 */
.ipad-section {
  justify-items: center;
  align-items: center;
  justify-content: space-between;
  padding: 2rem;
  background-color: #f8f9fa;
  height: 120vh;
  position: relative;
}

/* 텍스트 섹션 스타일 */
.text-section {
  display: flex;
  margin-top: 3.5rem;
  gap: 68rem;
  margin-bottom: 4rem;
}

#ipad-title {
  font-weight: 500;
  font-size: 75px;
}

/* ipad-Mainsubtitle을 세로 정렬하고 오른쪽 정렬 */
#ipad-Mainsubtitle {
  display: flex;
  flex-direction: column;
  text-align: left;
  margin-top: 1rem;
}

#ipad-subtitle1,
#ipad-subtitle2 {
  font-size: 23px;
  font-weight: 400;
}

#ipad-subtitle1,
#ipad-subtitle2{
  margin-bottom: -0.3rem;
}


/* 비디오 섹션 스타일 */
.video-section {
  width: 90%;
  position: relative;
  transition: width 0.5s ease, border-radius 0.5s ease;
  transform-origin: center center; /* 중심 기준으로 크기 변화 */
}

/* 스크롤 효과 - 모서리 둥글기 및 크기 조정 */
.ipad-video {
  object-fit: cover;
  width: 1920px;
  height: auto;
  max-height: 80vh;
  border-radius: 10px; /* 초기 모서리 둥글기 */
  transition: all 1.5s ease;
  transform-origin: center center; /* 중심 기준으로 크기 변화 */
  margin-left: -8.3rem;
}

.video-section.scrolled .ipad-video {
  width: 1500px;
  transform: scale(0.5); /* 크기 축소를 중심을 기준으로 조절 */
  border-radius: 50px; /* 스크롤 시 최대 둥글기 */
  margin-left: 4.5rem;
  transform-origin: center; /* 비디오의 중심을 기준으로 크기 변화 */
}

/* 스크롤 효과로 줄어드는 비디오 */
.scroll-effect-section {
  padding: 2rem;
  margin-top: 3rem;
  transform-origin: center; /* 비디오의 중심을 기준으로 크기 변화 */
}

#ipad-explore-title {
  font-size: 70px;
  font-weight: 500;
  margin-left: 9.5rem;
}

.carousel-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-direction: column;
}

.card-carousel {
  display: flex;
  gap: 1.4rem;
  overflow-x: auto;
  overflow-y: hidden;
  scroll-behavior: smooth;
  width: 100%; /* 전체 폭을 차지 */
  margin-top: 4rem;
  margin-left: 22rem;
}

.card {
  position: relative;
  width: 420px;
  height: 700px;
  background-color: #fff;
  border-radius: 20px;
  overflow: hidden;
  text-align: center;
  flex-shrink: 0; /* 카드가 줄어들지 않고 고정된 크기로 유지 */
}

.card-img-top {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-overlay {
  position: absolute;
  left: 0;
  width: 100%;
  color: #fff;
  padding: 2rem;
  text-align: left;
}

.card-title {
  font-weight: 500;
  font-size: 20px;
}

.card-content{
  font-weight: 400;
  font-size: 30px;
  margin-bottom: -0.5rem;
}

.card-content2{
  font-weight: 400;
  font-size: 30px;
}


.carousel-control {
  background: #e6e9e9;
  color: #8b8b8b;
  border: none;
  font-size: 14px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  cursor: pointer;
  margin: 0 10px;
  font-weight: 500; 
}

/* 하단 중앙에 배치된 좌우 스크롤 버튼 */
.carousel-controls {
  display: flex;
  justify-content: center;
  margin-top: 4rem;
  margin-left: 90rem;
  margin-bottom: 6rem;
}

.prev {
  left: -50px;
}

.next {
  right: -50px;
}

.lineup-section {
  padding: 2rem;
  background-color: #f8f9fa;
  margin-top: -0.1rem;
}

#lineup-title {
  font-size: 70px;
  font-weight: 500;
  margin-left: 9.5rem;
  margin-top: 10rem;
  margin-bottom: 3rem;
}

.card-carousel2 {
  display: flex;
  gap: 1.4rem;
  overflow-x: auto;
  overflow-y: hidden;
  scroll-behavior: smooth;
  width: 100%; /* 전체 폭을 차지 */
  margin-top: 4rem;
  margin-left: 45rem;
}

.card-carousel3 {
  display: flex;
  gap: 1.4rem;
  overflow-x: auto;
  overflow-y: hidden;
  scroll-behavior: smooth;
  width: 100%; /* 전체 폭을 차지 */
}

.card-carousel4 {
  display: flex;
  gap: 1.4rem;
  overflow-x: auto;
  overflow-y: hidden;
  scroll-behavior: smooth;
  width: 100%; /* 전체 폭을 차지 */
}

.card-overlay2 {
  position: absolute;
  left: 0;
  width: 100%;
  color: #fff;
  padding: 4rem;
  text-align: left;
}

.card-overlay3 {
  position: absolute;
  left: 0;
  width: 100%;
  color: #fff;
  padding: 4rem;
  text-align: left;
}

.card2 {
  position: relative;
  width: 500px;
  height: 320px;
  background-color: #fff;
  border-radius: 20px;
  overflow: hidden;
  text-align: center;
  flex-shrink: 0; /* 카드가 줄어들지 않고 고정된 크기로 유지 */
  margin-bottom: 3rem;
}

.card-img-top2 {
  position: absolute;
  top: 0;
  right: 0;
  margin-top: 5.5rem;
  margin-right: 0.5rem;
  width: 170px;
  height: 170px;
  object-fit: cover;
}


.lineup-item-content1 {
  color: #000;
  font-weight: 200;
  font-size: 20px;
  width: 270px;
}

.lineup-item-content2 {
  position: fixed;
  bottom: 0;
  right: 0;
  color: #737373;
  font-weight: 100;
  font-size: 16px;
  width: 300px;
  margin-right: 8.4rem;
  margin-bottom: 3.7rem;
}

.lineup-item-desc {
  font-size: 16px;
  color: #666;
}

/* 솔루션 섹션 스타일 */
.best-match-section {
  padding: 2rem;
  text-align: center;
}

#best-match-title {
  font-size: 35px;
  font-weight: 400;
}

.best-match-cards {
  display: flex;
  gap: 1rem;
}

.best-match-card {
  background-color: #f8f9fa;
  border-radius: 10px;
  text-align: center;
}

/* 연간 분석 섹션 스타일 */
.annual-analysis-section {
  background-color: #36672f;
  color: #fff;
  text-align: center;
  padding: 4rem 0;
}

.section-title {
  font-size: 2rem;
  font-weight: bold;
  margin-bottom: 1rem;
}

.section-description {
  font-size: 1rem;
  color: #aaa;
  margin-bottom: 2rem;
}

.stats-container {
  display: flex;
  justify-content: center;
  gap: 2rem;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.count-up {
  display: inline-block;
  font-size: 2rem;
  font-weight: bold;
  transition: transform 0.2s ease-in-out;
}

.count-up.updated {
  transform: scale(1.2);
}

.stat-title {
  font-size: 1.5rem;
  font-weight: bold;
  margin-top: 0.5rem;
}

.stat-desc {
  font-size: 1rem;
  color: #aaa;
  margin-top: 0.5rem;
}

/* 솔루션 섹션 스타일 */
.solution-section {
  padding: 4rem;
  text-align: center;
  background-color: #f8f9fa;
  transition: opacity 0.6s ease, transform 0.6s ease;
}

#solution-title {
  color: #000;
  font-size: 70px;
  font-weight: 500;
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.6s ease, transform 0.6s ease;
  margin-left: 68rem;
}

#solution-title.visible {
  opacity: 1;
  transform: translateY(0);
}

.carousel-container {
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.card-carousel4 {
  display: flex;
  gap: 1.4rem;
  overflow-x: auto;
  scroll-behavior: smooth;
  width: 100%;
  padding: 7rem;
  margin-left: 5rem;
}

.card3 {
  position: relative;
  width: 350px;
  height: 400px;
  background-color: none;
  border-radius: 20px;
  overflow: hidden;
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.card3.visible {
  opacity: 1;
  transform: translateY(0);
}

.solution-image {
  position: absolute;
  top: 0;
  right: 0;
  margin-top: 1rem;
  margin-right: 1rem;
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius:50%;

}

.card-overlay4 {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #000;
  text-align: center;
}

.solution-item-content1 {
  font-weight: 500;
  font-size: 25px;
  width: 270px;
}

.solution-item-content2 {
  color: #2e2e2e;
  font-weight: 200;
  font-size: 18px;
  width: 270px;
}
.hidden {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.visible {
  opacity: 1;
  transform: translateY(0);
}
}

@media (max-width: 1023px) {
  html, body {
    width: 360px;
    height: 740px;
    margin: 0;
    padding: 0;
    overflow: hidden;
  }

  .main-container {
    width: 360px;
    height: 740px;
    height: 100vh;
    overflow: auto;
    margin: 0 auto;
    position: relative;
    -ms-overflow-style: none; /* 인터넷 익스플로러 */
    scrollbar-width: none; /* 파이어폭스 */
  }
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding-top: 70px;
  padding-bottom: 70px;
  -ms-overflow-style: none; /* 인터넷 익스플로러 */
  scrollbar-width: none; /* 파이어폭스 */
}

/* 카드 스타일 */
.card-container {
  display: flex;
  gap: 10px;
  padding: 1rem;
}

.main-card {
  flex: 2;
  position: relative;
  overflow: hidden;  
}

.side-cards {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.side-card, .main-card {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
}

.CourseCard{
  width: 10rem;
  height: 17rem;
  object-fit: cover;
  border-radius: 5px;
}

.CourseCard-image{
  width: 165%;
  height: 170%;
  object-fit: cover;
}

.ComplaintCard{
  width: 10rem;
  height: 10rem;
  object-fit: cover;
}

.EmergencyCard{
  width: 10rem;
  height: 6.5rem;
  object-fit: cover;
}

#CourseText {
  position: absolute;
  top: 10px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 17px;
  font-weight: 700;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#CourseText2 {
  position: absolute;
  top: 34px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 300;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#ComplaintText {
  position: absolute;
  top: 10px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 17px;
  font-weight: 700;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#ComplaintText2 {
  position: absolute;
  top: 34px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 300;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#EmergencyText {
  position: absolute;
  top: 10px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 17px;
  font-weight: 700;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#EmergencyText2 {
  position: absolute;
  top: 34px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 300;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

/* 추가 카드 컨테이너 (가로 정렬) */
.additional-cards-container {
  display: flex;
  gap: 10px; /* 추천 코스 카드와 동일한 여백 */
  padding: 1rem;
  margin-top: -1.5rem;
}

.Fitness-card {
  position: relative;
  overflow: hidden;
  border-radius: 5px;
  width: 9.9rem; /* 카드 너비 고정 */
  height: 5rem; /* 카드 높이 고정 */
}

.Community-card {
  position: relative;
  overflow: hidden;
  border-radius: 5px;
  width: 10rem; /* 카드 너비 고정 */
  height: 5rem; /* 카드 높이 고정 */
}

.Fitness-card-image {
  width: 180%;
  height: 130%;
  object-fit: cover;
}

.Community-card-image {
  width: 165%;
  height: 170%;
  object-fit: cover;
}

#FitnessText{
  position: absolute;
  top: 10px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 17px;
  font-weight: 700;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#FitnessText2{
  position: absolute;
  top: 34px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 300;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#FitnessText3{
  position: absolute;
  top: 53px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 300;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#CommunityText{
  position: absolute;
  top: 10px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 17px;
  font-weight: 700;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#CommunityText2{
  position: absolute;
  top: 34px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 300;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#CommunityText3{
  position: absolute;
  top: 53px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 300;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

.additional-card-text {
  position: absolute;
  top: 10px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 17px;
  font-weight: 700;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

/* 프로필 리스트 */
.profile-section {
  padding: 1rem;
  margin-top: -0.5rem;
}

.profile-section::-webkit-scrollbar{
  display: none;
}

.profile-list {
  display: flex;
  overflow-x: auto; /* 가로 스크롤 가능 */
  gap: 10px;
  padding: 10px 0;
  white-space: nowrap; /* 요소들이 한 줄로 나열되도록 설정 */
  margin-top: -1rem;
  -ms-overflow-style: none; /* 인터넷 익스플로러 */
  scrollbar-width: none; /* 파이어폭스 */
}

.profile-circle {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  border: 3.5px solid #ddd;
  flex-shrink: 0; /* 아이템이 줄어들지 않도록 설정 */
}

.profile-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

#profile-text{
  font-family: 'TheJamsil4Bold';
  font-size: 17px;
  font-weight: 700;
}

#profile-text2{
  font-family: 'TheJamsil4Bold';
  font-size: 13px;
  font-weight: 300;
  opacity: 0.8;
  margin-top: -1rem;
}

/* 핫 게시물 섹션 */
.hot-posts-section {
  padding: 1rem;
  margin-top: -1.5rem;
}

.hot-posts-list {
  display: flex;
  overflow-x: auto;
  gap: 10px;
  padding: 10px 0;
  margin-top: -1rem;
  -ms-overflow-style: none; /* 인터넷 익스플로러 */
  scrollbar-width: none; /* 파이어폭스 */
}

.hot-post-card {
  min-width: 150px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.hot-post-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hot-post-text {
  position: absolute;
  bottom: 8px;
  left: 8px;
  color: white;
  font-family: 'TheJamsil4Bold';
  font-size: 17px;
  font-weight: 700;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.5);
}

#hot-post-text{
  font-family: 'TheJamsil4Bold';
  font-size: 17px;
  font-weight: 700;
}

/* 카드 눌림 효과 */
.main-card,
.side-card,
.Fitness-card,
.Community-card,
.hot-post-card {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  transition: transform 0.1s ease, filter 0.1s ease;
}

.main-card:active,
.side-card:active,
.Fitness-card:active,
.Community-card:active,
.hot-post-card:active {
  transform: scale(0.98); /* 살짝 축소하여 눌림 효과 */
  filter: brightness(0.85); /* 살짝 어둡게 */
}


</style>
