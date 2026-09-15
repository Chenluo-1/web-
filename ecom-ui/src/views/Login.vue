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
        <div class="reg-line">
          还没有账号？<el-link type="primary" :underline="false" @click="openRegister">消费者注册</el-link>
        </div>
      </el-form>
    </el-card>

    <!-- 消费者注册 -->
    <el-dialog v-model="regVisible" title="消费者注册" width="420px">
      <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="regForm.username" placeholder="4-20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="regForm.nickName" placeholder="选填，默认同用户名" maxlength="20" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="regForm.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="regForm.confirmPassword" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regVisible = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="submitRegister">注册</el-button>
      </template>
    </el-dialog>
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

// ---------- 消费者注册 ----------
const regVisible = ref(false)
const regLoading = ref(false)
const regFormRef = ref(null)
const regForm = reactive({ username: '', nickName: '', password: '', confirmPassword: '' })
const regRules = {
  username: [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '4-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== regForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const openRegister = () => {
  regForm.username = ''
  regForm.nickName = ''
  regForm.password = ''
  regForm.confirmPassword = ''
  regVisible.value = true
}

const submitRegister = () => {
  regFormRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    regLoading.value = true
    try {
      await loginHttp.register({
        username: regForm.username,
        nickName: regForm.nickName,
        password: regForm.password
      })
      ElNotification.success({ title: '注册成功', message: '请使用新账号登录' })
      loginForm.username = regForm.username
      loginForm.password = ''
      regVisible.value = false
    } finally {
      regLoading.value = false
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

.reg-line {
  margin-top: 12px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
</style>
