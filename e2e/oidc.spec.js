/**
 * OIDC / SSO E2E tests.
 *
 * Opt-in: these run only when E2E_OIDC=true, which makes global-setup.js start a
 * mock OpenID Connect provider and enable hybrid (password + SSO) auth on the
 * managed backend. Run via `make e2e-oidc`.
 *
 * They use the base Playwright test (no pre-auth fixture) so the flow is exercised
 * from a logged-out state.
 */

import { test, expect } from '@playwright/test';

test.skip(process.env.E2E_OIDC !== 'true', 'OIDC E2E requires E2E_OIDC=true (use `make e2e-oidc`)');

test.describe('OIDC / SSO', () => {
  test('login screen offers the SSO button alongside the password form', async ({ page }) => {
    await page.goto('/');
    // SSO button present (label from BRIEFEN_OIDC_PROVIDER_NAME=Mock SSO).
    await expect(page.getByRole('link', { name: /Sign in with Mock SSO/i })).toBeVisible();
    // Password form still available (hybrid mode).
    await expect(page.getByLabel('Username')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
  });

  test('completes the full SSO authorization-code flow and signs in', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('link', { name: /Sign in with Mock SSO/i }).click();

    // The browser is redirected app → IdP → callback → app, landing authenticated.
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible({ timeout: 20_000 });
    // Provisioned as the mock identity's username (preferred_username claim).
    await expect(page.getByText('e2e-sso')).toBeVisible();
  });

  test('signed-in SSO user keeps an active session across reloads', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('link', { name: /Sign in with Mock SSO/i }).click();
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible({ timeout: 20_000 });

    await page.reload();
    // Bearer session in sessionStorage persists → still authenticated after reload.
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign in' })).toHaveCount(0);
  });

  test('logging out of an SSO session returns to a login screen that still offers SSO', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('link', { name: /Sign in with Mock SSO/i }).click();
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible({ timeout: 20_000 });

    await page.getByRole('button', { name: 'Sign out' }).click();
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
    await expect(page.getByRole('link', { name: /Sign in with Mock SSO/i })).toBeVisible();
  });

  test('callback with a missing state shows a friendly error on the login page', async ({ page }) => {
    // Hitting the callback without the signed authz cookie must not sign the user in.
    await page.goto('/login/oauth2/code/briefen?state=forged&code=abc');
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
    await expect(page.getByRole('alert')).toContainText(/session expired/i);
  });
});
