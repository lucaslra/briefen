import { useState, useEffect } from 'react'
import Markdown from 'react-markdown'
import { STRINGS, MAX_LENGTH_ADJUSTMENTS } from '../constants/strings'
import { formatElapsed } from '../hooks/useElapsedTime'
import styles from './SummaryDisplay.module.css'

export function SummaryDisplay({ data, onMakeShorter, onMakeLonger, loading, elapsedMs }) {
  const [copied, setCopied] = useState(false)
  const [shorterCount, setShorterCount] = useState(0)
  const [longerCount, setLongerCount] = useState(0)

  // Reset adjustment counts when the underlying data changes (new article or fresh summary)
  useEffect(() => {
    setShorterCount(0)
    setLongerCount(0)
  }, [data?.url, data?.id])

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
      {data.title && <h2 className={styles.title}>{data.title}</h2>}
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
          {elapsedMs > 0 && (
            <span className={styles.elapsed}>
              {STRINGS.GENERATED_IN} {formatElapsed(elapsedMs)}
            </span>
          )}
        </div>
        <button className={styles.copyBtn} onClick={handleCopy}>
          {copied ? STRINGS.COPIED_TEXT : STRINGS.COPY_BUTTON}
        </button>
      </div>
    </div>
  )
}
