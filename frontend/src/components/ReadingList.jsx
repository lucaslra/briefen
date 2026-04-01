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

function ActionMenu({ url, isRead, onToggleRead, onDelete, onCopyMarkdown }) {
  const [open, setOpen] = useState(false)
  const [copyMdDone, setCopyMdDone] = useState(false)
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

  async function handleCopyMarkdown() {
    setOpen(false)
    await onCopyMarkdown()
    setCopyMdDone(true)
    setTimeout(() => setCopyMdDone(false), 2000)
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
          <button className={styles.menuItem} onClick={handleCopyMarkdown}>
            {copyMdDone ? STRINGS.READING_LIST_COPY_MD_DONE : STRINGS.READING_LIST_COPY_MD}
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

function ReadingListItem({ item, isExpanded, onToggleExpand, onToggleRead, onMarkRead, onDelete, error, onClearError, onUpdateNotes }) {
  const domain = extractDomain(item.url)
  const autoReadTimer = useRef(null)
  const itemRef = useRef(null)
  const [notesSaved, setNotesSaved] = useState(false)

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

  const handleCopyMarkdown = useCallback(async () => {
    const lines = [`## ${item.title || 'Untitled'}`]
    if (item.url) lines.push('', `*Source: ${item.url}*`)
    lines.push('', item.summary || '')
    try {
      await navigator.clipboard.writeText(lines.join('\n'))
    } catch {
      // Clipboard write failed
    }
  }, [item])

  const titleContent = item.title || item.summary?.substring(0, 60) || 'Untitled'
  const hasNotes = Boolean(item.notes && item.notes.trim())

  async function handleNotesBlur(e) {
    const value = e.target.value
    const ok = await onUpdateNotes(item.id, value)
    if (ok) {
      setNotesSaved(true)
      setTimeout(() => setNotesSaved(false), 2000)
    }
  }

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
          <div className={styles.itemTitleRow}>
            <button
              className={`${styles.itemTitle} ${styles.itemTitleLink} ${item.isRead ? styles.itemTitleRead : ''}`}
              onClick={onToggleExpand}
            >
              {titleContent}
            </button>
            {hasNotes && (
              <svg className={styles.notesIndicator} viewBox="0 0 16 16" width="12" height="12" aria-label="Has notes">
                <path d="M11.013 1.427a1.75 1.75 0 0 1 2.474 0l1.086 1.086a1.75 1.75 0 0 1 0 2.474l-8.61 8.61c-.21.21-.47.364-.756.445l-3.251.93a.75.75 0 0 1-.927-.928l.929-3.25c.081-.286.235-.547.445-.758l8.61-8.61zm1.414 1.06a.25.25 0 0 0-.354 0L10.811 3.75l1.439 1.44 1.263-1.263a.25.25 0 0 0 0-.354l-1.086-1.086zM11.189 6.25 9.75 4.81 3.23 11.33c-.03.03-.05.064-.063.101l-.652 2.281 2.278-.651a.2.2 0 0 0 .1-.063l6.496-6.748z" fill="currentColor"/>
              </svg>
            )}
          </div>
          {domain && <span className={styles.itemDomain}>{domain}</span>}
          {error && <span className={styles.itemError}>{error}</span>}
        </div>
        <span className={styles.itemDate}>{formatRelativeDate(item.savedAt || item.createdAt)}</span>
        <ActionMenu url={item.url} isRead={item.isRead} onToggleRead={onToggleRead} onDelete={onDelete} onCopyMarkdown={handleCopyMarkdown} />
      </div>

      {isExpanded && (
        <div className={styles.reader}>
          <div className={styles.readerContent}>
            <Markdown>{item.summary}</Markdown>
          </div>
          <div className={styles.notesSection}>
            <div className={styles.notesHeader}>
              <label className={styles.notesLabel}>{STRINGS.NOTES_LABEL}</label>
              {notesSaved && <span className={styles.notesSaved}>{STRINGS.NOTES_SAVED}</span>}
            </div>
            <textarea
              className={styles.notesTextarea}
              defaultValue={item.notes || ''}
              placeholder={STRINGS.NOTES_PLACEHOLDER}
              onBlur={handleNotesBlur}
              rows={3}
            />
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
    items, loading, filter, search, hasMore, itemErrors,
    changeFilter: rawChangeFilter, changeSearch, toggleReadStatus, deleteSummary,
    markAllAsRead, markAllAsUnread, loadMore, clearItemError, updateNotes,
  } = useReadingList(refreshUnreadCount, initialFilter)

  const changeFilter = useCallback((f) => {
    rawChangeFilter(f)
    setSearchParams(f === 'unread' ? {} : { filter: f }, { replace: true })
  }, [rawChangeFilter, setSearchParams])

  const [expandedId, setExpandedId] = useState(null)
  const [toast, setToast] = useState(null)
  const containerRef = useRef(null)
  const [searchInput, setSearchInput] = useState('')
  const searchTimer = useRef(null)

  const handleSearchInput = useCallback((e) => {
    const value = e.target.value
    setSearchInput(value)
    clearTimeout(searchTimer.current)
    searchTimer.current = setTimeout(() => {
      changeSearch(value.trim())
    }, 300)
  }, [changeSearch])

  const clearSearch = useCallback(() => {
    setSearchInput('')
    clearTimeout(searchTimer.current)
    changeSearch('')
  }, [changeSearch])

  useEffect(() => {
    return () => clearTimeout(searchTimer.current)
  }, [])

  const handleMarkAllAsRead = useCallback(async () => {
    const ok = await markAllAsRead()
    if (ok) setToast(STRINGS.READING_LIST_ALL_MARKED)
  }, [markAllAsRead])

  const handleMarkAllAsUnread = useCallback(async () => {
    const ok = await markAllAsUnread()
    if (ok) setToast(STRINGS.READING_LIST_ALL_MARKED_UNREAD)
  }, [markAllAsUnread])

  const dismissToast = useCallback(() => setToast(null), [])

  const showMarkAllRead = filter === 'unread' && items.length > 0 && items.some(i => !i.isRead)
  const showMarkAllUnread = filter === 'read' && items.length > 0 && items.some(i => i.isRead)

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

  // Collapse when filter or search changes (adjust state during render)
  const [prevFilter, setPrevFilter] = useState(filter)
  const [prevSearch, setPrevSearch] = useState(search)
  if (prevFilter !== filter || prevSearch !== search) {
    setPrevFilter(filter)
    setPrevSearch(search)
    setExpandedId(null)
  }

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
        {showMarkAllRead && (
          <button className={styles.markAllBtn} onClick={handleMarkAllAsRead}>
            {STRINGS.READING_LIST_MARK_ALL_READ}
          </button>
        )}
        {showMarkAllUnread && (
          <button className={styles.markAllBtn} onClick={handleMarkAllAsUnread}>
            {STRINGS.READING_LIST_MARK_ALL_UNREAD}
          </button>
        )}
        {items.length > 0 && (
          <a
            className={styles.exportBtn}
            href={`/api/summaries/export?format=md&filter=${filter}${search ? `&search=${encodeURIComponent(search)}` : ''}`}
            download="briefen-export.md"
          >
            {STRINGS.READING_LIST_EXPORT}
          </a>
        )}
      </div>

      <div className={styles.searchBar}>
        <input
          type="text"
          className={styles.searchInput}
          value={searchInput}
          onChange={handleSearchInput}
          placeholder={STRINGS.READING_LIST_SEARCH_PLACEHOLDER}
        />
        {searchInput && (
          <button
            className={styles.searchClear}
            onClick={clearSearch}
            aria-label={STRINGS.READING_LIST_SEARCH_CLEAR}
          >
            &times;
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
        <p className={styles.empty}>
          {search ? STRINGS.READING_LIST_SEARCH_EMPTY : EMPTY_MESSAGES[filter]}
        </p>
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
              onUpdateNotes={updateNotes}
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
