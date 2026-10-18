// 非遗项目与门类 API
import request from '@/utils/request'

// ---- 门类 ----
export function getCategoryList() {
  return request.get('/category/list')
}

// ---- 前台项目 ----
// 排序值映射：前端语义 → 后端 sortField（数据库列名）
const SORT_FIELD_MAP = { year: 'announce_year', views: 'view_count', favorites: 'favorite_count' }

export function getProjectList(params) {
  const p = { ...params }
  if (p.sort) {
    p.sortField = SORT_FIELD_MAP[p.sort] || p.sort
    p.sortOrder = 'desc'
    delete p.sort
  }
  return request.get('/project/list', { params: p })
}

export function getHotProjects() {
  return request.get('/project/hot')
}

export function getProjectDetail(id) {
  return request.get(`/project/${id}`)
}

export function getCategoryStats() {
  return request.get('/project/stats/category')
}

// ---- 内容发布（登录用户）----
export function createProject(data) {
  return request.post('/project', data)
}

export function updateProject(id, data) {
  return request.put(`/project/${id}`, data)
}

export function deleteProject(id) {
  return request.delete(`/project/${id}`)
}

// ---- 管理端 ----
export function getAdminProjectList(params) {
  return request.get('/project/admin/list', { params })
}

// 审核（query 参数 auditStatus/remark）
export function auditProject(id, auditStatus, remark) {
  const params = { auditStatus }
  if (remark) params.remark = remark
  return request.put(`/project/${id}/audit`, null, { params })
}
