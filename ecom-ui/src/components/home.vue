<template>
  <div>
    <el-card shadow="never">
      <h2 class="welcome">欢迎使用电商智能系统</h2>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前用户">{{ user.nickName }}</el-descriptions-item>
        <el-descriptions-item label="登录名">{{ user.username }}</el-descriptions-item>
        <el-descriptions-item label="用户类型">{{ user.userType === 0 ? '运营' : '消费者' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag v-for="role in roles" :key="role" class="role-tag">{{ role }}</el-tag>
          <span v-if="!roles.length">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="权限数量">{{ permissions.length }}</el-descriptions-item>
        <el-descriptions-item label="登录时间">{{ loginTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="tips-card">
      <template #header>系统模块</template>
      <el-row :gutter="12">
        <el-col :span="6" v-for="mod in modules" :key="mod.name">
          <div class="mod-item">
            <div class="mod-name">{{ mod.name }}</div>
            <div class="mod-desc">{{ mod.desc }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import useInfo from '@/store/userInfo'

const userStore = useInfo()
const user = computed(() => userStore.userInfo.user || {})
const roles = computed(() => userStore.userInfo.roles || [])
const permissions = computed(() => userStore.userInfo.permissions || [])
const loginTime = new Date().toLocaleString()

const modules = [
  { name: '鉴权与权限', desc: '已完成（P1）' },
  { name: '商品管理', desc: '待开发（P2）' },
  { name: '交易闭环', desc: '待开发（P3）' },
  { name: '订单售后', desc: '待开发（P4）' },
  { name: '智能供应链', desc: '待开发（P5）' },
  { name: '数据看板', desc: '待开发（P6）' },
  { name: '智能客服与搜索', desc: '待开发（P7）' },
  { name: '联调部署', desc: '待开发（P8）' }
]
</script>

<style scoped>
.welcome {
  margin: 0 0 16px;
  color: #303133;
}

.role-tag {
  margin-right: 6px;
}

.tips-card {
  margin-top: 16px;
}

.mod-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
  text-align: center;
}

.mod-name {
  font-weight: bold;
  color: #303133;
}

.mod-desc {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}
</style>
