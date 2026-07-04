import request from '@/utils/request'

// 登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 退出
export function logout() {
  return request.post('/auth/logout')
}

// 获取用户信息
export function getUserInfo() {
  return request.get('/auth/user-info')
}

// 获取用户菜单
export function getUserMenus() {
  return request.get('/auth/user-menus')
}
