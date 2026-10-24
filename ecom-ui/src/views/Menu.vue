<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="logo" title="回到首页" @click="$router.push('/home')">电商智能系统</div>
      <el-menu
        :default-active="activePath"
        router
        unique-opened
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <template v-for="menu in menus" :key="menu.id">
          <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.urlPath || String(menu.id)">
            <template #title>{{ menu.name }}</template>
            <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.urlPath">
              {{ child.name }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.urlPath">{{ menu.name }}</el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <el-dropdown @command="handleCommand">
          <span class="user-info">{{ nickName }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <chat-widget />

  <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" @click="submitPassword">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import loginHttp from '@/api/loginHttp'
import sysUserHttp from '@/api/sysUserHttp'
import ChatWidget from '@/components/chatWidget.vue'
import useInfo from '@/store/userInfo'

const route = useRoute()
const router = useRouter()
const userStore = useInfo()

const menus = computed(() => userStore.userInfo.menu || [])
const nickName = computed(() => (userStore.userInfo.user || {}).nickName || '未登录')
const activePath = computed(() => route.path)

const currentTitle = computed(() => findMenuName(menus.value, route.path) || '首页')

function findMenuName(list, path) {
  for (const menu of list) {
    if (menu.urlPath === path) {
      return menu.name
    }
    if (menu.children && menu.children.length) {
      const found = findMenuName(menu.children, path)
      if (found) {
        return found
      }
    }
  }
  return null
}

// ---------- 修改密码 ----------
const pwdVisible = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '原密码不能为空', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' }
  ]
}

const submitPassword = () => {
  pwdFormRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    await sysUserHttp.changePassword(pwdForm)
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    doLogout()
  })
}

// ---------- 退出登录 ----------
const handleCommand = command => {
  if (command === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdVisible.value = true
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(() => doLogout())
      .catch(() => {})
  }
}

const doLogout = async () => {
  try {
    await loginHttp.logout()
  } catch (e) {
    // 忽略退出接口异常，本地态一定要清
  }
  window.sessionStorage.clear()
  userStore.clear()
  window.location.href = '/login'
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background-color: #304156;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 17px;
  font-weight: bold;
  background-color: #263445;
  cursor: pointer;
  transition: background-color 0.18s ease;
}

.logo:hover {
  background-color: #1f2c3a;
}

.aside :deep(.el-menu) {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
}

.header-title {
  font-size: 16px;
  color: #303133;
}

.user-info {
  cursor: pointer;
  color: #409eff;
}

.main {
  background-color: #f0f2f5;
  padding: 16px;
}
</style>
