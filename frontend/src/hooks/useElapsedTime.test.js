import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useElapsedTime } from './useElapsedTime'

describe('useElapsedTime', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('shouldStartAtZero', () => {
    // Arrange / Act
    const { result } = renderHook(() => useElapsedTime(false))

    // Assert
    expect(result.current).toBe(0)
  })

  it('shouldIncrementWhileRunning', async () => {
    // Arrange
    const { result } = renderHook(() => useElapsedTime(true))

    // Act — advance fake timers by 500ms (5 intervals of 100ms)
    act(() => {
      vi.advanceTimersByTime(500)
    })

    // Assert — elapsed should be greater than 0
    expect(result.current).toBeGreaterThan(0)
  })
})
