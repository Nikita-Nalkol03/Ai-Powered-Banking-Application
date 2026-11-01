import { apiRequest } from './http.js'

export const aiApi = {
  ask: (message) => apiRequest('/api/ai/chat', {
    method: 'POST',
    body: JSON.stringify({ message }),
  }),

  monthlySpendingInsights: (accountNumber) => apiRequest('/api/ai/spending-insights', {
    method: 'POST',
    body: JSON.stringify({ accountNumber }),
  }),
}
