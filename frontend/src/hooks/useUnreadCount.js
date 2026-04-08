import { useState, useEffect, useCallback } from 'react'
import { apiFetch } from '../apiFetch.js'

export function useUnreadCount() {
  const [unreadCount, setUnreadCount] = useState(0)

  const refreshUnreadCount = useCallback(async () => {
    try {
      const res = await apiFetch('/api/summaries/unread-count')
      if (res.ok) {
        const data = await res.json()
        setUnreadCount(data.count)
      }
    } catch {
      // Non-critical — badge will show stale count
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    async function fetchInitial() {
      try {
        const res = await apiFetch('/api/summaries/unread-count')
        if (res.ok && !cancelled) {
          const data = await res.json()
          setUnreadCount(data.count)
        }
      } catch {
        // Non-critical — badge will show stale count
      }
    }
    fetchInitial()
    return () => { cancelled = true }
  }, [])

  return { unreadCount, refreshUnreadCount }
}
