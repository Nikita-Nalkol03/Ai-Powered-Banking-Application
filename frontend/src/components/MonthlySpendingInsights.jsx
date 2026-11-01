import { useState } from 'react'
import { aiApi } from '../api/aiApi.js'
import { formatMoney } from '../utils/format.js'

export function MonthlySpendingInsights({ accountNumber }) {
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [lastAccount, setLastAccount] = useState('')
  const currentResult = lastAccount === accountNumber ? result : null
  const currentError = lastAccount === accountNumber ? error : ''

  async function analyzeSpending() {
    if (!accountNumber || busy) return

    setBusy(true)
    setError('')
    setResult(null)
    setLastAccount(accountNumber)

    try {
      const insight = await aiApi.monthlySpendingInsights(accountNumber)
      setResult(insight)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel insights-panel monthly-ai-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Personalized monthly summary</p>
          <h2>AI spending insights</h2>
        </div>
        <span className="ai-badge"><span></span> Gemini AI</span>
      </div>

      <p className="ai-privacy-note">
        When you request an analysis, ArthVeda sends only monthly totals and transaction counts to Gemini.
        Account numbers, descriptions and individual transactions are not sent.
      </p>

      {!accountNumber ? (
        <p className="muted">Select an account to analyze its monthly spending.</p>
      ) : (
        <button
          className="button button-primary"
          disabled={busy}
          onClick={analyzeSpending}
          type="button"
        >
          {busy ? 'Analyzing monthly totals...' : 'Analyze this month'}
        </button>
      )}

      {currentError && <div className="alert alert-error ai-error" role="alert">{currentError}</div>}

      {currentResult && (
        <div aria-live="polite" className="monthly-analysis">
          <p className="eyebrow">{currentResult.month}</p>
          <div className="summary-values">
            <div>
              <span>Spent this month</span>
              <strong>{formatMoney(currentResult.monthlySpend)}</strong>
            </div>
            <div>
              <span>Credits this month</span>
              <strong>{formatMoney(currentResult.monthlyCredits)}</strong>
            </div>
            <div>
              <span>Spent last month</span>
              <strong>{formatMoney(currentResult.previousMonthSpend)}</strong>
            </div>
          </div>
          <article className="monthly-ai-answer">
            <strong>What this could mean</strong>
            <p>{currentResult.analysis}</p>
          </article>
          <p className="ai-disclaimer">
            Based on successful transaction totals, not spending categories. AI can be incorrect;
            this is general information, not financial advice. {currentResult.transactionCount} transactions counted this month.
          </p>
        </div>
      )}
    </section>
  )
}
