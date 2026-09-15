import request from '@/util/request'

// 我的购物车
function selectMyCart() {
  return request.get('/cart')
}

// 加购
function addCart(data) {
  return request.post('/cart', data)
}

// 修改数量
function updateCartQuantity(id, quantity) {
  return request.put('/cart/' + id + '/quantity?quantity=' + quantity)
}

// 单件勾选
function updateCartChecked(id, checked) {
  return request.put('/cart/' + id + '/checked?checked=' + checked)
}

// 全选/全不选
function checkAllCart(checked) {
  return request.put('/cart/checkAll?checked=' + checked)
}

// 删除（支持批量）
function delCart(ids) {
  return request.delete('/cart/' + ids.toString())
}

export default {
  selectMyCart,
  addCart,
  updateCartQuantity,
  updateCartChecked,
  checkAllCart,
  delCart
}
