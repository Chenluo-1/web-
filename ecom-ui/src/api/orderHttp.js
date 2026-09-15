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

// 订单详情（时间轴 + 物流 + 退款单）
function getOrderDetail(orderNo) {
  return request.get('/order/' + orderNo + '/detail')
}

// 取消订单（仅待支付）
function cancelOrder(orderNo) {
  return request.post('/order/' + orderNo + '/cancel')
}

// 确认收货（仅运输中）
function receiveOrder(orderNo) {
  return request.post('/order/' + orderNo + '/receive')
}

// 申请退款（仅待发货）
function applyRefund(orderNo, data) {
  return request.post('/order/' + orderNo + '/refund', data)
}

// 物流轨迹
function getLogistics(orderNo) {
  return request.get('/logistics/' + orderNo)
}

// ---------- 管理端 ----------

// 后台订单分页
function selectManageOrderByPage(data) {
  return request.get('/order/manage', { params: data })
}

// 后台订单详情
function getManageOrderDetail(orderNo) {
  return request.get('/order/manage/' + orderNo)
}

// 发货
function shipOrder(orderNo, data) {
  return request.post('/order/' + orderNo + '/ship', data)
}

// 审核退款
function auditRefund(refundId, data) {
  return request.put('/order/refund/' + refundId + '/audit', data)
}

export default {
  createOrder,
  payOrder,
  payCallback,
  getOrderStatus,
  selectMyOrderByPage,
  getOrderDetail,
  cancelOrder,
  receiveOrder,
  applyRefund,
  getLogistics,
  selectManageOrderByPage,
  getManageOrderDetail,
  shipOrder,
  auditRefund
}
