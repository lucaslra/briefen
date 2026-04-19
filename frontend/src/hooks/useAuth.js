import { useState, useEffect, useCallback } from 'react'
import { setAuthHeader, clearCredentials, apiFetch } from '../apiFetch.js'

const SESSION_KEY = 'briefen_auth'

function loadFromSession() {
  try {
    const stored = sessionStorage.getItem(SESSION_KEY)
    if (stored) return JSON.parse(stored)
  } catch {
    // Corrupted — ignore
  }
  return null
}

export function useAuth() {
  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    const stored = loadFromSession()
    return !!(stored?.username && stored?.authHeader)
  })
  const [username, setUsername] = useState(() => loadFromSession()?.username ?? null)
  const [userId, setUserId] = useState(() => loadFromSession()?.userId ?? null)
  const [role, setRole] = useState(() => loadFromSession()?.role ?? null)

  useEffect(() => {
    // Restore auth header into the apiFetch singleton on mount (safety net for HMR / edge cases;
    // apiFetch already initializes itself from sessionStorage at module load time).
    const stored = loadFromSession()
    if (stored?.authHeader) {
      setAuthHeader(stored.authHeader)
    }

    // Force logout when any request receives a 401
    function handleUnauthorized() {
      clearCredentials()
      sessionStorage.removeItem(SESSION_KEY)
      setIsAuthenticated(false)
      setUsername(null)
      setUserId(null)
      setRole(null)
    }
    window.addEventListener('briefen:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('briefen:unauthorized', handleUnauthorized)
  }, [])

  const login = useCallback(async (usernameInput, password) => {
    const computedAuthHeader = 'Basic ' + btoa(`${usernameInput}:${password}`)
    setAuthHeader(computedAuthHeader)
    try {
      const res = await apiFetch('/api/settings')
      if (res.ok) {
        let fetchedUserId = null
        let fetchedRole = null
        try {
          const meRes = await apiFetch('/api/users/me')
          if (meRes.ok) {
            const me = await meRes.json()
            fetchedUserId = me.id ?? null
            fetchedRole = me.role ?? null
          }
        } catch {
          // non-critical
        }
        sessionStorage.setItem(SESSION_KEY, JSON.stringify({
          username: usernameInput,
          authHeader: computedAuthHeader,
          userId: fetchedUserId,
          role: fetchedRole,
        }))
        setIsAuthenticated(true)
        setUsername(usernameInput)
        setUserId(fetchedUserId)
        setRole(fetchedRole)
        return true
      }
    } catch {
      // Network error — fall through
    }
    clearCredentials()
    return false
  }, [])

  const logout = useCallback(() => {
    clearCredentials()
    sessionStorage.removeItem(SESSION_KEY)
    setIsAuthenticated(false)
    setUsername(null)
    setUserId(null)
    setRole(null)
  }, [])

  return { isAuthenticated, username, userId, role, login, logout }
}
