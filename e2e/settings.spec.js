import { test, expect } from './fixtures.js';

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

  test('admin sees Users tab', async ({ page }) => {
    // The fixture pre-populates role=ADMIN so the Users tab should render
    await expect(page.getByRole('button', { name: 'Users' })).toBeVisible();
  });

  test('Users tab shows user management UI', async ({ page }) => {
    await page.getByRole('button', { name: 'Users' }).click();
    await expect(page.getByRole('heading', { name: 'User Accounts' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Add User' })).toBeVisible();
    await expect(page.getByPlaceholder('Username')).toBeVisible();
    await expect(page.getByPlaceholder('Password')).toBeVisible();
  });

  test('Create User button is disabled when fields are empty', async ({ page }) => {
    await page.getByRole('button', { name: 'Users' }).click();
    await expect(page.getByRole('button', { name: 'Create User' })).toBeDisabled();
  });

  test('Create User button enables when both fields are filled', async ({ page }) => {
    await page.getByRole('button', { name: 'Users' }).click();
    await page.getByPlaceholder('Username').fill('newuser');
    await page.getByPlaceholder('Password').fill('password');
    await expect(page.getByRole('button', { name: 'Create User' })).toBeEnabled();
  });

  test('Integrations tab shows OpenAI and Anthropic key fields', async ({ page }) => {
    await page.getByRole('button', { name: 'Integrations' }).click();
    await expect(page.getByText('OpenAI API Key')).toBeVisible();
    await expect(page.getByText('Anthropic API Key')).toBeVisible();
  });

  test('Integrations tab shows Readeck configuration', async ({ page }) => {
    await page.getByRole('button', { name: 'Integrations' }).click();
    await expect(page.getByRole('heading', { name: 'Readeck' })).toBeVisible();
  });
});
