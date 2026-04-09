import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { useSetup } from './useSetup'

const server = setupServer(
  http.get('/api/setup/status', () => {
    return HttpResponse.json({ setupRequired: true })
  })
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('useSetup', () => {
  it('should detect setup required on mount', async () => {
    const { result } = renderHook(() => useSetup())

    // Initially loading
    expect(result.current.loading).toBe(true)

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
    expect(result.current.setupRequired).toBe(true)
  })

  it('should detect setup not required', async () => {
    server.use(
      http.get('/api/setup/status', () => {
        return HttpResponse.json({ setupRequired: false })
      })
    )

    const { result } = renderHook(() => useSetup())

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
    expect(result.current.setupRequired).toBe(false)
  })

  it('should complete setup successfully', async () => {
    server.use(
      http.post('/api/setup', () => {
        return HttpResponse.json({ id: '1', username: 'admin', role: 'ADMIN' }, { status: 201 })
      })
    )

    const { result } = renderHook(() => useSetup())

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    let setupResult
    await act(async () => {
      setupResult = await result.current.completeSetup('admin', 'Str0ng!Pass1')
    })

    expect(setupResult.ok).toBe(true)
    expect(result.current.setupRequired).toBe(false)
  })

  it('should return error on failed setup', async () => {
    server.use(
      http.post('/api/setup', () => {
        return HttpResponse.json({ error: 'Setup has already been completed' }, { status: 409 })
      })
    )

    const { result } = renderHook(() => useSetup())

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    let setupResult
    await act(async () => {
      setupResult = await result.current.completeSetup('admin', 'Str0ng!Pass1')
    })

    expect(setupResult.ok).toBe(false)
    expect(setupResult.status).toBe(409)
  })
})
