<template>
    <div class="detail-container">
      <MobileHeaderView class="header" />
      <div style="text-align: left;">
        <button class="back-button ms-2 btn btn-success btn-sm mb-3 mt-3 ms-3 mx-2 custom-btn" @click="goBack">뒤로가기</button>
      </div>
  
      <div class="detail-content">
        <div v-if="isLoading" class="loading">
          <p>Loading...</p>
        </div>
        
        <div v-else-if="communityDetail" class="post-details">
          
          <div class="post-meta">
            <div class="d-flex align-items-left mb-3">
                  <img
                    src="/images/profile.png"
                    alt="프로필 사진"
                    class="profile-image me-2"
                  />
            </div>
            <span class="author mt-2"> {{ communityDetail.userNickName }}</span><br />
            <span class="date ms-3 mt-2"> {{ communityDetail.communityRegDate }}</span><br />
          </div>
          
  
          <!-- 이미지 출력 -->
          <div class="image-map-container mb-2">
            <img
              v-if="communityDetail.communityUrl"
              :src="communityDetail.communityUrl"
              alt="게시글 이미지"
              class="img-fluid"
              style="width: 500px; height: 225px; object-fit: cover;"
            />
          </div>
          <p class="post-body">{{ communityDetail.communityBody }}</p>
  
  
          <div class="comments-section" v-if="comments.length > 0">
    <div v-for="comment in comments" :key="comment.commentId" class="comment-item">
      <div class="comment-header">
        <img
          src="/images/profile.png"
          alt="프로필 사진"
          class="profile-image me-2"
        />
        <span class="author">{{ communityDetail.userNickName }}</span>
      </div>
      <div class="underline"></div> <!-- 닉네임 아래 밑줄 -->
      <div class="comment-body-container">
        <p class="comment-body">{{ comment.commentBody }}</p>
      </div>
    </div>
  </div>
  <div v-else>
    <p class="no-comments">댓글이 없습니다.</p>
  </div>
  
  
          <!-- 댓글 입력 영역 (푸터 위 고정) -->
      <div class="comment-input-container">
        <div class="comment-input">
          <input type="text" v-model="newComment" placeholder="댓글을 입력하세요..." />
          <button @click="postComment" class="btn btn-success">댓글 등록</button>
        </div>
      </div>
        </div>
        <div v-else>
          <p>게시글을 불러올 수 없습니다.</p>
        </div>
      </div>
  
      <MobileFooterView5 class="footer" />
    </div>
  </template>
  
  <script setup>
  import MobileHeaderView from '@/components/MobileHeaderView.vue';
  import MobileFooterView5 from "@/components/MobileFooterView5.vue";
  import { ref, onMounted } from 'vue';
  import axios from 'axios';
  import { useRoute, useRouter } from 'vue-router';
  
  
  
  const route = useRoute();
  const router = useRouter();
  const communityDetail = ref(JSON.parse(route.query.community));
  const comments = ref([]);
  const newComment = ref("");
  const isLoading = ref(true);
  const totalComments =ref(0);
  
  
  const getCommunityDetail = async () => {
  
    const communityId = communityDetail.value.communityPk;
  
    try {
      // 백엔드의 @GetMapping("/detail/{id}")과 동일한 경로로 설정
      const response = await axios.get(`/api/detail/${communityId}`);
      
      // 데이터를 성공적으로 가져오면 communityDetail에 할당하고 로딩을 종료합니다
      communityDetail.value = response.data;
    } catch (error) {
      console.error('Error fetching community detail:', error.message);
    } finally {
      // 로딩 상태는 항상 비활성화하여 UI 상태를 업데이트
      isLoading.value = false;
    }
  };
  
  
  // 댓글 가져오기
  const getComments = async () => {
    const communityId = communityDetail.value.communityPk;
    try {
      const response = await axios.get(`/api/comments/${communityId}`);
      comments.value = response.data;
      console.log(comments.value)
      // 댓글 수를 업데이트
      totalComments.value = comments.value.length;
      console.log('총 댓글 수:', totalComments.value);
  
    } catch (error) {
      console.error('Error fetching comments:', error);
    }
  };
  
  
  // 댓글 추가하기
  const postComment = async () => {
    const communityPk = communityDetail.value.communityPk;
    let userNickName = localStorage.getItem('userNickName');
    try {
      await axios.post('/api/comments/add', {
        userNickName: userNickName,
        communityPk: communityPk,
        commentBody: newComment.value // 댓글 내용
      });
      newComment.value = "";
  
      // 댓글 추가 후, 바로 반영하기 위해 getComments() 호출
      await getComments();
  
      // 댓글 수 업데이트
      totalComments.value = comments.value.length; // 댓글 배열 길이로 총 댓글 수 업데이트
  
    } catch (error) {
      console.error('Error posting comment:', error);
    }
  };
  
  
  onMounted(() => {
    getCommunityDetail();
    getComments();
  });
  
  // 뒤로가기 메소드 정의
  const goBack = () => {
    router.push({ name: 'mobilecommunity' }); // 'communityview'로 리디렉션
  };
  </script>
  
  
  <style scoped>
  .detail-container {
    display: flex;
    flex-direction: column;
    height: 100vh;
  }
  
  .detail-content {
    flex: 1;
    overflow-y: auto; /* 스크롤 가능하도록 설정 */
    padding: 25px;
    padding-top: 0%;
    padding-bottom: 80px; /* 푸터 공간 */
  }
  
  .loading {
    text-align: center;
    font-size: 18px;
    color: #555;
  }
  
  .post-details {
    margin-bottom: 30px;
  }
  
  .post-title {
    font-size: 24px;
    font-weight: bold;
  }
  
  .post-meta {
    display: flex;
    color: #000000;
  }
  
  .post-body {
    font-size: 16px;
    margin-top: 15px;
    white-space: pre-line;
    text-align: left;  /* 왼쪽 정렬 추가 */
  }
  
  .button-group {
    display: flex;
    gap: 10px;
    margin-top: 15px;
  }
  
  .edit-button,
  .delete-button {
    padding: 5px 15px;
    font-size: 14px;
    cursor: pointer;
    border: none;
    color: white;
    border-radius: 5px;
  }
  
  .edit-button {
    background-color: #4CAF50;
  }
  
  .delete-button {
    background-color: #f1a82b;
  }
  
  .comments-title {
    font-size: 20px;
    font-weight: bold;
    margin-top: 10px;
  }
  
  .comments-section {
    margin-top: 15px;
  }
  
  .comment-item {
    margin-bottom: 15px;
    padding: 10px;
    background-color: #f9f9f9;
    border-radius: 10px;
    display: flex; /* 플렉스 박스를 사용하여 수평 정렬 */
    flex-direction: column; /* 세로로 정렬 */
    text-align: left; /* 텍스트 왼쪽 정렬 */
    width: 100%; /* 부모 요소의 너비 */
    box-sizing: border-box;
  }
  
  .comment-header {
    display: flex;
    align-items: center;
    margin-bottom: 5px; /* 프로필 이미지와 닉네임 간의 간격 */
  }
  
  /* 댓글 입력 부분 */
  .comment-input-container {
    position: fixed;
    bottom: 70px; /* 푸터 바로 위로 고정 */
    left: 0;
    width: 100%;
    background-color: white;
    padding: 10px;
    box-shadow: 0 -2px 5px rgba(0, 0, 0, 0.1);
    z-index: 10; /* 푸터 위에 표시되도록 */
  }
  
  .comment-input input {
    flex: 1;
    padding: 8px;
    border-radius: 5px;
    border: 1px solid #ccc;
  }
  
  .comment-input button {
    margin-left: 10px;
    padding: 8px 15px;
    color: white;
    border: none;
    border-radius: 5px;
    cursor: pointer;
  }
  
  .footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
  }
  
  .image-map-container img {
    width: 100%;
  
    border-radius: 15px;
  }
  
  .profile-image {
    width: 40px;  /* 원하는 너비 */
    height: 40px; /* 원하는 높이 */
    border-radius: 50%; /* 동그란 형태로 만들기 */
    object-fit: cover; /* 이미지 비율 유지 */
    margin-right: 10px; /* 이미지와 텍스트 간의 간격 추가 */
  }
  
  .author {
    font-weight: bold;
    font-size: 16px;
    margin-left: 0%;
    margin-top: 5px; /* 날짜와 닉네임 간 간격 */
  }
  
  .comment-body-container {
    margin-top: 10px; /* 댓글 본문과 유저 닉네임 간의 간격 */
  }
  
  .comment-body {
    margin-top: 10px; /* 댓글 본문과 유저 닉네임 간의 간격 */
    padding-left: 10px;
    font-size: 14px;
    color: #333; /* 댓글 본문 색상 */
    border-top: 1px solid #ddd; /* 상단에 밑줄을 추가 */
    padding-top: 10px;
    white-space: pre-line; /* 줄바꿈을 유지하여 텍스트가 자동으로 줄바꿈되게 */
  }
  
  .date {
    font-size: 12px; /* 날짜 크기 */
    color: #777; /* 회색 글자색 */
    text-align: right; /* 오른쪽 정렬 */
    display: block; /* 텍스트를 블록 요소로 처리하여 오른쪽 정렬 */
  }
  
  /* 폰트 추가 */
  @font-face {
      font-family: 'TheJamsil5Bold';
      src: url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2302_01@1.0/TheJamsil5Bold.woff2') format('woff2');
      font-weight: 700;
      font-style: normal;
      color: #555;  
  }
  
  
  
  /* 특정 요소에 대한 폰트 적용 */
  h1, h2, h3, .author, .post-body, .post-title, .comment-body-container, .comment-body, .no-comments {
    font-family: 'TheJamsil5Bold', sans-serif !important;
    color: #555 !important;
  }
  
  .post-body {
    font-family: 'TheJamsil3Regular', sans-serif !important;
    color: #555;
    font-size: 14px;  /* 글씨 크기 줄이기 */
    line-height: 1.6; /* 본문 내용의 가독성 향상 */
  }
  
  /* 뒤로가기 버튼 폰트 적용 */
  button {
    font-family: 'TheJamsil5Bold', sans-serif !important; /* 폰트 적용 */
    font-weight: bold; /* 폰트 두께 설정 (선택 사항) */
    font-size: 16px; /* 폰트 크기 설정 (선택 사항) */
  }
  
  
  </style>
  