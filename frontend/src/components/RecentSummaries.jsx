import { useState } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './RecentSummaries.module.css'

export function RecentSummaries({ summaries, loading, hasMore, onLoadMore, onSelect }) {
  const [expanded, setExpanded] = useState(false)

  if (summaries.length === 0 && !loading) {
    return (
      <section className={styles.section}>
        <p className={styles.emptyHint}>{STRINGS.RECENT_EMPTY_HINT}</p>
      </section>
    )
  }

  return (
    <section className={styles.section}>
      <button
        className={styles.toggleBtn}
        onClick={() => setExpanded(prev => !prev)}
      >
        {expanded ? STRINGS.RECENT_HIDE : STRINGS.RECENT_SHOW}
        <span className={`${styles.chevron} ${expanded ? styles.chevronUp : ''}`}>
          &#8250;
        </span>
      </button>

      {expanded && (
        <div className={styles.list}>
          {summaries.map(item => (
            <button
              key={item.id}
              className={styles.card}
              onClick={() => onSelect(item)}
            >
              <span className={styles.cardTitle}>
                {item.title || 'Untitled'}
              </span>
              <span className={styles.cardPreview}>
                {item.summary.length > 120
                  ? item.summary.slice(0, 120) + '\u2026'
                  : item.summary}
              </span>
              <span className={styles.cardDate}>
                {new Date(item.createdAt).toLocaleDateString(undefined, {
                  month: 'short',
                  day: 'numeric',
                })}
              </span>
            </button>
          ))}

          {hasMore && (
            <button
              className={styles.loadMore}
              onClick={onLoadMore}
              disabled={loading}
            >
              {loading ? STRINGS.RECENT_LOADING : STRINGS.RECENT_LOAD_MORE}
            </button>
          )}
        </div>
      )}
    </section>
  )
}
