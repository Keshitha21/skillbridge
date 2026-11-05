import { createContext, useContext, useEffect, useState } from 'react'
import { api, getToken, setToken, cacheUser, getCachedUser } from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [booting, setBooting] = useState(!!getToken())

  useEffect(() => {
    if (!getToken()) return
    api('/api/auth/me')
      .then((me) => { setUser(me); cacheUser(me) })
      .catch(() => { setToken(null); cacheUser(null) })
      .finally(() => setBooting(false))
  }, [])

  const login = async (email, password) => {
    const res = await api('/api/auth/login', { method: 'POST', body: { email, password } })
    setToken(res.token)
    setUser(res.user)
    cacheUser(res.user)
    return res.user
  }

  const register = async (fullName, email, password) =>
    api('/api/auth/register', { method: 'POST', body: { fullName, email, password } })

  const logout = async () => {
    try { await api('/api/auth/logout', { method: 'POST' }) } catch { /* session already gone */ }
    setToken(null)
    setUser(null)
    cacheUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, booting, login, register, logout, isAdmin: user?.role === 'ADMIN' }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
export const getCached = getCachedUser
