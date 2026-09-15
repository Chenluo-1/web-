<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>我的订单</span>
          <el-radio-group v-model="query.status" size="small" @change="loadPage">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="0">待支付</el-radio-button>
            <el-radio-button :value="1">待发货</el-radio-button>
            <el-radio-button :value="2">运输中</el-radio-button>
            <el-radio-button :value="3">已完成</el-radio-button>
            <el-radio-button :value="4">已取消</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div v-for="vo in list" :key="vo.order.id" class="order-block">
        <div class="order-head">
          <span class="order-no">订单号：{{ vo.order.orderNo }}</span>
          <span class="order-time">{{ formatTime(vo.order.createTime) }}</span>
          <el-tag :type="statusType(vo.order.status)">{{ vo.statusName }}</el-tag>
        </div>
        <div v-for="item in vo.items" :key="item.id" class="order-item">
          <el-image :src="item.productImage" fit="cover" class="thumb">
            <template #error><div class="thumb-holder">无图</div></template>
          </el-image>
          <div class="item-mid">
            <div>{{ item.productName }}</div>
            <div class="spec">{{ formatSpecs(item.specs) }}</div>
          </div>
          <div class="item-price">¥{{ item.price }} × {{ item.quantity }}</div>
        </div>
        <div class="order-foot">
          <span>共 {{ totalQty(vo) }} 件，实付：<span class="pay-amount">¥{{ vo.order.payAmount }}</span></span>
          <div>
            <el-button
              v-if="vo.order.status === 0"
              type="danger"
              size="small"
              @click="$router.push('/order/pay?orderNo=' + vo.order.orderNo)"
            >去支付</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !list.length" description="暂无订单" />

      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        @current-change="loadPage"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import orderHttp from '@/api/orderHttp'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 5, status: null })

const loadPage = async () => {
  loading.value = true
  try {
    const res = await orderHttp.selectMyOrderByPage(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const statusType = status => {
  const map = { 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'warning', 6: 'info' }
  return map[status] || 'info'
}

const totalQty = vo => vo.items.reduce((s, i) => s + i.quantity, 0)

const formatSpecs = specs => {
  try {
    const map = JSON.parse(specs || '{}')
    return Object.keys(map).map(k => k + ': ' + map[k]).join(' / ')
  } catch (e) {
    return specs
  }
}

const formatTime = t => (t ? String(t).replace('T', ' ').substring(0, 19) : '')

onMounted(loadPage)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-block {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 14px;
}

.order-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #ebeef5;
}

.order-no {
  font-weight: bold;
  color: #303133;
}

.order-time {
  color: #909399;
  font-size: 12px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
}

.thumb {
  width: 56px;
  height: 56px;
}

.thumb-holder {
  width: 56px;
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #c0c4cc;
  background: #f5f7fa;
  font-size: 12px;
}

.item-mid {
  flex: 1;
}

.spec {
  color: #909399;
  font-size: 12px;
  margin-top: 3px;
}

.item-price {
  color: #606266;
}

.order-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
}

.pay-amount {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.pager {
  margin-top: 6px;
  justify-content: flex-end;
}
</style>
