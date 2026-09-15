<template>
  <div v-if="detail">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>订单详情：{{ detail.order.orderNo }}</span>
          <el-tag :type="statusType(detail.order.status)" size="large">{{ detail.statusName }}</el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="收货人">{{ detail.order.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.order.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ detail.order.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="商品总额">¥{{ detail.order.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">-¥{{ detail.order.discountAmount }}</el-descriptions-item>
        <el-descriptions-item label="实付金额"><span class="pay">¥{{ detail.order.payAmount }}</span></el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ fmt(detail.order.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 操作按钮：随状态显隐 -->
      <div class="actions" v-if="!isManage">
        <template v-if="detail.order.status === 0">
          <el-button type="primary" @click="$router.push('/order/pay?orderNo=' + detail.order.orderNo)">去支付</el-button>
          <el-button type="danger" plain @click="cancel">取消订单</el-button>
        </template>
        <el-button v-if="detail.order.status === 1" type="warning" @click="refundVisible = true">申请退款</el-button>
        <el-button v-if="detail.order.status === 2" type="success" @click="receive">确认收货</el-button>
        <el-alert v-if="detail.order.status === 5" type="warning" :closable="false" title="退款申请审核中，请等待商家处理" class="alert-inline" />
        <el-alert v-if="detail.order.status === 3" type="success" :closable="false" title="订单已完成，如有问题可联系客服" class="alert-inline" />
        <el-alert v-if="detail.order.status === 4" type="info" :closable="false" title="订单已取消" class="alert-inline" />
        <el-alert v-if="detail.order.status === 6" type="info" :closable="false" title="退款已完成" class="alert-inline" />
      </div>

      <el-divider content-position="left">商品清单</el-divider>
      <el-table :data="detail.items" size="small">
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div class="item-cell">
              <el-image :src="row.productImage" fit="cover" class="thumb">
                <template #error><div class="thumb-holder">无图</div></template>
              </el-image>
              <div>
                <div>{{ row.productName }}</div>
                <div class="spec">{{ formatSpecs(row.specs) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column label="小计" width="110">
          <template #default="{ row }">¥{{ row.subtotal }}</template>
        </el-table-column>
        <el-table-column v-if="!isManage && detail.order.status === 3" label="评价" width="90">
          <template #default="{ row }">
            <el-tag v-if="reviewedIds.includes(row.id)" type="info" size="small">已评价</el-tag>
            <el-button v-else size="small" type="primary" plain @click="openReview(row)">评价</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-row :gutter="16" class="track-area">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>订单状态时间轴</template>
            <el-timeline>
              <el-timeline-item
                v-for="log in detail.logs"
                :key="log.id"
                :timestamp="fmt(log.createTime)"
                placement="top"
                :type="timelineType(log.toStatus)"
              >
                <div>{{ statusText(log.toStatus) }}<span class="op">（{{ operatorText(log.operatorType) }}）</span></div>
                <div v-if="log.remark" class="remark">{{ log.remark }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              物流轨迹
              <span v-if="detail.logistics" class="lg-info">
                {{ detail.logistics.company }} ｜ {{ detail.logistics.trackingNo }}
              </span>
            </template>
            <el-timeline v-if="detail.logistics">
              <el-timeline-item
                v-for="(t, i) in traceList"
                :key="i"
                :timestamp="t.time"
                placement="top"
                :type="i === 0 ? 'primary' : ''"
              >{{ t.text }}</el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无物流信息（发货后可查看）" :image-size="70" />
          </el-card>
        </el-col>
      </el-row>

      <div v-if="detail.refund" class="refund-info">
        <el-divider content-position="left">退款信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="退款金额">¥{{ detail.refund.amount }}</el-descriptions-item>
          <el-descriptions-item label="退款状态">
            {{ ['待审核', '已通过', '已拒绝'][detail.refund.status] }}
          </el-descriptions-item>
          <el-descriptions-item label="退款原因" :span="2">{{ detail.refund.reason }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.refund.auditRemark" label="审核备注" :span="2">
            {{ detail.refund.auditRemark }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <!-- 申请退款 -->
    <el-dialog v-model="refundVisible" title="申请退款" width="440px">
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="80px">
        <el-form-item label="退款原因" prop="reason">
          <el-input v-model="refundForm.reason" type="textarea" :rows="3" placeholder="请填写退款原因" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 商品评价（简化版：星级 + 文字） -->
    <el-dialog v-model="reviewVisible" title="商品评价" width="440px">
      <div class="rv-product">{{ reviewForm.productName }}</div>
      <el-form label-width="60px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说说使用感受，帮助其他买家参考"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import orderHttp from '@/api/orderHttp'
import reviewHttp from '@/api/reviewHttp'

const route = useRoute()
const detail = ref(null)
const isManage = route.query.manage === '1'

const loadDetail = async () => {
  const res = isManage
    ? await orderHttp.getManageOrderDetail(route.query.orderNo)
    : await orderHttp.getOrderDetail(route.query.orderNo)
  detail.value = res.data
  loadReviewed()
}

// ---------- 评价（简化版） ----------
const reviewedIds = ref([])
const reviewVisible = ref(false)
const reviewForm = reactive({ orderItemId: null, productName: '', rating: 5, content: '' })

const loadReviewed = async () => {
  if (isManage || !detail.value || detail.value.order.status !== 3) {
    reviewedIds.value = []
    return
  }
  try {
    const res = await reviewHttp.selectMyByOrder(detail.value.order.orderNo)
    reviewedIds.value = (res.data || []).map(r => r.orderItemId)
  } catch (e) {
    reviewedIds.value = []
  }
}

const openReview = row => {
  reviewForm.orderItemId = row.id
  reviewForm.productName = row.productName
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

const submitReview = async () => {
  await reviewHttp.addReview({
    orderItemId: reviewForm.orderItemId,
    rating: reviewForm.rating,
    content: reviewForm.content
  })
  ElMessage.success('评价成功，感谢您的反馈')
  reviewVisible.value = false
  loadReviewed()
}

const traceList = computed(() => {
  if (!detail.value || !detail.value.logistics) return []
  try {
    const list = JSON.parse(detail.value.logistics.traceJson || '[]')
    return [...list].reverse()
  } catch (e) {
    return []
  }
})

const statusType = s => ({ 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'warning', 6: 'info' }[s] || 'info')
const statusText = s => ({ '-1': '创建订单', 0: '待支付', 1: '待发货', 2: '运输中', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款' }[String(s)] || '')
const timelineType = s => ({ 0: 'warning', 1: 'primary', 2: 'primary', 3: 'success', 4: 'info', 5: 'warning', 6: 'info' }[s] || '')
const operatorText = t => ({ 1: '用户', 2: '管理员', 3: '系统' }[t] || '')
const fmt = t => (t ? String(t).replace('T', ' ').substring(0, 19) : '')
const formatSpecs = specs => {
  try {
    const map = JSON.parse(specs || '{}')
    return Object.keys(map).map(k => k + ': ' + map[k]).join(' / ')
  } catch (e) {
    return specs
  }
}

const cancel = () => {
  ElMessageBox.confirm('确定取消该订单吗？库存和优惠券将释放。', '提示', { type: 'warning' })
    .then(async () => {
      await orderHttp.cancelOrder(detail.value.order.orderNo)
      ElMessage.success('订单已取消')
      loadDetail()
    })
    .catch(() => {})
}

const receive = () => {
  ElMessageBox.confirm('确认已收到货物吗？', '提示', { type: 'info' })
    .then(async () => {
      await orderHttp.receiveOrder(detail.value.order.orderNo)
      ElMessage.success('确认收货成功')
      loadDetail()
    })
    .catch(() => {})
}

const refundVisible = ref(false)
const refundFormRef = ref(null)
const refundForm = reactive({ reason: '' })
const refundRules = { reason: [{ required: true, message: '请填写退款原因', trigger: 'blur' }] }

const submitRefund = () => {
  refundFormRef.value.validate(async valid => {
    if (!valid) return
    await orderHttp.applyRefund(detail.value.order.orderNo, refundForm)
    ElMessage.success('退款申请已提交，等待商家审核')
    refundVisible.value = false
    loadDetail()
  })
}

onMounted(loadDetail)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pay {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.actions {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.alert-inline {
  width: auto;
}

.item-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.thumb {
  width: 50px;
  height: 50px;
}

.thumb-holder {
  width: 50px;
  height: 50px;
  line-height: 50px;
  text-align: center;
  color: #c0c4cc;
  background: #f5f7fa;
  font-size: 12px;
}

.spec {
  color: #909399;
  font-size: 12px;
}

.track-area {
  margin-top: 16px;
}

.op {
  color: #909399;
  font-size: 12px;
}

.remark {
  color: #606266;
  font-size: 13px;
  margin-top: 2px;
}

.lg-info {
  color: #909399;
  font-size: 12px;
  font-weight: normal;
  margin-left: 10px;
}

.refund-info {
  margin-top: 8px;
}

.rv-product {
  color: #606266;
  font-size: 13px;
  margin-bottom: 12px;
  padding-left: 4px;
}
</style>
