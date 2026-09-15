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

    <!-- 实时指标卡片（营销数据权限） -->
    <el-row :gutter="14" v-if="canMarket && realtime">
      <el-col :span="5" v-for="card in realtimeCards" :key="card.label">
        <el-card shadow="never" class="metric-card">
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
      </el-col>
    </el-row>

    <!-- 趋势 -->
    <el-card shadow="never" class="chart-card" v-if="canMarket">
      <template #header>
        <div class="bar">
          <span>销售趋势（GMV，支付成功口径）</span>
          <el-radio-group v-model="dimension" size="small" @change="loadTrend">
            <el-radio-button value="day">日</el-radio-button>
            <el-radio-button value="week">周</el-radio-button>
            <el-radio-button value="month">月</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendRef" class="chart"></div>
    </el-card>

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

    <!-- 漏斗 -->
    <el-card shadow="never" class="chart-card" v-if="canMarket">
      <template #header>订单漏斗（{{ funnelRange }}：曝光→点击→加购→成交）</template>
      <div ref="funnelRef" class="chart"></div>
    </el-card>
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
const dimension = ref('day')
const rankTab = ref('hot')
const rankRange = ref('近 30 天')
const funnelRange = ref('近 7 天')
const autoRefresh = ref(false)
const aggregating = ref(false)
let refreshTimer = null

const trendRef = ref(null)
const rankRef = ref(null)
const pieRef = ref(null)
const funnelRef = ref(null)
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

let trendData = null
const loadTrend = async () => {
  if (!canMarket.value) return
  const res = await dashboardHttp.getSalesTrend(dimension.value)
  trendData = res.data
  await nextTick()
  renderTrend()
}

const renderTrend = () => {
  const chart = initChart(trendRef.value, 'trend')
  if (!chart || !trendData) return
  const points = trendData.points
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        const p = points[params[0].dataIndex]
        return `${p.label}<br/>GMV：¥${p.gmv}<br/>支付单数：${p.orderCount}` +
          `<br/>环比：${p.mom === null ? '-' : p.mom + '%'}<br/>同比：${p.yoy === null ? '-' : p.yoy + '%'}`
      }
    },
    grid: { left: 70, right: 30, top: 30, bottom: 60 },
    xAxis: { type: 'category', data: points.map(p => p.label), axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value' },
    series: [{
      name: 'GMV',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.15 },
      data: points.map(p => p.gmv),
      itemStyle: { color: '#409eff' }
    }]
  })
  chart.resize()
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

const loadFunnel = async () => {
  if (!canMarket.value) return
  const res = await dashboardHttp.getOrderFunnel()
  const f = res.data
  funnelRange.value = f.range
  await nextTick()
  const chart = initChart(funnelRef.value, 'funnel')
  if (!chart) return
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c}' },
    series: [{
      name: '漏斗',
      type: 'funnel',
      left: '10%',
      width: '80%',
      minSize: '18%',
      sort: 'descending',
      label: { show: true, position: 'inside', formatter: '{b}：{c}' },
      data: [
        { value: f.expose, name: '曝光' },
        { value: f.click, name: '点击' },
        { value: f.cart, name: '加购' },
        { value: f.order, name: '成交' }
      ]
    }]
  })
  chart.resize()
}

const loadAll = async () => {
  await Promise.all([loadRealtime(), loadTrend(), loadRank(), loadPie(), loadFunnel()])
}

const doAggregate = async () => {
  aggregating.value = true
  try {
    const res = await dashboardHttp.aggregate(7)
    ElMessage.success('已重算最近 ' + res.data + ' 天汇总')
    await Promise.all([loadTrend(), loadRank(), loadPie()])
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

.metric-card {
  text-align: center;
  margin-bottom: 14px;
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
