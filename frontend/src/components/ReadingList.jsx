import { useState, useEffect, useCallback, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import Markdown from 'react-markdown'
import { STRINGS } from '../constants/strings'
import { useReadingList } from '../hooks/useReadingList'
import { formatRelativeDate } from '../utils/relativeDate'
import styles from './ReadingList.module.css'

function extractDomain(url) {
  if (!url) return null
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return null
  }
}

function Toast({ message, onDone }) {
  useEffect(() => {
    const timer = setTimeout(onDone, 2500)
    return () => clearTimeout(timer)
  }, [onDone])

  return <div className={styles.toast}>{message}</div>
}

function ActionMenu({ url, isRead, onToggleRead, onDelete }) {
  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    if (!open) return
    function handleClick(e) {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [open])

  function handleToggleRead() {
    setOpen(false)
    onToggleRead()
  }

  function handleDelete() {
    setOpen(false)
    if (window.confirm(STRINGS.READING_LIST_DELETE_CONFIRM)) {
      onDelete()
    }
  }

  return (
    <div className={styles.menuWrapper} ref={ref}>
      <button
        className={styles.menuBtn}
        onClick={() => setOpen(prev => !prev)}
        aria-label="Actions"
      >
        ···
      </button>
      {open && (
        <div className={styles.menu}>
          <button className={styles.menuItem} onClick={handleToggleRead}>
            {isRead ? STRINGS.READING_LIST_MARK_UNREAD : STRINGS.READING_LIST_MARK_READ}
          </button>
          {url && (
            <a
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.menuItem}
              onClick={() => setOpen(false)}
            >
              {STRINGS.READING_LIST_OPEN_ARTICLE}
            </a>
          )}
          <button className={`${styles.menuItem} ${styles.menuItemDanger}`} onClick={handleDelete}>
            {STRINGS.READING_LIST_DELETE}
          </button>
        </div>
      )}
    </div>
  )
}

const AUTO_READ_DELAY_MS = 3000

function ReadingListItem({ item, isExpanded, onToggleExpand, onToggleRead, onMarkRead, onDelete, error, onClearError }) {
  const domain = extractDomain(item.url)
  const autoReadTimer = useRef(null)
  const itemRef = useRef(null)

  useEffect(() => {
    return () => { if (autoReadTimer.current) clearTimeout(autoReadTimer.current) }
  }, [])

  // Auto-mark as read when expanded
  useEffect(() => {
    if (isExpanded && !item.isRead) {
      autoReadTimer.current = setTimeout(() => {
        onMarkRead()
      }, AUTO_READ_DELAY_MS)
    }
    return () => { if (autoReadTimer.current) clearTimeout(autoReadTimer.current) }
  }, [isExpanded, item.isRead, onMarkRead])

  // Scroll into view when expanded
  useEffect(() => {
    if (isExpanded && itemRef.current) {
      itemRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  }, [isExpanded])

  useEffect(() => {
    if (!error) return
    const timer = setTimeout(() => onClearError(item.id), 3000)
    return () => clearTimeout(timer)
  }, [error, item.id, onClearError])

  const titleContent = item.title || item.summary?.substring(0, 60) || 'Untitled'

  return (
    <div
      ref={itemRef}
      className={`${styles.item} ${isExpanded ? styles.itemExpanded : ''}`}
    >
      <div className={styles.itemRow}>
        <button
          className={`${styles.dot} ${item.isRead ? styles.dotRead : styles.dotUnread}`}
          onClick={onToggleRead}
          aria-label={item.isRead ? 'Mark as unread' : 'Mark as read'}
          title={item.isRead ? 'Mark as unread' : 'Mark as read'}
        />
        <div className={styles.itemContent}>
          <button
            className={`${styles.itemTitle} ${styles.itemTitleLink} ${item.isRead ? styles.itemTitleRead : ''}`}
            onClick={onToggleExpand}
          >
            {titleContent}
          </button>
          {domain && <span className={styles.itemDomain}>{domain}</span>}
          {error && <span className={styles.itemError}>{error}</span>}
        </div>
        <span className={styles.itemDate}>{formatRelativeDate(item.savedAt || item.createdAt)}</span>
        <ActionMenu url={item.url} isRead={item.isRead} onToggleRead={onToggleRead} onDelete={onDelete} />
      </div>

      {isExpanded && (
        <div className={styles.reader}>
          <div className={styles.readerContent}>
            <Markdown>{item.summary}</Markdown>
          </div>
          <div className={styles.readerFooter}>
            {item.url && (
              <a href={item.url} target="_blank" rel="noopener noreferrer" className={styles.readerSource}>
                {domain || item.url}
              </a>
            )}
            {item.modelUsed && (
              <span className={styles.readerModel}>
                {STRINGS.GENERATED_WITH} {item.modelUsed}
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

function Skeleton() {
  return (
    <div className={styles.skeleton}>
      {[...Array(5)].map((_, i) => (
        <div key={i} className={styles.skeletonRow}>
          <div className={styles.skeletonDot} />
          <div className={styles.skeletonLines}>
            <div className={styles.skeletonLine} style={{ width: `${60 + (i % 3) * 15}%` }} />
            <div className={styles.skeletonLineShort} style={{ width: `${25 + (i % 2) * 10}%` }} />
          </div>
          <div className={styles.skeletonDate} />
        </div>
      ))}
    </div>
  )
}

const FILTERS = ['all', 'unread', 'read']
const FILTER_LABELS = {
  all: STRINGS.READING_LIST_FILTER_ALL,
  unread: STRINGS.READING_LIST_FILTER_UNREAD,
  read: STRINGS.READING_LIST_FILTER_READ,
}
const EMPTY_MESSAGES = {
  all: STRINGS.READING_LIST_EMPTY_ALL,
  unread: STRINGS.READING_LIST_EMPTY_UNREAD,
  read: STRINGS.READING_LIST_EMPTY_READ,
}

export function ReadingList({ refreshUnreadCount }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialFilter = FILTERS.includes(searchParams.get('filter')) ? searchParams.get('filter') : 'unread'

  const {
    items, loading, filter, hasMore, itemErrors,
    changeFilter: rawChangeFilter, toggleReadStatus, deleteSummary,
    markAllAsRead, loadMore, clearItemError,
  } = useReadingList(refreshUnreadCount, initialFilter)

  const changeFilter = useCallback((f) => {
    rawChangeFilter(f)
    setSearchParams(f === 'unread' ? {} : { filter: f }, { replace: true })
  }, [rawChangeFilter, setSearchParams])

  const [expandedId, setExpandedId] = useState(null)
  const [toast, setToast] = useState(null)
  const containerRef = useRef(null)

  const handleMarkAllAsRead = useCallback(async () => {
    const ok = await markAllAsRead()
    if (ok) setToast(STRINGS.READING_LIST_ALL_MARKED)
  }, [markAllAsRead])

  const dismissToast = useCallback(() => setToast(null), [])

  const showMarkAll = filter === 'unread' && items.length > 0 && items.some(i => !i.isRead)

  const toggleExpand = useCallback((id) => {
    setExpandedId(prev => prev === id ? null : id)
  }, [])

  // Arrow key navigation
  useEffect(() => {
    function handleKeyDown(e) {
      // Only handle arrows when a summary is expanded and no input is focused
      const tag = document.activeElement?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return

      if (e.key === 'Escape') {
        if (expandedId) {
          e.preventDefault()
          setExpandedId(null)
        }
        return
      }

      if (e.key === 'ArrowDown' || e.key === 'ArrowUp' || e.key === 'j' || e.key === 'k') {
        if (items.length === 0) return
        e.preventDefault()

        const direction = (e.key === 'ArrowDown' || e.key === 'j') ? 1 : -1
        const currentIndex = expandedId ? items.findIndex(i => i.id === expandedId) : -1

        let nextIndex
        if (currentIndex === -1) {
          // Nothing expanded — open the first (down) or last (up) item
          nextIndex = direction === 1 ? 0 : items.length - 1
        } else {
          nextIndex = currentIndex + direction
        }

        if (nextIndex >= 0 && nextIndex < items.length) {
          setExpandedId(items[nextIndex].id)
        }
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [expandedId, items])

  // Collapse when filter changes
  useEffect(() => {
    setExpandedId(null)
  }, [filter])

  return (
    <section className={styles.container} ref={containerRef}>
      <div className={styles.header}>
        <div className={styles.filters}>
          {FILTERS.map(f => (
            <button
              key={f}
              className={`${styles.filterTab} ${filter === f ? styles.filterTabActive : ''}`}
              onClick={() => changeFilter(f)}
            >
              {FILTER_LABELS[f]}
            </button>
          ))}
        </div>
        {showMarkAll && (
          <button className={styles.markAllBtn} onClick={handleMarkAllAsRead}>
            {STRINGS.READING_LIST_MARK_ALL_READ}
          </button>
        )}
      </div>

      {expandedId && (
        <div className={styles.navHint}>
          <kbd>↑</kbd> <kbd>↓</kbd> navigate &middot; <kbd>Esc</kbd> close
        </div>
      )}

      {loading && items.length === 0 && <Skeleton />}

      {!loading && items.length === 0 && (
        <p className={styles.empty}>{EMPTY_MESSAGES[filter]}</p>
      )}

      {items.length > 0 && (
        <div className={styles.list}>
          {items.map(item => (
            <ReadingListItem
              key={item.id}
              item={item}
              isExpanded={expandedId === item.id}
              onToggleExpand={() => toggleExpand(item.id)}
              onToggleRead={() => toggleReadStatus(item.id, item.isRead)}
              onMarkRead={() => { if (!item.isRead) toggleReadStatus(item.id, false) }}
              onDelete={() => deleteSummary(item.id)}
              error={itemErrors[item.id]}
              onClearError={clearItemError}
            />
          ))}
        </div>
      )}

      {hasMore && !loading && (
        <button className={styles.loadMore} onClick={loadMore}>
          {STRINGS.READING_LIST_LOAD_MORE}
        </button>
      )}

      {toast && <Toast message={toast} onDone={dismissToast} />}
    </section>
  )
}
