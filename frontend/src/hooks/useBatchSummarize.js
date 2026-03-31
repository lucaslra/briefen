import { useState, useCallback, useRef } from 'react'
import { STRINGS } from '../constants/strings'

/**
 * Status per URL: 'queued' | 'processing' | 'done' | 'error'
 * Each entry: { url, status, title, error, result }
 *
 * Articles are processed sequentially to avoid overloading the LLM backend.
 * When running a local model (Ollama), concurrent requests queue up and
 * later ones can timeout waiting for earlier ones to finish.
 */
export function useBatchSummarize() {
  const [jobs, setJobs] = useState([])
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

    // Process URLs sequentially to avoid LLM queue timeouts
    for (let index = 0; index < urls.length; index++) {
      if (controller.signal.aborted) break

      const url = urls[index]

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
        } else {
          setJobs(prev => prev.map((j, i) =>
            i === index ? { ...j, status: 'done', title: body.title, result: body } : j
          ))
        }
      } catch (err) {
        if (err.name === 'AbortError') break
        setJobs(prev => prev.map((j, i) =>
          i === index ? { ...j, status: 'error', error: STRINGS.ERROR_NETWORK } : j
        ))
      }
    }

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
