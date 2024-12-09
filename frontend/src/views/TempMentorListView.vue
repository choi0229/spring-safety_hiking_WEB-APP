<template>
  <div class="container mt-4">
    <h2>게시글 리스트</h2>
    <ul>
      <li v-for="(post, index) in posts" :key="index">
        <h5>{{ post.title }}</h5>
        <p>{{ post.contentDetail }}</p>
        <p><small>{{ post.contentHashtags }} | {{ post.created_at }}</small></p>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const posts = ref([]);

const fetchPosts = async () => {
  try {
    const response = await fetch('/api/mentordetail/list');
    posts.value = await response.json();
  } catch (error) {
    console.error('Error fetching posts:', error);
  }
};

onMounted(() => {
  fetchPosts();
});
</script>

<style scoped>
.container {
  max-width: 600px;
}
</style>
