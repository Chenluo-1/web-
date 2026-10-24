<template>
  <div class="login-page">
    <!-- 左侧品牌区：深色渐变 + 网格 + 迷你看板 -->
    <section class="brand">
      <div class="brand-grid"></div>
      <div class="blob blob-a"></div>
      <div class="blob blob-b"></div>

      <div class="brand-inner">
        <div class="brand-head">
          <div class="logo">E</div>
          <div>
            <div class="logo-name">电商智能系统</div>
            <div class="logo-sub">ecom · 店铺运营中台</div>
          </div>
        </div>

        <h1 class="headline">让运营决策<br /><span>自己会算数</span></h1>
        <p class="lead">
          规则引擎替代重复性人工判断：补货建议、库存预警、客服应答、个性化推荐，
          把每天的琐事交给系统。
        </p>

        <ul class="feats">
          <li v-for="feat in feats" :key="feat.title">
            <span class="feat-icon" v-html="feat.icon"></span>
            <span>
              <b>{{ feat.title }}</b>
              <i>{{ feat.desc }}</i>
            </span>
          </li>
        </ul>

        <div class="mock">
          <div class="mock-head">
            <span>今日经营</span>
            <span class="dot">实时</span>
          </div>
          <div class="mock-kpis">
            <div>
              <em>GMV</em>
              <strong>¥4,963</strong>
            </div>
            <div>
              <em>订单</em>
              <strong>23</strong>
            </div>
            <div>
              <em>转化</em>
              <strong>11.7%</strong>
            </div>
          </div>
          <div class="mock-bars">
            <span v-for="(h, i) in bars" :key="i" :style="{ height: h + '%', animationDelay: i * 90 + 'ms' }"></span>
          </div>
        </div>
      </div>
    </section>

    <!-- 右侧表单区 -->
    <section class="panel">
      <div class="form-wrap" :class="{ shake: shaking }">
        <p class="hello">欢迎回来</p>
        <h2 class="title">登录你的工作台</h2>
        <p class="sub">账号决定角色，角色决定你能看到哪些模块</p>

        <div class="quick">
          <span class="quick-label">演示账号</span>
          <button v-for="acc in demoAccounts" :key="acc.username" type="button" @click="fillAccount(acc)">
            {{ acc.label }}
          </button>
        </div>

        <el-form ref="formRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="账号" size="large" clearable>
              <template #prefix>
                <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <circle cx="12" cy="8" r="3.6" />
                  <path d="M4.5 20c1.6-3.6 4.2-5.4 7.5-5.4S18 16.4 19.5 20" stroke-linecap="round" />
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password>
              <template #prefix>
                <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <rect x="4.5" y="10" width="15" height="10" rx="2.5" />
                  <path d="M8 10V7.5a4 4 0 0 1 8 0V10" stroke-linecap="round" />
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <div class="row-between">
            <el-checkbox v-model="remember">记住账号</el-checkbox>
            <el-link type="primary" :underline="false" @click="forgotPassword">忘记密码？</el-link>
          </div>

          <el-button class="submit" size="large" :loading="loading" @click="handleLogin">
            {{ loading ? '正在登录' : '登 录' }}
          </el-button>
        </el-form>

        <div class="reg">
          <span>还没有账号？</span>
          <el-link type="primary" :underline="false" @click="openRegister">注册消费者账号</el-link>
        </div>

        <div class="tip">
          <span class="pulse"></span>
          服务运行中 · 看板数据 5 分钟刷新
        </div>
      </div>

      <footer class="foot">CW3104 电商智能系统 · v1.0</footer>
    </section>

    <!-- 消费者注册 -->
    <el-dialog v-model="regVisible" width="440px" align-center class="reg-dialog">
      <template #header>
        <div class="dlg-head">
          <b>注册消费者账号</b>
          <span>注册后自动绑定「消费者」角色，可浏览商品、下单与评价</span>
        </div>
      </template>
      <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-position="top">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElNotification, ElMessage } from 'element-plus'
import loginHttp from '@/api/loginHttp'
import useInfo from '@/store/userInfo'

const router = useRouter()
const userStore = useInfo()

const formRef = ref(null)
const loading = ref(false)
const shaking = ref(false)
const remember = ref(true)
const loginForm = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
}

// 品牌区文案与图标（内联 SVG，避免引入图标库依赖）
const feats = [
  {
    title: '智能补货与库存预警',
    desc: '低于阈值自动标记，按销量预测给出补货量',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3.5 8.5 12 4l8.5 4.5v7L12 20l-8.5-4.5z"/><path d="M3.5 8.5 12 13l8.5-4.5M12 13v7"/></svg>'
  },
  {
    title: '规则版智能客服',
    desc: '知识库 2-gram 匹配，答不上来转人工并沉淀',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 6.5h16v9.5H9l-4 3.5v-3.5H4z" stroke-linejoin="round"/><path d="M8.5 10h7M8.5 12.8h4.5" stroke-linecap="round"/></svg>'
  },
  {
    title: '经营看板与漏斗',
    desc: 'GMV、转化、类目占比、曝光到成交全链路',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M5 19V9M12 19V4.5M19 19v-6" stroke-linecap="round"/><path d="M3.5 21h17" stroke-linecap="round"/></svg>'
  }
]
const bars = [38, 56, 44, 72, 61, 88, 70, 95]

// 演示账号：一键填充，方便答辩时切角色
const demoAccounts = [
  { label: '超管 admin', username: 'admin', password: 'admin123' },
  { label: '商品运营 op1', username: 'op1', password: '123456' },
  { label: '客服主管 cs1', username: 'cs1', password: '123456' },
  { label: '消费者 member1', username: 'member1', password: '123456' }
]

const fillAccount = acc => {
  loginForm.username = acc.username
  loginForm.password = acc.password
  formRef.value && formRef.value.clearValidate()
}

const forgotPassword = () => {
  ElMessage.info('演示系统不开放自助改密，请联系管理员在「系统管理-用户管理」中重置')
}

onMounted(() => {
  const saved = window.localStorage.getItem('ecom:login:username')
  if (saved) {
    loginForm.username = saved
    remember.value = true
  }
})

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
      if (remember.value) {
        window.localStorage.setItem('ecom:login:username', loginForm.username)
      } else {
        window.localStorage.removeItem('ecom:login:username')
      }
      ElNotification.success({ title: '登录成功', message: '正在进入工作台' })
      router.push('/home')
    } catch (e) {
      // 错误提示由 request 拦截器统一处理，这里只做一次抖动反馈
      shaking.value = true
      window.setTimeout(() => {
        shaking.value = false
      }, 520)
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
  background: #f5f7fb;
  overflow: hidden;
}

/* ---------------- 左侧品牌区 ---------------- */
.brand {
  position: relative;
  flex: 0 0 53%;
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 56px 64px;
  color: #fff;
  background: linear-gradient(150deg, #08142c 0%, #10275c 46%, #1b4fa8 100%);
}

.brand-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: radial-gradient(ellipse at 30% 20%, #000 20%, transparent 78%);
  -webkit-mask-image: radial-gradient(ellipse at 30% 20%, #000 20%, transparent 78%);
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.55;
  animation: float 14s ease-in-out infinite;
}

.blob-a {
  width: 380px;
  height: 380px;
  top: -90px;
  right: -60px;
  background: radial-gradient(circle, rgba(76, 151, 255, 0.9), transparent 68%);
}

.blob-b {
  width: 300px;
  height: 300px;
  bottom: -80px;
  left: -40px;
  background: radial-gradient(circle, rgba(124, 58, 237, 0.55), transparent 70%);
  animation-delay: -6s;
}

@keyframes float {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(18px, -22px, 0) scale(1.08); }
}

.brand-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 470px;
  animation: rise 0.6s ease both;
}

.brand-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 46px;
}

.logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  font-weight: 800;
  color: #0a1a3c;
  background: linear-gradient(140deg, #bcd8ff, #7cb8ff);
  box-shadow: 0 10px 24px -12px rgba(124, 184, 255, 0.9);
}

.logo-name {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.logo-sub {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 1px;
}

.headline {
  margin: 0 0 16px;
  font-size: 40px;
  line-height: 1.28;
  font-weight: 700;
  letter-spacing: 1px;
}

.headline span {
  background: linear-gradient(96deg, #8dc4ff, #c9e2ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.lead {
  margin: 0 0 30px;
  font-size: 13.5px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.62);
}

.feats {
  list-style: none;
  margin: 0 0 34px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feats li {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.feat-icon {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ac8ff;
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.feat-icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.feats b {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 2px;
}

.feats i {
  font-style: normal;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

/* 迷你看板 */
.mock {
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(10px);
  animation: rise 0.8s 0.1s ease both;
}

.mock-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 12px;
}

.mock-head .dot {
  font-size: 11px;
  color: #7ee2a8;
  padding-left: 12px;
  position: relative;
}

.mock-head .dot::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #7ee2a8;
  box-shadow: 0 0 0 3px rgba(126, 226, 168, 0.18);
}

.mock-kpis {
  display: flex;
  gap: 22px;
  margin-bottom: 14px;
}

.mock-kpis em {
  font-style: normal;
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  margin-bottom: 2px;
}

.mock-kpis strong {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.mock-bars {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 58px;
}

.mock-bars span {
  flex: 1;
  border-radius: 4px 4px 2px 2px;
  background: linear-gradient(180deg, #8dc4ff, rgba(60, 130, 246, 0.55));
  animation: grow 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

@keyframes grow {
  from { transform: scaleY(0.15); opacity: 0.2; }
  to { transform: scaleY(1); opacity: 1; }
}

/* ---------------- 右侧表单区 ---------------- */
.panel {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 28px;
  background: #fff;
}

.form-wrap {
  width: 100%;
  max-width: 356px;
  margin: 0 auto;
  animation: rise 0.55s ease both;
}

.hello {
  margin: 0 0 6px;
  font-size: 12.5px;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: #94a3b8;
}

.title {
  margin: 0 0 8px;
  font-size: 25px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.5px;
}

.sub {
  margin: 0 0 24px;
  font-size: 12.5px;
  color: #94a3b8;
}

.quick {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 20px;
}

.quick-label {
  grid-column: 1 / -1;
  font-size: 11.5px;
  color: #b0b8c4;
  margin-bottom: -2px;
}

.quick button {
  border: 1px solid #e7ebf3;
  background: #fbfcfe;
  color: #64748b;
  border-radius: 9px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quick button:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

.ico {
  width: 16px;
  height: 16px;
  color: #b6bfcc;
}

:deep(.el-input__wrapper) {
  border-radius: 11px;
  box-shadow: 0 0 0 1px #e7ebf3 inset;
  padding: 3px 12px;
  transition: box-shadow 0.2s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2563eb inset, 0 0 0 4px rgba(37, 99, 235, 0.12);
}

:deep(.el-input__inner) {
  height: 40px;
  font-size: 14px;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

.row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 20px;
}

.submit {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 11px;
  font-size: 14.5px;
  letter-spacing: 3px;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  box-shadow: 0 12px 22px -12px rgba(37, 99, 235, 0.9);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 26px -12px rgba(37, 99, 235, 0.95);
}

.submit:active {
  transform: translateY(0);
}

.reg {
  margin-top: 18px;
  text-align: center;
  font-size: 12.5px;
  color: #94a3b8;
}

.tip {
  margin-top: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-size: 11.5px;
  color: #b0b8c4;
}

.pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.5);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  70% { box-shadow: 0 0 0 7px rgba(34, 197, 94, 0); }
  100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}

.foot {
  position: absolute;
  bottom: 22px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 11.5px;
  color: #c3cad6;
}

@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes shake {
  10%, 90% { transform: translateX(-2px); }
  20%, 80% { transform: translateX(4px); }
  30%, 50%, 70% { transform: translateX(-7px); }
  40%, 60% { transform: translateX(7px); }
}

.form-wrap.shake {
  animation: shake 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}

/* 注册弹窗 */
.dlg-head b {
  display: block;
  font-size: 16px;
  color: #0f172a;
}

.dlg-head span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

/* 响应式：窄屏收起品牌区 */
@media (max-width: 980px) {
  .brand {
    display: none;
  }

  .panel {
    background: linear-gradient(170deg, #f7f9fd, #eef3fb);
  }
}

@media (max-width: 480px) {
  .login-page {
    width: 100%;
    overflow-x: hidden;
  }

  .panel {
    width: 100%;
    padding: 32px 20px;
  }

  .title {
    font-size: 22px;
  }

  .quick {
    grid-template-columns: 1fr;
  }
}
</style>
