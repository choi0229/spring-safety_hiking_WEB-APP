<template>
    <nav class="navbar bg-body-custom" style=" height: 63px; color: black; background-color: white;">
      <div class="container">
        <a class="navbar-brand" href="/" style="margin-left: -13em;">
          <img src="/images/BrandIcon.png" style="width: 60px; height: 60px; position: fixed; top:0;">
          <span style="margin-left:70px; font-size:28px; font-weight: 400">안전등산</span>
        </a>
    
    
        <div class="d-flex flex-row" style="margin-top: 0.1em; margin-right: -17em; margin-bottom: -1rem;">
            <p style="margin-right: 2em;" @click="goToRealTimePeople()">실시간 등산인구</p>
            
            <p style="margin-right: 2em;" @click="goToManegeList()">민원처리</p>
            <div v-if="!loginState" style="margin-right: 2em;" @click="goToLogin()">로그인</div>
            <div v-else style="margin-right: 2em;" @click="logout()">로그아웃</div>
            <p style="margin-right: 2em;">커뮤니티</p>
            <div style="border: 1px; border-radius: 50%; width: 40px; height: 40px; background-color: black; margin-top: -0.4em; text-align: center;"><p style="margin-top: 0.5rem; color: white;">A</p></div>
        </div>
      </div>
    </nav>
    </template>
    
    <script setup>
    import { ref, onMounted  } from 'vue';

    const loginState = ref(false);

      function goToRealTimePeople() {
        window.location.href = '/realtime';
      }

      function goToManegeList() {
        window.location.href = '/manageList';
      }

      function goToLogin(){
        window.location.href = '/login';
      }

      function logout() {
        // 로그아웃 시 localStorage에서 userId 제거 및 상태 갱신
        localStorage.removeItem('userId');
        loginState.value = false;
        window.location.reload(); // 상태 반영 후 새로고침
      }
    

      onMounted(() => {
  const userIdInput = localStorage.getItem('userId');
  console.log('userId in localStorage:', userIdInput); // 값 확인용
  if (userIdInput) {
    loginState.value = true;
    console.log('Login state set to true');
  } else {
    console.log('No userId found, login state remains false');
  }
});
    </script>
    
    <style scoped>

    *{
      font-family: TheJamsil;
      font-weight: 100; 
    }

    .navbar{
      box-shadow: 0 1px 10px rgba(0, 0, 0, 0.3);
      z-index: 1000;
    }
    </style>