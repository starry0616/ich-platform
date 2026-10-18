// 传承人 API
import request from '@/utils/request'

export function getInheritorList(params) {
  return request.get('/inheritor/list', { params })
}

export function getInheritorDetail(id) {
  return request.get(`/inheritor/${id}`)
}

// 传承人申请认证（登录用户）
export function applyInheritor(data) {
  return request.post('/inheritor/apply', data)
}

export function getApplyStatus() {
  return request.get('/inheritor/apply/status')
}

// 完善传承人资料（已认证）
export function updateInheritorProfile(data) {
  return request.put('/inheritor/profile', data)
}

// 管理端
export function getAdminInheritorList(params) {
  return request.get('/inheritor/admin/audit/list', { params })
}

export function auditInheritor(id, certStatus) {
  return request.put(`/inheritor/${id}/audit`, null, { params: { certStatus } })
}

export function updateInheritor(id, data) {
  return request.put(`/inheritor/${id}`, data)
}

export function deleteInheritor(id) {
  return request.delete(`/inheritor/${id}`)
}
