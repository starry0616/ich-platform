// 用户中心 API
import request from '@/utils/request'

export function getProfile() {
  return request.get('/user/profile')
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}

export function updatePassword(data) {
  return request.put('/user/password', data)
}

// 头像：后端接收 JSON { avatar: url }，先经 media 上传拿到 URL 再提交
export function updateAvatar(avatarUrl) {
  return request.post('/user/avatar', { avatar: avatarUrl })
}

export function getFavorites(params) {
  return request.get('/user/favorites', { params })
}

export function getHistory(params) {
  return request.get('/user/history', { params })
}

export function getMenus() {
  return request.get('/user/menus')
}
