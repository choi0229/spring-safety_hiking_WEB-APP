<template>
  <div class="container mt-4">
    <h2>글 작성하기</h2>

    <form @submit.prevent="submitPost">
      <!-- 제목 입력 -->
      <div class="form-group">
        <label for="title">제목</label>
        <input
          type="text"
          id="title"
          v-model="titleInput"
          class="form-control"
          required
        />
      </div>

      <!-- 내용 입력 -->
      <div class="form-group mt-3">
        <label for="content_detail">내용</label>
        <textarea
          id="content_detail"
          v-model="contentDetailInput"
          class="form-control"
          rows="5"
          required
        ></textarea>
      </div>

      <!-- 해시태그 입력 -->
      <div class="form-group mt-3">
        <label for="content_hashtags">해시태그</label>
        <input
          type="text"
          id="content_hashtags"
          v-model="contentHashtagsInput"
          class="form-control"
          placeholder="#해시태그"
          required
        />
      </div>

        <!-- 제목 입력 -->
        <div class="form-group">
          <label for="totalPerson">모집인원</label>
          <input
            type="text"
            id="totaPerson"
            v-model="totalPersonInput"
            class="form-control"
            required
          />
      </div>

      <!-- 저장 버튼 -->
      <button type="submit" class="btn btn-primary mt-3">저장</button>
    </form>
  </div>
</template>

<script setup>
import axios from 'axios';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

// 각각의 ref 변수
const titleInput = ref('');
const contentDetailInput = ref('');
const contentHashtagsInput = ref('');
const totalPersonInput = ref('');
let userNicknameInput = localStorage.getItem("userNickName")
let userAddressInput =  localStorage.getItem("userAddress")
let profileImageInput = ref(''); // userProfile 테이블에서 참조

const router = useRouter();

// 글 저장 처리
const submitPost = async () => {
  try {
    const formData = {
      title: titleInput.value,
      contentDetail: contentDetailInput.value,
      contentHashtags: contentHashtagsInput.value,
      totalPerson:totalPersonInput.value,
      userNickname: userNicknameInput,
      userAddress: userAddressInput,
      profileImage: profileImageInput.value
    };

    console.log(userNicknameInput); // 요청 내용 확인

    await axios.post('/api/mentordetail/create', formData);

    console.log('요청됨');
    
    // 글 작성 후 TempMentorListView 페이지로 이동
    router.push('/test');
  } catch (error) {
    console.error('Error creating post:', error);
  }
};
</script>

<style scoped>
.container {
  max-width: 600px;
}
</style>
