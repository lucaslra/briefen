import { useState, useCallback } from 'react'
import { apiFetch } from '../apiFetch.js'

export function useReadeck() {
  const [articles, setArticles] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [configured, setConfigured] = useState(null) // null = unknown, true/false
  const [hasMore, setHasMore] = useState(false)
  const [page, setPage] = useState(1)
  const [searchQuery, setSearchQuery] = useState('')

  const checkStatus = useCallback(async () => {
    try {
      const res = await apiFetch('/api/readeck/status')
      if (res.ok) {
        const data = await res.json()
        setConfigured(data.configured)
        return data.configured
      }
    } catch {
      // ignore
    }
    setConfigured(false)
    return false
  }, [])

  const fetchArticles = useCallback(async (search = '', pageNum = 1, append = false) => {
    setLoading(true)
    setError(null)
    setSearchQuery(search)
    setPage(pageNum)

    try {
      let url = `/api/readeck/bookmarks?page=${pageNum}&limit=20`
      if (search) url += `&search=${encodeURIComponent(search)}`

      const res = await apiFetch(url)
      if (!res.ok) {
        const text = await res.text()
        setError(text || 'Failed to fetch articles')
        return
      }

      const data = await res.json()
      const items = Array.isArray(data) ? data : (data.items || data.results || [])

      if (append) {
        setArticles(prev => [...prev, ...items])
        if (items.length === 0) {
          setHasMore(false)
          return
        }
      } else {
        setArticles(items)
      }
      setHasMore(items.length >= 20)
    } catch {
      setError('Could not connect to Readeck.')
    } finally {
      setLoading(false)
    }
  }, [])

  const loadMore = useCallback(() => {
    fetchArticles(searchQuery, page + 1, true)
  }, [fetchArticles, searchQuery, page])

  const getArticleContent = useCallback(async (id) => {
    try {
      const res = await apiFetch(`/api/readeck/bookmarks/${id}/article`)
      if (res.ok) {
        const data = await res.json()
        if (data.error) {
          return { error: data.error }
        }
        return data
      }
      const text = await res.text()
      return { error: text || 'Failed to fetch article content' }
    } catch {
      return { error: 'Could not connect to Readeck.' }
    }
  }, [])

  return {
    articles,
    loading,
    error,
    configured,
    hasMore,
    checkStatus,
    fetchArticles,
    loadMore,
    getArticleContent,
  }
}
