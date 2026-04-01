import { test, expect } from '@playwright/test';

test.describe('Smoke tests', () => {
  test('homepage loads with title and input', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/Briefen/);
    await expect(page.getByText('Briefen')).toBeVisible();
    await expect(page.getByPlaceholder('Paste an article URL...')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Summarize' })).toBeVisible();
  });

  test('health endpoint returns UP', async ({ request }) => {
    const response = await request.get('/actuator/health');
    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body.status).toBe('UP');
  });
});
