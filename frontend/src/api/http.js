const ACCESS_TOKEN_KEY = 'smartbank.accessToken'
const REFRESH_TOKEN_KEY = 'smartbank.refreshToken'
const USER_KEY = 'smartbank.user'
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '')

function apiUrl(path) {
  return `${API_BASE_URL}${path}`
}

export function getSession() {
  const accessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY)
  const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY)
  const savedUser = sessionStorage.getItem(USER_KEY)

  return {
    accessToken,
    refreshToken,
    user: savedUser ? JSON.parse(savedUser) : null,
  }
}

export function saveSession(loginResponse, user) {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, loginResponse.accessToken)
  sessionStorage.setItem(REFRESH_TOKEN_KEY, loginResponse.refreshToken)
  sessionStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

async function readResponse(response) {
  const body = await response.text()
  if (!body) return null

  try {
    return JSON.parse(body)
  } catch {
    return body
  }
}

function getErrorMessage(body, status) {
  if (body && typeof body === 'object') {
    if (body.message) return body.message
    if (body.detail) return body.detail
    if (body.errors) return Object.values(body.errors).join(', ')
  }

  if (typeof body === 'string' && body.trim()) return body
  if (status >= 500) return 'The banking service had a problem. Please try again.'
  return `Request failed (${status}). Please check your information and try again.`
}

async function refreshAccessToken() {
  const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY)
  if (!refreshToken) return false

  const response = await fetch(apiUrl('/api/auth/refresh'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })

  if (!response.ok) {
    clearSession()
    return false
  }

  const result = await readResponse(response)
  const savedUser = sessionStorage.getItem(USER_KEY)
  if (!result?.accessToken || !result?.refreshToken || !savedUser) {
    clearSession()
    return false
  }

  sessionStorage.setItem(ACCESS_TOKEN_KEY, result.accessToken)
  sessionStorage.setItem(REFRESH_TOKEN_KEY, result.refreshToken)
  return true
}

export async function apiRequest(path, options = {}, retryAfterRefresh = true) {
  const headers = new Headers(options.headers || {})
  const accessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY)
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  let response
  try {
    response = await fetch(apiUrl(path), { ...options, headers })
  } catch {
    throw new Error('Cannot reach ArthVeda. Check that the backend is running.')
  }

  if (response.status === 401 && retryAfterRefresh && await refreshAccessToken()) {
    return apiRequest(path, options, false)
  }

  const result = await readResponse(response)
  if (!response.ok) throw new Error(getErrorMessage(result, response.status))
  return result
}
