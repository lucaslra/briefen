import { useEffect, useRef } from 'react'
import { Routes, Route, useLocation } from 'react-router-dom'
import { STRINGS } from './constants/strings'
import { useTheme } from './hooks/useTheme'
import { useAuth } from './hooks/useAuth'
import { useSetup } from './hooks/useSetup'
import { useSummarize } from './hooks/useSummarize'
import { useBatchSummarize } from './hooks/useBatchSummarize'
import { useSummaries } from './hooks/useSummaries'
import { useElapsedTime } from './hooks/useElapsedTime'
import { useSettings } from './hooks/useSettings'
import { useNotification } from './hooks/useNotification'
import { useReadeck } from './hooks/useReadeck'
import { useUnreadCount } from './hooks/useUnreadCount'
import { Header } from './components/Header'
import { Login } from './components/Login'
import { Setup } from './components/Setup'
import { UrlInput } from './components/UrlInput'
import { LoadingSkeleton } from './components/LoadingSkeleton'
import { BatchProgress } from './components/BatchProgress'
import { SummaryDisplay } from './components/SummaryDisplay'
import { RecentSummaries } from './components/RecentSummaries'
import { Settings } from './components/Settings'
import { ReadingList } from './components/ReadingList'

function HomePage({ settings, refreshUnreadCount }) {
  const { notify } = useNotification()
  const readeck = useReadeck()
  const { summarize, summarizeText, data, setData, loading, error, clear, cancel } = useSummarize()
  const batch = useBatchSummarize()
  const { summaries, loading: loadingSummaries, hasMore, refresh, loadMore, search: recentSearch, setSearch: setRecentSearch } = useSummaries()
  const elapsed = useElapsedTime(loading)

  const defaultLengthHint = settings.defaultLength === 'default' ? null : settings.defaultLength
  const selectedModel = settings.model || null
  const readeckConfigured = !!(settings.readeckApiKey && settings.readeckUrl)
  const readeckKey = `${settings.readeckApiKey || ''}-${settings.readeckUrl || ''}`

  async function handleSubmitUrl(url) {
    const result = await summarize(url, defaultLengthHint, selectedModel)
    if (result) {
      refresh()
      refreshUnreadCount()
      if (settings.notificationsEnabled) notify(STRINGS.NOTIFICATION_DONE_TITLE, result.title || STRINGS.NOTIFICATION_DONE_BODY)
    }
  }

  async function handleSubmitBatch(urls) {
    await batch.summarizeBatch(urls, defaultLengthHint, selectedModel)
    refresh()
    refreshUnreadCount()
    if (settings.notificationsEnabled) notify(STRINGS.NOTIFICATION_DONE_TITLE, STRINGS.NOTIFICATION_DONE_BODY)
  }

  function handleBatchDismiss() {
    batch.clear()
  }

  async function handleSubmitText(text, title, sourceUrl = null) {
    const result = await summarizeText(text, title, defaultLengthHint, selectedModel, sourceUrl)
    if (result) {
      refresh()
      refreshUnreadCount()
      if (settings.notificationsEnabled) notify(STRINGS.NOTIFICATION_DONE_TITLE, title || STRINGS.NOTIFICATION_DONE_BODY)
    }
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

  const isBatchActive = batch.active

  return (
    <>
      <UrlInput
        onSubmitUrl={handleSubmitUrl}
        onSubmitBatch={handleSubmitBatch}
        onSubmitText={handleSubmitText}
        loading={loading || batch.isProcessing}
        error={error}
        readeck={readeck}
        readeckConfigured={readeckConfigured}
        readeckKey={readeckKey}
      />

      {isBatchActive && (
        <BatchProgress
          jobs={batch.jobs}
          isProcessing={batch.isProcessing}
          isComplete={batch.isComplete}
          doneCount={batch.doneCount}
          errorCount={batch.errorCount}
          onDismiss={handleBatchDismiss}
          onCancel={batch.clear}
        />
      )}

      {!isBatchActive && loading && <LoadingSkeleton elapsed={elapsed} onCancel={cancel} />}
      {!isBatchActive && !loading && data && (
        <SummaryDisplay
          data={data}
          loading={loading}
          elapsedMs={elapsed}
          onMakeShorter={handleMakeShorter}
          onMakeLonger={handleMakeLonger}
          onRegenerate={handleRegenerate}
          onClear={clear}
        />
      )}

      <RecentSummaries
        summaries={summaries}
        loading={loadingSummaries}
        hasMore={hasMore}
        onLoadMore={loadMore}
        onSelect={handleSelectRecent}
        search={recentSearch}
        onSearchChange={setRecentSearch}
      />
    </>
  )
}

export default function App() {
  const { theme, toggleTheme } = useTheme()
  const { setupRequired, loading: setupLoading, completeSetup } = useSetup()
  const { isAuthenticated, username, userId, role, login, logout } = useAuth()
  const { settings, updateSetting, updateSettings } = useSettings()
  const { unreadCount, refreshUnreadCount } = useUnreadCount()
  const mainRef = useRef(null)
  const location = useLocation()

  // Move focus to main content when route changes
  useEffect(() => {
    if (mainRef.current) {
      mainRef.current.focus({ preventScroll: true })
    }
  }, [location.pathname])

  // Show nothing while checking setup status
  if (setupLoading) return null

  // First-run setup guard — blocks all other views
  if (setupRequired) {
    return <Setup onComplete={completeSetup} />
  }

  if (!isAuthenticated) {
    return <Login onLogin={login} />
  }

  return (
    <>
      <Header
        theme={theme}
        onToggleTheme={toggleTheme}
        unreadCount={unreadCount}
        onLogout={logout}
        username={username}
      />

      <main ref={mainRef} tabIndex={-1} style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '0 20px', paddingBottom: '40px', outline: 'none' }}>
        <Routes>
          <Route path="/" element={<HomePage settings={settings} refreshUnreadCount={refreshUnreadCount} />} />
          <Route path="/reading-list" element={<ReadingList refreshUnreadCount={refreshUnreadCount} />} />
          <Route path="/settings" element={
            <Settings
              settings={settings}
              onUpdateSetting={updateSetting}
              onUpdateSettings={updateSettings}
              isAdmin={role === 'ADMIN'}
              currentUserId={userId}
            />
          } />
        </Routes>
      </main>

      <footer style={{ padding: '12px 20px 20px', textAlign: 'center' }}>
        <span style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'monospace', letterSpacing: '0.03em' }}>
          {__APP_COMMIT__} · {__BUILD_DATE__}
        </span>
      </footer>
    </>
  )
}
