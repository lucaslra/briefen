import { useState } from 'react'
import { STRINGS } from './constants/strings'
import { useTheme } from './hooks/useTheme'
import { useSummarize } from './hooks/useSummarize'
import { useSummaries } from './hooks/useSummaries'
import { useElapsedTime } from './hooks/useElapsedTime'
import { useSettings } from './hooks/useSettings'
import { useNotification } from './hooks/useNotification'
import { useReadeck } from './hooks/useReadeck'
import { Header } from './components/Header'
import { UrlInput } from './components/UrlInput'
import { LoadingSkeleton } from './components/LoadingSkeleton'
import { SummaryDisplay } from './components/SummaryDisplay'
import { RecentSummaries } from './components/RecentSummaries'
import { Settings } from './components/Settings'

export default function App() {
  const [page, setPage] = useState('home')
  const { theme, toggleTheme } = useTheme()
  const { settings, updateSetting } = useSettings()
  const { notify } = useNotification()
  const readeck = useReadeck()
  const { summarize, summarizeText, data, setData, loading, error } = useSummarize()
  const { summaries, loading: loadingSummaries, hasMore, refresh, loadMore } = useSummaries()
  const elapsed = useElapsedTime(loading)

  // Use the configured default length, converting 'default' to null (backend treats null as default)
  const defaultLengthHint = settings.defaultLength === 'default' ? null : settings.defaultLength
  const selectedModel = settings.model || null
  const readeckConfigured = !!(settings.readeckApiKey && settings.readeckUrl)
  // Key changes when readeck config changes, forcing ReadeckBrowser to remount and recheck status
  const readeckKey = `${settings.readeckApiKey || ''}-${settings.readeckUrl || ''}`

  async function handleSubmitUrl(url) {
    const result = await summarize(url, defaultLengthHint, selectedModel)
    if (result) {
      refresh()
      if (settings.notificationsEnabled) notify(STRINGS.NOTIFICATION_DONE_TITLE, result.title || STRINGS.NOTIFICATION_DONE_BODY)
    }
  }

  async function handleSubmitText(text, title, sourceUrl = null) {
    const result = await summarizeText(text, title, defaultLengthHint, selectedModel, sourceUrl)
    if (result && settings.notificationsEnabled) notify(STRINGS.NOTIFICATION_DONE_TITLE, title || STRINGS.NOTIFICATION_DONE_BODY)
  }

  function handleSelectRecent(item) {
    setData(item)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function handleMakeShorter() {
    if (data?.url) {
      summarize(data.url, 'shorter', selectedModel)
    }
  }

  function handleMakeLonger() {
    if (data?.url) {
      summarize(data.url, 'longer', selectedModel)
    }
  }

  async function handleRegenerate() {
    if (data?.url) {
      const result = await summarize(data.url, defaultLengthHint, selectedModel, true)
      if (result) refresh()
    }
  }

  return (
    <>
      <Header
        theme={theme}
        onToggleTheme={toggleTheme}
        onNavigate={setPage}
        currentPage={page}
      />

      <main style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '0 20px', paddingBottom: '60px' }}>
        {page === 'settings' ? (
          <Settings
            settings={settings}
            onUpdateSetting={updateSetting}
            onBack={() => setPage('home')}
          />
        ) : (
          <>
            <UrlInput
              onSubmitUrl={handleSubmitUrl}
              onSubmitText={handleSubmitText}
              loading={loading}
              readeck={readeck}
              readeckConfigured={readeckConfigured}
              readeckKey={readeckKey}
            />

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

            {loading && <LoadingSkeleton elapsed={elapsed} />}
            {!loading && data && (
              <SummaryDisplay
                data={data}
                loading={loading}
                elapsedMs={elapsed}
                onMakeShorter={handleMakeShorter}
                onMakeLonger={handleMakeLonger}
                onRegenerate={handleRegenerate}
              />
            )}

            <RecentSummaries
              summaries={summaries}
              loading={loadingSummaries}
              hasMore={hasMore}
              onLoadMore={loadMore}
              onSelect={handleSelectRecent}
            />
          </>
        )}
      </main>
    </>
  )
}
