import request from '@/util/request'

// 分页查询角色
function selectRoleByPage(data) {
  return request.get('/sys/role', { params: data })
}

// 查询全部角色（下拉用）
function selectAllRole() {
  return request.get('/sys/role/getAll')
}

// 按 id 查询角色
function getRoleById(id) {
  return request.get('/sys/role/' + id)
}

// 新增角色
function addRole(data) {
  return request.post('/sys/role', data)
}

// 修改角色
function updateRole(data) {
  return request.put('/sys/role', data)
}

// 删除角色（支持批量）
function delRole(ids) {
  return request.delete('/sys/role/' + ids.toString())
}

// 角色已分配的菜单 id 集合
function getRoleMenuIds(roleId) {
  return request.get('/sys/role/' + roleId + '/menu')
}

// 给角色分配菜单
function setRoleMenu(roleId, menuIds) {
  return request.put('/sys/role/' + roleId + '/menu', menuIds)
}

// 全量菜单树（分配权限用）
function getMenuTree() {
  return request.get('/sys/menu/tree')
}

export default {
  selectRoleByPage,
  selectAllRole,
  getRoleById,
  addRole,
  updateRole,
  delRole,
  getRoleMenuIds,
  setRoleMenu,
  getMenuTree
}
