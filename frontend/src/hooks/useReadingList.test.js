import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { useReadingList } from './useReadingList'

const MOCK_ITEMS = [
  { id: 1, title: 'Article One', summary: 'Summary one.', isRead: false, url: 'https://example.com/1', createdAt: new Date().toISOString() },
  { id: 2, title: 'Article Two', summary: 'Summary two.', isRead: true, url: 'https://example.com/2', createdAt: new Date().toISOString() },
]

const server = setupServer(
  http.get('/api/summaries', ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '0')
    if (page > 0) {
      return HttpResponse.json({ content: [], last: true })
    }
    return HttpResponse.json({ content: MOCK_ITEMS, last: false })
  }),
  http.patch('/api/summaries/:id/read-status', () => {
    return new HttpResponse(null, { status: 200 })
  }),
  http.delete('/api/summaries/:id', () => {
    return new HttpResponse(null, { status: 200 })
  }),
  http.patch('/api/summaries/read-status/bulk', () => {
    return new HttpResponse(null, { status: 200 })
  }),
  http.patch('/api/summaries/unread-status/bulk', () => {
    return new HttpResponse(null, { status: 200 })
  }),
  http.patch('/api/summaries/:id/notes', () => {
    return new HttpResponse(null, { status: 200 })
  })
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('useReadingList', () => {
  it('should fetch items on mount', async () => {
    const { result } = renderHook(() => useReadingList(vi.fn()))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })
    expect(result.current.items[0].title).toBe('Article One')
    expect(result.current.hasMore).toBe(true)
  })

  it('should set loading while fetching', async () => {
    let resolveRequest
    server.use(
      http.get('/api/summaries', () => {
        return new Promise(resolve => {
          resolveRequest = () => resolve(HttpResponse.json({ content: MOCK_ITEMS, last: true }))
        })
      })
    )

    const { result } = renderHook(() => useReadingList(vi.fn()))

    await waitFor(() => {
      expect(result.current.loading).toBe(true)
    })

    await act(async () => {
      resolveRequest()
    })

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
  })

  it('should toggle read status optimistically', async () => {
    const refreshUnread = vi.fn()
    const { result } = renderHook(() => useReadingList(refreshUnread))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    // Item 1 starts as unread (isRead: false)
    await act(async () => {
      await result.current.toggleReadStatus(1, false)
    })

    expect(result.current.items.find(i => i.id === 1).isRead).toBe(true)
    expect(refreshUnread).toHaveBeenCalled()
  })

  it('should revert read status on API failure', async () => {
    server.use(
      http.patch('/api/summaries/:id/read-status', () => {
        return HttpResponse.json({}, { status: 500 })
      })
    )

    const { result } = renderHook(() => useReadingList(vi.fn()))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    await act(async () => {
      await result.current.toggleReadStatus(1, false)
    })

    // Should revert to original isRead: false
    await waitFor(() => {
      expect(result.current.items.find(i => i.id === 1).isRead).toBe(false)
    })
    expect(result.current.itemErrors[1]).toBeTruthy()
  })

  it('should delete a summary optimistically', async () => {
    const refreshUnread = vi.fn()
    const { result } = renderHook(() => useReadingList(refreshUnread))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    await act(async () => {
      await result.current.deleteSummary(1)
    })

    expect(result.current.items).toHaveLength(1)
    expect(result.current.items[0].id).toBe(2)
    expect(refreshUnread).toHaveBeenCalled()
  })

  it('should revert delete on API failure', async () => {
    server.use(
      http.delete('/api/summaries/:id', () => {
        return HttpResponse.json({}, { status: 500 })
      })
    )

    const { result } = renderHook(() => useReadingList(vi.fn()))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    // The deleteSummary function uses setItems with a callback that captures
    // removedItem/removedIndex as side effects, then does the API call,
    // then reverts on failure. We need to let all state updates flush.
    act(() => {
      result.current.deleteSummary(1)
    })

    // Optimistic removal should happen first
    await waitFor(() => {
      expect(result.current.items).toHaveLength(1)
    })

    // After API failure, the item should be restored
    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })
    expect(result.current.itemErrors[1]).toBeTruthy()
  })

  it('should mark all as read', async () => {
    const refreshUnread = vi.fn()
    const { result } = renderHook(() => useReadingList(refreshUnread))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    let ok
    await act(async () => {
      ok = await result.current.markAllAsRead()
    })

    expect(ok).toBe(true)
    expect(result.current.items.every(i => i.isRead)).toBe(true)
    expect(refreshUnread).toHaveBeenCalled()
  })

  it('should mark all as unread', async () => {
    const refreshUnread = vi.fn()
    const { result } = renderHook(() => useReadingList(refreshUnread))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    let ok
    await act(async () => {
      ok = await result.current.markAllAsUnread()
    })

    expect(ok).toBe(true)
    expect(result.current.items.every(i => !i.isRead)).toBe(true)
    expect(refreshUnread).toHaveBeenCalled()
  })

  it('should load more items', async () => {
    const extraItems = [
      { id: 3, title: 'Article Three', summary: 'Summary three.', isRead: false, url: 'https://example.com/3', createdAt: new Date().toISOString() },
    ]
    server.use(
      http.get('/api/summaries', ({ request }) => {
        const url = new URL(request.url)
        const page = parseInt(url.searchParams.get('page') || '0')
        if (page === 0) {
          return HttpResponse.json({ content: MOCK_ITEMS, last: false })
        }
        return HttpResponse.json({ content: extraItems, last: true })
      })
    )

    const { result } = renderHook(() => useReadingList(vi.fn()))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    await act(async () => {
      result.current.loadMore()
    })

    await waitFor(() => {
      expect(result.current.items).toHaveLength(3)
    })
    expect(result.current.hasMore).toBe(false)
  })

  it('should change filter and refetch', async () => {
    const { result } = renderHook(() => useReadingList(vi.fn()))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    act(() => {
      result.current.changeFilter('read')
    })

    expect(result.current.filter).toBe('read')

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
  })

  it('should update notes optimistically', async () => {
    const { result } = renderHook(() => useReadingList(vi.fn()))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    let ok
    await act(async () => {
      ok = await result.current.updateNotes(1, 'my note')
    })

    expect(ok).toBe(true)
    expect(result.current.items.find(i => i.id === 1).notes).toBe('my note')
  })

  it('should revert notes on API failure', async () => {
    server.use(
      http.patch('/api/summaries/:id/notes', () => {
        return HttpResponse.json({}, { status: 500 })
      })
    )

    const { result } = renderHook(() => useReadingList(vi.fn()))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    let ok
    await act(async () => {
      ok = await result.current.updateNotes(1, 'my note')
    })

    expect(ok).toBe(false)
    await waitFor(() => {
      expect(result.current.itemErrors[1]).toBeTruthy()
    })
  })

  it('should clear item errors', async () => {
    server.use(
      http.patch('/api/summaries/:id/read-status', () => {
        return HttpResponse.json({}, { status: 500 })
      })
    )

    const { result } = renderHook(() => useReadingList(vi.fn()))

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
    })

    await act(async () => {
      await result.current.toggleReadStatus(1, false)
    })

    await waitFor(() => {
      expect(result.current.itemErrors[1]).toBeTruthy()
    })

    act(() => {
      result.current.clearItemError(1)
    })

    expect(result.current.itemErrors[1]).toBeUndefined()
  })
})
