import { useState, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { STRINGS } from '../constants/strings'
import { ReadeckBrowser } from './ReadeckBrowser'
import styles from './UrlInput.module.css'

const URL_PATTERN = /^https?:\/\/.+/i
const VALID_MODES = ['url', 'text', 'readeck']
const MAX_URLS = 5

/**
 * Extract valid URLs from pasted text (newline, comma, or space-separated).
 * Returns an array of trimmed URL strings.
 */
function extractUrls(text) {
  return text
    .split(/[\n,]+/)
    .map(s => s.trim())
    .filter(s => URL_PATTERN.test(s))
}

export function UrlInput({ onSubmitUrl, onSubmitBatch, onSubmitText, loading, error, readeck, readeckConfigured, readeckKey }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialMode = VALID_MODES.includes(searchParams.get('mode')) ? searchParams.get('mode') : 'url'
  const [mode, setMode] = useState(initialMode)

  // Multi-URL state: array of URL strings. When length === 1, behaves like single mode.
  const [urls, setUrls] = useState([''])
  const [text, setText] = useState('')
  const [validationError, setValidationError] = useState(null)
  const inputRefs = useRef([])

  const isBatch = urls.length > 1

  function handleSubmit(e) {
    e.preventDefault()

    if (mode === 'url') {
      if (isBatch) {
        // Batch: validate all non-empty URLs
        const validUrls = urls.map(u => u.trim()).filter(u => u.length > 0)
        const invalid = validUrls.find(u => !URL_PATTERN.test(u))
        if (invalid) {
          setValidationError(STRINGS.ERROR_INVALID_URL)
          return
        }
        if (validUrls.length < 2) {
          // Fell back to 1 or 0 valid URLs
          if (validUrls.length === 1) {
            setValidationError(null)
            onSubmitUrl(validUrls[0])
            return
          }
          setValidationError(STRINGS.ERROR_INVALID_URL)
          return
        }
        setValidationError(null)
        onSubmitBatch(validUrls)
      } else {
        const trimmed = urls[0].trim()
        if (!URL_PATTERN.test(trimmed)) {
          setValidationError(STRINGS.ERROR_INVALID_URL)
          return
        }
        setValidationError(null)
        onSubmitUrl(trimmed)
      }
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

  function handleUrlChange(index, value) {
    setUrls(prev => {
      const next = [...prev]
      next[index] = value
      return next
    })
    if (validationError) setValidationError(null)
  }

  function handleUrlPaste(index, e) {
    const pasted = e.clipboardData.getData('text')
    const extracted = extractUrls(pasted)

    if (extracted.length > 1) {
      e.preventDefault()
      // Merge extracted URLs into the list, respecting the max
      setUrls(prev => {
        const before = prev.slice(0, index)
        const after = prev.slice(index + 1).filter(u => u.trim() !== '')
        const merged = [...before, ...extracted, ...after].slice(0, MAX_URLS)
        return merged
      })
      if (validationError) setValidationError(null)
    }
    // Single URL paste: let default behavior handle it
  }

  function addUrlRow() {
    if (urls.length >= MAX_URLS) return
    setUrls(prev => [...prev, ''])
    // Focus the new input after render
    setTimeout(() => {
      const idx = urls.length
      inputRefs.current[idx]?.focus()
    }, 0)
  }

  function removeUrlRow(index) {
    setUrls(prev => {
      const next = prev.filter((_, i) => i !== index)
      return next.length === 0 ? [''] : next
    })
  }

  async function handlePaste() {
    try {
      const clipboard = await navigator.clipboard.readText()
      if (mode === 'url') {
        const extracted = extractUrls(clipboard)
        if (extracted.length > 1) {
          setUrls(extracted.slice(0, MAX_URLS))
        } else {
          setUrls(prev => {
            const next = [...prev]
            next[0] = clipboard
            return next
          })
        }
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
    setSearchParams(newMode === 'url' ? {} : { mode: newMode }, { replace: true })
  }

  // Readeck article selected
  function handleReadeckSummarize(text, title, articleUrl) {
    if (text) {
      onSubmitText(text, title, articleUrl)
    } else if (articleUrl) {
      onSubmitUrl(articleUrl)
    }
  }

  const filledUrls = urls.filter(u => u.trim().length > 0)
  const canSubmit = mode === 'url'
    ? filledUrls.length > 0
    : text.trim().length > 0

  const submitLabel = isBatch && filledUrls.length > 1
    ? STRINGS.BATCH_SUMMARIZE.replace('{n}', filledUrls.length)
    : STRINGS.SUMMARIZE_BUTTON

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
        <div className={styles.urlMode}>
          {urls.map((urlValue, index) => (
            <div key={index} className={styles.inputRow}>
              <input
                ref={el => inputRefs.current[index] = el}
                type="url"
                className={styles.input}
                value={urlValue}
                onChange={(e) => handleUrlChange(index, e.target.value)}
                onPaste={(e) => handleUrlPaste(index, e)}
                placeholder={index === 0 ? STRINGS.INPUT_PLACEHOLDER : `URL ${index + 1}`}
                disabled={loading}
                autoFocus={index === 0}
              />
              {isBatch && (
                <button
                  type="button"
                  className={styles.removeUrlBtn}
                  onClick={() => removeUrlRow(index)}
                  disabled={loading}
                  aria-label="Remove URL"
                  title="Remove URL"
                >
                  &times;
                </button>
              )}
              {index === 0 && !isBatch && (
                <button
                  type="button"
                  className={styles.pasteBtn}
                  onClick={handlePaste}
                  disabled={loading}
                  title={STRINGS.PASTE_BUTTON}
                >
                  {STRINGS.PASTE_BUTTON}
                </button>
              )}
            </div>
          ))}

          <div className={styles.urlActions}>
            {urls.length < MAX_URLS && (
              <button
                type="button"
                className={styles.addUrlBtn}
                onClick={addUrlRow}
                disabled={loading}
              >
                {STRINGS.BATCH_ADD_URL}
              </button>
            )}
            {isBatch && (
              <span className={styles.urlCounter}>
                {STRINGS.BATCH_COUNTER.replace('{n}', urls.length).replace('{max}', MAX_URLS)}
              </span>
            )}
            <div className={styles.urlActionsRight}>
              {isBatch && (
                <button
                  type="button"
                  className={styles.pasteBtn}
                  onClick={handlePaste}
                  disabled={loading}
                  title={STRINGS.PASTE_BUTTON}
                >
                  {STRINGS.PASTE_BUTTON}
                </button>
              )}
              <button
                type="submit"
                className={styles.submitBtn}
                disabled={loading || !canSubmit}
              >
                {submitLabel}
              </button>
            </div>
          </div>
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
