import request from '@/utils/request'

// 用户管理
export function getUsers(params) { return request.get('/system/user/list', { params }) }
export function getUser(id) { return request.get(`/system/user/${id}`) }
export function addUser(data) { return request.post('/system/user', data) }
export function updateUser(data) { return request.put('/system/user', data) }
export function deleteUser(id) { return request.delete(`/system/user/${id}`) }
export function updatePassword(data) { return request.put('/system/user/password', data) }

// 角色管理
export function getRoles() { return request.get('/system/role/list') }
export function addRole(data) { return request.post('/system/role', data) }
export function updateRole(data) { return request.put('/system/role', data) }
export function deleteRole(id) { return request.delete(`/system/role/${id}`) }
export function assignRoleMenus(roleId, menuIds) { return request.put(`/system/role/${roleId}/menus`, { menuIds }) }
export function getRoleMenuIds(roleId) { return request.get(`/system/role/${roleId}/menu-ids`) }

// 菜单管理
export function getMenus() { return request.get('/system/menu/list') }
export function addMenu(data) { return request.post('/system/menu', data) }
export function updateMenu(data) { return request.put('/system/menu', data) }
export function deleteMenu(id) { return request.delete(`/system/menu/${id}`) }

// 操作日志
export function getOperationLogs(params) { return request.get('/system/log/list', { params }) }

// 文件上传
export function uploadFile(data) { return request.post('/common/upload', data, { headers: { 'Content-Type': 'multipart/form-data' } }) }
