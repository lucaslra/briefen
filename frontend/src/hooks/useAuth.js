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

/**
 * Consumes an SSO callback fragment (#sso_token=…&uid=…&role=…&username=…) left
 * by the OIDC success redirect: persists it as a Bearer session, primes the
 * apiFetch header so child effects can authenticate immediately, and strips the
 * token from the URL. Idempotent — safe to call from a render-phase initializer.
 */
function consumeSsoFragment() {
  try {
    const hash = window.location.hash
    if (!hash || !hash.includes('sso_token=')) return null
    const params = new URLSearchParams(hash.slice(1))
    const token = params.get('sso_token')
    if (!token) return null

    const session = {
      username: params.get('username') || null,
      authHeader: 'Bearer ' + token,
      userId: params.get('uid') || null,
      role: params.get('role') || null,
    }
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
    // Prime the singleton so requests fired during this render carry the token.
    setAuthHeader(session.authHeader)
    // Remove the token from the URL (and history) so it can't leak or be replayed.
    window.history.replaceState(null, '', window.location.pathname + window.location.search)
    return session
  } catch {
    return null
  }
}

export function useAuth() {
  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    // Promote an SSO callback fragment into the session before reading it (idempotent).
    consumeSsoFragment()
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
    // Best-effort server-side revocation of the bearer session (no-op for Basic auth).
    // Fire before clearing so the request still carries the current Authorization header.
    apiFetch('/api/auth/logout', { method: 'POST' }).catch(() => {})
    clearCredentials()
    sessionStorage.removeItem(SESSION_KEY)
    setIsAuthenticated(false)
    setUsername(null)
    setUserId(null)
    setRole(null)
  }, [])

  return { isAuthenticated, username, userId, role, login, logout }
}
