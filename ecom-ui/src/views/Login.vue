<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <template #header>
        <h2 class="login-title">电商智能系统</h2>
        <p class="login-subtitle">店铺运营 + 消费者前台</p>
      </template>
      <el-form ref="formRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入账号" size="large" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" class="login-btn" size="large" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'
import loginHttp from '@/api/loginHttp'
import useInfo from '@/store/userInfo'

const router = useRouter()
const userStore = useInfo()

const formRef = ref(null)
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
}

const handleLogin = () => {
  formRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    loading.value = true
    try {
      const res = await loginHttp.login(loginForm)
      const { token, user } = res.data
      window.sessionStorage.setItem('authorization', token)
      userStore.setLogin(token, user)
      ElNotification.success({ title: '登录成功' })
      router.push('/home')
    } catch (e) {
      // 失败提示已由 request 拦截器统一处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3b73 0%, #2c5bb4 60%, #409eff 100%);
}

.login-card {
  width: 420px;
  opacity: 0.97;
}

.login-title {
  margin: 0;
  text-align: center;
  color: #303133;
}

.login-subtitle {
  margin: 8px 0 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.login-btn {
  width: 100%;
}
</style>
