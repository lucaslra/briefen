import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { useAuth } from './useAuth'
import { clearCredentials } from '../apiFetch.js'

const SESSION_KEY = 'briefen_auth'

let logoutCalls = 0

const server = setupServer(
  http.get('/api/settings', () => {
    return HttpResponse.json({}, { status: 200 })
  }),
  http.get('/api/users/me', () => {
    return HttpResponse.json({ id: '42', role: 'ADMIN' })
  }),
  http.post('/api/auth/logout', () => {
    logoutCalls++
    return new HttpResponse(null, { status: 204 })
  })
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => {
  server.resetHandlers()
  sessionStorage.clear()
  clearCredentials()
  logoutCalls = 0
  window.history.replaceState(null, '', '/')
})
afterAll(() => server.close())

describe('useAuth', () => {
  it('should start unauthenticated when session is empty', () => {
    const { result } = renderHook(() => useAuth())

    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.username).toBeNull()
    expect(result.current.userId).toBeNull()
    expect(result.current.role).toBeNull()
  })

  it('should restore authentication from sessionStorage', () => {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({
      username: 'admin',
      authHeader: 'Basic ' + btoa('admin:pass'),
      userId: '42',
      role: 'ADMIN',
    }))

    const { result } = renderHook(() => useAuth())

    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.username).toBe('admin')
    expect(result.current.userId).toBe('42')
    expect(result.current.role).toBe('ADMIN')
  })

  it('should login successfully and persist to sessionStorage', async () => {
    const { result } = renderHook(() => useAuth())

    let success
    await act(async () => {
      success = await result.current.login('admin', 'secret')
    })

    expect(success).toBe(true)
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.username).toBe('admin')
    expect(result.current.userId).toBe('42')
    expect(result.current.role).toBe('ADMIN')

    const stored = JSON.parse(sessionStorage.getItem(SESSION_KEY))
    expect(stored.username).toBe('admin')
  })

  it('should return false on login failure (401)', async () => {
    server.use(
      http.get('/api/settings', () => {
        return HttpResponse.json({}, { status: 401 })
      })
    )

    const { result } = renderHook(() => useAuth())

    let success
    await act(async () => {
      success = await result.current.login('bad', 'creds')
    })

    expect(success).toBe(false)
    expect(result.current.isAuthenticated).toBe(false)
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('should return false on network error during login', async () => {
    server.use(
      http.get('/api/settings', () => {
        return HttpResponse.error()
      })
    )

    const { result } = renderHook(() => useAuth())

    let success
    await act(async () => {
      success = await result.current.login('admin', 'pass')
    })

    expect(success).toBe(false)
    expect(result.current.isAuthenticated).toBe(false)
  })

  it('should logout and clear sessionStorage', async () => {
    const { result } = renderHook(() => useAuth())

    await act(async () => {
      await result.current.login('admin', 'secret')
    })
    expect(result.current.isAuthenticated).toBe(true)

    act(() => {
      result.current.logout()
    })

    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.username).toBeNull()
    expect(result.current.userId).toBeNull()
    expect(result.current.role).toBeNull()
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('should logout on briefen:unauthorized event', async () => {
    const { result } = renderHook(() => useAuth())

    await act(async () => {
      await result.current.login('admin', 'secret')
    })
    expect(result.current.isAuthenticated).toBe(true)

    act(() => {
      window.dispatchEvent(new CustomEvent('briefen:unauthorized'))
    })

    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(false)
    })
    expect(result.current.username).toBeNull()
  })

  it('should promote an SSO callback fragment into a Bearer session and strip the URL', () => {
    window.history.replaceState(null, '', '/#sso_token=bfn_abc123&uid=7&role=USER&username=ssouser')

    const { result } = renderHook(() => useAuth())

    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.username).toBe('ssouser')
    expect(result.current.userId).toBe('7')
    expect(result.current.role).toBe('USER')

    const stored = JSON.parse(sessionStorage.getItem(SESSION_KEY))
    expect(stored.authHeader).toBe('Bearer bfn_abc123')

    // Token stripped from the URL (no lingering fragment).
    expect(window.location.hash).toBe('')
  })

  it('should POST /api/auth/logout to revoke the bearer session on logout', async () => {
    const { result } = renderHook(() => useAuth())

    await act(async () => {
      await result.current.login('admin', 'secret')
    })

    await act(async () => {
      result.current.logout()
    })

    await waitFor(() => expect(logoutCalls).toBe(1))
    expect(result.current.isAuthenticated).toBe(false)
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('should still login when /api/users/me fails', async () => {
    server.use(
      http.get('/api/users/me', () => {
        return HttpResponse.json({}, { status: 500 })
      })
    )

    const { result } = renderHook(() => useAuth())

    let success
    await act(async () => {
      success = await result.current.login('admin', 'secret')
    })

    expect(success).toBe(true)
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.username).toBe('admin')
    expect(result.current.userId).toBeNull()
    expect(result.current.role).toBeNull()
  })
})
