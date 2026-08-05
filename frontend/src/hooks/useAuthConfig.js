import { useState, useEffect } from 'react'
import { apiFetch } from '../apiFetch.js'

/**
 * Loads the public sign-in-method descriptor (GET /api/auth/config) so the login
 * screen knows whether to show the password form, the SSO button, or both.
 *
 * The endpoint is unauthenticated. On failure we fall back to password-only so
 * the login screen still works.
 */
export function useAuthConfig() {
  const [authConfig, setAuthConfig] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    apiFetch('/api/auth/config')
      .then(res => (res.ok ? res.json() : null))
      .then(cfg => {
        if (!cancelled) {
          setAuthConfig(cfg)
          setLoading(false)
        }
      })
      .catch(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return { authConfig, loading }
}
