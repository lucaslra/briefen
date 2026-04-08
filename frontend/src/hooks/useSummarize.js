import { useState, useCallback, useRef } from 'react'
import { STRINGS } from '../constants/strings'
import { apiFetch } from '../apiFetch.js'

export function useSummarize() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const abortRef = useRef(null)

  const summarize = useCallback(async (url, lengthHint = null, model = null, refresh = false) => {
    if (abortRef.current) abortRef.current.abort()
    const controller = new AbortController()
    abortRef.current = controller

    setLoading(true)
    setError(null)

    const payload = { url }
    if (lengthHint) payload.lengthHint = lengthHint
    if (model) payload.model = model

    const qs = refresh ? '?refresh=true' : ''
    try {
      const res = await apiFetch(`/api/summarize${qs}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        signal: controller.signal,
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
      if (err.name === 'AbortError') return null
      setError(STRINGS.ERROR_NETWORK)
      return null
    } finally {
      setLoading(false)
    }
  }, [])

  const summarizeText = useCallback(async (text, title = null, lengthHint = null, model = null, sourceUrl = null) => {
    if (abortRef.current) abortRef.current.abort()
    const controller = new AbortController()
    abortRef.current = controller

    setLoading(true)
    setError(null)

    const payload = { text }
    if (title) payload.title = title
    if (lengthHint) payload.lengthHint = lengthHint
    if (model) payload.model = model
    if (sourceUrl) payload.sourceUrl = sourceUrl

    try {
      const res = await apiFetch('/api/summarize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        signal: controller.signal,
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
      if (err.name === 'AbortError') return null
      setError(STRINGS.ERROR_NETWORK)
      return null
    } finally {
      setLoading(false)
    }
  }, [])

  const cancel = useCallback(() => {
    if (abortRef.current) {
      abortRef.current.abort()
      abortRef.current = null
    }
    setLoading(false)
    setError(null)
  }, [])

  const clear = useCallback(() => {
    setData(null)
    setError(null)
  }, [])

  return { summarize, summarizeText, data, setData, loading, error, clear, cancel }
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
