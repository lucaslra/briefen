import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { useBatchSummarize } from './useBatchSummarize'

const server = setupServer(
  http.post('/api/summarize', async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({
      id: Math.random(),
      url: body.url,
      title: `Title for ${body.url}`,
      summary: 'A summary.',
      modelUsed: 'gemma3:4b',
    })
  })
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('useBatchSummarize', () => {
  it('should start with empty state', () => {
    const { result } = renderHook(() => useBatchSummarize())

    expect(result.current.jobs).toEqual([])
    expect(result.current.active).toBe(false)
    expect(result.current.isProcessing).toBe(false)
    expect(result.current.isComplete).toBe(false)
    expect(result.current.doneCount).toBe(0)
    expect(result.current.errorCount).toBe(0)
  })

  it('should process a batch of URLs successfully', async () => {
    const { result } = renderHook(() => useBatchSummarize())
    const urls = ['https://example.com/1', 'https://example.com/2']

    await act(async () => {
      await result.current.summarizeBatch(urls)
    })

    expect(result.current.jobs).toHaveLength(2)
    expect(result.current.jobs[0].status).toBe('done')
    expect(result.current.jobs[1].status).toBe('done')
    expect(result.current.jobs[0].title).toBe('Title for https://example.com/1')
    expect(result.current.doneCount).toBe(2)
    expect(result.current.errorCount).toBe(0)
    expect(result.current.isComplete).toBe(true)
    expect(result.current.isProcessing).toBe(false)
  })

  it('should handle API errors for individual URLs', async () => {
    server.use(
      http.post('/api/summarize', async ({ request }) => {
        const body = await request.json()
        if (body.url === 'https://example.com/bad') {
          return HttpResponse.json({ error: 'Fetch failed' }, { status: 400 })
        }
        return HttpResponse.json({
          id: 1,
          url: body.url,
          title: 'Good',
          summary: 'Summary.',
        })
      })
    )

    const { result } = renderHook(() => useBatchSummarize())
    const urls = ['https://example.com/good', 'https://example.com/bad']

    await act(async () => {
      await result.current.summarizeBatch(urls)
    })

    expect(result.current.jobs[0].status).toBe('done')
    expect(result.current.jobs[1].status).toBe('error')
    expect(result.current.jobs[1].error).toBe('Fetch failed')
    expect(result.current.doneCount).toBe(1)
    expect(result.current.errorCount).toBe(1)
    expect(result.current.isComplete).toBe(true)
  })

  it('should pass lengthHint and model to API', async () => {
    let capturedBody
    server.use(
      http.post('/api/summarize', async ({ request }) => {
        capturedBody = await request.json()
        return HttpResponse.json({
          id: 1,
          url: capturedBody.url,
          title: 'Title',
          summary: 'Summary.',
        })
      })
    )

    const { result } = renderHook(() => useBatchSummarize())

    await act(async () => {
      await result.current.summarizeBatch(['https://example.com'], 'shorter', 'gpt-4o')
    })

    expect(capturedBody.lengthHint).toBe('shorter')
    expect(capturedBody.model).toBe('gpt-4o')
  })

  it('should clear jobs and abort', async () => {
    const { result } = renderHook(() => useBatchSummarize())

    await act(async () => {
      await result.current.summarizeBatch(['https://example.com/1'])
    })

    expect(result.current.jobs).toHaveLength(1)

    act(() => {
      result.current.clear()
    })

    expect(result.current.jobs).toEqual([])
    expect(result.current.active).toBe(false)
  })

  it('should handle network errors', async () => {
    server.use(
      http.post('/api/summarize', () => {
        return HttpResponse.error()
      })
    )

    const { result } = renderHook(() => useBatchSummarize())

    await act(async () => {
      await result.current.summarizeBatch(['https://example.com'])
    })

    expect(result.current.jobs[0].status).toBe('error')
    expect(result.current.errorCount).toBe(1)
  })

  it('should initialize jobs with queued status', async () => {
    let resolveFirst
    server.use(
      http.post('/api/summarize', () => {
        return new Promise(resolve => {
          resolveFirst = () => resolve(HttpResponse.json({
            id: 1,
            url: 'https://example.com/1',
            title: 'Title',
            summary: 'Summary.',
          }))
        })
      })
    )

    const { result } = renderHook(() => useBatchSummarize())

    act(() => {
      result.current.summarizeBatch(['https://example.com/1', 'https://example.com/2'])
    })

    // The first job should be processing, second should be queued
    await waitFor(() => {
      expect(result.current.jobs[0].status).toBe('processing')
    })
    expect(result.current.jobs[1].status).toBe('queued')
    expect(result.current.isProcessing).toBe(true)

    await act(async () => {
      resolveFirst()
    })
  })
})
