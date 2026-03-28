import { useTheme } from './hooks/useTheme'
import { useSummarize } from './hooks/useSummarize'
import { useSummaries } from './hooks/useSummaries'
import { Header } from './components/Header'
import { UrlInput } from './components/UrlInput'
import { LoadingSkeleton } from './components/LoadingSkeleton'
import { SummaryDisplay } from './components/SummaryDisplay'
import { RecentSummaries } from './components/RecentSummaries'

export default function App() {
  const { theme, toggleTheme } = useTheme()
  const { summarize, data, setData, loading, error } = useSummarize()
  const { summaries, loading: loadingSummaries, hasMore, refresh, loadMore } = useSummaries()

  async function handleSubmit(url) {
    const result = await summarize(url)
    if (result) refresh()
  }

  function handleSelectRecent(item) {
    setData(item)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <>
      <Header theme={theme} onToggleTheme={toggleTheme} />

      <main style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '0 20px', paddingBottom: '60px' }}>
        <UrlInput onSubmit={handleSubmit} loading={loading} />

        {error && (
          <div style={{
            width: '100%',
            maxWidth: 720,
            marginTop: 16,
            padding: '12px 16px',
            borderRadius: 8,
            backgroundColor: 'var(--error-bg)',
            color: 'var(--error)',
            fontSize: '0.9rem',
            border: '1px solid var(--error)',
          }}>
            {error}
          </div>
        )}

        {loading && <LoadingSkeleton />}
        {!loading && data && (
          <SummaryDisplay
            data={data}
            loading={loading}
            onMakeShorter={() => summarize(data.url, 'shorter')}
            onMakeLonger={() => summarize(data.url, 'longer')}
          />
        )}

        <RecentSummaries
          summaries={summaries}
          loading={loadingSummaries}
          hasMore={hasMore}
          onLoadMore={loadMore}
          onSelect={handleSelectRecent}
        />
      </main>
    </>
  )
}
