import { useState, useRef, useEffect } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './RecentSummaries.module.css'

export function RecentSummaries({ summaries, loading, hasMore, onLoadMore, onSelect, search, onSearchChange }) {
  const [expanded, setExpanded] = useState(false)
  const [searchInput, setSearchInput] = useState('')
  const searchTimer = useRef(null)

  function handleSearchInput(e) {
    const value = e.target.value
    setSearchInput(value)
    clearTimeout(searchTimer.current)
    searchTimer.current = setTimeout(() => {
      onSearchChange(value.trim())
    }, 300)
  }

  function clearSearch() {
    setSearchInput('')
    clearTimeout(searchTimer.current)
    onSearchChange('')
  }

  useEffect(() => {
    return () => clearTimeout(searchTimer.current)
  }, [])

  if (summaries.length === 0 && !loading && !search) {
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
          <div className={styles.searchBar}>
            <input
              type="text"
              className={styles.searchInput}
              value={searchInput}
              onChange={handleSearchInput}
              placeholder={STRINGS.RECENT_SEARCH_PLACEHOLDER}
            />
            {searchInput && (
              <button
                className={styles.searchClear}
                onClick={clearSearch}
                aria-label={STRINGS.RECENT_SEARCH_CLEAR}
              >
                &times;
              </button>
            )}
          </div>

          {summaries.length === 0 && !loading && search ? (
            <p className={styles.emptyHint}>{STRINGS.RECENT_SEARCH_EMPTY}</p>
          ) : (
            summaries.map(item => (
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
            ))
          )}

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
