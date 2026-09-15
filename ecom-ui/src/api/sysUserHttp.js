import request from '@/util/request'

// 分页查询用户
function selectUserByPage(data) {
  return request.get('/sys/user', { params: data })
}

// 查询全部用户
function selectAllUser() {
  return request.get('/sys/user/getAll')
}

// 按 id 查询用户
function getUserById(id) {
  return request.get('/sys/user/' + id)
}

// 新增用户
function addUser(data) {
  return request.post('/sys/user', data)
}

// 修改用户
function updateUser(data) {
  return request.put('/sys/user', data)
}

// 删除用户（支持批量，ids 数组）
function delUser(ids) {
  return request.delete('/sys/user/' + ids.toString())
}

// 管理员重置密码
function resetPassword(data) {
  return request.put('/sys/user/respwd', data)
}

// 本人修改密码
function changePassword(data) {
  return request.put('/sys/user/password', data)
}

// 用户的角色 id 集合
function getRoleIds(id) {
  return request.get('/sys/user/' + id + '/roleIds')
}

export default {
  selectUserByPage,
  selectAllUser,
  getUserById,
  addUser,
  updateUser,
  delUser,
  resetPassword,
  changePassword,
  getRoleIds
}
