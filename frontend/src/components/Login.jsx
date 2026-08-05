import { useState, useEffect } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './Login.module.css'

// Maps an OIDC callback ?auth_error=<reason> to a user-facing STRINGS key.
const AUTH_ERROR_KEYS = {
  provider_error: 'AUTH_ERROR_PROVIDER_ERROR',
  access_denied: 'AUTH_ERROR_ACCESS_DENIED',
  missing_state: 'AUTH_ERROR_SESSION_EXPIRED',
  state_mismatch: 'AUTH_ERROR_SESSION_EXPIRED',
  invalid_token: 'AUTH_ERROR_EXCHANGE_FAILED',
  exchange_failed: 'AUTH_ERROR_EXCHANGE_FAILED',
  missing_subject: 'AUTH_ERROR_MISSING_SUBJECT',
  signup_disabled: 'AUTH_ERROR_SIGNUP_DISABLED',
  server_error: 'AUTH_ERROR_SERVER_ERROR',
}

function authErrorFromUrl() {
  const reason = new URLSearchParams(window.location.search).get('auth_error')
  if (!reason) return null
  return STRINGS[AUTH_ERROR_KEYS[reason] || 'AUTH_ERROR_DEFAULT']
}

export function Login({ onLogin, authConfig }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  // Derive any OIDC callback error (?auth_error=…) up front so we don't setState in an effect.
  const [error, setError] = useState(authErrorFromUrl)

  const oidcEnabled = authConfig?.oidc?.enabled === true
  const providerName = authConfig?.oidc?.providerName || 'SSO'
  // Default to showing the password form unless the backend explicitly disables it.
  const passwordEnabled = authConfig ? authConfig.passwordLogin !== false : true
  const ssoUrl = `${import.meta.env.BASE_URL}oauth2/authorization/briefen`

  // Strip the auth_error param from the URL after it has been surfaced (no setState here).
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (params.has('auth_error')) {
      params.delete('auth_error')
      const qs = params.toString()
      window.history.replaceState(null, '', window.location.pathname + (qs ? `?${qs}` : ''))
    }
  }, [])

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

        {error && <p className={styles.error} role="alert">{error}</p>}

        {oidcEnabled && (
          <a className={styles.ssoButton} href={ssoUrl}>
            {STRINGS.LOGIN_SSO_PREFIX} {providerName}
          </a>
        )}

        {oidcEnabled && passwordEnabled && (
          <div className={styles.divider}><span>{STRINGS.LOGIN_OR}</span></div>
        )}

        {passwordEnabled && (
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

            <button type="submit" className={styles.button} disabled={loading || !username.trim() || !password}>
              {loading ? STRINGS.LOGIN_SIGNING_IN : STRINGS.LOGIN_BUTTON}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
