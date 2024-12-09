<template>
  <MobileHeaderView class="header" />
    <div id="form-container" :class="isApp ? 'app' : 'web'">
      <div id="form-inner-container">
        <!-- Sign up container -->
        <div id="sign-up-container">
          <h3 style="font-size: 30px;"><img src="/images/icon2.png" style="width: 45px; height: auto;">안전등산</h3>
          <div>
            <label for="id" style="font-weight: bold;">아이디</label>
            <input type="text" v-model="loginId" id="id" name="id" placeholder="아이디">
  
            <label for="password" style="font-weight: bold;">비밀번호</label>
            <input type="password" v-model="loginPw" name="password" id="password" placeholder="&#9679;&#9679;&#9679;&#9679;&#9679;&#9679;">
  
            <div id="form-controls">
              <button type="mainButton" id="toggleSignIn" @click="requestLogin">로그인</button>
            </div>
            <div>
              <span style="font-size: 14px; border-bottom: solid 1px gray; width: 12px;">안전등산 회원이 아니신가요? </span>
              <button class="link-button" @click="moveNewLoginPage"> 회원가입하기</button>
            </div>
            <div id="form-controls2">
              <button type="naverButton">
                <img src="/images/naver.png" alt="네이버 아이콘" style="width: 24px; height: auto;">
                네이버 로그인
              </button>
            </div>

            <div id="form-controls2">
              <button type="googleButton">
                <img src="/images/google.png" alt="구글 아이콘">
                Google 로그인
              </button>
            </div>

            <div id="form-controls2">
              <button type="kakaoButton">
                <img src="/images/kakao.png" alt="카카오 아이콘" style="width: 24px; height: auto;">
                카카오 로그인
              </button>
            </div>

            <input type="checkbox" name="terms" id="terms" style="display: none;">
          </div>
        </div>
      </div>
    </div>
    
    <MobileFooterView></MobileFooterView>
  </template>
  
  <script setup>
  import { ref } from 'vue';
  import axios from 'axios';
  import MobileHeaderView from '@/components/MobileHeaderView.vue';
  import MobileFooterView from '@/components/MobileFooterView.vue';
  
  // 로그인 아이디와 비밀번호
  let loginId = ref('');
  let loginPw = ref('');
  
  // 플랫폼 구분 (간단한 방법으로 예시)
  const isApp = window.navigator.userAgent.includes('Android') || window.navigator.userAgent.includes('iPhone');
  
  // 회원가입 페이지로 이동
  function moveNewLoginPage() {
    window.location.href = '/signup';
  }
  
  // 로그인 요청 처리
  const requestLogin = async () => {
    console.log("로그인 요청: ", loginId.value, loginPw.value);

    // 입력값 검증
    if (!loginId.value || loginId.value.trim() === '') {
      alert("아이디를 입력해주세요.");
      return;
    }
    if (!loginPw.value || loginPw.value.trim() === '') {
      alert("비밀번호를 입력해주세요.");
      return;
    }

    try {
      const response = await axios.post('/api/login', {
        userId: loginId.value,
        userPw: loginPw.value,
      }, {
        headers: {
          'Content-Type': 'application/json',
        },
      });

      console.log(response.data);

      // localStorage에 사용자 정보 저장
      localStorage.setItem('userNickName', response.data.userNickName);
      localStorage.setItem('userGender', response.data.userGender);
      localStorage.setItem('userId', response.data.userId); // userId 추가
      localStorage.setItem('userInstitution', response.data.userInstitution);

      // Android WebView와 통신 (userId 전달)
      if (isApp && window.Android && window.Android.sendUserIdToAndroid) {
        window.Android.sendUserIdToAndroid(response.data.userId);
        console.log("Android WebView로 userId 전송: ", response.data.userId);
      }

      // 페이지 이동
      window.location.href = '/mobilemainview';

      if (!response.data) {
        alert('아이디 또는 비밀번호가 다릅니다.');
      }
    } catch (err) {
      alert('아이디 또는 비밀번호가 일치하지 않습니다.');
      console.log(err);
    }
  };

</script>

<style>
/* 로그인 화면 전체를 세로 중앙 정렬 */
#form-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  box-sizing: border-box;
}

/* 로그인 폼 컨테이너 */
#form-inner-container {
  background-color: white;
  width: 100%;
  max-width: 350px;
  padding: 30px 20px;
  border-radius: 10px;
  text-align: center;
  box-sizing: border-box;
}

/* 폼 제목 */
#sign-up-container h3 {
  color: #327C2B; /* 메인 색상 */
  font-weight: bold;
  font-size: xx-large;
  margin-bottom: 20px;
}

/* 입력 필드 스타일 */
#sign-up-container label {
  color: #666666; /* 입력 필드 설명 색상 */
  font-size: 14px;
  display: block;
  text-align: left;
  margin-left: 5%;
}

#sign-up-container input {
  width: 90%;
  margin: 8px;
  padding: 12px;
  border: 1px solid #dadada;
  background-color: #ffffff;
  font-size: 14px;
  color: #333;
  box-sizing: border-box;
}

#sign-up-container input:focus {
  border-color: #327C2B;
  outline: none;
}

/* 버튼 컨트롤 */
#form-controls {
  margin-top: 20px;
}

/* 메인 버튼 스타일 */
#form-controls button[type="mainButton"] {
  width: 90%;
  padding: 12px;
  background-color: #327C2B;
  color: white;
  font-size: 16px;
  font-weight: bold;
  border: none;
  cursor: pointer;
}

/* 로그인 버튼 박스 */
#form-controls2 {
  margin-top: 10px;
  display: flex;
  justify-content: center; /* 가운데 정렬 */
  align-items: center;
  position: relative;
}

/* 네이버 로그인 버튼 */
#form-controls2 button[type="naverButton"] {
  width: 90%;
  padding: 12px;
  background-color: #03c75a;
  color: white;
  font-size: 16px;
  font-weight: bold;
  border: none;
  cursor: pointer;
  position: relative; /* 아이콘을 절대 위치로 배치하기 위한 부모 요소 */
  text-align: center; /* 텍스트를 버튼 중앙에 배치 */
}

#form-controls2 button[type="naverButton"]:hover {
  background-color: #02b753;
}

/* Google 로그인 버튼 */
#form-controls2 button[type="googleButton"] {
  width: 90%;
  padding: 12px;
  background-color: white;
  color: black;
  font-size: 16px;
  font-weight: bold;
  border: 1px solid #dadada;
  cursor: pointer;
  position: relative;
  text-align: center;
}

#form-controls2 button[type="googleButton"]:hover {
  background-color: #f5f5f5;
}

/* 카카오 로그인 버튼 */
#form-controls2 button[type="kakaoButton"] {
  width: 90%;
  padding: 12px;
  background-color: #fee500;
  color: black;
  font-size: 16px;
  font-weight: bold;
  border: none;
  cursor: pointer;
  position: relative;
  text-align: center;
}

#form-controls2 button[type="kakaoButton"]:hover {
  background-color: #ffd700;
}

/* 공통 아이콘 이미지 스타일 */
#form-controls2 img {
  position: absolute; /* 아이콘을 버튼 왼쪽에 배치 */
  left: 15px; /* 버튼의 왼쪽에서 15px 떨어지도록 설정 */
  top: 50%; /* 세로 중앙 정렬 */
  transform: translateY(-50%); /* 정확한 중앙 정렬 */
}


/* 회원가입 링크 스타일 */
.link-button {
  display: inline-block;
  font-size: 14px;
  font-weight: bold;
  color: #327C2B; /* 기본 글자 색상 */
  text-decoration: none;
  border: none;
  background: none;
  cursor: pointer;
  position: relative;
  margin-top: 20px;
}

.link-button:hover {
  text-decoration: underline; /* 마우스 오버 시 밑줄 */
}


/* 반응형 스타일 */
@media (max-width: 480px) {
  #form-container {
    padding: 10px 0;
  }

  #form-inner-container {
    width: 95%;
    padding: 20px;
  }

  #sign-up-container h3 {
    font-size: 18px;
  }

  #form-controls button {
    font-size: 14px;
  }
}
</style>