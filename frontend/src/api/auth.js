// 认证模块 API
import request from '@/utils/request'

export function register(data) {
  return request.post('/auth/register', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function refresh(refreshToken) {
  return request.post('/auth/refresh', null, { params: { refreshToken } })
}

export function logout(refreshToken) {
  return request.post('/auth/logout', null, { params: { refreshToken } })
}

export function getMe() {
  return request.get('/auth/me')
}
