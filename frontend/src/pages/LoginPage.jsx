import { useState } from 'react'
import { authApi } from '../api/authApi.js'
import { useAuth } from '../context/useAuth.js'
import '../styles/auth.css'     

const securityQuestions = [
  { value: 'FIRST_SCHOOL', label: 'What was the name of your first school?' },
  { value: 'CHILDHOOD_FRIEND', label: 'What was the first name of your childhood best friend?' },
  { value: 'FIRST_PET', label: 'What was the name of your first pet?' },
  { value: 'BIRTH_CITY', label: 'In which city were you born?' },
  { value: 'FAVORITE_TEACHER', label: 'What was the last name of your favorite teacher?' },
]
const securityQuestionLabels = Object.fromEntries(
  securityQuestions.map((question) => [question.value, question.label]),
)

const emptyRegistration = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  password: '',
  securityQuestion: '',
  securityAnswer: '',
}

export function LoginPage({ initialError, clearError, initialRegisterMode = false, onBack }) {
  const { login, register } = useAuth()
  const [isRegistering, setIsRegistering] = useState(initialRegisterMode)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [registration, setRegistration] = useState(emptyRegistration)
  const [view, setView] = useState(initialRegisterMode ? 'register' : 'login')
  const [recovery, setRecovery] = useState({
    email: '',
    securityQuestion: '',
    securityAnswer: '',
    newPassword: '',
  })
  const [recoveryQuestion, setRecoveryQuestion] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState(initialError || '')
  const [busy, setBusy] = useState(false)

  function startModeChange(registerMode) {
    setView(registerMode ? 'register' : 'login')
    setIsRegistering(registerMode)
    setMessage('')
    setError('')
    clearError()
  }

  function startRecovery() {
    setView('forgot')
    setRecoveryQuestion('')
    setRecovery((current) => ({
      email: current.email || email,
      securityQuestion: '',
      securityAnswer: '',
      newPassword: '',
    }))
    setMessage('')
    setError('')
    clearError()
  }

  async function handleFindRecoveryQuestion(event) {
    event.preventDefault()
    setBusy(true)
    setError('')

    try {
      const result = await authApi.recoveryQuestion(recovery.email)
      setRecoveryQuestion(result.securityQuestion)
      setRecovery((current) => ({
        ...current,
        securityQuestion: result.securityQuestion,
        securityAnswer: '',
        newPassword: '',
      }))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  function updateRecovery(event) {
    const { name, value } = event.target
    setRecovery((current) => ({ ...current, [name]: value }))
  }

  async function handleLogin(event) {
    event.preventDefault()
    setBusy(true)
    setError('')

    try {
      await login({ email, password })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleRegister(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')

    try {
      await register(registration)
      setEmail(registration.email)
      setRegistration(emptyRegistration)
      setIsRegistering(false)
      setMessage('Your account is ready. Sign in to continue.')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  async function handlePasswordReset(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')

    try {
      await authApi.resetPassword(recovery)
      setEmail(recovery.email)
      setPassword('')
      setRecovery({
        email: '',
        securityQuestion: '',
        securityAnswer: '',
        newPassword: '',
      })
      setRecoveryQuestion('')
      setView('login')
      setIsRegistering(false)
      setMessage('Password reset successfully. Sign in using your new password.')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  function updateRegistration(event) {
    const { name, value } = event.target
    setRegistration((current) => ({ ...current, [name]: value }))
  }

  return (
    <main className="auth-page">
      <section className="auth-intro">
        <a className="brand auth-brand" href="/">
          <span className="brand-mark">A</span>
          <span>ArthVeda</span>
        </a>
        <button className="back-link" onClick={onBack} type="button">← Back to home</button>
        <p className="eyebrow">Banking made simple</p>
        <h1>Your money, clearly in view.</h1>
        <p>Manage accounts, review transactions and move money securely from one easy dashboard.</p>
        <div className="auth-points">
          <span>Secure sign in</span>
          <span>Simple account management</span>
          <span>Clear transaction history</span>
        </div>
      </section>

      <section className="auth-card">
        {view !== 'forgot' && <div className="auth-switch" role="tablist" aria-label="Account access">
          <button
            className={!isRegistering ? 'active' : ''}
            onClick={() => startModeChange(false)}
            role="tab"
            type="button"
          >
            Sign in
          </button>
          <button
            className={isRegistering ? 'active' : ''}
            onClick={() => startModeChange(true)}
            role="tab"
            type="button"
          >
            Create account
          </button>
        </div>}

        <h2>{view === 'forgot' ? 'Reset your password' : isRegistering ? 'Create your account' : 'Welcome back'}</h2>
        <p className="form-help">
          {view === 'forgot'
            ? 'Answer the security question you selected when you registered, then choose a new password.'
            : isRegistering
              ? 'A few details and you are ready to go.'
              : 'Sign in to your ArthVeda account.'}
        </p>

        {error && <div className="alert alert-error" role="alert">{error}</div>}
        {message && <div className="alert alert-success" role="status">{message}</div>}

        {view === 'forgot' ? (
          <>
            {!recoveryQuestion ? (
              <form className="form-stack" onSubmit={handleFindRecoveryQuestion}>
                <label>
                  Email address
                  <input autoComplete="email" name="email" onChange={updateRecovery} required type="email" value={recovery.email} />
                </label>
                <button className="button button-primary button-full" disabled={busy} type="submit">
                  {busy ? 'Finding your question...' : 'Continue'}
                </button>
                <button className="auth-back-button" onClick={() => startModeChange(false)} type="button">
                  Back to sign in
                </button>
              </form>
            ) : (
              <form className="form-stack" onSubmit={handlePasswordReset}>
                <p className="recovery-question">
                  {securityQuestionLabels[recoveryQuestion] || 'Answer your saved security question'}
                </p>
                <label>
                  Your answer
                  <input autoComplete="off" maxLength="100" name="securityAnswer" onChange={updateRecovery} required value={recovery.securityAnswer} />
                </label>
                <label>
                  New password
                  <input autoComplete="new-password" minLength="8" name="newPassword" onChange={updateRecovery} required type="password" value={recovery.newPassword} />
                  <span className="field-hint">Use at least 8 characters.</span>
                </label>
                <button className="button button-primary button-full" disabled={busy} type="submit">
                  {busy ? 'Verifying answer...' : 'Verify and reset password'}
                </button>
                <button className="auth-back-button" onClick={() => setRecoveryQuestion('')} type="button">
                  Use a different email
                </button>
              </form>
            )}
          </>
        ) : isRegistering ? (
          <form className="form-stack" onSubmit={handleRegister}>
            <div className="form-row">
              <label>
                First name
                <input autoComplete="given-name" maxLength="50" name="firstName" onChange={updateRegistration} required value={registration.firstName} />
              </label>
              <label>
                Last name
                <input autoComplete="family-name" maxLength="50" name="lastName" onChange={updateRegistration} required value={registration.lastName} />
              </label>
            </div>
            <label>
              Email address
              <input autoComplete="email" name="email" onChange={updateRegistration} required type="email" value={registration.email} />
            </label>
            
<label htmlFor="phone">Mobile Number</label>

<div className="phone-input">
  
  <input
    type="tel"
    id="phone"
    name="phone"
    autoComplete="tel-national"
    inputMode="numeric"
    maxLength={10}
    pattern="[6-9][0-9]{9}"
    placeholder="Enter 10-digit number"
    onChange={updateRegistration}
    required
    value={registration.phone}
  />
</div>

            <label>
              Password
              <input autoComplete="new-password" minLength="8" name="password" onChange={updateRegistration} required type="password" value={registration.password} />
              <span className="field-hint">Use at least 8 characters.</span>
            </label>
            <label>
              Security question
              <select name="securityQuestion" onChange={updateRegistration} required value={registration.securityQuestion}>
                <option value="">Choose a question for password recovery</option>
                {securityQuestions.map((question) => (
                  <option key={question.value} value={question.value}>{question.label}</option>
                ))}
              </select>
            </label>
            <label>
              Security answer
              <input autoComplete="off" maxLength="100" name="securityAnswer" onChange={updateRegistration} required value={registration.securityAnswer} />
              <span className="field-hint">Remember your answer. It is saved securely in a hashed form.</span>
            </label>
            <button className="button button-primary button-full" disabled={busy} type="submit">
              {busy ? 'Creating account...' : 'Create account'}
            </button>
          </form>
        ) : (
          <form className="form-stack" onSubmit={handleLogin}>
            <label>
              Email address
              <input autoComplete="username" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
            </label>
            <label>
              Password
              <input autoComplete="current-password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
            </label>
            <button className="button button-primary button-full" disabled={busy} type="submit">
              {busy ? 'Signing in...' : 'Sign in'}
            </button>
            <button className="auth-back-button" onClick={startRecovery} type="button">
              Forgot password?
            </button>
            <p className="field-hint">Your dashboard is selected securely from your account role.</p>
          </form>
        )}
      </section>
    </main>
  )
}
