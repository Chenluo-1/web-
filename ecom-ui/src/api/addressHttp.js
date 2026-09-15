import request from '@/util/request'

// 我的地址
function selectMyAddress() {
  return request.get('/address')
}

function addAddress(data) {
  return request.post('/address', data)
}

function updateAddress(data) {
  return request.put('/address', data)
}

function delAddress(id) {
  return request.delete('/address/' + id)
}

// 设为默认
function setDefaultAddress(id) {
  return request.put('/address/' + id + '/default')
}

export default {
  selectMyAddress,
  addAddress,
  updateAddress,
  delAddress,
  setDefaultAddress
}
