const features = [
  {
    number: '01',
    title: 'Your money, made clear',
    text: 'See your accounts, balances and recent activity together in one calm, easy-to-read dashboard.',
  },
  {
    number: '02',
    title: 'Move money with confidence',
    text: 'Make transfers and withdrawals with clear confirmations and secure, protected requests.',
  },
  {
    number: '03',
    title: 'Insights that make sense',
    text: 'Explore monthly spending totals and get helpful AI explanations based on your account aggregates.',
  },
]

export function LandingPage({ onLogin, onRegister }) {
  return (
    <main className="landing-page">
      <header className="landing-header">
        <a className="brand" href="#home" aria-label="ArthVeda home">
          <span className="brand-mark">A</span>
          <span>ArthVeda</span>
        </a>
        <nav className="landing-nav" aria-label="Landing page navigation">
          <a href="#features">Features</a>
          <a href="#security">Our approach</a>
        </nav>
        <div className="landing-actions">
          <button className="landing-signin" onClick={onLogin} type="button">Sign in</button>
          <button className="button button-primary" onClick={onRegister} type="button">Open account</button>
        </div>
      </header>

      <section className="landing-hero" id="home">
        <div className="landing-copy">
          <p className="landing-kicker"><span></span> Banking with clarity</p>
          <h1>Make every rupee<br />feel <em>well considered.</em></h1>
          <p className="landing-description">
            ArthVeda brings everyday banking, thoughtful insights and secure money management together in one place.
          </p>
          <div className="landing-actions landing-hero-actions">
            <button className="button button-primary" onClick={onRegister} type="button">
              Get started <span aria-hidden="true">→</span>
            </button>
            <button className="landing-text-link" onClick={onLogin} type="button">I already have an account</button>
          </div>
          <div className="landing-trust"><span className="trust-check">✓</span> Everyday banking designed around you</div>
        </div>

        <div className="landing-visual" aria-label="Illustration of the ArthVeda banking dashboard">
          <div className="visual-orbit orbit-one"></div>
          <div className="visual-orbit orbit-two"></div>
          <div className="visual-card">
            <div className="visual-card-head">
              <span className="visual-logo"><span>A</span> ArthVeda</span>
              <span className="visual-dots">•••</span>
            </div>
            <span className="visual-label">Your finances, at a glance</span>
            <strong className="visual-balance">Clarity <span>in every step</span></strong>
            <div className="visual-card-bottom">
              <span>BANKING WITH WISDOM</span>
              <span className="visual-chip"></span>
            </div>
          </div>
          <div className="float-note note-transfer">
            <span className="note-icon">↗</span>
            <span><small>Transfers</small><strong>Clear and secure</strong></span>
            <span className="note-ok">✓</span>
          </div>
          <div className="float-note note-growth">
            <span className="growth-icon">↗</span>
            <span><small>Monthly insights</small><strong>Understand your spend</strong></span>
          </div>
          <div className="visual-decoration decoration-a"></div>
          <div className="visual-decoration decoration-b"></div>
        </div>
      </section>

      <section className="landing-stats" id="security">
        <div><strong>Thoughtful</strong><span>clear everyday banking</span></div>
        <div><strong>Protected</strong><span>secure account access</span></div>
        <div><strong>In control</strong><span>your money, your view</span></div>
      </section>

      <section className="features-section" id="features">
        <div className="features-heading">
          <p className="landing-kicker"><span></span> A more considered way to bank</p>
          <h2>Everything you need.<br /><em>Clear by design.</em></h2>
        </div>
        <div className="features-grid">
          {features.map((feature) => (
            <article className="feature-card" key={feature.number}>
              <span className="feature-number">{feature.number}</span>
              <h3>{feature.title}</h3>
              <p>{feature.text}</p>
              <span className="feature-arrow" aria-hidden="true">↗</span>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-cta">
        <div>
          <p className="landing-kicker"><span></span> Your next step starts here</p>
          <h2>Bring a little more<br /><em>clarity to your money.</em></h2>
        </div>
        <button className="button landing-cta-button" onClick={onRegister} type="button">
          Create your account <span aria-hidden="true">→</span>
        </button>
      </section>

      <footer className="landing-footer">
        <a className="brand" href="#home"><span className="brand-mark">A</span><span>ArthVeda</span></a>
        <span>Banking with clarity. Built around you.</span>
        <span>© {new Date().getFullYear()} ArthVeda</span>
      </footer>
    </main>
  )
}
