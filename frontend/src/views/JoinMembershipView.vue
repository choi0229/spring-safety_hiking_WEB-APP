<template>
  <MobileHeaderView class="header" />
  <div id="scrollable-container">
    <div id="form-container">
      <div id="form-inner-container">
        <div id="sign-up-container">
          <div id="toggle-controls">
            <button :class="{active:activeForm ==='user'}" @click="setActiveForm('user')"> 일반 유저 </button>
            <button :class="{ active: activeForm === 'admin' }"  @click="setActiveForm('admin')"> 관리자 </button>
          </div>
          <div v-if="activeForm === 'user'" id="login-container">
              <label for="id">아이디</label>
            <input type="text" v-model="newId" id="id" name="id" placeholder="아이디" @blur="checkId" />
            <p v-if="idError" style="color:red">{{ idError }}</p>

            <label for="password">비밀번호</label>
            <input type="password" v-model="newPw" name="password" id="password" placeholder="&#9679;&#9679;&#9679;&#9679;&#9679;&#9679;" />

            <label for="name">이름</label>
            <input type="text" v-model="newName" name="name" id="name" placeholder="Name" @blur="checkNickName" />

            <label for="nickname">닉네임</label>
            <input type="text" v-model="newNickName" name="nickname" id="nickname" placeholder="NickName" />
            <p v-if="nickNameError" style="color:red">{{ nickNameError }}</p>

            <label for="email">이메일</label>
            <input type="email" v-model="newEmail" name="email" id="email" placeholder="Email" @blur="checkEmail" />
            <p v-if="emailError" style="color:red">{{ emailError }}</p>

            <label for="age">나이</label>
            <input type="text" v-model="newAge" id="age" name="age" placeholder="ex) 20" />

            <label for="address">지역</label>
            <select v-model="newAddress" name="address" id="address">
              <option v-for="region in regions" :key="region" :value="region">{{ region }}</option>
            </select>

            <label for="gender">성별</label>
            <select v-model="newGender" name="gender" id="gender">
              <option v-for="gender in genders" :key="gender" :value="gender">{{ gender }}</option>
          </select>
          </div>

          <div v-if="activeForm === 'admin'" id="signiup-container">
              <label for="id">아이디</label>
            <input type="text" v-model="newId" id="id" name="id" placeholder="아이디" @blur="checkId" />
            <p v-if="idError" style="color:red">{{ idError }}</p>

            <label for="password">비밀번호</label>
            <input type="password" v-model="newPw" name="password" id="password" placeholder="&#9679;&#9679;&#9679;&#9679;&#9679;&#9679;" />

            <label for="name">이름</label>
            <input type="text" v-model="newName" name="name" id="name" placeholder="Name" @blur="checkNickName" />

            <label for="nickname">닉네임</label>
            <input type="text" v-model="newNickName" name="nickname" id="nickname" placeholder="NickName" />
            <p v-if="nickNameError" style="color:red">{{ nickNameError }}</p>

            <label for="email">이메일</label>
            <input type="email" v-model="newEmail" name="email" id="email" placeholder="Email" @blur="checkEmail" />
            <p v-if="emailError" style="color:red">{{ emailError }}</p>

            <label for="age">나이</label>
            <input type="text" v-model="newAge" id="age" name="age" placeholder="ex) 20" />

            <label for="address">지역</label>
            <select v-model="newAddress" name="address" id="address">
              <option v-for="region in regions" :key="region" :value="region">{{ region }}</option>
            </select>

            <label for="institution">기관</label>
            <select v-model="newInstitution" id="institution">
              <option value="">지역을 먼저 선택해주세요</option>
              <option v-for="institution in filteredInstitutions" :key="institution" :value="institution">
                {{ institution }}
              </option>
            </select>

          <label for="gender">성별</label>
          <select v-model="newGender" name="gender" id="gender">
            <option v-for="gender in genders" :key="gender" :value="gender">{{ gender }}</option>
          </select>
          </div>
          
          <div id="form-controls">
            <button type="mainbutton" @click="requestNewLogin">회원가입</button>
            <button type="subbutton" id="toggleSignIn" @click="moveLoginPage">로그인</button>
          </div>

          <input type="checkbox" name="terms" id="terms" style="display: none;" />
        </div>
      </div>
    </div>
  </div>
  <MobileFooterView></MobileFooterView>
</template>

<script setup>
import { ref, computed } from 'vue';
import axios from 'axios';
import MobileHeaderView from '@/components/MobileHeaderView.vue';
import MobileFooterView from '@/components/MobileFooterView.vue';


// 폼 상태
const activeForm = ref("user"); // 기본값: 로그인 폼

let userType = ref('user');
let newId = ref('');
let newPw = ref('');
let newName = ref('');
let newNickName = ref('');
let newEmail = ref('');
let newAge = ref('');
let newAddress = ref('');
let newInstitution = ref('');
let newGender = ref('');

// 오류 메시지
let idError = ref('');
let emailError = ref('');
let nickNameError = ref('');

// 지역 리스트
const regions = ['서울', '경기', '인천', '부산', '대구', '광주', '대전', '울산', '세종', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주'];

// 기관 리스트
const institutionsByRegion = {
  '서울': [
    '강남구청 공원녹지과', '강남구청 환경과', '강남구청 안전재난과',
    '강동구청 푸른도시과', '강동구청 환경과', '강동구청 안전재난과',
    '강북구청 푸른도시과', '강북구청 환경과', '강북구청 안전재난과',
    '강서구청 공원녹지과', '강서구청 환경과', '강서구청 안전재난과',
    '관악구청 공원녹지과', '관악구청 환경과', '관악구청 안전재난과',
    '광진구청 공원녹지과', '광진구청 환경과', '광진구청 안전재난과',
    '구로구청 공원녹지과', '구로구청 환경과', '구로구청 안전재난과',
    '국립공원공단 북한산국립공원사무소',
    '금천구청 공원녹지과', '금천구청 환경과', '금천구청 안전재난과',
    '노원구청 공원녹지과', '노원구청 환경과', '노원구청 안전재난과',
    '도봉구청 공원녹지과', '도봉구청 환경과', '도봉구청 안전재난과',
    '동대문구청 공원녹지과', '동대문구청 환경과', '동대문구청 안전재난과',
    '동작구청 공원녹지과', '동작구청 환경과', '동작구청 안전재난과',
    '마포구청 공원녹지과', '마포구청 환경과', '마포구청 안전재난과',
    '서대문구청 푸른도시과', '서대문구청 환경과', '서대문구청 안전재난과',
    '서울시 야생동물센터',
    '서초구청 공원녹지과', '서초구청 환경과', '서초구청 안전재난과',
    '성동구청 공원녹지과', '성동구청 환경과', '성동구청 안전재난과',
    '성북구청 공원녹지과', '성북구청 환경과', '성북구청 안전재난과',
    '송파구청 공원녹지과', '송파구청 환경과', '송파구청 안전재난과',
    '양천구청 공원녹지과', '양천구청 환경과', '양천구청 안전재난과',
    '영등포구청 푸른도시과', '영등포구청 환경과', '영등포구청 안전재난과',
    '용산구청 공원녹지과', '용산구청 환경과', '용산구청 안전재난과',
    '은평구청 공원녹지과', '은평구청 환경과', '은평구청 안전재난과',
    '종로구청 공원녹지과', '종로구청 환경과', '종로구청 안전재난과',
    '중구청 공원녹지과', '중구청 환경과', '중구청 안전재난과',
    '중랑구청 공원녹지과', '중랑구청 환경과', '중랑구청 안전재난과'
  ],
  '강원': ['국립공원공단 설악산국립공원사무소', '국립공원공단 치악산국립공원사무소', '국립공원공단 오대산국립공원사무소'],
  '충북': ['국립공원공단 속리산국립공원사무소', '국립공원공단 월악산국립공원사무소'],
  '충남': ['국립공원공단 계룡산국립공원사무소', '국립공원공단 태안해안국립공원사무소'],
  '전북': ['국립공원공단 내장산국립공원사무소', '국립공원공단 변산반도국립공원사무소'],
  '전남': ['국립공원공단 다도해해상국립공원사무소', '국립공원공단 무등산국립공원사무소'],
  '경북': ['국립공원공단 주왕산국립공원사무소', '국립공원공단 경주국립공원사무소'],
  '경남': ['국립공원공단 지리산국립공원사무소', '국립공원공단 가야산국립공원사무소', '국립공원공단 한려해상국립공원사무소', '국립공원공단 덕유산국립공원사무소'],
  '제주': ['국립공원공단 한라산국립공원사무소']
};

const setActiveForm = (form) => {
  activeForm.value = form;
  
  if (form === "admin") {
    userType.value = "admin";
    newAddress.value = ""; // 지역 초기화
    newInstitution.value = ""; // 기관 초기화
  } else {
    userType.value = "user";
  }
};

// 선택된 지역에 따라 표시될 기관 리스트x
const filteredInstitutions = computed(() => {
  return institutionsByRegion[newAddress.value] || [];
});

// 성별 리스트
const genders = ['남자', '여자'];

// 로그인 페이지로 이동
function moveLoginPage() {
  window.location.href = '/login';
}

//ID 중복 체크 함수
const checkId = async () => {
  if (newId.value === '') {
    idError.value = '아이디를 입력해주세요'
    return;
  }

  try {
    const response = await axios.post('/api/checkId', {userId: newId.value});
    if (!response.data.success) {
      idError.value = '이미사용중인 아이디입니다';
    } else {
      idError.value = '';
    }
  } catch (error) {
    console.error('ID 중복 체크 오류', error);
  }
};

// Email 중복 체크 함수
const checkEmail = async () => {
  if (newEmail.value === '') return;

  try {
    const response = await axios.post('/api/checkEmail', {userEmail: newEmail.value});
    if(!response.data.success) {
      emailError.value = '이미 사용중인 이메일입니다';
    } else {
      emailError.value = '';
    }
  } catch (error) {
    console.error('Email 중복 체크 오류', error);
  }
}

// NickName 중복 체크 함수
const checkNickName = async () => {
  if(newNickName.value === '') return;
  
  try {
    const response = await axios.post('/api/checkNickName', {userNickName: newNickName.value});
    if(!response.data.success) {
      nickNameError.value = '이미 사용중인 닉네임입니다';
    } else {
      nickNameError.value = '';
    }
  } catch (error) {
    console.error('NickName 중복 체크 오류', error);
  }
}

const requestNewLogin = async () => {

  // 입력값 유효성 검사 (공백 또는 길이 0 확인)
  if (!newId.value.trim()) {
    alert('아이디를 입력하세요.');
    return;
  }
  if (!newPw.value.trim()) {
    alert('비밀번호를 입력하세요.');
    return;
  }
  if (!newName.value.trim()) {
    alert('이름을 입력하세요.');
    return;
  }
  if (!newNickName.value.trim()) {
    alert('닉네임을 입력하세요.');
    return;
  }
  if (!newEmail.value.trim()) {
    alert('이메일을 입력하세요.');
    return;
  }
  if (!newAge.value.trim()) {
    alert('생년월일을 입력하세요.');
    return;
  }
  if (!newAddress.value.trim()) {
    alert('주소를 선택하세요.');
    return;
  }
  if (!newInstitution.value.trim()) {
    alert('소속을 선택하세요.');
    return;
  }
  if (!newGender.value.trim()) {
    alert('성별을 선택하세요.');
    return;
  }

  //중복 체크 에러가 있으면 회원가입 중단
  if(idError.value || emailError.value || nickNameError.value) return;
  
  try {
    const response = await axios.post('/api/signup', {
      userId: newId.value,
      userPw: newPw.value,
      userName: newName.value,
      userNickName: newNickName.value,
      userEmail: newEmail.value,
      userAge: newAge.value,
      userAddress: newAddress.value,
      userGender: newGender.value,
      userType: userType.value,
      userInstitution: userType.value === 'admin' ? newInstitution.value : null,
    }, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    console.log(response.data); // API 응답 데이터 확인
    console.log(idError.value, emailError.value, nickNameError.value); // 에러 상태 확인
    // 회원가입이 성공했다면 Nick_Name을 localStorage에 저장
    if (response.data.success) {
      alert(`환영합니다, ${response.data.userNickname}님!`);
      localStorage.setItem('userNickName', response.data.userNickname);
      localStorage.setItem('userGender', response.data.userGender);
      localStorage.setItem('userId', response.data.userId);
      // 페이지 이동 (예: aboutView로)
      window.location.href = '/login';
    } else {
      alert('회원가입에 실패했습니다. 다시 시도해주세요.');
    }

  } catch (err) {
  console.error('Error during signup:', err); // 에러를 자세히 출력
  alert('회원가입 도중 오류가 발생했습니다.');
  }
}

</script>

<style>
/* 전체 페이지 설정 */
body {
  margin: 0;
  padding: 0;
  font-family: Verdana, Geneva, Tahoma, sans-serif;
  background-color: #f3f4f9;
  overflow: hidden; /* 전체 페이지에서 스크롤을 막음 */
}

/* 스크롤 가능한 컨테이너 */
#scrollable-container {
  display: flex;
  flex-direction: column;
  position: absolute;
  top: 60px; /* header 높이 */
  bottom: 60px; /* footer 높이 */
  left: 0;
  right: 0;
  overflow-y: auto; /* 스크롤 활성화 */
  box-sizing: border-box;
  background-color: #ffffff; /* 배경색 */
  padding-top: 5px;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px; /* 헤더 높이 */
  z-index: 1000;
  background-color: white;
  box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.1); /* 하단 그림자 */
}

/* form-container (폼 내부) */
#form-container {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-grow: 1;
}

/* 폼 컨테이너 */
#form-inner-container {
  background-color: #ffffff;
  color: #000000;
  padding: 20px 30px;
  border-radius: 10px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.1);
}

/* 토글 버튼 컨트롤 */
#toggle-controls {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

#toggle-controls button {
  background: none;
  color: #aaa;
  border: none;
  font-size: 16px;
  padding: 10px 20px;
  cursor: pointer;
  transition: color 0.3s;
}

#toggle-controls button.active {
  color: #327C2B; /* 활성화된 버튼 색상 */
  font-weight: bold;
  border-bottom: 2px solid #327C2B;
}

select {
  width: 100%;
  padding: 10px;
  margin-bottom: 15px;
  background-color: #fafafa;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
}

select:hover {
  background-color: #ffffff; /* hover 시 배경색 */
  border-color: #327c2b; /* hover 시 테두리 색상 */
}

select:focus {
  outline: none; /* 기본 브라우저 테두리 제거 */
  border-color: #028c5a; /* focus 시 테두리 색상 */
  background-color: #ffffff; /* focus 시 배경색 */
  color: #000; /* focus 시 텍스트 색상 */
}

/* 입력 필드 */
input {
  width: 100%;
  padding: 10px;
  margin-bottom: 15px;
  background-color: #fafafa;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
}

input:focus {
  outline: none;
  border-color: #327C2B;
}

/* 버튼 */
button[type="mainButton"] {
  width: 100%;
  padding: 10px;
  background-color: #327C2B;
  color: #ffffff;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
  margin-top: 5px;
  margin-bottom: 5px;
}

button[type="mainButton"]:hover {
  background-color: #028c5a;
}

/* 보조 버튼 */
button[type="subButton"] {
  width: 100%;
  padding: 10px;
  background-color: transparent;
  color: #327C2B;
  border: 1px solid #327C2B;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
}

button[type="subButton"]:hover {
  background-color: #e8f7f0;
}

/* 폼 내부 폰트 색상 */
label {
  color: #666;
  font-weight: bold;
}

 

</style>