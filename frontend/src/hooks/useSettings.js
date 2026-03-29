import { useState, useEffect, useCallback } from 'react'

const STORAGE_KEY = 'briefen-settings'

const DEFAULTS = {
  defaultLength: 'default', // 'shorter' | 'default' | 'longer'
  model: null,              // null means server default
  notificationsEnabled: true,
  openaiApiKey: null,
  readeckApiKey: null,
  readeckUrl: null,
}

/**
 * Loads settings from localStorage as an immediate cache,
 * then fetches the authoritative copy from the API.
 */
function loadFromCache() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) return { ...DEFAULTS, ...JSON.parse(stored) }
  } catch {
    // Corrupted — ignore
  }
  return { ...DEFAULTS }
}

function writeCache(settings) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
  } catch {
    // Storage unavailable — ignore
  }
}

export function useSettings() {
  const [settings, setSettings] = useState(loadFromCache)

  // On mount, fetch the authoritative settings from the API
  useEffect(() => {
    let cancelled = false
    fetch('/api/settings')
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (!cancelled && data) {
          const merged = { ...DEFAULTS, ...data }
          setSettings(merged)
          writeCache(merged)
        }
      })
      .catch(() => {
        // API unreachable — keep using cached settings
      })
    return () => { cancelled = true }
  }, [])

  const updateSetting = useCallback((key, value) => {
    // Optimistic update
    setSettings(prev => {
      const next = { ...prev, [key]: value }
      writeCache(next)
      return next
    })

    // Only send the changed field to avoid overwriting masked keys
    // with their masked representations (e.g., "sk-...abc1").
    return fetch('/api/settings', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ [key]: value }),
    }).then(res => {
      if (!res.ok) return res.text().then(t => { throw new Error(t || 'Save failed') })
    }).catch(err => {
      // Revert optimistic update by re-fetching authoritative settings
      fetch('/api/settings')
        .then(r => r.ok ? r.json() : null)
        .then(data => {
          if (data) {
            const merged = { ...DEFAULTS, ...data }
            setSettings(merged)
            writeCache(merged)
          }
        })
        .catch(() => {})
      throw err
    })
  }, [])

  const updateSettings = useCallback((updates) => {
    // Optimistic update — apply all fields at once
    setSettings(prev => {
      const next = { ...prev, ...updates }
      writeCache(next)
      return next
    })

    return fetch('/api/settings', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updates),
    }).then(res => {
      if (!res.ok) return res.text().then(t => { throw new Error(t || 'Save failed') })
    }).catch(err => {
      fetch('/api/settings')
        .then(r => r.ok ? r.json() : null)
        .then(data => {
          if (data) {
            const merged = { ...DEFAULTS, ...data }
            setSettings(merged)
            writeCache(merged)
          }
        })
        .catch(() => {})
      throw err
    })
  }, [])

  return { settings, updateSetting, updateSettings }
}
