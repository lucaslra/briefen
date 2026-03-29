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
      // Try to get full article content from Readeck
      const content = await getArticleContent(article.id)

      if (content?.text) {
        // Summarize the extracted text, passing the title and original URL for attribution
        onSummarize(content.text, article.title || content.title || null, article.url || null)
      } else if (article.url) {
        // Fallback: summarize by URL directly
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
      </form>

      {error && (
        <p className={styles.error}>{STRINGS.READECK_ERROR}</p>
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
