import { useState } from 'react'
import { aiApi } from '../api/aiApi.js'

const suggestions = [
  'How can I make a simple monthly budget?',
  'What is the difference between a debit and credit?',
  'How do I keep my online banking account safe?',
]

export function AiAssistant() {
  const [messages, setMessages] = useState([])
  const [question, setQuestion] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function askAssistant(text) {
    const cleanedQuestion = text.trim()
    if (!cleanedQuestion || busy) return

    setQuestion('')
    setError('')
    setMessages((current) => [
      ...current,
      { role: 'user', text: cleanedQuestion },
    ])
    setBusy(true)

    try {
      const result = await aiApi.ask(cleanedQuestion)
      setMessages((current) => [
        ...current,
        { role: 'assistant', text: result.answer },
      ])
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  function handleSubmit(event) {
    event.preventDefault()
    askAssistant(question)
  }

  return (
    <section className="panel ai-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">ArthVeda assistant</p>
          <h2>Ask a banking question</h2>
        </div>
        <span className="ai-badge"><span></span> AI powered</span>
      </div>

      <p className="ai-privacy-note">
        General banking help from Gemini. Your account and transaction data are not sent.
        Avoid sharing passwords, PINs, card details or other private information.
      </p>

      {messages.length === 0 ? (
        <div className="ai-suggestions">
          <p>Try asking:</p>
          {suggestions.map((suggestion) => (
            <button
              className="ai-suggestion"
              disabled={busy}
              key={suggestion}
              onClick={() => askAssistant(suggestion)}
              type="button"
            >
              {suggestion}<span aria-hidden="true">↗</span>
            </button>
          ))}
        </div>
      ) : (
        <div aria-live="polite" className="ai-messages">
          {messages.map((message, index) => (
            <article className={`ai-message ${message.role}`} key={`${message.role}-${index}`}>
              <span className="ai-message-label">
                {message.role === 'user' ? 'You' : 'ArthVeda AI'}
              </span>
              <p>{message.text}</p>
            </article>
          ))}
          {busy && (
            <p aria-live="polite" className="ai-thinking">ArthVeda AI is preparing an answer…</p>
          )}
        </div>
      )}

      {error && <div className="alert alert-error ai-error" role="alert">{error}</div>}

      <form className="ai-form" onSubmit={handleSubmit}>
        <label className="sr-only" htmlFor="ai-question">Ask a general banking question</label>
        <textarea
          id="ai-question"
          maxLength="1000"
          onChange={(event) => setQuestion(event.target.value)}
          placeholder="Ask a general banking question..."
          required
          rows="2"
          value={question}
        />
        <button className="button button-primary" disabled={busy || !question.trim()} type="submit">
          {busy ? 'Thinking...' : 'Ask AI'}
        </button>
      </form>
      <p className="ai-disclaimer">
        AI responses can be incorrect and are for general education, not financial advice.
        Each question is sent to Google Gemini; chat history is not sent or saved.
      </p>
    </section>
  )
}
