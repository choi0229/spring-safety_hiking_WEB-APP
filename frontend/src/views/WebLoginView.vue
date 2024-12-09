<style>
body {
    background-color: #F3F4F9;
    margin: 0;
    padding: 0;
    font-family: Verdana, Geneva, Tahoma, sans-serif;
}

#form-container {
    height: 95vh;
    display: flex;
    justify-content: center;
    align-items: center;
}

#form-inner-container {
    background-color: white;
    max-width: 100%;
    border-radius: 8px;
    box-shadow: 0 0 20px gainsboro;
    
}

#sign-up-container {
    padding: 20px;
    width: 320px;
    height: 600px;
}

#form-controls {
    margin-top: 30px; /* 폼 요소와 버튼들 사이의 여백 추가 */
    margin-bottom: 20px;
}

input:not(:last-of-type), select {
    display: block;
    margin-bottom: 30px;
    border: 1px solid #E5E9F5;
    background-color: #F6F7FA;
    padding: 15px;
    margin-top: 10px;
    border-radius: 10px;
    width: 100%;
}

h3 {
    color: #367c45;
    font-size: 150%;
    font-weight: 500;
    margin-bottom: 20px;
}

label {
    color: #7369AB;
}

::placeholder {
    color: #C0C7DB;
    font-size: larger;
    letter-spacing: 1.2px;
}

#form-controls button {
    border: none;
    font-size: 120%;
    margin-bottom: 10px; /* 두 버튼 간의 간격 */
    border: solid 1px #367c45;
    border-radius: 10px;
}

#form-controls button:hover {
    cursor: pointer;
}

button[type="mainButton"] {
    padding: 16px 75px;
    background-color: #327C2B;
    border-radius: 10px;
    color: white;
    margin-bottom: 20px; /* Submit 버튼 아래쪽 여백 */
}

button[type="mainButton"]:hover {
    background-color: #367c45;
}

button[type="subButton"] {
    padding: 16px 0 16px 0px;
    background-color: transparent;
    color: #327C2B;
    margin-top: 20px;
    margin-bottom: 20px; /* Button 아래쪽 여백 */
}


#terms:checked:after {
    content: '\2713';
    color: #7369AB;
    font-size: 24px;
    position: absolute;
    top: 0;
    left: 3px;
}

label[for="terms"] {
    display: inline-block;
    width: 80%;
    margin-left: 10px;
}

.termsLink {
    color: #367c45;
    text-decoration: none;
}

.hide {
    display: none!important;
}

/* responsive display */

@media(max-width:1438px) {
    lottie-player {
        width: 300px!important;
    }
}

@media(max-width:1124px) {
    #animation-container {
        display: none;
    }

    #form-inner-container{
        display: flex;
        justify-content: center;
    }
}

@media(max-width: 684px) {
    #form-controls {
        text-align: center;
        margin: 0;
        padding: 0;
    }

    button {
        width: 100%;
    }

    input:not(:last-of-type) {
        width: 85%;
    }

    #toggleSignIn, #toggleSignUp {
        padding: 16px 75px;
    }

    
    label[for="terms"] {
        display: inline-block;
        font-size: smaller;
    }
}
</style>
<template>
       <div id="form-container">
        <div id="form-inner-container">
            <!-- Sign up container -->
            <div id="sign-up-container">
                <h3>로그인</h3>
                <div>
                    <label for="id">아이디</label>
                    <input type="text" v-model="loginId" id="id" name="id" placeholder="아이디">

                    <label for="password">비밀번호</label>
                    <input type="password" v-model="loginPw" name="password" id="password" placeholder="&#9679;&#9679;&#9679;&#9679;&#9679;&#9679;">

                    <div id="form-controls">
                        <button type="mainButton" id="toggleSignIn" @click="requestLogin">로그인</button>
                        <button type="subButton" @click="moveNewLoginPage">회원가입</button>
                    </div>

                    <input type="checkbox" name="terms" id="terms" style="display: none;">
                   
                </div>
            </div>
        </div>
    </div>
  </template>
  
  <script setup>
  import { ref } from 'vue';
  import axios from 'axios';
  
  let loginId = ref('');
  let loginPw = ref('');
  
  // 회원가입 페이지로 이동
  function moveNewLoginPage() {
    window.location.href = '/join';
  }

  const requestLogin = async () => {
  console.log(loginId.value)

  if(!loginId.value || loginPw.value ==='') {

    return;
  } else{
      try {
        const response = await axios.post('/api/login', {
          userId: loginId.value,
          userPw: loginPw.value,
        }, {
          headers: {
            'Content-Type': 'application/json'
          }
        });
    
        console.log(response.data);
        // 로그인 성공 시 정보를 로컬 스토리지에 저장
        localStorage.setItem('userNickName', response.data.userNickName);
        localStorage.setItem('userGender', response.data.userGender);
        localStorage.setItem('userId', response.data.userId); // userId 추가

        console.log(response.data); // API 응답 확인

        window.location.href='/mentor';
        if (!response.data) {
          alert('아이디 또는 비밀번호가 다릅니다.');
        }
    
      } catch (err) {
        alert('아이디 또는 비밀번호가 일치하지 않습니다.');
        console.log(err);
      }
    }
  }

  </script>