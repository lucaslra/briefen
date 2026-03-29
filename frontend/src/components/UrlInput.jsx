import { useState } from 'react'
import { STRINGS } from '../constants/strings'
import { ReadeckBrowser } from './ReadeckBrowser'
import styles from './UrlInput.module.css'

const URL_PATTERN = /^https?:\/\/.+/i

export function UrlInput({ onSubmitUrl, onSubmitText, loading, error, readeck, readeckConfigured, readeckKey }) {
  const [mode, setMode] = useState('url') // 'url' | 'text' | 'readeck'
  const [url, setUrl] = useState('')
  const [text, setText] = useState('')
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
    } else if (mode === 'text') {
      const trimmed = text.trim()
      if (!trimmed) {
        setValidationError(STRINGS.ERROR_EMPTY_TEXT)
        return
      }
      setValidationError(null)
      onSubmitText(trimmed, null)
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
      setValidationError(STRINGS.ERROR_CLIPBOARD)
    }
  }

  function switchMode(newMode) {
    setMode(newMode)
    setValidationError(null)
  }

  // Readeck article selected — can provide text+title+sourceUrl or just URL
  function handleReadeckSummarize(text, title, articleUrl) {
    if (text) {
      onSubmitText(text, title, articleUrl)
    } else if (articleUrl) {
      onSubmitUrl(articleUrl)
    }
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
        {readeckConfigured && (
          <button
            type="button"
            className={`${styles.tab} ${mode === 'readeck' ? styles.tabActive : ''}`}
            onClick={() => switchMode('readeck')}
          >
            {STRINGS.TAB_READECK}
          </button>
        )}
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
      ) : mode === 'text' ? (
        <div className={styles.textMode}>
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
      ) : (
        <ReadeckBrowser
          key={readeckKey}
          readeck={readeck}
          onSummarize={handleReadeckSummarize}
          loading={loading}
        />
      )}

      {(validationError || error) && (
        <div className={styles.errorBox}>
          {validationError || error}
        </div>
      )}
    </form>
  )
}
