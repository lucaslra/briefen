/**
 * Shared Playwright fixtures for authenticated tests.
 *
 * The `test` exported here extends the default Playwright `test` with:
 *   - page: sessionStorage pre-populated with auth so React's useAuth skips the Login screen
 *   - request: APIRequestContext with the Authorization header injected
 *
 * Credentials are read from:
 *   E2E_USERNAME (default: "admin")
 *   E2E_PASSWORD (default: "admin")
 *
 * In managed mode (E2E_MANAGED=true), global-setup.js sets these automatically.
 * In non-managed mode, set them before running: E2E_USERNAME=admin E2E_PASSWORD=<your-password> make e2e
 */

import { test as base, expect } from '@playwright/test'

export { expect }

export function getUsername() { return process.env.E2E_USERNAME || 'admin' }
export function getPassword() { return process.env.E2E_PASSWORD || 'admin' }

export function basicAuth() {
  return 'Basic ' + Buffer.from(`${getUsername()}:${getPassword()}`).toString('base64')
}

export const test = base.extend({
  /**
   * Pre-populate sessionStorage before the page loads so the React app treats
   * the visitor as already logged in (role=ADMIN).
   */
  page: async ({ page }, use) => {
    await page.addInitScript(([u, p]) => {
      sessionStorage.setItem('briefen_auth', JSON.stringify({
        username: u,
        password: p,
        userId: null,
        role: 'ADMIN',
      }))
    }, [getUsername(), getPassword()])
    await use(page)
  },

  /**
   * Override the request fixture so all APIRequestContext calls include the
   * Authorization header.
   */
  request: async ({ playwright, baseURL }, use) => {
    const ctx = await playwright.request.newContext({
      baseURL,
      extraHTTPHeaders: { Authorization: basicAuth() },
    })
    await use(ctx)
    await ctx.dispose()
  },
})
