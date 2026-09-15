<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" v-hasPermi="'coupon:add'" @click="openAdd">新增优惠券</el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 0 ? 'danger' : 'warning'">{{ row.type === 0 ? '满减' : '折扣' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规则" min-width="150">
          <template #default="{ row }">
            满 {{ row.thresholdAmount }} 元
            <template v-if="row.type === 0">减 {{ row.discountAmount }} 元</template>
            <template v-else>打 {{ (Number(row.discountRate) * 10).toFixed(1) }} 折</template>
          </template>
        </el-table-column>
        <el-table-column label="发放" width="120">
          <template #default="{ row }">{{ row.receivedCount }} / {{ row.totalCount || '不限' }}</template>
        </el-table-column>
        <el-table-column prop="perUserLimit" label="每人限领" width="90" />
        <el-table-column label="有效期" min-width="200">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }} ~ {{ formatDate(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="'coupon:edit'" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" v-hasPermi="'coupon:del'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑优惠券' : '新增优惠券'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">满减</el-radio>
            <el-radio :value="1">折扣</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用门槛" prop="thresholdAmount">
          <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" /> 元
        </el-form-item>
        <el-form-item v-if="form.type === 0" label="满减金额" prop="discountAmount">
          <el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" /> 元
        </el-form-item>
        <el-form-item v-else label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0.1" :max="0.99" :step="0.05" :precision="2" />
          （0.85 = 8.5 折）
        </el-form-item>
        <el-form-item label="发放总量">
          <el-input-number v-model="form.totalCount" :min="0" />
          <span class="tip">0 = 不限</span>
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perUserLimit" :min="1" />
        </el-form-item>
        <el-form-item label="有效期" prop="dateRange">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="0" :inactive-value="1" active-text="正常" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import couponHttp from '@/api/couponHttp'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

const loadPage = async () => {
  loading.value = true
  try {
    const res = await couponHttp.selectCouponByPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const formRef = ref(null)
const dateRange = ref([])
const form = reactive({
  id: null, name: '', type: 0, thresholdAmount: 100, discountAmount: 10,
  discountRate: 0.85, totalCount: 0, perUserLimit: 1, status: 0
})
const rules = {
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  dateRange: [{ required: true, message: '请选择有效期', trigger: 'change' }]
}

const openAdd = () => {
  Object.assign(form, {
    id: null, name: '', type: 0, thresholdAmount: 100, discountAmount: 10,
    discountRate: 0.85, totalCount: 0, perUserLimit: 1, status: 0
  })
  dateRange.value = []
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, row)
  dateRange.value = [formatDate(row.startTime), formatDate(row.endTime)]
  dialogVisible.value = true
}

const submit = () => {
  formRef.value.validate(async valid => {
    if (!valid) return
    if (!dateRange.value || dateRange.value.length !== 2) {
      ElMessage.warning('请选择有效期')
      return
    }
    const payload = { ...form, startTime: dateRange.value[0], endTime: dateRange.value[1] }
    if (form.id) {
      await couponHttp.updateCoupon(payload)
      ElMessage.success('修改成功')
    } else {
      await couponHttp.addCoupon(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadPage()
  })
}

const handleDelete = row => {
  ElMessageBox.confirm(`确定删除优惠券「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await couponHttp.delCoupon([row.id])
      ElMessage.success('删除成功')
      loadPage()
    })
    .catch(() => {})
}

const formatDate = t => (t ? String(t).replace('T', ' ').substring(0, 19) : '')

onMounted(loadPage)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
