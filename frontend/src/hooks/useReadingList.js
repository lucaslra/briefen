import { useState, useEffect, useCallback } from 'react'
import { STRINGS } from '../constants/strings'

const PAGE_SIZE = 20

export function useReadingList(refreshUnreadCount) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [filter, setFilter] = useState('unread')
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [itemErrors, setItemErrors] = useState({})

  const fetchPage = useCallback(async (pageNum, currentFilter, replace = false) => {
    setLoading(true)
    try {
      const res = await fetch(`/api/summaries?page=${pageNum}&size=${PAGE_SIZE}&filter=${currentFilter}`)
      if (!res.ok) return

      const data = await res.json()
      const fetched = data.content || []

      setItems(prev => replace ? fetched : [...prev, ...fetched])
      setHasMore(!data.last)
      setPage(pageNum)
    } catch {
      // silent
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchPage(0, filter, true)
  }, [filter, fetchPage])

  const changeFilter = useCallback((f) => {
    setFilter(f)
    setPage(0)
    setItemErrors({})
  }, [])

  const loadMore = useCallback(() => {
    if (!loading && hasMore) {
      fetchPage(page + 1, filter)
    }
  }, [loading, hasMore, page, filter, fetchPage])

  const refresh = useCallback(() => {
    fetchPage(0, filter, true)
    refreshUnreadCount?.()
  }, [filter, fetchPage, refreshUnreadCount])

  const clearItemError = useCallback((id) => {
    setItemErrors(prev => {
      const next = { ...prev }
      delete next[id]
      return next
    })
  }, [])

  const toggleReadStatus = useCallback(async (id, currentIsRead) => {
    const newIsRead = !currentIsRead

    // Optimistic update
    setItems(prev => prev.map(item =>
      item.id === id ? { ...item, isRead: newIsRead } : item
    ))
    setItemErrors(prev => { const next = { ...prev }; delete next[id]; return next })

    try {
      const res = await fetch(`/api/summaries/${id}/read-status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isRead: newIsRead }),
      })
      if (!res.ok) throw new Error()
      refreshUnreadCount?.()
    } catch {
      // Revert
      setItems(prev => prev.map(item =>
        item.id === id ? { ...item, isRead: currentIsRead } : item
      ))
      setItemErrors(prev => ({ ...prev, [id]: STRINGS.READING_LIST_UPDATE_FAILED }))
    }
  }, [refreshUnreadCount])

  const deleteSummary = useCallback(async (id) => {
    // Save for revert
    let removedItem = null
    let removedIndex = -1

    setItems(prev => {
      const idx = prev.findIndex(item => item.id === id)
      if (idx !== -1) {
        removedItem = prev[idx]
        removedIndex = idx
      }
      return prev.filter(item => item.id !== id)
    })
    setItemErrors(prev => { const next = { ...prev }; delete next[id]; return next })

    try {
      const res = await fetch(`/api/summaries/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error()
      refreshUnreadCount?.()
    } catch {
      // Revert
      if (removedItem) {
        setItems(prev => {
          const next = [...prev]
          next.splice(removedIndex, 0, removedItem)
          return next
        })
        setItemErrors(prev => ({ ...prev, [id]: STRINGS.READING_LIST_DELETE_FAILED }))
      }
    }
  }, [refreshUnreadCount])

  const markAllAsRead = useCallback(async () => {
    const previousItems = items

    // Optimistic
    setItems(prev => prev.map(item => ({ ...item, isRead: true })))

    try {
      const res = await fetch('/api/summaries/read-status/bulk', { method: 'PATCH' })
      if (!res.ok) throw new Error()
      refreshUnreadCount?.()
      return true
    } catch {
      setItems(previousItems)
      return false
    }
  }, [items, refreshUnreadCount])

  return {
    items, loading, filter, hasMore, itemErrors,
    changeFilter, toggleReadStatus, deleteSummary,
    markAllAsRead, loadMore, refresh, clearItemError,
  }
}
