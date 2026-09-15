import request from '@/util/request'

// 提交评价（仅自己的已完成订单）
function addReview(data) {
  return request.post('/review', data)
}

// 商品评价分页（游客可看）
function selectByProduct(productId, data) {
  return request.get('/review/product/' + productId, { params: data })
}

// 商品平均评分
function selectAvgRating(productId) {
  return request.get('/review/product/' + productId + '/avg')
}

// 我在某订单下已提交的评价
function selectMyByOrder(orderNo) {
  return request.get('/review/order/' + orderNo)
}

export default { addReview, selectByProduct, selectAvgRating, selectMyByOrder }
