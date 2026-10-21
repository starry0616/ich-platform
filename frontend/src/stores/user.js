import { defineStore } from 'pinia'
import { login as apiLogin, register as apiRegister, getMe, logout as apiLogout } from '@/api/auth'
import { getToken, getRefreshToken, setToken, setRefreshToken, getUser, setUser, clearAuth } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    user: getUser()
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.user && state.user.roleCode === 'ROLE_ADMIN',
    isInheritor: (state) => state.user && state.user.roleCode === 'ROLE_INHERITOR',
    nickname: (state) => (state.user && state.user.nickname) || (state.user && state.user.username) || ''
  },
  actions: {
    async login(payload) {
      // 先清掉残留的旧登录态，避免旧 token 触发 401 拦截导致"登录闪退"
      clearAuth()
      const res = await apiLogin(payload)
      const data = res.data
      setToken(data.accessToken)
      setRefreshToken(data.refreshToken)
      this.token = data.accessToken
      const user = {
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        roleCode: data.roleCode,
        avatar: data.avatar
      }
      setUser(user)
      this.user = user
      return data
    },
    async register(payload) {
      await apiRegister(payload)
    },
    async refreshMe() {
      if (!this.token) return null
      const prev = this.user || {}
      try {
        const res = await getMe()
        const d = res.data
        const user = {
          userId: d.userId,
          username: d.username,
          nickname: d.nickname || prev.nickname || '',
          roleCode: d.roleCode,
          avatar: d.avatar || prev.avatar || ''
        }
        setUser(user)
        this.user = user
        return user
      } catch (e) {
        return null
      }
    },
    async logout() {
      // 先通知后端撤销 access/refresh 令牌；网络失败不阻塞本地退出
      try {
        await apiLogout(getRefreshToken())
      } catch (e) {
        // 忽略：本地仍清除登录态
      }
      clearAuth()
      this.token = null
      this.user = null
    }
  }
})
