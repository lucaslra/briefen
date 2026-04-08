import { test, expect } from './fixtures.js';

test.describe('URL input validation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('submit button is disabled when URL input is empty', async ({ page }) => {
    const button = page.getByRole('button', { name: 'Summarize' });
    await expect(button).toBeDisabled();
  });

  test('shows error for non-HTTP URL', async ({ page }) => {
    const input = page.getByPlaceholder('Paste an article URL...');
    // ftp:// passes browser type="url" validation but fails the app's http/https check
    await input.fill('ftp://example.com/file');
    await page.getByRole('button', { name: 'Summarize' }).click();
    await expect(page.getByText('Please enter a valid HTTP or HTTPS URL.')).toBeVisible();
  });

  test('submit button is enabled with valid-looking input', async ({ page }) => {
    const input = page.getByPlaceholder('Paste an article URL...');
    await input.fill('https://example.com/article');
    const button = page.getByRole('button', { name: 'Summarize' });
    await expect(button).toBeEnabled();
  });

  test('can switch between URL and Paste Content tabs', async ({ page }) => {
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible();

    await page.getByRole('button', { name: 'Paste Content' }).click();
    await expect(page.getByPlaceholder('Paste the article content here...')).toBeVisible();

    await page.getByRole('button', { name: 'URL' }).click();
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible();
  });

  test('submit button is disabled when paste content is empty', async ({ page }) => {
    await page.getByRole('button', { name: 'Paste Content' }).click();
    const button = page.getByRole('button', { name: 'Summarize' });
    await expect(button).toBeDisabled();
  });
});
