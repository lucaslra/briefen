import { useState, useEffect, useCallback, useRef } from 'react'
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

function ActionMenu({ url, onDelete }) {
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

function ReadingListItem({ item, onToggleRead, onDelete, error, onClearError }) {
  const domain = extractDomain(item.url)

  useEffect(() => {
    if (!error) return
    const timer = setTimeout(() => onClearError(item.id), 3000)
    return () => clearTimeout(timer)
  }, [error, item.id, onClearError])

  return (
    <div className={styles.item}>
      <button
        className={`${styles.dot} ${item.isRead ? styles.dotRead : styles.dotUnread}`}
        onClick={onToggleRead}
        aria-label={item.isRead ? 'Mark as unread' : 'Mark as read'}
        title={item.isRead ? 'Mark as unread' : 'Mark as read'}
      />
      <div className={styles.itemContent}>
        <span className={`${styles.itemTitle} ${item.isRead ? styles.itemTitleRead : ''}`}>
          {item.title || item.summary?.substring(0, 60) || 'Untitled'}
        </span>
        {domain && <span className={styles.itemDomain}>{domain}</span>}
        {error && <span className={styles.itemError}>{error}</span>}
      </div>
      <span className={styles.itemDate}>{formatRelativeDate(item.savedAt || item.createdAt)}</span>
      <ActionMenu url={item.url} onDelete={onDelete} />
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
  const {
    items, loading, filter, hasMore, itemErrors,
    changeFilter, toggleReadStatus, deleteSummary,
    markAllAsRead, loadMore, clearItemError,
  } = useReadingList(refreshUnreadCount)

  const [toast, setToast] = useState(null)

  const handleMarkAllAsRead = useCallback(async () => {
    const ok = await markAllAsRead()
    if (ok) setToast(STRINGS.READING_LIST_ALL_MARKED)
  }, [markAllAsRead])

  const dismissToast = useCallback(() => setToast(null), [])

  const showMarkAll = filter === 'unread' && items.length > 0 && items.some(i => !i.isRead)

  return (
    <section className={styles.container}>
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
              error={itemErrors[item.id]}
              onToggleRead={() => toggleReadStatus(item.id, item.isRead)}
              onDelete={() => deleteSummary(item.id)}
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
