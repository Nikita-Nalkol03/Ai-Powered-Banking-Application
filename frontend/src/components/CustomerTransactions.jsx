import { useEffect, useState } from 'react'
import { bankingApi } from '../api/bankingApi.js'
import { formatDate, formatMoney } from '../utils/format.js'

const CSV_COLUMNS = [
  'Transaction reference',
  'Date',
  'Type',
  'Status',
  'Description',
  'Amount (INR)',
]

function csvCell(value) {
  const text = String(value ?? '')
  const safeText = /^[\s]*[=+\-@]/.test(text) ? `'${text}` : text
  return `"${safeText.replaceAll('"', '""')}"`
}

function downloadCsv(rows, accountNumber) {
  const csv = [CSV_COLUMNS, ...rows.map((transaction) => [
    transaction.transactionReference,
    transaction.createdAt,
    transaction.type,
    transaction.status,
    transaction.description,
    transaction.amount,
  ])].map((row) => row.map(csvCell).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob(['\uFEFF', csv], {
    type: 'text/csv;charset=utf-8',
  }))
  const link = document.createElement('a')
  link.href = url
  link.download = `arthveda-statement-${accountNumber.slice(-4)}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

export function CustomerTransactions({ accountNumber }) {
  const [filters, setFilters] = useState({
    reference: '',
    type: '',
    status: '',
    startDate: '',
    endDate: '',
  })
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(false)
  const invalidDateRange =
    Boolean(filters.startDate) !== Boolean(filters.endDate)

  useEffect(() => {
    if (!accountNumber || invalidDateRange) {
      return undefined
    }
    let current = true
    const delay = filters.reference ? 250 : 0
    const timer = window.setTimeout(() => {
      setLoading(true)
      setError('')
      Promise.all([
        bankingApi.transactions(accountNumber, {
          ...filters,
          page,
          size: 8,
        }),
        bankingApi.transactionSummary(accountNumber, filters),
      ])
        .then(([pageResult, summaryResult]) => {
          if (!current) return
          setResult({ accountNumber, data: pageResult })
          setSummary({ accountNumber, data: summaryResult })
        })
        .catch((requestError) => {
          if (current) setError(requestError.message)
        })
        .finally(() => {
          if (current) setLoading(false)
        })
    }, delay)
    return () => {
      current = false
      window.clearTimeout(timer)
    }
  }, [accountNumber, filters, invalidDateRange, page])

  function changeFilter(name, value) {
    setPage(0)
    setFilters((current) => ({ ...current, [name]: value }))
  }

  async function exportStatement() {
    if (!accountNumber || busy) return
    setBusy(true)
    setError('')
    try {
      const firstPage = await bankingApi.transactions(accountNumber, {
        ...filters,
        page: 0,
        size: 100,
      })
      const rows = [...(firstPage.content || [])]
      for (let currentPage = 1; currentPage < firstPage.totalPages; currentPage += 1) {
        const nextPage = await bankingApi.transactions(accountNumber, {
          ...filters,
          page: currentPage,
          size: 100,
        })
        rows.push(...(nextPage.content || []))
      }
      downloadCsv(rows, accountNumber)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  const accountResult = result?.accountNumber === accountNumber ? result.data : null
  const accountSummary = summary?.accountNumber === accountNumber ? summary.data : null
  const transactions = accountResult?.content || []
  const totalPages = accountResult?.totalPages || 0
  const credit = Number(accountSummary?.totalCredit || 0)
  const debit = Number(accountSummary?.totalDebit || 0)
  const chartMaximum = Math.max(credit, debit, 1)

  return (
    <section className="panel customer-transactions" id="transactions">
      <div className="section-heading">
        <div>
          <p className="eyebrow">History and statements</p>
          <h2>Transactions</h2>
        </div>
        <button
          className="button button-outline button-small"
          disabled={!accountNumber || busy || invalidDateRange}
          onClick={exportStatement}
          type="button"
        >
          {busy ? 'Preparing statement…' : 'Download CSV'}
        </button>
      </div>

      {accountSummary && (
        <div className="transaction-summary" aria-label="Filtered transaction summary">
          <div className="transaction-summary-values">
            <div><span>Credits</span><strong>{formatMoney(accountSummary.totalCredit)}</strong></div>
            <div><span>Debits</span><strong>{formatMoney(accountSummary.totalDebit)}</strong></div>
            <div><span>Net movement</span><strong>{formatMoney(accountSummary.netMovement)}</strong></div>
          </div>
          <div className="cashflow-chart" aria-label="Credits compared with debits">
            <div className="cashflow-row">
              <span>Credits</span><div><i className="cashflow-credit" style={{ width: `${credit / chartMaximum * 100}%` }} /></div>
            </div>
            <div className="cashflow-row">
              <span>Debits</span><div><i className="cashflow-debit" style={{ width: `${debit / chartMaximum * 100}%` }} /></div>
            </div>
          </div>
        </div>
      )}

      <div className="transaction-filters" aria-label="Filter transactions">
        <label>
          Reference
          <input
            onChange={(event) => changeFilter('reference', event.target.value)}
            placeholder="Search reference"
            value={filters.reference}
          />
        </label>
        <label>
          Type
          <select onChange={(event) => changeFilter('type', event.target.value)} value={filters.type}>
            <option value="">All types</option>
            <option value="CREDIT">Credit</option>
            <option value="DEBIT">Debit</option>
          </select>
        </label>
        <label>
          Status
          <select onChange={(event) => changeFilter('status', event.target.value)} value={filters.status}>
            <option value="">All statuses</option>
            <option value="SUCCESS">Success</option>
            <option value="PENDING">Pending</option>
            <option value="FAILED">Failed</option>
          </select>
        </label>
        <label>
          From
          <input
            onChange={(event) => changeFilter('startDate', event.target.value)}
            type="date"
            value={filters.startDate}
          />
        </label>
        <label>
          To
          <input
            onChange={(event) => changeFilter('endDate', event.target.value)}
            type="date"
            value={filters.endDate}
          />
        </label>
      </div>

      {error && <div className="alert alert-error" role="alert">{error}</div>}
      {!accountNumber ? (
        <p className="empty-state">Select an account to view its statement.</p>
      ) : invalidDateRange ? (
        <p className="field-hint" role="status">Choose both a start and end date to filter the statement.</p>
      ) : loading ? (
        <p aria-live="polite" className="muted">Loading secure transaction history…</p>
      ) : transactions.length ? (
        <>
          <div className="transaction-list">
            {transactions.map((transaction) => (
              <article className="transaction-row" key={transaction.transactionReference}>
                <span className={`transaction-icon ${transaction.type === 'CREDIT' ? 'credit' : 'debit'}`}>
                  {transaction.type === 'CREDIT' ? '+' : '−'}
                </span>
                <span className="transaction-details">
                  <strong>{transaction.description || transaction.type}</strong>
                  <small>{formatDate(transaction.createdAt)} · {transaction.status}</small>
                  <small>Reference: {transaction.transactionReference}</small>
                </span>
                <strong className={transaction.type === 'CREDIT' ? 'money-credit' : 'money-debit'}>
                  {transaction.type === 'CREDIT' ? '+' : '−'}{formatMoney(transaction.amount)}
                </strong>
              </article>
            ))}
          </div>
          <div className="transaction-pagination">
            <span>
              {accountResult.totalElements} records · Page {page + 1} of {Math.max(totalPages, 1)}
            </span>
            <div>
              <button className="button button-outline button-small" disabled={page === 0} onClick={() => setPage((current) => current - 1)} type="button">Previous</button>
              <button className="button button-outline button-small" disabled={page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)} type="button">Next</button>
            </div>
          </div>
        </>
      ) : (
        <div className="empty-state">
          <h3>No transactions match these filters</h3>
          <p>Try widening your date range or clearing the reference search.</p>
        </div>
      )}
    </section>
  )
}
