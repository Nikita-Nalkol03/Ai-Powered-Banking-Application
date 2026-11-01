import { useState } from 'react'
import { AdminDashboard } from './pages/AdminDashboard.jsx'
import { CustomerDashboard } from './pages/CustomerDashboard.jsx'
import { LoginPage } from './pages/LoginPage.jsx'
import { LandingPage } from './pages/LandingPage.jsx'
import { useAuth } from './context/useAuth.js'
import './styles/dashboard.css'
import './styles/forms.css'
import './styles/landing.css'
import './styles/arthveda.css'

function App() {
  const { user, loading, error, logout, clearError } = useAuth()
  const [authMode, setAuthMode] = useState(null)

  if (loading) {
    return <main className="loading-screen">Loading your secure banking session...</main>
  }

  if (!user) {
    return authMode
      ? (
        <LoginPage
          clearError={clearError}
          initialError={error}
          initialRegisterMode={authMode === 'register'}
          onBack={() => setAuthMode(null)}
        />
      )
      : <LandingPage onLogin={() => setAuthMode('login')} onRegister={() => setAuthMode('register')} />
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="ArthVeda home">
          <span className="brand-mark">A</span>
          <span>ArthVeda</span>
        </a>
        <div className="topbar-user">
          <span className="role-label">{user.role === 'ADMIN' ? 'Administrator' : 'Customer'}</span>
          <span className="user-name">{user.firstName} {user.lastName}</span>
          <button className="button button-outline button-small" onClick={logout} type="button">
            Sign out
          </button>
        </div>
      </header>

      {user.role === 'ADMIN'
        ? <AdminDashboard user={user} />
        : <CustomerDashboard user={user} />}
    </div>
  )
}

export default App
