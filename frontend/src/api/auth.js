/**
 * 认证接口（Mock 模式下由 stores/user.js 直接处理，此处预留真实后端对接）
 */
import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function getUserInfo() {
  return request.get('/auth/userInfo')
}

export function getMenus() {
  return request.get('/auth/menus')
}
