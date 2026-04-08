/**
 * Module-level singleton that injects the Authorization header into every
 * API request. Call setCredentials() after login and clearCredentials() on logout.
 */

// Initialize from sessionStorage so the header is available immediately on
// module load — before useAuth.useEffect has a chance to call setCredentials().
// This prevents child-component effects (e.g. useSummaries) from firing
// unauthenticated requests during the very first render.
function loadAuthFromSession() {
  try {
    const stored = sessionStorage.getItem('briefen_auth')
    if (stored) {
      const { username, password } = JSON.parse(stored)
      if (username && password) return 'Basic ' + btoa(`${username}:${password}`)
    }
  } catch {
    // sessionStorage unavailable or corrupted
  }
  return null
}

let authHeader = loadAuthFromSession()

export function setCredentials(username, password) {
  authHeader = 'Basic ' + btoa(`${username}:${password}`)
}

export function clearCredentials() {
  authHeader = null
}

/**
 * Drop-in replacement for fetch() that:
 *   - Adds the Authorization header when credentials are present
 *   - Dispatches a 'briefen:unauthorized' DOM event on 401 so any listener can react
 */
export async function apiFetch(url, options = {}) {
  const headers = { ...options.headers }
  if (authHeader) {
    headers['Authorization'] = authHeader
  }
  const response = await fetch(url, { ...options, headers })
  if (response.status === 401) {
    window.dispatchEvent(new CustomEvent('briefen:unauthorized'))
  }
  return response
}
