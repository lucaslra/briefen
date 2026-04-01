import { test, expect } from '@playwright/test';

test.describe('Settings page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/settings');
  });

  test('displays summarization settings with length options', async ({ page }) => {
    await expect(page.getByText('Summary Length')).toBeVisible();
    await expect(page.getByText('Short')).toBeVisible();
    await expect(page.getByText('Standard')).toBeVisible();
    await expect(page.getByText('Detailed')).toBeVisible();
  });

  test('displays LLM model selection', async ({ page }) => {
    await expect(page.getByText('LLM Model')).toBeVisible();
  });

  test('can switch between settings tabs', async ({ page }) => {
    // Default: Summarization tab
    await expect(page.getByText('Summary Length')).toBeVisible();

    // Switch to Integrations
    await page.getByRole('button', { name: 'Integrations' }).click();
    await expect(page.getByRole('heading', { name: 'API Keys' })).toBeVisible();

    // Switch to Preferences
    await page.getByRole('button', { name: 'Preferences' }).click();
    await expect(page.getByRole('heading', { name: 'Notifications' })).toBeVisible();
  });
});
