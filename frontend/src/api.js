const TOKEN_KEY = 'skillbridge_token'
const USER_KEY = 'skillbridge_user'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (t) => (t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY))
export const getCachedUser = () => {
  try { return JSON.parse(localStorage.getItem(USER_KEY)) } catch { return null }
}
export const cacheUser = (u) => (u ? localStorage.setItem(USER_KEY, JSON.stringify(u)) : localStorage.removeItem(USER_KEY))

/** Thin fetch wrapper that always points at the Spring Boot REST API. */
export async function api(path, { method = 'GET', body, raw = false } = {}) {
  const headers = { Authorization: getToken() ? `Bearer ${getToken()}` : undefined }
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json'
  const res = await fetch(path, {
    method,
    headers,
    body: body instanceof FormData ? body : body === undefined ? undefined : JSON.stringify(body),
  })
  if (raw) return res
  let data = null
  const text = await res.text()
  if (text) { try { data = JSON.parse(text) } catch { data = null } }
  if (!res.ok) {
    const error = new Error((data && data.message) || `Request failed (${res.status})`)
    error.status = res.status
    throw error
  }
  return data
}

export const get = (p) => api(p)
export const post = (p, body) => api(p, { method: 'POST', body })
export const put = (p, body) => api(p, { method: 'PUT', body })
export const del = (p) => api(p, { method: 'DELETE' })
