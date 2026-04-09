import { useState, useEffect, useCallback } from 'react'

export function useSetup() {
  const [setupRequired, setSetupRequired] = useState(null) // null = loading
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    async function checkSetup() {
      try {
        const res = await fetch('/api/setup/status')
        if (!cancelled && res.ok) {
          const data = await res.json()
          setSetupRequired(data.setupRequired)
        }
      } catch {
        // Network error — assume setup not required (will fail at login)
        if (!cancelled) setSetupRequired(false)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    checkSetup()
    return () => { cancelled = true }
  }, [])

  const completeSetup = useCallback(async (username, password) => {
    const res = await fetch('/api/setup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    if (res.ok) {
      setSetupRequired(false)
      return { ok: true }
    }
    const data = await res.json().catch(() => ({}))
    return { ok: false, status: res.status, error: data.error || '' }
  }, [])

  return { setupRequired, loading, completeSetup }
}
