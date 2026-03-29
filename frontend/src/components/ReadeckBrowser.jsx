import { useState, useEffect, useCallback } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './ReadeckBrowser.module.css'

export function ReadeckBrowser({ readeck, onSummarize, loading }) {
  const {
    articles, loading: loadingArticles, error, configured,
    hasMore, checkStatus, fetchArticles, loadMore, getArticleContent,
  } = readeck

  const [search, setSearch] = useState('')
  const [fetchingId, setFetchingId] = useState(null)

  // Check status and load articles every time the tab is shown
  useEffect(() => {
    checkStatus().then(isConfigured => {
      if (isConfigured) fetchArticles()
    })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = useCallback((e) => {
    e.preventDefault()
    fetchArticles(search.trim())
  }, [search, fetchArticles])

  async function handleSelectArticle(article) {
    if (loading || fetchingId) return
    setFetchingId(article.id)

    try {
      const content = await getArticleContent(article.id)

      if (content?.error) {
        // Content fetch failed — offer URL fallback if available
        if (article.url && window.confirm(
          `Could not fetch article content: ${content.error}\n\nSummarize from URL instead?`
        )) {
          onSummarize(null, null, article.url)
        }
      } else if (content?.text) {
        onSummarize(content.text, article.title || content.title || null, article.url || null)
      } else if (article.url) {
        // No text returned — fall back to URL
        onSummarize(null, null, article.url)
      }
    } finally {
      setFetchingId(null)
    }
  }

  if (configured === false) {
    return (
      <div className={styles.message}>
        {STRINGS.READECK_NOT_CONFIGURED}
      </div>
    )
  }

  return (
    <div className={styles.container}>
      <form className={styles.searchForm} onSubmit={handleSearch}>
        <input
          type="text"
          className={styles.searchInput}
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder={STRINGS.READECK_SEARCH_PLACEHOLDER}
          disabled={loading || loadingArticles}
        />
        <button
          type="submit"
          className={styles.searchBtn}
          disabled={loading || loadingArticles}
        >
          {STRINGS.READECK_SEARCH_BUTTON}
        </button>
      </form>

      {error && (
        <p className={styles.error}>
          {typeof error === 'string' && error.includes('401')
            ? STRINGS.READECK_AUTH_ERROR
            : typeof error === 'string' && error.includes('429')
            ? STRINGS.READECK_RATE_LIMITED
            : STRINGS.READECK_ERROR}
        </p>
      )}

      {loadingArticles && articles.length === 0 && (
        <p className={styles.message}>{STRINGS.READECK_LOADING}</p>
      )}

      {!loadingArticles && articles.length === 0 && !error && (
        <p className={styles.message}>{STRINGS.READECK_EMPTY}</p>
      )}

      <div className={styles.articleList}>
        {articles.map(article => (
          <button
            key={article.id}
            className={`${styles.articleItem} ${fetchingId === article.id ? styles.articleFetching : ''}`}
            onClick={() => handleSelectArticle(article)}
            disabled={loading || fetchingId != null}
          >
            <span className={styles.articleTitle}>
              {fetchingId === article.id ? STRINGS.READECK_FETCHING + ' ' : ''}
              {article.title || article.url}
            </span>
            <span className={styles.articleMeta}>
              {article.site_name || article.domain || ''}
              {article.word_count ? ` · ${article.word_count} words` : ''}
            </span>
          </button>
        ))}
      </div>

      {hasMore && (
        <button
          className={styles.loadMoreBtn}
          onClick={loadMore}
          disabled={loadingArticles}
        >
          {STRINGS.READECK_LOAD_MORE}
        </button>
      )}
    </div>
  )
}
