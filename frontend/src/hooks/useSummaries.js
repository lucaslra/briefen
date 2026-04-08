import { useState, useEffect, useCallback } from 'react'
import { apiFetch } from '../apiFetch.js'

export function useSummaries() {
  const [summaries, setSummaries] = useState([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)

  const fetchPage = useCallback(async (pageNum, replace = false) => {
    setLoading(true)
    try {
      const res = await apiFetch(`/api/summaries?page=${pageNum}&size=10`)
      if (!res.ok) return

      const data = await res.json()
      const items = data.content || []

      setSummaries(prev => replace ? items : [...prev, ...items])
      setHasMore(!data.last)
      setPage(pageNum)
    } catch {
      // Silently fail — recent summaries are non-critical
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchPage(0, true)
  }, [fetchPage])

  const refresh = useCallback(() => {
    fetchPage(0, true)
  }, [fetchPage])

  const loadMore = useCallback(() => {
    if (!loading && hasMore) {
      fetchPage(page + 1)
    }
  }, [loading, hasMore, page, fetchPage])

  return { summaries, loading, hasMore, refresh, loadMore }
}
