import { test, expect } from './fixtures.js';

test.describe('Theme toggle', () => {
  test('can toggle between light and dark mode', async ({ page }) => {
    await page.goto('/');

    const html = page.locator('html');
    const initialTheme = await html.getAttribute('data-theme');

    // Click the theme toggle button (aria-label contains "Switch to ... mode")
    await page.getByRole('button', { name: /switch to .+ mode/i }).click();

    const newTheme = await html.getAttribute('data-theme');
    expect(newTheme).not.toBe(initialTheme);
  });
});
