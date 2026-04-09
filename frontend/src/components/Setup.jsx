import { useState, useMemo } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './Setup.module.css'

function validatePassword(password) {
  return {
    minLength: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    digit: /\d/.test(password),
    special: /[^A-Za-z0-9]/.test(password),
  }
}

function PasswordRule({ met, label }) {
  return (
    <li className={met ? styles.ruleMet : styles.ruleUnmet}>
      <span className={styles.ruleIcon}>{met ? '\u2713' : '\u2022'}</span>
      {label}
    </li>
  )
}

export function Setup({ onComplete }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [touched, setTouched] = useState({ username: false, password: false, confirm: false })

  const rules = useMemo(() => validatePassword(password), [password])
  const allRulesMet = rules.minLength && rules.uppercase && rules.lowercase && rules.digit && rules.special
  const passwordsMatch = password === confirmPassword
  const usernameValid = username.trim().length >= 3
  const canSubmit = usernameValid && allRulesMet && passwordsMatch && confirmPassword.length > 0

  async function handleSubmit(e) {
    e.preventDefault()
    if (!canSubmit) return

    setLoading(true)
    setError(null)

    const result = await onComplete(username.trim(), password)
    if (!result.ok) {
      if (result.status === 409) {
        setError(STRINGS.SETUP_ERROR_CONFLICT)
      } else {
        setError(result.error || STRINGS.SETUP_ERROR_GENERIC)
      }
    }

    setLoading(false)
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div>
          <h1 className={styles.title}>{STRINGS.SETUP_TITLE}</h1>
          <p className={styles.subtitle}>{STRINGS.SETUP_SUBTITLE}</p>
        </div>

        <form className={styles.form} onSubmit={handleSubmit} action="/api/setup" method="POST">
          <div className={styles.field}>
            <label className={styles.label} htmlFor="setup-username">
              {STRINGS.SETUP_USERNAME_LABEL}
            </label>
            <input
              id="setup-username"
              name="username"
              type="text"
              className={styles.input}
              value={username}
              onChange={e => setUsername(e.target.value)}
              onBlur={() => setTouched(t => ({ ...t, username: true }))}
              autoComplete="username"
              autoFocus
              disabled={loading}
            />
            {touched.username && !usernameValid && (
              <p className={styles.fieldError}>{STRINGS.SETUP_USERNAME_MIN_LENGTH}</p>
            )}
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="setup-password">
              {STRINGS.SETUP_PASSWORD_LABEL}
            </label>
            <input
              id="setup-password"
              name="password"
              type="password"
              className={styles.input}
              value={password}
              onChange={e => setPassword(e.target.value)}
              onBlur={() => setTouched(t => ({ ...t, password: true }))}
              autoComplete="new-password"
              disabled={loading}
            />
            {touched.password && password.length > 0 && (
              <ul className={styles.rules}>
                <PasswordRule met={rules.minLength} label={STRINGS.SETUP_PASSWORD_MIN_LENGTH} />
                <PasswordRule met={rules.uppercase} label={STRINGS.SETUP_PASSWORD_UPPERCASE} />
                <PasswordRule met={rules.lowercase} label={STRINGS.SETUP_PASSWORD_LOWERCASE} />
                <PasswordRule met={rules.digit} label={STRINGS.SETUP_PASSWORD_DIGIT} />
                <PasswordRule met={rules.special} label={STRINGS.SETUP_PASSWORD_SPECIAL} />
              </ul>
            )}
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="setup-confirm-password">
              {STRINGS.SETUP_CONFIRM_PASSWORD_LABEL}
            </label>
            <input
              id="setup-confirm-password"
              name="confirm-password"
              type="password"
              className={styles.input}
              value={confirmPassword}
              onChange={e => setConfirmPassword(e.target.value)}
              onBlur={() => setTouched(t => ({ ...t, confirm: true }))}
              autoComplete="new-password"
              disabled={loading}
            />
            {touched.confirm && confirmPassword.length > 0 && !passwordsMatch && (
              <p className={styles.fieldError}>{STRINGS.SETUP_PASSWORD_MISMATCH}</p>
            )}
          </div>

          {error && <p className={styles.error}>{error}</p>}

          <button type="submit" className={styles.button} disabled={loading || !canSubmit}>
            {loading ? STRINGS.SETUP_CREATING : STRINGS.SETUP_BUTTON}
          </button>
        </form>
      </div>
    </div>
  )
}
