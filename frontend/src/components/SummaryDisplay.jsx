import { useState } from 'react'
import Markdown from 'react-markdown'
import { STRINGS, MAX_LENGTH_ADJUSTMENTS } from '../constants/strings'
import { formatElapsed } from '../hooks/useElapsedTime'
import styles from './SummaryDisplay.module.css'

export function SummaryDisplay({ data, onMakeShorter, onMakeLonger, onRegenerate, onClear, loading, elapsedMs }) {
  const [copied, setCopied] = useState(false)
  const [shorterCount, setShorterCount] = useState(0)
  const [longerCount, setLongerCount] = useState(0)

  // Reset adjustment counts when the underlying data changes (adjust state during render)
  const [prevDataKey, setPrevDataKey] = useState(() => `${data?.url}|${data?.id}`)
  const currentDataKey = `${data?.url}|${data?.id}`
  if (prevDataKey !== currentDataKey) {
    setPrevDataKey(currentDataKey)
    setShorterCount(0)
    setLongerCount(0)
  }

  if (!data) return null

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(data.summary)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // Clipboard write failed — ignore
    }
  }

  function handleShorter() {
    if (shorterCount < MAX_LENGTH_ADJUSTMENTS && !loading) {
      setShorterCount(prev => prev + 1)
      onMakeShorter()
    }
  }

  function handleLonger() {
    if (longerCount < MAX_LENGTH_ADJUSTMENTS && !loading) {
      setLongerCount(prev => prev + 1)
      onMakeLonger()
    }
  }

  const date = new Date(data.createdAt).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })

  return (
    <div className={styles.container}>
      <div className={styles.titleRow}>
        {data.title && <h2 className={styles.title}>{data.title}</h2>}
        {onClear && (
          <button className={styles.clearBtn} onClick={onClear} title="New summary">
            &times;
          </button>
        )}
      </div>
      <div className={styles.summary}>
        <Markdown>{data.summary}</Markdown>
      </div>

      {data.url && (
        <div className={styles.adjustButtons}>
          <button
            className={styles.adjustBtn}
            onClick={handleShorter}
            disabled={shorterCount >= MAX_LENGTH_ADJUSTMENTS || loading}
          >
            {STRINGS.MAKE_SHORTER}
          </button>
          <button
            className={styles.adjustBtn}
            onClick={handleLonger}
            disabled={longerCount >= MAX_LENGTH_ADJUSTMENTS || loading}
          >
            {STRINGS.MAKE_LONGER}
          </button>
        </div>
      )}

      <div className={styles.footer}>
        <div className={styles.meta}>
          {data.url ? (
            <a
              href={data.url}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.source}
            >
              {STRINGS.SOURCE_LABEL}: {new URL(data.url).hostname}
            </a>
          ) : (
            <span className={styles.pastedLabel}>Pasted content</span>
          )}
          <span className={styles.date}>{date}</span>
          {data.modelUsed && (
            <span className={styles.elapsed}>
              {STRINGS.GENERATED_WITH} {data.modelUsed}
            </span>
          )}
          {elapsedMs > 0 && (
            <span className={styles.elapsed}>
              {elapsedMs < 1000 ? STRINGS.CACHED : `${STRINGS.GENERATED_IN} ${formatElapsed(elapsedMs)}`}
            </span>
          )}
        </div>
        <div className={styles.footerActions}>
          {data.url && onRegenerate && (
            <button
              className={styles.regenerateBtn}
              onClick={onRegenerate}
              disabled={loading}
              title={STRINGS.REGENERATE_BUTTON}
            >
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M1.5 2v5h5" />
                <path d="M2.3 10.5a6.5 6.5 0 1 0 1.2-5.5L1.5 7" />
              </svg>
              {STRINGS.REGENERATE_BUTTON}
            </button>
          )}
          <button className={styles.copyBtn} onClick={handleCopy}>
            {copied ? STRINGS.COPIED_TEXT : STRINGS.COPY_BUTTON}
          </button>
        </div>
      </div>
    </div>
  )
}
