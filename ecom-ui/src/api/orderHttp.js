import request from '@/util/request'

// 下单（幂等键防重复提交）
function createOrder(data) {
  return request.post('/order', data)
}

// 模拟支付
function payOrder(orderNo) {
  return request.post('/order/' + orderNo + '/pay')
}

// 支付回调（模拟网关，幂等）
function payCallback(orderNo) {
  return request.post('/order/pay/callback?orderNo=' + orderNo)
}

// 查询订单状态
function getOrderStatus(orderNo) {
  return request.get('/order/' + orderNo + '/status')
}

// 我的订单分页
function selectMyOrderByPage(data) {
  return request.get('/order/mine', { params: data })
}

export default {
  createOrder,
  payOrder,
  payCallback,
  getOrderStatus,
  selectMyOrderByPage
}
