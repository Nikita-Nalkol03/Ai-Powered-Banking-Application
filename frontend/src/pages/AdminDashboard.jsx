import { useEffect, useState } from 'react'
import { bankingApi } from '../api/bankingApi.js'
import { formatMoney, formatDate } from '../utils/format.js'

export function AdminDashboard({ user }) {
  const [dashboard, setDashboard] = useState(null)
  const [loadingDashboard, setLoadingDashboard] = useState(true)
  const [dashboardError, setDashboardError] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [amount, setAmount] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function loadDashboard() {
    setLoadingDashboard(true)
    setDashboardError('')
    try {
      setDashboard(await bankingApi.adminDashboard())
    } catch (requestError) {
      setDashboardError(requestError.message)
    } finally {
      setLoadingDashboard(false)
    }
  }

  useEffect(() => {
    let current = true
    bankingApi.adminDashboard()
      .then((result) => {
        if (current) setDashboard(result)
      })
      .catch((requestError) => {
        if (current) setDashboardError(requestError.message)
      })
      .finally(() => {
        if (current) setLoadingDashboard(false)
      })
    return () => {
      current = false
    }
  }, [])

  async function handleDeposit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setResult(null)

    try {
      const deposit = await bankingApi.adminDeposit(accountNumber.trim(), Number(amount))
      setResult(deposit)
      setAmount('')
      await loadDashboard()
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="dashboard admin-dashboard">
      <section className="dashboard-hero admin-hero">
        <div className="admin-hero-copy">
          <div>
            <p className="eyebrow">ArthVeda operations</p>
            <h1>Good to see you, {user.firstName || 'Admin'}.</h1>
            <p>Keep customer funding clear, accurate and securely recorded.</p>
          </div>
          <span className="admin-badge"><span></span> ADMIN ACCESS</span>
        </div>
        <nav aria-label="Admin dashboard sections" className="admin-section-nav">
          <a href="#operations-overview">Operations overview <span>01</span></a>
          <a href="#fund-account">Account funding <span>02</span></a>
          <a href="#admin-guidance">Operations guide <span>03</span></a>
        </nav>
      </section>

      <section className="admin-overview" id="operations-overview">
        <div className="section-heading admin-overview-heading">
          <div>
            <p className="eyebrow">Live database metrics</p>
            <h2>Operations overview</h2>
          </div>
          <button className="button button-outline button-small" disabled={loadingDashboard} onClick={loadDashboard} type="button">
            {loadingDashboard ? 'Refreshing…' : 'Refresh data'}
          </button>
        </div>
        {dashboardError && <div className="alert alert-error" role="alert">{dashboardError}</div>}
        {loadingDashboard && !dashboard ? (
          <p className="muted" aria-live="polite">Loading current operations data…</p>
        ) : dashboard ? (
          <>
            <div className="admin-metric-grid">
              <article className="admin-metric-card"><span>Registered customers</span><strong>{dashboard.registeredCustomers}</strong><small>{dashboard.activeCustomers} active · {dashboard.blockedCustomers} blocked</small></article>
              <article className="admin-metric-card"><span>Total accounts</span><strong>{dashboard.totalAccounts}</strong><small>{dashboard.activeAccounts} active · {dashboard.blockedAccounts} blocked · {dashboard.closedAccounts} closed</small></article>
              <article className="admin-metric-card"><span>Successful ledger entries</span><strong>{dashboard.successfulTransactionCount}</strong><small>{dashboard.savingsAccounts} savings · {dashboard.currentAccounts} current</small></article>
              <article className="admin-metric-card"><span>Credit volume</span><strong>{formatMoney(dashboard.totalCreditVolume)}</strong><small>Ledger credits, not bank revenue</small></article>
              <article className="admin-metric-card"><span>Debit volume</span><strong>{formatMoney(dashboard.totalDebitVolume)}</strong><small>Ledger debits, not bank revenue</small></article>
              <article className="admin-metric-card"><span>Transfer volume</span><strong>{formatMoney(dashboard.totalTransferVolume)}</strong><small>Credit side of successful transfer entries</small></article>
            </div>
            <div className="admin-period-grid">
              {[['Today', dashboard.dailyActivity], ['This month', dashboard.monthlyActivity]].map(([label, activity]) => (
                <article className="admin-period-card" key={label}>
                  <p className="eyebrow">{label}</p>
                  <strong>{activity.transactionCount} successful ledger entries</strong>
                  <div><span>Credits</span><b>{formatMoney(activity.creditVolume)}</b></div>
                  <div><span>Debits</span><b>{formatMoney(activity.debitVolume)}</b></div>
                  <div><span>Transfer volume</span><b>{formatMoney(activity.transferVolume)}</b></div>
                </article>
              ))}
            </div>
            <section className="panel admin-recent-panel">
              <div className="section-heading">
                <div><p className="eyebrow">Latest activity</p><h2>Recent ledger entries</h2></div>
              </div>
              {dashboard.recentTransactions?.length ? (
                <div className="admin-table-wrap">
                  <table className="admin-table">
                    <thead><tr><th>Reference</th><th>Customer</th><th>Account</th><th>Type</th><th>Status</th><th>Amount</th><th>Time</th></tr></thead>
                    <tbody>{dashboard.recentTransactions.map((transaction) => (
                      <tr key={transaction.transactionReference}>
                        <td>{transaction.transactionReference}</td>
                        <td>{transaction.customerName}</td>
                        <td>•••• {transaction.accountNumber.slice(-4)}</td>
                        <td>{transaction.type}</td>
                        <td><span className={`status-badge status-${transaction.status.toLowerCase()}`}>{transaction.status}</span></td>
                        <td>{formatMoney(transaction.amount)}</td>
                        <td>{formatDate(transaction.createdAt)}</td>
                      </tr>
                    ))}</tbody>
                  </table>
                </div>
              ) : <p className="muted">No ledger entries have been recorded yet.</p>}
            </section>
          </>
        ) : null}
      </section>

      {error && <div className="alert alert-error" role="alert">{error}</div>}
      {result && (
        <div className="alert alert-success" role="status">
          Deposit recorded. New balance: {formatMoney(result.newBalance)}. Reference: {result.transactionReference}
        </div>
      )}

      <div className="admin-grid">
        <section className="panel admin-funding-panel" id="fund-account">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Secure operation · 01</p>
              <h2>Fund a customer account</h2>
            </div>
            <span className="admin-operation-icon" aria-hidden="true">↗</span>
          </div>
          <p className="admin-panel-intro">
            Record a deposit against a verified customer account. The ledger and updated balance are handled by ArthVeda.
          </p>
          <form className="form-stack admin-form" onSubmit={handleDeposit}>
            <label>
              Customer account number
              <input autoComplete="off" onChange={(event) => setAccountNumber(event.target.value)} placeholder="Enter the account number" required value={accountNumber} />
            </label>
            <label>
              Deposit amount (INR)
              <input min="0.01" onChange={(event) => setAmount(event.target.value)} placeholder="0.00" required step="0.01" type="number" value={amount} />
            </label>
            <button className="button button-primary" disabled={busy} type="submit">
              {busy ? 'Recording securely...' : 'Confirm and record deposit'}
            </button>
          </form>
        </section>

        <aside className="panel admin-info" id="admin-guidance">
          <p className="eyebrow">Operations guide · 02</p>
          <h2>Made for careful account funding.</h2>
          <p className="admin-info-intro">
            Deposits are restricted to administrator accounts and recorded in the customer transaction ledger.
          </p>
          <ul className="admin-checklist">
            <li><span>01</span><div><strong>Confirm the account</strong><small>Check the customer account number before continuing.</small></div></li>
            <li><span>02</span><div><strong>Enter the amount</strong><small>Use a positive deposit amount in Indian rupees.</small></div></li>
            <li><span>03</span><div><strong>Keep the reference</strong><small>Review the success confirmation and transaction reference.</small></div></li>
          </ul>
          {result && (
            <dl className="result-details admin-result-details">
              <div><dt>Account</dt><dd>{result.accountNumber}</dd></div>
              <div><dt>Status</dt><dd>{result.status}</dd></div>
              <div><dt>Time</dt><dd>{formatDate(result.createdAt)}</dd></div>
            </dl>
          )}
        </aside>
      </div>
    </main>
  )
}
