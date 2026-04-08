import { test, expect } from './fixtures.js';

test.describe('Navigation', () => {
  test('navigate to reading list and back', async ({ page }) => {
    await page.goto('/');

    // Click reading list button in header (aria-label="Reading list")
    await page.getByRole('button', { name: 'Reading list' }).click();
    await expect(page).toHaveURL(/\/reading-list/);

    // Navigate back to home via brand
    await page.locator('[role="button"]', { hasText: 'Briefen' }).click();
    await expect(page).toHaveURL('/');
  });

  test('navigate to settings and back', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('button', { name: 'Settings' }).click();
    await expect(page).toHaveURL(/\/settings/);

    // Navigate back via brand
    await page.locator('[role="button"]', { hasText: 'Briefen' }).click();
    await expect(page).toHaveURL('/');
  });

  test('direct navigation to /reading-list works (SPA routing)', async ({ page }) => {
    await page.goto('/reading-list');
    await expect(page.getByRole('button', { name: 'All', exact: true })).toBeVisible();
  });

  test('direct navigation to /settings works (SPA routing)', async ({ page }) => {
    await page.goto('/settings');
    await expect(page.getByText('Summary Length')).toBeVisible();
  });
});
