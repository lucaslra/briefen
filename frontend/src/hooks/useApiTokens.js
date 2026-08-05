import { useState, useEffect, useCallback } from 'react'
import { apiFetch } from '../apiFetch.js'
import { STRINGS } from '../constants/strings'

/**
 * Manages the current user's personal access tokens (for browser extensions and
 * headless clients). The plaintext token is returned only once, by createToken().
 */
export function useApiTokens() {
  const [tokens, setTokens] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await apiFetch('/api/auth/tokens')
      if (res.ok) {
        setTokens(await res.json())
      } else {
        setError(STRINGS.SETTINGS_TOKENS_LOAD_ERROR)
      }
    } catch {
      setError(STRINGS.SETTINGS_TOKENS_LOAD_ERROR)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])

  const createToken = useCallback(async (name) => {
    const res = await apiFetch('/api/auth/tokens', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name }),
    })
    if (!res.ok) throw new Error('create')
    const data = await res.json()
    await refresh()
    return data.token
  }, [refresh])

  const revokeToken = useCallback(async (id) => {
    const res = await apiFetch(`/api/auth/tokens/${id}`, { method: 'DELETE' })
    if (!res.ok) throw new Error('revoke')
    await refresh()
  }, [refresh])

  return { tokens, loading, error, createToken, revokeToken }
}
