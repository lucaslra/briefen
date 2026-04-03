import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useTheme } from './useTheme'

describe('useTheme', () => {
  beforeEach(() => {
    localStorage.clear()
    // Reset document attribute
    document.documentElement.removeAttribute('data-theme')
    // Default matchMedia mock: prefers light
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false, // prefers-color-scheme: dark → false means light preferred
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })
  })

  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('shouldDefaultToLightTheme', () => {
    // Arrange: no stored theme, matchMedia returns false (light preferred)

    // Act
    const { result } = renderHook(() => useTheme())

    // Assert
    expect(result.current.theme).toBe('light')
  })

  it('shouldToggleToDarkWhenCurrentlyLight', () => {
    // Arrange
    localStorage.setItem('theme', 'light')
    const { result } = renderHook(() => useTheme())
    expect(result.current.theme).toBe('light')

    // Act
    act(() => {
      result.current.toggleTheme()
    })

    // Assert
    expect(result.current.theme).toBe('dark')
  })

  it('shouldToggleToLightWhenCurrentlyDark', () => {
    // Arrange
    localStorage.setItem('theme', 'dark')
    const { result } = renderHook(() => useTheme())
    expect(result.current.theme).toBe('dark')

    // Act
    act(() => {
      result.current.toggleTheme()
    })

    // Assert
    expect(result.current.theme).toBe('light')
  })

  it('shouldPersistThemeToLocalStorage', () => {
    // Arrange
    localStorage.setItem('theme', 'light')
    const { result } = renderHook(() => useTheme())

    // Act
    act(() => {
      result.current.toggleTheme()
    })

    // Assert
    expect(localStorage.getItem('theme')).toBe('dark')
  })
})
