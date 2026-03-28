import { useState } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './UrlInput.module.css'

const URL_PATTERN = /^https?:\/\/.+/i

export function UrlInput({ onSubmitUrl, onSubmitText, loading }) {
  const [mode, setMode] = useState('url') // 'url' | 'text'
  const [url, setUrl] = useState('')
  const [text, setText] = useState('')
  const [title, setTitle] = useState('')
  const [validationError, setValidationError] = useState(null)

  function handleSubmit(e) {
    e.preventDefault()

    if (mode === 'url') {
      const trimmed = url.trim()
      if (!URL_PATTERN.test(trimmed)) {
        setValidationError(STRINGS.ERROR_INVALID_URL)
        return
      }
      setValidationError(null)
      onSubmitUrl(trimmed)
    } else {
      const trimmed = text.trim()
      if (!trimmed) {
        setValidationError(STRINGS.ERROR_EMPTY_TEXT)
        return
      }
      setValidationError(null)
      onSubmitText(trimmed, title.trim() || null)
    }
  }

  async function handlePaste() {
    try {
      const clipboard = await navigator.clipboard.readText()
      if (mode === 'url') {
        setUrl(clipboard)
      } else {
        setText(clipboard)
      }
      setValidationError(null)
    } catch {
      // Clipboard access denied — ignore silently
    }
  }

  function switchMode(newMode) {
    setMode(newMode)
    setValidationError(null)
  }

  const canSubmit = mode === 'url' ? url.trim().length > 0 : text.trim().length > 0

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.tabs}>
        <button
          type="button"
          className={`${styles.tab} ${mode === 'url' ? styles.tabActive : ''}`}
          onClick={() => switchMode('url')}
        >
          {STRINGS.TAB_URL}
        </button>
        <button
          type="button"
          className={`${styles.tab} ${mode === 'text' ? styles.tabActive : ''}`}
          onClick={() => switchMode('text')}
        >
          {STRINGS.TAB_TEXT}
        </button>
      </div>

      {mode === 'url' ? (
        <div className={styles.inputRow}>
          <input
            type="url"
            className={styles.input}
            value={url}
            onChange={(e) => { setUrl(e.target.value); if (validationError) setValidationError(null) }}
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
            disabled={loading || !canSubmit}
          >
            {STRINGS.SUMMARIZE_BUTTON}
          </button>
        </div>
      ) : (
        <div className={styles.textMode}>
          <input
            type="text"
            className={styles.input}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder={STRINGS.TITLE_PLACEHOLDER}
            disabled={loading}
          />
          <textarea
            className={styles.textarea}
            value={text}
            onChange={(e) => { setText(e.target.value); if (validationError) setValidationError(null) }}
            placeholder={STRINGS.TEXT_PLACEHOLDER}
            disabled={loading}
            rows={8}
          />
          <div className={styles.textActions}>
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
              disabled={loading || !canSubmit}
            >
              {STRINGS.SUMMARIZE_BUTTON}
            </button>
          </div>
        </div>
      )}

      {validationError && (
        <p className={styles.error}>{validationError}</p>
      )}
    </form>
  )
}
