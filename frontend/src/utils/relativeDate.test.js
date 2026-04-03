import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { formatRelativeDate } from './relativeDate'

describe('formatRelativeDate', () => {
  const NOW = new Date('2026-04-03T12:00:00.000Z').getTime()

  beforeEach(() => {
    vi.spyOn(Date, 'now').mockReturnValue(NOW)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shouldReturnJustNowForTimestampsUnder60Seconds', () => {
    // Arrange
    const thirtySecondsAgo = new Date(NOW - 30_000).toISOString()

    // Act
    const result = formatRelativeDate(thirtySecondsAgo)

    // Assert
    expect(result).toBe('just now')
  })

  it('shouldReturnMinutesAgoForRecentTimestamps', () => {
    // Arrange
    const fiveMinutesAgo = new Date(NOW - 5 * 60_000).toISOString()

    // Act
    const result = formatRelativeDate(fiveMinutesAgo)

    // Assert
    expect(result).toBe('5m ago')
  })

  it('shouldReturnHoursAgoForOlderTimestamps', () => {
    // Arrange
    const threeHoursAgo = new Date(NOW - 3 * 3_600_000).toISOString()

    // Act
    const result = formatRelativeDate(threeHoursAgo)

    // Assert
    expect(result).toBe('3h ago')
  })

  it('shouldReturnDaysAgoForOldTimestamps', () => {
    // Arrange
    const twoDaysAgo = new Date(NOW - 2 * 86_400_000).toISOString()

    // Act
    const result = formatRelativeDate(twoDaysAgo)

    // Assert
    expect(result).toBe('2d ago')
  })

  it('shouldReturnEmptyStringForNullInput', () => {
    // Arrange / Act / Assert
    expect(formatRelativeDate(null)).toBe('')
    expect(formatRelativeDate(undefined)).toBe('')
    expect(formatRelativeDate('')).toBe('')
  })
})
