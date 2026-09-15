<template>
  <div>
    <el-card shadow="never" class="pay-card">
      <template #header>订单支付</template>
      <template v-if="order">
        <div class="pay-info">
          <div class="row"><span class="label">订单号：</span>{{ order.orderNo }}</div>
          <div class="row"><span class="label">应付金额：</span><span class="amount">¥{{ order.payAmount }}</span></div>
          <div class="row">
            <span class="label">剩余支付时间：</span>
            <span class="countdown" v-if="order.status === 0">{{ countdownText }}</span>
            <el-tag v-else :type="order.status === 1 ? 'success' : 'info'">{{ statusName }}</el-tag>
          </div>
        </div>

        <div class="qr-area" v-if="order.status === 0">
          <div class="qr-placeholder">
            <div class="qr-text">模拟支付</div>
            <div class="qr-sub">课程项目不接真实网关<br>点击下方按钮模拟支付成功</div>
          </div>
          <el-button type="primary" size="large" :loading="paying" @click="doPay">模拟支付成功</el-button>
          <el-button size="large" @click="refreshStatus">查询支付结果</el-button>
        </div>

        <div class="paid-area" v-else>
          <el-result
            :icon="order.status === 4 ? 'warning' : 'success'"
            :title="statusName"
            :sub-title="order.status === 4 ? '订单已超时或已取消' : '支付成功，商家备货中'"
          >
            <template #extra>
              <el-button type="primary" @click="$router.replace('/order/list')">查看我的订单</el-button>
            </template>
          </el-result>
        </div>
      </template>
      <el-empty v-else description="订单不存在" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import orderHttp from '@/api/orderHttp'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const paying = ref(false)
const now = ref(Date.now())
let timer = null
let pollTimer = null

const statusName = computed(() => {
  const map = { 0: '待支付', 1: '待发货', 2: '运输中', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款' }
  return order.value ? map[order.value.status] || '未知' : ''
})

const countdownText = computed(() => {
  if (!order.value || !order.value.expireTime) return '-'
  const left = new Date(order.value.expireTime).getTime() - now.value
  if (left <= 0) return '已超时，等待系统取消'
  const m = Math.floor(left / 60000)
  const s = Math.floor((left % 60000) / 1000)
  return m + ' 分 ' + s + ' 秒'
})

const refreshStatus = async () => {
  const orderNo = route.query.orderNo
  if (!orderNo) return
  const res = await orderHttp.getOrderStatus(orderNo)
  order.value = res.data.order
}

const doPay = async () => {
  paying.value = true
  try {
    await orderHttp.payOrder(order.value.orderNo)
    // 模拟网关回调（幂等）：验证回调链路
    await orderHttp.payCallback(order.value.orderNo)
    ElMessage.success('支付成功')
    await refreshStatus()
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  const orderNo = route.query.orderNo
  if (!orderNo) {
    router.replace('/order/list')
    return
  }
  await refreshStatus()
  timer = setInterval(() => { now.value = Date.now() }, 1000)
  // 每 20 秒主动查询支付结果（第三方支付场景轮询用）
  pollTimer = setInterval(refreshStatus, 20000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.pay-card {
  max-width: 640px;
  margin: 0 auto;
}

.pay-info {
  margin-bottom: 20px;
}

.row {
  margin: 8px 0;
  color: #303133;
}

.label {
  color: #909399;
}

.amount {
  color: #f56c6c;
  font-size: 22px;
  font-weight: bold;
}

.countdown {
  color: #e6a23c;
  font-weight: bold;
}

.qr-area {
  text-align: center;
}

.qr-placeholder {
  width: 220px;
  height: 220px;
  margin: 0 auto 18px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.qr-text {
  font-size: 22px;
  color: #409eff;
  font-weight: bold;
}

.qr-sub {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  line-height: 1.8;
}
</style>
