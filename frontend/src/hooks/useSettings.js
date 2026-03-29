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
    setSettings(prev => {
      const next = { ...prev, [key]: value }
      writeCache(next)

      // Fire-and-forget save to API
      fetch('/api/settings', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(next),
      }).catch(() => {
        // API save failed — cached value is still fine
      })

      return next
    })
  }, [])

  return { settings, updateSetting }
}
