import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { useSettings } from './useSettings'

const MOCK_SETTINGS = {
  defaultLength: 'default',
  model: 'gemma3:4b',
  notificationsEnabled: true,
  openaiApiKey: null,
  readeckApiKey: null,
  readeckUrl: null,
}

const server = setupServer(
  http.get('/api/settings', () => {
    return HttpResponse.json(MOCK_SETTINGS, { status: 200 })
  }),
  http.put('/api/settings', () => {
    return new HttpResponse(null, { status: 200 })
  })
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => {
  server.resetHandlers()
  localStorage.clear()
})
afterAll(() => server.close())

describe('useSettings', () => {
  it('shouldFetchSettingsOnMount', async () => {
    // Arrange / Act
    const { result } = renderHook(() => useSettings())

    // Assert — wait for the API response to be applied
    await waitFor(() => {
      expect(result.current.settings.model).toBe('gemma3:4b')
    })
    expect(result.current.settings.defaultLength).toBe('default')
  })

  it('shouldUpdateSettingOptimistically', async () => {
    // Arrange
    const { result } = renderHook(() => useSettings())
    // Wait for initial fetch to settle
    await waitFor(() => {
      expect(result.current.settings.model).toBe('gemma3:4b')
    })

    // Act — update a setting
    act(() => {
      result.current.updateSetting('defaultLength', 'shorter')
    })

    // Assert — optimistic update should be immediate
    expect(result.current.settings.defaultLength).toBe('shorter')
  })
})
