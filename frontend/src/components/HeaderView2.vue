<template>
    <nav class="navbar bg-body-custom" style=" height: 63px; color: black; background-color: white;">
      <div class="container">
        <a class="navbar-brand" href="/" style="margin-left: -13em;">
          <img src="/images/BrandIcon.png" style="width: 60px; height: 60px; position: fixed; top:0;">
        </a>
    
    
        <div class="d-flex flex-row" style="margin-top: 0.1em; margin-right: -17em; margin-bottom: -1rem;">
            <p style="margin-right: 2em;" @click="goToRealTimePeople()">실시간 등산인구</p>
            
            <p style="margin-right: 2em;">소개</p>
            <p style="margin-right: 2em;">커뮤니티</p>
            <div style="border: 1px; border-radius: 50%; width: 40px; height: 40px; background-color: black; margin-top: -0.4em; text-align: center;"><p style="margin-top: 0.5rem; color: white;">A</p></div>
        </div>
      </div>
    </nav>
    </template>
    
    <script setup>

      function goToRealTimePeople() {
        window.location.href = '/RealTimePeople';
      }
    
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