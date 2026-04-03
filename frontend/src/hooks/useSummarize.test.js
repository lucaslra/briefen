import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { useSummarize } from './useSummarize'

const MOCK_SUMMARY = {
  id: 1,
  url: 'https://example.com/article',
  title: 'Test Article',
  summary: 'This is a test summary.',
  modelUsed: 'gemma3:4b',
  createdAt: new Date().toISOString(),
}

const server = setupServer(
  http.post('/api/summarize', () => {
    return HttpResponse.json(MOCK_SUMMARY, { status: 200 })
  })
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('useSummarize', () => {
  it('shouldReturnDataOnSuccessfulSummarize', async () => {
    // Arrange
    const { result } = renderHook(() => useSummarize())

    // Act
    let returned
    await act(async () => {
      returned = await result.current.summarize('https://example.com/article')
    })

    // Assert
    expect(returned).toEqual(MOCK_SUMMARY)
    expect(result.current.data).toEqual(MOCK_SUMMARY)
    expect(result.current.error).toBeNull()
  })

  it('shouldSetErrorOnApiFailure', async () => {
    // Arrange
    server.use(
      http.post('/api/summarize', () => {
        return HttpResponse.json({ error: 'Internal server error' }, { status: 500 })
      })
    )
    const { result } = renderHook(() => useSummarize())

    // Act
    await act(async () => {
      await result.current.summarize('https://example.com/article')
    })

    // Assert
    expect(result.current.error).toBeTruthy()
    expect(result.current.data).toBeNull()
  })

  it('shouldSetLoadingWhileRequestInFlight', async () => {
    // Arrange: use a delayed response to capture the loading state
    let resolveRequest
    server.use(
      http.post('/api/summarize', () => {
        return new Promise(resolve => {
          resolveRequest = () => resolve(HttpResponse.json(MOCK_SUMMARY))
        })
      })
    )
    const { result } = renderHook(() => useSummarize())

    // Act — start the request but don't await
    act(() => {
      result.current.summarize('https://example.com/article')
    })

    // Assert — loading should be true while request is in flight
    await waitFor(() => {
      expect(result.current.loading).toBe(true)
    })

    // Resolve and verify loading goes false
    await act(async () => {
      resolveRequest()
    })

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
  })
})
