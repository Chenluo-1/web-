<template>
  <div>
    <el-card shadow="never">
      <div class="bar">
        <el-select v-model="selectedSkuId" placeholder="选择 SKU" filterable style="width: 320px" @change="loadData">
          <el-option
            v-for="s in skuOptions"
            :key="s.skuId"
            :label="s.productTitle + '（' + formatSpecs(s.specs) + '）'"
            :value="s.skuId"
          />
        </el-select>
        <el-button type="warning" v-hasPermi="'stock:forecast:generate'" :loading="generating" @click="doGenerate">
          重新生成预测（每日 2:00 也会自动跑）
        </el-button>
      </div>
      <el-alert
        type="info"
        :closable="false"
        class="note"
        title="预测为规则版建议（加权移动平均 0.5/0.3/0.2 × 季节/促销系数），仅供备货参考，不会自动补货；新品无历史销量时用同类目均值兜底，突发活动期间可能偏差较大。"
      />
    </el-card>

    <el-card shadow="never" class="chart-card">
      <template #header>
        历史实际销量 vs 未来 28 天预测
        <el-tag v-if="fallback" type="warning" size="small" class="fallback-tag">新品兜底值</el-tag>
      </template>
      <div ref="chartRef" class="chart"></div>
      <el-empty v-if="!data" description="暂无预测数据，请先点「重新生成预测」" :image-size="80" />
    </el-card>

    <el-card shadow="never" class="table-card" v-if="data">
      <template #header>未来 28 天预测明细（合计 {{ total28 }} 件）</template>
      <el-table :data="data.forecast" size="small" border>
        <el-table-column prop="date" label="日期" min-width="120" />
        <el-table-column prop="qty" label="预测销量（件）" min-width="140" />
        <el-table-column label="星期" min-width="90">
          <template #default="{ row }">{{ weekday(row.date) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import stockHttp from '@/api/stockHttp'

const skuOptions = ref([])
const selectedSkuId = ref(null)
const data = ref(null)
const generating = ref(false)
const fallback = computed(() => data.value && data.value.fallback)

const chartRef = ref(null)
let chart = null

const total28 = computed(() =>
  data.value ? data.value.forecast.reduce((s, p) => s + (p.qty || 0), 0) : 0
)

const loadSkus = async () => {
  const res = await stockHttp.selectForecastSkus()
  skuOptions.value = res.data
  if (!selectedSkuId.value && skuOptions.value.length) {
    selectedSkuId.value = skuOptions.value[0].skuId
  }
}

const loadData = async () => {
  if (!selectedSkuId.value) {
    data.value = null
    return
  }
  const res = await stockHttp.getForecastData(selectedSkuId.value)
  data.value = res.data
  await nextTick()
  renderChart()
}

const renderChart = () => {
  if (!chartRef.value || !data.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const history = data.value.history
  const forecast = data.value.forecast
  const dates = [...history.map(p => p.date), ...forecast.map(p => p.date)]
  const historySeries = [...history.map(p => p.qty), ...forecast.map(() => null)]
  const forecastSeries = [...history.map(() => null), ...forecast.map(p => p.qty)]
  // 让预测线从最后一个实际点接上
  forecastSeries[history.length - 1] = history[history.length - 1].qty

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['历史实际', '预测'] },
    grid: { left: 50, right: 30, top: 40, bottom: 50 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '历史实际', type: 'line', data: historySeries, smooth: true, connectNulls: false, itemStyle: { color: '#409eff' } },
      { name: '预测', type: 'line', data: forecastSeries, smooth: true, connectNulls: false, lineStyle: { type: 'dashed' }, itemStyle: { color: '#e6a23c' } }
    ]
  })
  chart.resize()
}

const doGenerate = async () => {
  generating.value = true
  try {
    const res = await stockHttp.generateForecast()
    ElMessage.success('预测生成完成，共 ' + res.data + ' 行（28 天 × SKU）')
    await loadSkus()
    await loadData()
  } finally {
    generating.value = false
  }
}

const formatSpecs = specs => {
  try {
    const map = JSON.parse(specs || '{}')
    return Object.keys(map).map(k => map[k]).join('/')
  } catch (e) {
    return specs
  }
}

const weekday = date => ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][new Date(date).getDay()] || ''

const onResize = () => chart && chart.resize()

onMounted(async () => {
  await loadSkus()
  await loadData()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (chart) chart.dispose()
})
</script>

<style scoped>
.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.note {
  margin-top: 12px;
}

.chart-card {
  margin-top: 16px;
}

.chart {
  height: 360px;
}

.fallback-tag {
  margin-left: 10px;
}

.table-card {
  margin-top: 16px;
}
</style>
