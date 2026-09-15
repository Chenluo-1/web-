<template>
  <div class="health-page">
    <h1>电商智能系统（ecom）</h1>
    <p>P0 前后端连通验证</p>
    <el-button type="primary" @click="checkHealth">调用后端 /health</el-button>
    <p class="result">后端返回：{{ result }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from './util/request'

const result = ref('未调用')

const checkHealth = async () => {
  try {
    result.value = JSON.stringify(await request.get('/health'))
  } catch (e) {
    result.value = '请求失败：' + e.message
  }
}

onMounted(checkHealth)
</script>

<style>
.health-page { padding: 60px; text-align: center; }
.result { margin-top: 20px; color: #409eff; }
</style>
