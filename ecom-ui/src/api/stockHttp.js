import request from '@/util/request'

// 预警分页
function selectAlertByPage(data) {
  return request.get('/stock/alert', { params: data })
}

// 未处理预警数
function getUnhandledCount() {
  return request.get('/stock/alert/unhandledCount')
}

// 手动扫描
function scanAlerts() {
  return request.post('/stock/alert/scan')
}

// 标记已处理
function handleAlert(id) {
  return request.put('/stock/alert/' + id + '/handle')
}

// 预测 SKU 列表
function selectForecastSkus() {
  return request.get('/stock/forecast/skus')
}

// 单 SKU 预测数据
function getForecastData(skuId) {
  return request.get('/stock/forecast/' + skuId)
}

// 手动生成预测
function generateForecast() {
  return request.post('/stock/forecast/generate')
}

export default {
  selectAlertByPage,
  getUnhandledCount,
  scanAlerts,
  handleAlert,
  selectForecastSkus,
  getForecastData,
  generateForecast
}
