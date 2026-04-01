import { test, expect } from '@playwright/test';

test.describe('Reading list page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/reading-list');
  });

  test('displays filter tabs', async ({ page }) => {
    // Reading list defaults to "unread" filter, but all tabs should be visible
    await expect(page.getByRole('button', { name: 'All', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Unread', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Read', exact: true })).toBeVisible();
  });

  test('displays search input', async ({ page }) => {
    await expect(page.getByPlaceholder(/search/i)).toBeVisible();
  });

  test('can switch between filter tabs', async ({ page }) => {
    await page.getByRole('button', { name: 'All', exact: true }).click();
    await page.getByRole('button', { name: 'Read', exact: true }).click();
    await page.getByRole('button', { name: 'Unread', exact: true }).click();
  });
});
