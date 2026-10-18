// 资讯文章 API
import request from '@/utils/request'

export function getArticleList(params) {
  return request.get('/article/list', { params })
}

export function getArticleCategories() {
  return request.get('/article/categories')
}

export function getArticleDetail(id) {
  return request.get(`/article/detail/${id}`)
}

export function getRelatedArticles(params) {
  // 后端参数：projectId（必需）、excludeId（可选）
  return request.get('/article/related', { params })
}

export function getMyArticles(params) {
  return request.get('/article/mine', { params })
}

export function createArticle(data) {
  return request.post('/article', data)
}

export function updateArticle(id, data) {
  return request.put(`/article/${id}`, data)
}

export function deleteArticle(id) {
  return request.delete(`/article/${id}`)
}

// 管理端
export function getAdminArticleList(params) {
  return request.get('/article/admin/list', { params })
}

// 审核（query 参数 auditStatus/remark）
export function auditArticle(id, auditStatus, remark) {
  const params = { auditStatus }
  if (remark) params.remark = remark
  return request.put(`/article/${id}/audit`, null, { params })
}

// 置顶（query 参数 isTop）
export function toggleArticleTop(id, isTop) {
  return request.put(`/article/${id}/top`, null, { params: { isTop } })
}
