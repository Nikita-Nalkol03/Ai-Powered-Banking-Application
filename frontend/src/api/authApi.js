import { apiRequest } from './http.js'

export const authApi = {
  login: (details) => apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(details),
  }),

  register: (details) => apiRequest('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(details),
  }),

  resetPassword: (details) => apiRequest('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(details),
  }, false),

  recoveryQuestion: (email) => apiRequest('/api/auth/recovery-question', {
    method: 'POST',
    body: JSON.stringify({ email }),
  }, false),

  currentUser: () => apiRequest('/api/users/me'),

  logout: (refreshToken) => apiRequest('/api/auth/logout', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  }, false),
}
