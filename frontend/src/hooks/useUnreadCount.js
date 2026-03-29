import { useState, useEffect, useCallback } from 'react'

export function useUnreadCount() {
  const [unreadCount, setUnreadCount] = useState(0)

  const refreshUnreadCount = useCallback(async () => {
    try {
      const res = await fetch('/api/summaries/unread-count')
      if (res.ok) {
        const data = await res.json()
        setUnreadCount(data.count)
      }
    } catch {
      // Non-critical — badge will show stale count
    }
  }, [])

  useEffect(() => {
    refreshUnreadCount()
  }, [refreshUnreadCount])

  return { unreadCount, refreshUnreadCount }
}
