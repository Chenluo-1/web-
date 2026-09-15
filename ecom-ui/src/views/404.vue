<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在或无权限访问</p>
    <el-button type="primary" @click="$router.push('/home')">返回首页</el-button>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #606266;
}
</style>
