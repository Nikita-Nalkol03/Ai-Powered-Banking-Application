import { useEffect, useMemo, useState } from 'react'
import { bankingApi } from '../api/bankingApi.js'
import { AiAssistant } from '../components/AiAssistant.jsx'
import { CustomerTransactions } from '../components/CustomerTransactions.jsx'
import { MonthlySpendingInsights } from '../components/MonthlySpendingInsights.jsx'
import { SmartInsights } from '../components/SmartInsights.jsx'
import { formatMoney } from '../utils/format.js'

export function CustomerDashboard({ user }) {
  const [accounts, setAccounts] = useState([])
  const [selectedAccount, setSelectedAccount] = useState('')
  const [accountType, setAccountType] = useState('SAVINGS')
  const [receiverAccountNumber, setReceiverAccountNumber] = useState('')
  const [amount, setAmount] = useState('')
  const [withdrawalAmount, setWithdrawalAmount] = useState('')
  const [description, setDescription] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const totalBalance = useMemo(
    () => accounts
      .filter((account) => account.status === 'ACTIVE')
      .reduce((total, account) => total + Number(account.balance || 0), 0),
    [accounts],
  )
  const activeAccount = accounts.find((account) => account.accountNumber === selectedAccount)
  async function loadAccounts() {
    setError('')
    try {
      const result = await bankingApi.accounts()
      setAccounts(result)
      setSelectedAccount((current) => (
        result.some((account) => account.accountNumber === current)
          ? current
          : result[0]?.accountNumber || ''
      ))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let current = true
    bankingApi.accounts()
      .then((result) => {
        if (!current) return
        setAccounts(result)
        setSelectedAccount(result[0]?.accountNumber || '')
      })
      .catch((requestError) => {
        if (current) setError(requestError.message)
      })
      .finally(() => {
        if (current) setLoading(false)
      })

    return () => {
      current = false
    }
  }, [])

  async function handleCreateAccount(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await bankingApi.createAccount(accountType)
      setNotice(`${accountType.toLowerCase()} account created.`)
      await loadAccounts()
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleTransfer(event) {
    event.preventDefault()
    if (!selectedAccount) {
      setError('Create an account before making a transfer.')
      return
    }
    if (activeAccount?.status !== 'ACTIVE') {
      setError('Transfers are only available from active accounts.')
      return
    }
    const transferAmount = Number(amount)
    if (!Number.isFinite(transferAmount) || transferAmount <= 0) {
      setError('Enter a transfer amount greater than zero.')
      return
    }
    if (!window.confirm(
      `Confirm transfer of ${formatMoney(transferAmount)} from account ending `
      + `${selectedAccount.slice(-4)} to account ending ${receiverAccountNumber.trim().slice(-4)}?`,
    )) return

    setBusy(true)
    setError('')
    setNotice('')
    try {
      const result = await bankingApi.transfer({
        senderAccountNumber: selectedAccount,
        receiverAccountNumber: receiverAccountNumber.trim(),
        amount: Number(amount),
        description: description.trim(),
      })
      setNotice(`Transfer completed. Reference: ${result.transactionReference}`)
      setReceiverAccountNumber('')
      setAmount('')
      setDescription('')
      await loadAccounts()
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleWithdrawal(event) {
    event.preventDefault()
    if (!selectedAccount) {
      setError('Create an account before withdrawing money.')
      return
    }
    if (activeAccount?.status !== 'ACTIVE') {
      setError('Withdrawals are only available from active accounts.')
      return
    }
    const requestedAmount = Number(withdrawalAmount)
    if (!Number.isFinite(requestedAmount) || requestedAmount <= 0) {
      setError('Enter a withdrawal amount greater than zero.')
      return
    }
    if (requestedAmount > Number(activeAccount?.balance || 0)) {
      setError('Withdrawal amount exceeds the available account balance.')
      return
    }
    if (!window.confirm(
      `Confirm withdrawal of ${formatMoney(requestedAmount)} from account ending `
      + `${selectedAccount.slice(-4)}?`,
    )) return

    setBusy(true)
    setError('')
    setNotice('')
    try {
      const result = await bankingApi.withdraw(selectedAccount, Number(withdrawalAmount))
      setNotice(`Withdrawal completed. Reference: ${result.transactionReference}`)
      setWithdrawalAmount('')
      await loadAccounts()
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="dashboard customer-dashboard">
      <section className="dashboard-hero customer-hero">
        <section className="welcome-row">
          <div>
            <p className="eyebrow">Your ArthVeda dashboard</p>
            <h1>Welcome back, {user.firstName || 'there'}.</h1>
            <p className="muted">A clear view of your money, accounts and recent activity.</p>
          </div>
          <div className="total-card">
            <span>Available balance</span>
            <strong>{formatMoney(totalBalance)}</strong>
            <small>Across {accounts.length} {accounts.length === 1 ? 'account' : 'accounts'}</small>
          </div>
        </section>
        <div className="customer-overview-strip">
          <div><span>Accounts</span><strong>{accounts.length}</strong></div>
          <div><span>Selected account</span><strong>{activeAccount?.accountType || 'Choose an account'}</strong></div>
          <div><span>Active accounts</span><strong>{accounts.filter((account) => account.status === 'ACTIVE').length}</strong></div>
        </div>
      </section>

      <nav aria-label="Customer dashboard sections" className="customer-section-nav">
        <a href="#customer-accounts">Accounts</a>
        <a href="#customer-transfer">Transfer</a>
        <a href="#customer-withdrawal">Withdrawal</a>
        <a href="#transactions">Statements</a>
        <a href="#customer-open-account">Open an account</a>
      </nav>

      {error && <div className="alert alert-error" role="alert">{error}</div>}
      {notice && <div className="alert alert-success" role="status">{notice}</div>}

      <div className="dashboard-grid">
        <section className="panel" id="customer-accounts">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Your banking</p>
              <h2>Accounts</h2>
            </div>
          </div>
          {loading ? (
            <p className="muted">Loading accounts...</p>
          ) : accounts.length ? (
            <div className="account-list">
              {accounts.map((account) => (
                <article
                  className={`account-card ${selectedAccount === account.accountNumber ? 'selected' : ''}`}
                  key={account.accountNumber}
                >
                  <button
                    aria-label={`Show ${account.accountType.toLowerCase()} account ending ${account.accountNumber.slice(-4)}`}
                    className="account-card-select"
                    onClick={() => setSelectedAccount(account.accountNumber)}
                    type="button"
                  >
                    <span className="account-symbol">{account.accountType === 'CURRENT' ? 'C' : 'S'}</span>
                    <span className="account-details">
                      <strong>{account.accountType} account</strong>
                      <small>•••• {account.accountNumber.slice(-4)}</small>
                      <small>Status: <span className={`status-badge status-${account.status.toLowerCase()}`}>{account.status}</span></small>
                    </span>
                    <strong className="account-money">{formatMoney(account.balance)}</strong>
                  </button>
                  <button
                    aria-label={`Copy account number ending ${account.accountNumber.slice(-4)}`}
                    className="copy-account"
                    onClick={async () => {
                      try {
                        await navigator.clipboard.writeText(account.accountNumber)
                        setNotice('Account number copied.')
                      } catch {
                        setError('Could not access the clipboard. Select the account to view its details.')
                      }
                    }}
                    type="button"
                  >
                    Copy
                  </button>
                </article>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <h3>No accounts yet</h3>
              <p>Create a savings or current account to start using your dashboard.</p>
            </div>
          )}
        </section>

        <section className="panel" id="customer-transfer">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Send money</p>
              <h2>Make a transfer</h2>
            </div>
          </div>
          <form className="form-stack" onSubmit={handleTransfer}>
            <label>
              From account
              <select onChange={(event) => setSelectedAccount(event.target.value)} required value={selectedAccount}>
                <option value="">Select an account</option>
                {accounts.map((account) => (
                  <option key={account.accountNumber} value={account.accountNumber}>
                    {account.accountType} · •••• {account.accountNumber.slice(-4)} · {account.status}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Recipient account number
              <input onChange={(event) => setReceiverAccountNumber(event.target.value)} required value={receiverAccountNumber} />
            </label>
            <label>
              Amount (INR)
              <input min="0.01" onChange={(event) => setAmount(event.target.value)} required step="0.01" type="number" value={amount} />
            </label>
            <label>
              Note (optional)
              <input maxLength="255" onChange={(event) => setDescription(event.target.value)} value={description} />
            </label>
            <button className="button button-primary" disabled={busy || activeAccount?.status !== 'ACTIVE'} type="submit">
              {busy ? 'Please wait...' : 'Transfer money'}
            </button>
          </form>
        </section>

        <section className="panel" id="customer-withdrawal">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Take out money</p>
              <h2>Withdraw funds</h2>
            </div>
          </div>
          <p className="muted withdraw-help">
            This records a debit from your selected account. It does not dispense cash at an ATM.
          </p>
          <form className="form-stack withdraw-form" onSubmit={handleWithdrawal}>
            <label>
              From account
              <select onChange={(event) => setSelectedAccount(event.target.value)} required value={selectedAccount}>
                <option value="">Select an account</option>
                {accounts.map((account) => (
                  <option key={account.accountNumber} value={account.accountNumber}>
                    {account.accountType} · •••• {account.accountNumber.slice(-4)} · {account.status}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Amount (INR)
              <input max={activeAccount?.balance || undefined} min="0.01" onChange={(event) => setWithdrawalAmount(event.target.value)} required step="0.01" type="number" value={withdrawalAmount} />
            </label>
            <button className="button button-outline" disabled={busy || activeAccount?.status !== 'ACTIVE'} type="submit">
              {busy ? 'Please wait...' : 'Withdraw funds'}
            </button>
          </form>
        </section>

        <CustomerTransactions accountNumber={activeAccount?.accountNumber || ''} />

        <section className="panel" id="customer-open-account">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Get started</p>
              <h2>Open an account</h2>
            </div>
          </div>
          <form className="form-stack" onSubmit={handleCreateAccount}>
            <label>
              Account type
              <select onChange={(event) => setAccountType(event.target.value)} value={accountType}>
                <option value="SAVINGS">Savings</option>
                <option value="CURRENT">Current</option>
              </select>
            </label>
            <button className="button button-outline" disabled={busy} type="submit">
              {busy ? 'Please wait...' : 'Create account'}
            </button>
          </form>
        </section>

        <MonthlySpendingInsights accountNumber={selectedAccount} />
        <SmartInsights accountNumber={selectedAccount} />
        <AiAssistant />
      </div>
    </main>
  )
}
