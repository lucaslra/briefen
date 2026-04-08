/**
 * Authentication E2E tests.
 *
 * These tests use the base Playwright test (no pre-auth fixture) to exercise
 * the login/logout flow directly. Credentials are read from E2E_USERNAME /
 * E2E_PASSWORD env vars. In managed mode global-setup.js sets them automatically.
 */

import { test, expect } from '@playwright/test';
import { getUsername, getPassword } from './fixtures.js';

test.describe('Authentication', () => {
  test('login form is shown when unauthenticated', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
    await expect(page.getByText('Sign in to continue')).toBeVisible();
  });

  test('invalid credentials show an error message', async ({ page }) => {
    await page.goto('/');
    await page.getByLabel('Username').fill('wronguser');
    await page.getByLabel('Password').fill('wrongpassword');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByText('Invalid username or password.')).toBeVisible();
  });

  test('sign-in button is disabled with empty fields', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeDisabled();
  });

  test('valid credentials log in and show the homepage', async ({ page }) => {
    await page.goto('/');
    await page.getByLabel('Username').fill(getUsername());
    await page.getByLabel('Password').fill(getPassword());
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible({ timeout: 10_000 });
  });

  test('logged-in username is displayed in the header', async ({ page }) => {
    await page.goto('/');
    await page.getByLabel('Username').fill(getUsername());
    await page.getByLabel('Password').fill(getPassword());
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(getUsername())).toBeVisible();
  });

  test('logout returns to the login screen', async ({ page }) => {
    await page.goto('/');
    await page.getByLabel('Username').fill(getUsername());
    await page.getByLabel('Password').fill(getPassword());
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible({ timeout: 10_000 });

    await page.getByRole('button', { name: 'Sign out' }).click();
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
  });
});
