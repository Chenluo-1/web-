import request from '@/util/request'

// 类目树
function selectCategoryTree() {
  return request.get('/product/category')
}

// 新增类目
function addCategory(data) {
  return request.post('/product/category', data)
}

// 修改类目
function updateCategory(data) {
  return request.put('/product/category', data)
}

// 删除类目
function delCategory(id) {
  return request.delete('/product/category/' + id)
}

// 分页查询商品
function selectProductByPage(data) {
  return request.get('/product', { params: data })
}

// 商品详情（编辑回显）
function getProductById(id) {
  return request.get('/product/' + id)
}

// 新增商品
function addProduct(data) {
  return request.post('/product', data)
}

// 修改商品
function updateProduct(data) {
  return request.put('/product', data)
}

// 删除商品（支持批量）
function delProduct(ids) {
  return request.delete('/product/' + ids.toString())
}

// 上下架
function updateProductStatus(id, status) {
  return request.put('/product/' + id + '/status?status=' + status)
}

// 上传图片（返回 {url, name}）
function uploadFile(formData) {
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export default {
  selectCategoryTree,
  addCategory,
  updateCategory,
  delCategory,
  selectProductByPage,
  getProductById,
  addProduct,
  updateProduct,
  delProduct,
  updateProductStatus,
  uploadFile
}
