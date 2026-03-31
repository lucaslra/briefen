import { useState, useCallback, useRef } from 'react'
import { STRINGS } from '../constants/strings'

/**
 * Status per URL: 'queued' | 'processing' | 'done' | 'error'
 * Each entry: { url, status, title, error }
 */
export function useBatchSummarize() {
  const [jobs, setJobs] = useState([])     // [{ url, status, title, error, result }]
  const [active, setActive] = useState(false)
  const abortRef = useRef(null)

  const summarizeBatch = useCallback(async (urls, lengthHint = null, model = null) => {
    const initial = urls.map(url => ({
      url,
      status: 'queued',
      title: null,
      error: null,
      result: null,
    }))

    setJobs(initial)
    setActive(true)

    const controller = new AbortController()
    abortRef.current = controller

    // Process all URLs in parallel
    const promises = urls.map(async (url, index) => {
      // Mark as processing
      setJobs(prev => prev.map((j, i) => i === index ? { ...j, status: 'processing' } : j))

      const payload = { url }
      if (lengthHint) payload.lengthHint = lengthHint
      if (model) payload.model = model

      try {
        const res = await fetch('/api/summarize', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
          signal: controller.signal,
        })

        const body = await res.json()

        if (!res.ok) {
          const message = body?.error || STRINGS.ERROR_GENERIC
          setJobs(prev => prev.map((j, i) =>
            i === index ? { ...j, status: 'error', error: message } : j
          ))
          return null
        }

        setJobs(prev => prev.map((j, i) =>
          i === index ? { ...j, status: 'done', title: body.title, result: body } : j
        ))
        return body
      } catch (err) {
        if (err.name === 'AbortError') return null
        setJobs(prev => prev.map((j, i) =>
          i === index ? { ...j, status: 'error', error: STRINGS.ERROR_NETWORK } : j
        ))
        return null
      }
    })

    await Promise.allSettled(promises)
    return true
  }, [])

  const clear = useCallback(() => {
    if (abortRef.current) abortRef.current.abort()
    setJobs([])
    setActive(false)
  }, [])

  const doneCount = jobs.filter(j => j.status === 'done').length
  const errorCount = jobs.filter(j => j.status === 'error').length
  const isComplete = active && jobs.length > 0 && (doneCount + errorCount) === jobs.length
  const isProcessing = active && jobs.length > 0 && !isComplete

  return {
    jobs,
    active,
    isProcessing,
    isComplete,
    doneCount,
    errorCount,
    summarizeBatch,
    clear,
  }
}
