import { apiRequest } from './http.js'

export const bankingApi = {
  adminDashboard: () => apiRequest('/api/admin/dashboard'),

  accounts: () => apiRequest('/api/accounts'),

  createAccount: (accountType) => apiRequest('/api/accounts', {
    method: 'POST',
    body: JSON.stringify({ accountType }),
  }),

  transactions: (accountNumber, filters = {}) => {
    const params = new URLSearchParams({
      page: String(filters.page ?? 0),
      size: String(filters.size ?? 8),
      sortBy: filters.sortBy || 'createdAt',
      direction: filters.direction || 'desc',
    })
    for (const key of ['type', 'status', 'reference', 'startDate', 'endDate']) {
      if (filters[key]) params.set(key, filters[key])
    }
    return apiRequest(
      `/api/accounts/${encodeURIComponent(accountNumber)}/transactions?${params}`,
    )
  },

  transactionSummary: (accountNumber, filters = {}) => {
    const params = new URLSearchParams()
    if (filters.startDate) params.set('startDate', filters.startDate)
    if (filters.endDate) params.set('endDate', filters.endDate)
    const query = params.size ? `?${params}` : ''
    return apiRequest(
      `/api/accounts/${encodeURIComponent(accountNumber)}/transactions/summary${query}`,
    )
  },

  transfer: (details) => apiRequest('/api/transfers', {
    method: 'POST',
    headers: { 'Idempotency-Key': crypto.randomUUID() },
    body: JSON.stringify(details),
  }),

  withdraw: (accountNumber, amount) => apiRequest(
    `/api/accounts/${encodeURIComponent(accountNumber)}/withdraw`,
    {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify({ amount }),
    },
  ),

  adminDeposit: (accountNumber, amount) => apiRequest(
    `/api/admin/accounts/${encodeURIComponent(accountNumber)}/deposit`,
    {
      method: 'POST',
      body: JSON.stringify({ amount }),
    },
  ),
}
