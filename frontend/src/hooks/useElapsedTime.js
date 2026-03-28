import { useState, useEffect, useRef } from 'react'

/**
 * Tracks elapsed time while `running` is true.
 * Updates every 100ms for a smooth real-time counter.
 * Returns the current elapsed time in milliseconds.
 */
export function useElapsedTime(running) {
  const [elapsed, setElapsed] = useState(0)
  const startRef = useRef(null)
  const intervalRef = useRef(null)

  useEffect(() => {
    if (running) {
      startRef.current = Date.now()
      setElapsed(0)
      intervalRef.current = setInterval(() => {
        setElapsed(Date.now() - startRef.current)
      }, 100)
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
      // Keep the final elapsed value (don't reset to 0)
      if (startRef.current) {
        setElapsed(Date.now() - startRef.current)
      }
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
      }
    }
  }, [running])

  return elapsed
}

/**
 * Formats milliseconds as "Xs" or "M:SS" depending on duration.
 */
export function formatElapsed(ms) {
  const totalSeconds = ms / 1000
  if (totalSeconds < 60) {
    return `${totalSeconds.toFixed(1)}s`
  }
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = Math.floor(totalSeconds % 60)
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}
