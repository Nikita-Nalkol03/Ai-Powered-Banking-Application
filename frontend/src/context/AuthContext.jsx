import { useEffect, useMemo, useState } from 'react'
import { authApi } from '../api/authApi.js'
import { clearSession, getSession, saveSession } from '../api/http.js'
import { AuthContext } from './authContext.js'

export function AuthProvider({ children }) {
  const [initialSession] = useState(() => getSession())
  const [user, setUser] = useState(initialSession.user)
  const [loading, setLoading] = useState(Boolean(initialSession.accessToken))
  const [error, setError] = useState('')

  useEffect(() => {
    if (!initialSession.accessToken) return

    authApi.currentUser()
      .then((profile) => {
        setUser(profile)
        sessionStorage.setItem('smartbank.user', JSON.stringify(profile))
      })
      .catch((requestError) => {
        clearSession()
        setError(requestError.message)
      })
      .finally(() => setLoading(false))
  }, [initialSession.accessToken])

  async function login(details) {
    const loginResponse = await authApi.login(details)
    const loginUser = {
      email: loginResponse.email,
      role: loginResponse.role,
      firstName: '',
      lastName: '',
    }
    saveSession(loginResponse, loginUser)

    try {
      const profile = await authApi.currentUser()
      saveSession(loginResponse, profile)
      setUser(profile)
      setError('')
    } catch (requestError) {
      clearSession()
      throw requestError
    }
  }

  async function register(details) {
    return authApi.register(details)
  }

  async function logout() {
    const { refreshToken } = getSession()
    try {
      if (refreshToken) await authApi.logout(refreshToken)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      clearSession()
      setUser(null)
    }
  }

  const value = useMemo(() => ({
    user,
    loading,
    error,
    login,
    register,
    logout,
    clearError: () => setError(''),
  }), [user, loading, error])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
