import request from '@/utils/request'

export function fetchUserList() {
  return request.get('/system/user/list')
}

export function fetchRoleList() {
  return request.get('/system/role/list')
}

export function fetchMenuList() {
  return request.get('/system/menu/list')
}

export function fetchPermissionList() {
  return request.get('/system/permission/list')
}

export function fetchOperationLogList() {
  return request.get('/system/operationLog/list')
}

export function insertUser(data) {
  return request.post('/system/user/insert', null, { params: data })
}

export function updateUser(data) {
  return request.post('/system/user/update', null, { params: data })
}

export function deleteUser(userId) {
  return request.post('/system/user/delete', null, { params: { userId } })
}

export function insertRole(data) {
  return request.post('/system/role/insert', null, { params: data })
}

export function updateRole(data) {
  return request.post('/system/role/update', null, { params: data })
}

export function deleteRole(roleId) {
  return request.post('/system/role/delete', null, { params: { roleId } })
}

export function insertMenu(data) {
  return request.post('/system/menu/insert', null, { params: data })
}

export function updateMenu(data) {
  return request.post('/system/menu/update', null, { params: data })
}

export function deleteMenu(menuId) {
  return request.post('/system/menu/delete', null, { params: { menuId } })
}

export function insertPermission(data) {
  return request.post('/system/permission/insert', null, { params: data })
}

export function updatePermission(data) {
  return request.post('/system/permission/update', null, { params: data })
}

export function deletePermission(permissionId) {
  return request.post('/system/permission/delete', null, { params: { permissionId } })
}

export function deleteOperationLog(logId) {
  return request.post('/system/operationLog/delete', null, { params: { logId } })
}
