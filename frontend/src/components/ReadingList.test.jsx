import { describe, it, expect, vi, beforeAll, afterAll, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { ReadingList } from './ReadingList'

const MOCK_ITEMS = [
  {
    id: 1,
    title: 'First Article',
    summary: 'Summary of the first article.',
    isRead: false,
    url: 'https://example.com/first',
    createdAt: new Date().toISOString(),
    modelUsed: 'gemma3:4b',
    notes: null,
  },
  {
    id: 2,
    title: 'Second Article',
    summary: 'Summary of the second article.',
    isRead: true,
    url: 'https://blog.test/second',
    createdAt: new Date().toISOString(),
    modelUsed: 'gpt-4o',
    notes: 'Some note',
  },
]

const server = setupServer(
  http.get('/api/summaries', () => {
    return HttpResponse.json({ content: MOCK_ITEMS, last: true })
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

beforeAll(() => {
  // jsdom does not implement scrollIntoView
  Element.prototype.scrollIntoView = vi.fn()
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

function renderReadingList(props = {}) {
  const defaults = {
    refreshUnreadCount: vi.fn(),
  }
  return render(
    <MemoryRouter>
      <ReadingList {...defaults} {...props} />
    </MemoryRouter>
  )
}

describe('ReadingList', () => {
  it('should render filter tabs', async () => {
    renderReadingList()

    expect(screen.getByText('All')).toBeInTheDocument()
    expect(screen.getByText('Unread')).toBeInTheDocument()
    expect(screen.getByText('Read')).toBeInTheDocument()
  })

  it('should render items after loading', async () => {
    renderReadingList()

    await waitFor(() => {
      expect(screen.getByText('First Article')).toBeInTheDocument()
    })
    expect(screen.getByText('Second Article')).toBeInTheDocument()
  })

  it('should show search input', () => {
    renderReadingList()

    expect(screen.getByPlaceholderText(/search summaries/i)).toBeInTheDocument()
  })

  it('should show empty state when no items', async () => {
    server.use(
      http.get('/api/summaries', () => {
        return HttpResponse.json({ content: [], last: true })
      })
    )

    renderReadingList()

    await waitFor(() => {
      expect(screen.getByText(/all caught up/i)).toBeInTheDocument()
    })
  })

  it('should expand an item on click', async () => {
    renderReadingList()
    const user = userEvent.setup()

    await waitFor(() => {
      expect(screen.getByText('First Article')).toBeInTheDocument()
    })

    await user.click(screen.getByText('First Article'))

    await waitFor(() => {
      expect(screen.getByText('Summary of the first article.')).toBeInTheDocument()
    })
  })

  it('should show domain extracted from URL', async () => {
    renderReadingList()

    await waitFor(() => {
      expect(screen.getByText('example.com')).toBeInTheDocument()
    })
    expect(screen.getByText('blog.test')).toBeInTheDocument()
  })

  it('should switch filter tabs', async () => {
    renderReadingList()
    const user = userEvent.setup()

    await waitFor(() => {
      expect(screen.getByText('First Article')).toBeInTheDocument()
    })

    await user.click(screen.getByText('All'))

    // The filter should change (causes refetch)
    await waitFor(() => {
      expect(screen.getByText('First Article')).toBeInTheDocument()
    })
  })

  it('should show export button when items exist', async () => {
    renderReadingList()

    await waitFor(() => {
      expect(screen.getByText('Export')).toBeInTheDocument()
    })
  })

  it('should show mark all as read button on unread filter', async () => {
    renderReadingList()

    await waitFor(() => {
      expect(screen.getByText('First Article')).toBeInTheDocument()
    })

    // Default filter is 'unread' and we have unread items
    expect(screen.getByText('Mark all as read')).toBeInTheDocument()
  })

  it('should show skeleton while loading', async () => {
    let resolveRequest
    server.use(
      http.get('/api/summaries', () => {
        return new Promise(resolve => {
          resolveRequest = () => resolve(HttpResponse.json({ content: MOCK_ITEMS, last: true }))
        })
      })
    )

    const { container } = renderReadingList()

    // Skeleton should be visible while loading
    await waitFor(() => {
      expect(container.querySelector('[class*="skeleton"]')).toBeInTheDocument()
    })

    resolveRequest()

    await waitFor(() => {
      expect(screen.getByText('First Article')).toBeInTheDocument()
    })
  })

  it('should show search empty message when search has no results', async () => {
    server.use(
      http.get('/api/summaries', ({ request }) => {
        const url = new URL(request.url)
        if (url.searchParams.get('search')) {
          return HttpResponse.json({ content: [], last: true })
        }
        return HttpResponse.json({ content: MOCK_ITEMS, last: true })
      })
    )

    renderReadingList()
    const user = userEvent.setup()

    await waitFor(() => {
      expect(screen.getByText('First Article')).toBeInTheDocument()
    })

    const searchInput = screen.getByPlaceholderText(/search summaries/i)
    await user.type(searchInput, 'nonexistent')

    await waitFor(() => {
      expect(screen.getByText(/no summaries match/i)).toBeInTheDocument()
    })
  })

  it('should clear search input', async () => {
    renderReadingList()
    const user = userEvent.setup()

    await waitFor(() => {
      expect(screen.getByText('First Article')).toBeInTheDocument()
    })

    const searchInput = screen.getByPlaceholderText(/search summaries/i)
    await user.type(searchInput, 'test')

    const clearButton = screen.getByLabelText(/clear search/i)
    await user.click(clearButton)

    expect(searchInput.value).toBe('')
  })

  it('should show load more button when hasMore is true', async () => {
    server.use(
      http.get('/api/summaries', () => {
        return HttpResponse.json({ content: MOCK_ITEMS, last: false })
      })
    )

    renderReadingList()

    await waitFor(() => {
      expect(screen.getByText('Load more')).toBeInTheDocument()
    })
  })

  it('should not show load more when all items loaded', async () => {
    renderReadingList()

    await waitFor(() => {
      expect(screen.getByText('First Article')).toBeInTheDocument()
    })

    expect(screen.queryByText('Load more')).not.toBeInTheDocument()
  })
})
