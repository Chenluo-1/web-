<template>
  <div>
    <!-- 工具条 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="bar">
        <span class="title">数据看板</span>
        <div class="tools">
          <el-button
            v-if="canRefresh"
            size="small"
            :loading="aggregating"
            @click="doAggregate"
          >重算最近 7 天汇总</el-button>
          <span class="auto-label">自动刷新（30 秒）</span>
          <el-switch v-model="autoRefresh" @change="toggleAuto" />
        </div>
      </div>
    </el-card>

    <!-- 实时指标卡片（营销数据权限）：CSS Grid 五列自适应，窄屏自动折行 -->
    <div class="metrics" v-if="canMarket && realtime">
      <el-card shadow="never" class="metric-card" v-for="card in realtimeCards" :key="card.label">
        <div class="metric-label">{{ card.label }}</div>
        <div class="metric-value">{{ card.value }}</div>
        <div class="metric-mom" v-if="card.mom !== undefined">
          <template v-if="card.mom === null">环比：-</template>
          <template v-else>
            <span :class="Number(card.mom) >= 0 ? 'up' : 'down'">
              {{ Number(card.mom) >= 0 ? '↑' : '↓' }}{{ Math.abs(card.mom) }}%
            </span>
            <span class="mom-label">环比昨日</span>
          </template>
        </div>
      </el-card>
    </div>

    <el-row :gutter="14" v-if="canProduct">
      <!-- 商品排行 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="bar">
              <span>商品排行（{{ rankRange }}）</span>
              <el-radio-group v-model="rankTab" size="small">
                <el-radio-button value="hot">热销 TOP10</el-radio-button>
                <el-radio-button value="cold">滞销 TOP10</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="rankRef" class="chart"></div>
        </el-card>
      </el-col>
      <!-- 类目占比 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>类目销售占比（近 30 天，一级类目）</template>
          <div ref="pieRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import dashboardHttp from '@/api/dashboardHttp'
import useInfo from '@/store/userInfo'

const userStore = useInfo()
const canMarket = computed(() => {
  const roles = userStore.userInfo.roles || []
  const perms = userStore.userInfo.permissions || []
  return roles.includes('super_admin') || perms.includes('dashboard:market')
})
const canProduct = computed(() => {
  const roles = userStore.userInfo.roles || []
  const perms = userStore.userInfo.permissions || []
  return roles.includes('super_admin') || perms.includes('dashboard:product')
})
const canRefresh = computed(() => {
  const roles = userStore.userInfo.roles || []
  const perms = userStore.userInfo.permissions || []
  return roles.includes('super_admin') || perms.includes('dashboard:refresh')
})

const realtime = ref(null)
const rankTab = ref('hot')
const rankRange = ref('近 30 天')
const autoRefresh = ref(false)
const aggregating = ref(false)
let refreshTimer = null

const rankRef = ref(null)
const pieRef = ref(null)
const charts = {}

const realtimeCards = computed(() => {
  const rt = realtime.value
  if (!rt) return []
  return [
    { label: '今日 GMV（元）', value: rt.gmv, mom: rt.gmvMoM },
    { label: '支付订单量', value: rt.paidCount, mom: rt.paidCountMoM },
    { label: '客单价（元）', value: rt.avgAmount },
    { label: '访客数', value: rt.visitorCount, mom: rt.visitorMoM },
    { label: '转化率（%）', value: rt.conversion === null ? '-' : rt.conversion }
  ]
})

const initChart = (refEl, key) => {
  if (!refEl) return null
  if (!charts[key]) {
    charts[key] = echarts.init(refEl)
  }
  return charts[key]
}

// ---------- 数据加载 ----------
const loadRealtime = async () => {
  if (!canMarket.value) return
  try {
    const res = await dashboardHttp.getRealtime()
    realtime.value = res.data
  } catch (e) { /* 403 等静默 */ }
}

const rankData = ref(null)
const loadRank = async () => {
  if (!canProduct.value) return
  const res = await dashboardHttp.getProductRank()
  rankData.value = res.data
  rankRange.value = res.data.range
  await nextTick()
  renderRank()
}

const renderRank = () => {
  const chart = initChart(rankRef.value, 'rank')
  if (!chart || !rankData.value) return
  const list = rankTab.value === 'hot' ? rankData.value.hot : rankData.value.cold
  const rows = [...list].reverse()
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 40, top: 20, bottom: 20, containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: rows.map(i => i.title) },
    series: [{
      name: '销量',
      type: 'bar',
      data: rows.map(i => i.qty),
      itemStyle: { color: rankTab.value === 'hot' ? '#f56c6c' : '#909399' },
      label: { show: true, position: 'right' }
    }]
  })
  chart.resize()
}

const loadPie = async () => {
  if (!canProduct.value) return
  const res = await dashboardHttp.getCategoryAnalysis()
  await nextTick()
  const chart = initChart(pieRef.value, 'pie')
  if (!chart) return
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：¥{c}（{d}%）' },
    legend: { orient: 'vertical', left: 0 },
    series: [{
      name: '类目销售额',
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['55%', '50%'],
      data: res.data.map(c => ({ name: c.name, value: c.amount })),
      label: { formatter: '{b}\n{d}%' }
    }]
  })
  chart.resize()
}

const loadAll = async () => {
  await Promise.all([loadRealtime(), loadRank(), loadPie()])
}

const doAggregate = async () => {
  aggregating.value = true
  try {
    const res = await dashboardHttp.aggregate(7)
    ElMessage.success('已重算最近 ' + res.data + ' 天汇总')
    await Promise.all([loadRank(), loadPie()])
  } finally {
    aggregating.value = false
  }
}

const toggleAuto = val => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (val) {
    refreshTimer = setInterval(loadAll, 30000)
    ElMessage.success('已开启自动刷新（30 秒）')
  }
}

watch(rankTab, () => renderRank())

const onResize = () => Object.values(charts).forEach(c => c.resize())

onMounted(async () => {
  await loadAll()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (refreshTimer) clearInterval(refreshTimer)
  Object.values(charts).forEach(c => c.dispose())
})
</script>

<style scoped>
.toolbar-card {
  margin-bottom: 14px;
}

.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 16px;
  font-weight: bold;
}

.tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.auto-label {
  color: #909399;
  font-size: 13px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}

.metrics .metric-card {
  margin-bottom: 0;
}

@media (max-width: 1280px) {
  .metrics {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 760px) {
  .metrics {
    grid-template-columns: repeat(2, 1fr);
  }
}

.metric-card {
  text-align: center;
}

.metric-label {
  color: #909399;
  font-size: 13px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin: 8px 0 4px;
}

.metric-mom {
  font-size: 12px;
  color: #909399;
}

.up {
  color: #67c23a;
}

.down {
  color: #f56c6c;
}

.mom-label {
  margin-left: 6px;
}

.chart-card {
  margin-bottom: 14px;
}

.chart {
  height: 320px;
}
</style>
