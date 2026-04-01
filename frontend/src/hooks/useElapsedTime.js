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
      const start = Date.now()
      startRef.current = start
      // The first tick at 100ms effectively resets elapsed to ~0;
      // use a 0ms initial setTimeout to reset immediately without
      // calling setState synchronously inside the effect.
      const resetTimer = setTimeout(() => setElapsed(0), 0)
      intervalRef.current = setInterval(() => {
        setElapsed(Date.now() - start)
      }, 100)
      return () => {
        clearTimeout(resetTimer)
        clearInterval(intervalRef.current)
      }
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
      // Keep the final elapsed value (don't reset to 0)
      if (startRef.current) {
        const finalElapsed = Date.now() - startRef.current
        const timer = setTimeout(() => setElapsed(finalElapsed), 0)
        return () => clearTimeout(timer)
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
