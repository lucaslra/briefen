import { useState } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './UrlInput.module.css'

const URL_PATTERN = /^https?:\/\/.+/i

export function UrlInput({ onSubmit, loading }) {
  const [url, setUrl] = useState('')
  const [validationError, setValidationError] = useState(null)

  function handleSubmit(e) {
    e.preventDefault()
    const trimmed = url.trim()
    if (!URL_PATTERN.test(trimmed)) {
      setValidationError(STRINGS.ERROR_INVALID_URL)
      return
    }
    setValidationError(null)
    onSubmit(trimmed)
  }

  async function handlePaste() {
    try {
      const text = await navigator.clipboard.readText()
      setUrl(text)
      setValidationError(null)
    } catch {
      // Clipboard access denied — ignore silently
    }
  }

  function handleChange(e) {
    setUrl(e.target.value)
    if (validationError) setValidationError(null)
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.inputRow}>
        <input
          type="url"
          className={styles.input}
          value={url}
          onChange={handleChange}
          placeholder={STRINGS.INPUT_PLACEHOLDER}
          disabled={loading}
          autoFocus
        />
        <button
          type="button"
          className={styles.pasteBtn}
          onClick={handlePaste}
          disabled={loading}
          title={STRINGS.PASTE_BUTTON}
        >
          {STRINGS.PASTE_BUTTON}
        </button>
        <button
          type="submit"
          className={styles.submitBtn}
          disabled={loading || !url.trim()}
        >
          {STRINGS.SUMMARIZE_BUTTON}
        </button>
      </div>
      {validationError && (
        <p className={styles.error}>{validationError}</p>
      )}
    </form>
  )
}
