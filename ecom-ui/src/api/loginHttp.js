import request from '@/util/request'

// 登录
function login(data) {
  return request.post('/login', data)
}

// 退出登录
function logout() {
  return request.get('/logout')
}

// 当前用户信息（权限标识集合 + 角色集合）
function getInfo() {
  return request.get('/getInfo')
}

// 当前用户可见菜单树
function getRoutes() {
  return request.get('/getRouters')
}

export default {
  login,
  logout,
  getInfo,
  getRoutes
}
