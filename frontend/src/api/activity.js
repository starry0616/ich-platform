// 活动 API
import request from '@/utils/request'

export function getActivityList(params) {
  return request.get('/activity/list', { params })
}

export function getActivityDetail(id) {
  return request.get(`/activity/${id}`)
}

// 报名 / 取消
export function signupActivity(id) {
  return request.post(`/activity/${id}/signup`)
}

export function cancelSignup(id) {
  return request.delete(`/activity/${id}/signup`)
}

// 我的报名
export function getMySignups(params) {
  return request.get('/activity/my/signups', { params })
}

// 某活动报名名单（活动组织者/管理员）
export function getActivitySignups(id, params) {
  return request.get(`/activity/${id}/signups`, { params })
}

// 签到核销（query 参数 signupNo）
export function checkin(id, signupNo) {
  return request.put(`/activity/${id}/checkin`, null, { params: { signupNo } })
}

// 管理端
export function getAdminActivityList(params) {
  return request.get('/activity/admin/list', { params })
}

// 审核（query 参数 status/remark）
export function auditActivity(id, status, remark) {
  const params = { status }
  if (remark) params.remark = remark
  return request.put(`/activity/${id}/audit`, null, { params })
}

export function createActivity(data) {
  return request.post('/activity', data)
}

export function updateActivity(id, data) {
  return request.put(`/activity/${id}`, data)
}

export function deleteActivity(id) {
  return request.delete(`/activity/${id}`)
}
