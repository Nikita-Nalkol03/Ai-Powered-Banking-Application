import { useEffect, useState } from 'react'
import { bankingApi } from '../api/bankingApi.js'
import { formatMoney } from '../utils/format.js'

export function SmartInsights({ accountNumber }) {
  const [summaryResult, setSummaryResult] = useState(null)
  const [errorResult, setErrorResult] = useState(null)
  const summary = summaryResult?.accountNumber === accountNumber ? summaryResult.data : null
  const error = errorResult?.accountNumber === accountNumber ? errorResult.message : ''

  useEffect(() => {
    if (!accountNumber) return undefined

    let current = true
    bankingApi.transactionSummary(accountNumber)
      .then((data) => {
        if (current) {
          setSummaryResult({ accountNumber, data })
          setErrorResult(null)
        }
      })
      .catch((requestError) => {
        if (current) setErrorResult({ accountNumber, message: requestError.message })
      })

    return () => {
      current = false
    }
  }, [accountNumber])

  let insight = 'Choose an account to see a simple summary of your money movement.'
  if (summary) {
    const debit = Number(summary.totalDebit || 0)
    const credit = Number(summary.totalCredit || 0)
    insight = credit > debit
      ? 'Credits are higher than debits for the selected period. Keep building your savings habit.'
      : debit > credit
        ? 'Debits are higher than credits for the selected period. Review recent spending before making new purchases.'
        : 'Credits and debits are balanced for the selected period.'
  }

  return (
    <section className="panel insights-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Money movement</p>
          <h2>Smart insights</h2>
        </div>
        <span className="small-tag">Simple summary</span>
      </div>
      {error ? <p className="muted">{error}</p> : <p className="insight-copy">{insight}</p>}
      {summary && (
        <div className="summary-values">
          <div><span>Credits</span><strong>{formatMoney(summary.totalCredit)}</strong></div>
          <div><span>Debits</span><strong>{formatMoney(summary.totalDebit)}</strong></div>
          <div><span>Transactions</span><strong>{summary.totalTransactions}</strong></div>
        </div>
      )}
      <p className="field-hint">These insights are rule-based summaries from your bank data, not AI-generated financial advice.</p>
    </section>
  )
}
