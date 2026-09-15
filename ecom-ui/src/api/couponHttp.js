import request from '@/util/request'

// 可领取的券
function selectAvailableCoupon() {
  return request.get('/coupon/available')
}

// 领取
function receiveCoupon(couponId) {
  return request.post('/coupon/' + couponId + '/receive')
}

// 我的券（未使用）
function selectMyCoupon() {
  return request.get('/coupon/mine')
}

// 管理端：分页
function selectCouponByPage(data) {
  return request.get('/coupon', { params: data })
}

function addCoupon(data) {
  return request.post('/coupon', data)
}

function updateCoupon(data) {
  return request.put('/coupon', data)
}

function delCoupon(ids) {
  return request.delete('/coupon/' + ids.toString())
}

export default {
  selectAvailableCoupon,
  receiveCoupon,
  selectMyCoupon,
  selectCouponByPage,
  addCoupon,
  updateCoupon,
  delCoupon
}
