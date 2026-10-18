// 管理端 API
import request from '@/utils/request'

// ---- 用户管理 ----
export function getAdminUserList(params) {
  return request.get('/admin/user/list', { params })
}

export function getRoles() {
  return request.get('/admin/user/roles')
}

export function setUserStatus(id, data) {
  return request.put(`/admin/user/${id}/status`, data)
}

export function setUserRole(id, data) {
  return request.put(`/admin/user/${id}/role`, data)
}

// ---- 举报与评论管理 ----
export function getReportList(params) {
  return request.get('/admin/report/list', { params })
}

export function handleReport(id, data) {
  return request.put(`/admin/report/${id}/handle`, data)
}

export function rejectReport(id, data) {
  return request.put(`/admin/report/${id}/reject`, data)
}

export function getCommentList(params) {
  return request.get('/admin/comment/list', { params })
}

export function setCommentStatus(id, data) {
  return request.put(`/admin/comment/${id}/status`, data)
}

// ---- 日志 ----
export function getOperLog(params) {
  return request.get('/admin/log', { params })
}

export function getLoginLog(params) {
  return request.get('/admin/login-log', { params })
}

// ---- 统计数据 ----
export function getStatsOverview() {
  return request.get('/admin/stats/overview')
}

export function getTrends(params) {
  return request.get('/admin/stats/trends', { params })
}

export function getCategoryStats() {
  return request.get('/admin/stats/category')
}

export function getInheritorStats() {
  return request.get('/admin/stats/inheritor')
}

export function getTopActivities(params) {
  return request.get('/admin/stats/top-activities', { params })
}

// ---- Banner 管理 ----
export function getAdminBannerList(params) {
  return request.get('/admin/banner/list', { params })
}

export function createBanner(data) {
  return request.post('/admin/banner', data)
}

export function updateBanner(id, data) {
  return request.put(`/admin/banner/${id}`, data)
}

export function deleteBanner(id) {
  return request.delete(`/admin/banner/${id}`)
}

// ---- AI 知识库管理 ----
export function getKnowledgeList(params) {
  return request.get('/admin/ai/knowledge', { params })
}

export function createKnowledge(data) {
  return request.post('/admin/ai/knowledge', data)
}

export function updateKnowledge(id, data) {
  return request.put(`/admin/ai/knowledge/${id}`, data)
}

export function deleteKnowledge(id) {
  return request.delete(`/admin/ai/knowledge/${id}`)
}

export function reindexKnowledge() {
  return request.post('/admin/ai/reindex')
}

export function getChatLogs(params) {
  return request.get('/admin/ai/chat/logs', { params })
}

export function reviewChat(id, data) {
  return request.put(`/admin/ai/chat/${id}/review`, data)
}

export function getUnresolvedChats() {
  return request.get('/admin/ai/chat/unresolved')
}

// ---- 前台 Banner ----
export function getBannerList() {
  return request.get('/banner/list')
}
