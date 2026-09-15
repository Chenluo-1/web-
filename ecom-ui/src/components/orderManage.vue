<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="待支付" :value="0" />
            <el-option label="待发货" :value="1" />
            <el-option label="运输中" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="退款中" :value="5" />
            <el-option label="已退款" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="order.orderNo" label="订单号" min-width="200" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div v-for="item in row.items" :key="item.id" class="item-line">
              {{ item.productName }} × {{ item.quantity }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="order.receiverName" label="收货人" width="100" />
        <el-table-column label="实付" width="100">
          <template #default="{ row }"><span class="pay">¥{{ row.order.payAmount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.order.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="170">
          <template #default="{ row }">{{ fmt(row.order.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button
              v-if="row.order.status === 1"
              link type="success"
              v-hasPermi="'order:ship'"
              @click="openShip(row)"
            >发货</el-button>
            <el-button
              v-if="row.order.status === 5"
              link type="warning"
              v-hasPermi="'order:refund'"
              @click="openAudit(row)"
            >审核退款</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[5, 10, 20]"
        @current-change="loadPage"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 发货 -->
    <el-dialog v-model="shipVisible" title="订单发货" width="440px">
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="90px">
        <el-form-item label="物流公司" prop="company">
          <el-select v-model="shipForm.company" placeholder="选择物流公司" style="width: 100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="京东物流" value="京东物流" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号" prop="trackingNo">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入运单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 审核退款 -->
    <el-dialog v-model="auditVisible" title="退款审核" width="460px">
      <template v-if="auditRefundInfo">
        <el-descriptions :column="1" border class="audit-info">
          <el-descriptions-item label="订单号">{{ auditRefundInfo.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">¥{{ auditRefundInfo.amount }}</el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ auditRefundInfo.reason }}</el-descriptions-item>
        </el-descriptions>
        <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px" class="audit-form">
          <el-form-item label="审核结果" prop="approve">
            <el-radio-group v-model="auditForm.approve">
              <el-radio :value="true">通过（退款并回滚库存）</el-radio>
              <el-radio :value="false">驳回（订单回待发货）</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核备注">
            <el-input v-model="auditForm.remark" placeholder="选填" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import orderHttp from '@/api/orderHttp'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, orderNo: '', status: null })

const loadPage = async () => {
  loading.value = true
  try {
    const res = await orderHttp.selectManageOrderByPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  loadPage()
}

const resetQuery = () => {
  query.orderNo = ''
  query.status = null
  handleSearch()
}

const handleSizeChange = () => {
  query.pageNum = 1
  loadPage()
}

const statusType = s => ({ 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'warning', 6: 'info' }[s] || 'info')
const fmt = t => (t ? String(t).replace('T', ' ').substring(0, 19) : '')

const goDetail = row => {
  router.push('/order/detail?orderNo=' + row.order.orderNo + '&manage=1')
}

// ---------- 发货 ----------
const shipVisible = ref(false)
const shipFormRef = ref(null)
const shipForm = reactive({ company: '顺丰速运', trackingNo: '' })
const shipRules = {
  company: [{ required: true, message: '请选择物流公司', trigger: 'change' }],
  trackingNo: [{ required: true, message: '请填写运单号', trigger: 'blur' }]
}
let shipOrderNo = null

const openShip = row => {
  shipOrderNo = row.order.orderNo
  shipForm.trackingNo = 'SF' + Date.now()
  shipVisible.value = true
}

const submitShip = () => {
  shipFormRef.value.validate(async valid => {
    if (!valid) return
    await orderHttp.shipOrder(shipOrderNo, shipForm)
    ElMessage.success('发货成功')
    shipVisible.value = false
    loadPage()
  })
}

// ---------- 审核退款 ----------
const auditVisible = ref(false)
const auditFormRef = ref(null)
const auditRefundInfo = ref(null)
const auditForm = reactive({ approve: true, remark: '' })
const auditRules = { approve: [{ required: true, message: '请选择审核结果', trigger: 'change' }] }

const openAudit = async row => {
  const res = await orderHttp.getManageOrderDetail(row.order.orderNo)
  auditRefundInfo.value = res.data.refund
  if (!auditRefundInfo.value) {
    ElMessage.warning('该订单没有待审核的退款单')
    return
  }
  auditForm.approve = true
  auditForm.remark = ''
  auditVisible.value = true
}

const submitAudit = () => {
  auditFormRef.value.validate(async valid => {
    if (!valid) return
    await orderHttp.auditRefund(auditRefundInfo.value.id, auditForm)
    ElMessage.success(auditForm.approve ? '已通过退款' : '已驳回退款')
    auditVisible.value = false
    loadPage()
  })
}

onMounted(loadPage)
</script>

<style scoped>
.table-card {
  margin-top: 16px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.item-line {
  color: #606266;
  font-size: 13px;
}

.pay {
  color: #f56c6c;
  font-weight: bold;
}

.audit-info {
  margin-bottom: 14px;
}

.audit-form {
  margin-top: 6px;
}
</style>
