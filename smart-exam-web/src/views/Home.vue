<template>
  <div class="home-container">
    <h1>智能考试系统</h1>
    <p>欢迎使用智能考试系统！</p>
    <el-button type="primary">开始使用</el-button>
  </div>
</template>

<script setup lang="ts">
// Home 页面组件
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  text-align: center;
  gap: 20px;
}

h1 {
  font-size: 2rem;
  color: #333;
}

p {
  font-size: 1.2rem;
  color: #666;
}
</style>
