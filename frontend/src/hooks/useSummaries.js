import { useState, useEffect, useCallback } from 'react'
import { apiFetch } from '../apiFetch.js'

export function useSummaries() {
  const [summaries, setSummaries] = useState([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [search, setSearch] = useState('')

  const fetchPage = useCallback(async (pageNum, currentSearch, replace = false) => {
    setLoading(true)
    try {
      const params = new URLSearchParams({ page: pageNum, size: 10 })
      if (currentSearch) params.set('search', currentSearch)

      const res = await apiFetch(`/api/summaries?${params}`)
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
    fetchPage(0, search, true)
  }, [fetchPage, search])

  const refresh = useCallback(() => {
    fetchPage(0, search, true)
  }, [fetchPage, search])

  const loadMore = useCallback(() => {
    if (!loading && hasMore) {
      fetchPage(page + 1, search)
    }
  }, [loading, hasMore, page, search, fetchPage])

  return { summaries, loading, hasMore, refresh, loadMore, search, setSearch }
}
