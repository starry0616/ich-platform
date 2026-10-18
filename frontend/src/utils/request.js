// 统一请求封装：axios 实例 + 拦截器
// 后端统一返回 Result{code,message,data}，成功 code=200
import axios from 'axios'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截：携带 JWT（Bearer 前缀由后端约定，这里只带 token）
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('ich_token')
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    // 后端分页参数统一为 pageNum/pageSize；前端页面沿用 page/size 语义，这里映射
    if (config.params) {
      const p = { ...config.params }
      if (p.page !== undefined && p.pageNum === undefined) {
        p.pageNum = p.page
        delete p.page
      }
      if (p.size !== undefined && p.pageSize === undefined) {
        p.pageSize = p.size
        delete p.size
      }
      config.params = p
    }
    return config
  },
  (error) => Promise.reject(error)
)

let refreshing = false
let waitQueue = []

function flushQueue(err = null) {
  waitQueue.forEach((p) => (err ? p.reject(err) : p.resolve()))
  waitQueue = []
}

// 响应拦截：统一处理 code 与 401
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非标准结构（如二进制）直接返回
    if (res === null || typeof res !== 'object' || !('code' in res)) {
      return res
    }
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      // token 失效：尝试用 refreshToken 刷新一次
      const refreshToken = localStorage.getItem('ich_refresh_token')
      if (refreshToken && !refreshing) {
        refreshing = true
        return axios
          .post('/api/auth/refresh', null, { params: { refreshToken } })
          .then((r) => {
            const d = r.data
            if (d && d.code === 200 && d.data) {
              localStorage.setItem('ich_token', d.data.accessToken)
              localStorage.setItem('ich_refresh_token', d.data.refreshToken)
              // 重放等待队列
              flushQueue()
              // 重试原请求
              const cfg = { ...response.config }
              cfg.headers.Authorization = 'Bearer ' + d.data.accessToken
              return service(cfg)
            }
            throw new Error('refresh failed')
          })
          .catch((e) => {
            flushQueue(e)
            localStorage.removeItem('ich_token')
            localStorage.removeItem('ich_refresh_token')
            localStorage.removeItem('ich_user')
            window.location.href = '/login'
            return Promise.reject(e)
          })
          .finally(() => {
            refreshing = false
          })
      }
      if (refreshing) {
        // 等待刷新完成
        return new Promise((resolve, reject) => {
          waitQueue.push({ resolve, reject })
        }).then(() => service(response.config))
      }
      localStorage.removeItem('ich_token')
      localStorage.removeItem('ich_refresh_token')
      localStorage.removeItem('ich_user')
      window.location.href = '/login'
      return Promise.reject(new Error(res.message || '登录已过期'))
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      localStorage.removeItem('ich_token')
      localStorage.removeItem('ich_refresh_token')
      localStorage.removeItem('ich_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default service
