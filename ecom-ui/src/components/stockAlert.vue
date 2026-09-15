<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="类型">
          <el-select v-model="query.alertType" placeholder="全部" clearable style="width: 130px">
            <el-option label="缺货预警" :value="0" />
            <el-option label="积压预警" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="未处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="warning" v-hasPermi="'stock:alert:scan'" :loading="scanning" @click="doScan">
            立即扫描一次
          </el-button>
        </el-form-item>
      </el-form>
      <div class="tip">定时任务每 6 小时自动扫描；阈值在系统参数表 ec_sys_config 配置（默认低 10 / 高 1000）</div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.alertType === 0 ? 'danger' : 'warning'">
              {{ row.alertType === 0 ? '缺货预警' : '积压预警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentStock" label="触发时库存" width="110" />
        <el-table-column prop="thresholdValue" label="阈值" width="90" />
        <el-table-column prop="skuId" label="SKU ID" width="90" />
        <el-table-column prop="productId" label="商品 ID" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'danger' : 'success'">
              {{ row.status === 0 ? '未处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="触发时间" min-width="170">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" min-width="170">
          <template #default="{ row }">{{ fmt(row.handleTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              v-hasPermi="'stock:alert:handle'"
              :disabled="row.status === 1"
              @click="doHandle(row)"
            >标记已处理</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import stockHttp from '@/api/stockHttp'

const loading = ref(false)
const scanning = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, alertType: null, status: null })

const loadPage = async () => {
  loading.value = true
  try {
    const res = await stockHttp.selectAlertByPage(query)
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
  query.alertType = null
  query.status = null
  handleSearch()
}

const handleSizeChange = () => {
  query.pageNum = 1
  loadPage()
}

const doScan = async () => {
  scanning.value = true
  try {
    const res = await stockHttp.scanAlerts()
    ElMessage.success('扫描完成，新生成 ' + res.data + ' 条预警')
    loadPage()
  } finally {
    scanning.value = false
  }
}

const doHandle = async row => {
  await stockHttp.handleAlert(row.id)
  ElMessage.success('已标记处理')
  loadPage()
}

const fmt = t => (t ? String(t).replace('T', ' ').substring(0, 19) : '-')

onMounted(loadPage)
</script>

<style scoped>
.table-card {
  margin-top: 16px;
}

.tip {
  color: #909399;
  font-size: 12px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
