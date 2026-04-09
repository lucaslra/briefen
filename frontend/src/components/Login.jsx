import { useState } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './Login.module.css'

export function Login({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    if (!username.trim() || !password) return

    setLoading(true)
    setError(null)

    const success = await onLogin(username.trim(), password)
    if (!success) {
      setError(STRINGS.LOGIN_INVALID_CREDENTIALS)
    }

    setLoading(false)
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div>
          <h1 className={styles.title}>{STRINGS.APP_TITLE}</h1>
          <p className={styles.subtitle}>{STRINGS.LOGIN_SUBTITLE}</p>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="login-username">
              {STRINGS.LOGIN_USERNAME_LABEL}
            </label>
            <input
              id="login-username"
              name="username"
              type="text"
              className={styles.input}
              value={username}
              onChange={e => setUsername(e.target.value)}
              autoComplete="username"
              autoFocus
              disabled={loading}
            />
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="login-password">
              {STRINGS.LOGIN_PASSWORD_LABEL}
            </label>
            <input
              id="login-password"
              name="password"
              type="password"
              className={styles.input}
              value={password}
              onChange={e => setPassword(e.target.value)}
              autoComplete="current-password"
              disabled={loading}
            />
          </div>

          {error && <p className={styles.error}>{error}</p>}

          <button type="submit" className={styles.button} disabled={loading || !username.trim() || !password}>
            {loading ? STRINGS.LOGIN_SIGNING_IN : STRINGS.LOGIN_BUTTON}
          </button>
        </form>
      </div>
    </div>
  )
}
