import request from '@/util/request'

// 今日实时指标（Redis 缓存 5 分钟）
function getRealtime() {
  return request.get('/dashboard/realtime')
}

// 商品排行（热销/滞销 TOP10）
function getProductRank() {
  return request.get('/dashboard/productRank')
}

// 类目销售占比
function getCategoryAnalysis() {
  return request.get('/dashboard/categoryAnalysis')
}

// 手动重算最近 N 天汇总
function aggregate(days) {
  return request.post('/dashboard/aggregate?days=' + days)
}

export default {
  getRealtime,
  getProductRank,
  getCategoryAnalysis,
  aggregate
}
