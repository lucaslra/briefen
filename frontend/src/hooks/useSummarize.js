import { useState, useCallback } from 'react'
import { STRINGS } from '../constants/strings'

export function useSummarize() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const summarize = useCallback(async (url, lengthHint = null) => {
    setLoading(true)
    setError(null)

    const payload = { url }
    if (lengthHint) payload.lengthHint = lengthHint

    try {
      const res = await fetch('/api/summarize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })

      const resBody = await res.json()

      if (!res.ok) {
        const message = parseError(resBody, res.status)
        setError(message)
        return null
      }

      setData(resBody)
      return resBody
    } catch (err) {
      setError(STRINGS.ERROR_NETWORK)
      return null
    } finally {
      setLoading(false)
    }
  }, [])

  const clear = useCallback(() => {
    setData(null)
    setError(null)
  }, [])

  return { summarize, data, setData, loading, error, clear }
}

function parseError(body, status) {
  if (body?.error) {
    if (status === 504) return STRINGS.ERROR_TIMEOUT
    if (status === 400 && body.error.includes('too short')) return STRINGS.ERROR_EXTRACTION
    if (status === 502 && body.error.includes('bot protection')) return STRINGS.ERROR_BOT_PROTECTION
    return body.error
  }
  return STRINGS.ERROR_GENERIC
}
