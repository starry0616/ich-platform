// 本地存储封装：token 与用户信息
export function getToken() {
  return localStorage.getItem('ich_token')
}

export function setToken(token) {
  localStorage.setItem('ich_token', token)
}

export function getRefreshToken() {
  return localStorage.getItem('ich_refresh_token')
}

export function setRefreshToken(token) {
  localStorage.setItem('ich_refresh_token', token)
}

export function getUser() {
  try {
    const raw = localStorage.getItem('ich_user')
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export function setUser(user) {
  if (user) {
    localStorage.setItem('ich_user', JSON.stringify(user))
  } else {
    localStorage.removeItem('ich_user')
  }
}

export function clearAuth() {
  localStorage.removeItem('ich_token')
  localStorage.removeItem('ich_refresh_token')
  localStorage.removeItem('ich_user')
}

export function isLoggedIn() {
  return !!getToken()
}

export function isAdmin() {
  const u = getUser()
  return u && u.role === 'ADMIN'
}
