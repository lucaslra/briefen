/**
 * User management E2E tests.
 *
 * These tests require the managed environment (WireMock + backend) to be running
 * because they create/delete real users and depend on a clean database state.
 * They are automatically skipped when E2E_MANAGED is not set.
 *
 * Run with: E2E_MANAGED=true npx playwright test e2e/users.spec.js
 *       or: make e2e-managed
 */

import { test, expect } from './fixtures.js';

const MANAGED = process.env.E2E_MANAGED === 'true';

test.describe('User management (managed)', () => {
  test.beforeEach(({ }, testInfo) => {
    if (!MANAGED) testInfo.skip();
  });

  // ── API-level ───────────────────────────────────────────────────────────────

  test('GET /api/users/me returns current user info', async ({ request }) => {
    const res = await request.get('/api/users/me');
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('id');
    expect(body).toHaveProperty('username');
    expect(body).toHaveProperty('role');
    expect(body.role).toBe('ADMIN');
    expect(body.mainAdmin).toBe(true);
  });

  test('GET /api/users lists all users (admin)', async ({ request }) => {
    const res = await request.get('/api/users');
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(Array.isArray(body)).toBeTruthy();
    expect(body.length).toBeGreaterThanOrEqual(1);
    const admin = body.find(u => u.mainAdmin);
    expect(admin).toBeTruthy();
  });

  test('POST /api/users creates a new user', async ({ request }) => {
    const res = await request.post('/api/users', {
      data: { username: 'e2e-new-user', password: 'password123', role: 'USER' },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body.username).toBe('e2e-new-user');
    expect(body.role).toBe('USER');
    expect(body.mainAdmin).toBe(false);

    // Clean up
    await request.delete(`/api/users/${body.id}`);
  });

  test('POST /api/users with duplicate username returns 409', async ({ request }) => {
    // Create the user first
    const createRes = await request.post('/api/users', {
      data: { username: 'e2e-dup-user', password: 'pass', role: 'USER' },
    });
    expect(createRes.status()).toBe(201);
    const { id } = await createRes.json();

    // Duplicate
    const dupRes = await request.post('/api/users', {
      data: { username: 'e2e-dup-user', password: 'pass', role: 'USER' },
    });
    expect(dupRes.status()).toBe(409);

    // Clean up
    await request.delete(`/api/users/${id}`);
  });

  test('DELETE /api/users/:id removes a user', async ({ request }) => {
    const createRes = await request.post('/api/users', {
      data: { username: 'e2e-delete-target', password: 'pass', role: 'USER' },
    });
    const { id } = await createRes.json();

    const deleteRes = await request.delete(`/api/users/${id}`);
    expect(deleteRes.status()).toBe(204);

    // Verify gone
    const listRes = await request.get('/api/users');
    const users = await listRes.json();
    expect(users.find(u => u.id === id)).toBeUndefined();
  });

  test('DELETE /api/users/:id on mainAdmin returns 403', async ({ request }) => {
    const meRes = await request.get('/api/users/me');
    const { id } = await meRes.json();

    const res = await request.delete(`/api/users/${id}`);
    expect(res.status()).toBe(403);
  });

  test('DELETE /api/users/:id on self returns 400', async ({ request }) => {
    const meRes = await request.get('/api/users/me');
    const { id } = await meRes.json();

    // The main admin IS self here, so we get 403 (mainAdmin check runs first)
    // Create a second admin to test self-delete as non-mainAdmin
    const createRes = await request.post('/api/users', {
      data: { username: 'e2e-self-delete', password: 'pass', role: 'ADMIN' },
    });
    const newUser = await createRes.json();

    // Use new user's credentials to delete themselves
    const newAuth = 'Basic ' + Buffer.from('e2e-self-delete:pass').toString('base64');
    const selfDeleteRes = await request.delete(`/api/users/${newUser.id}`, {
      headers: { Authorization: newAuth },
    });
    expect(selfDeleteRes.status()).toBe(400);

    // Clean up via admin
    await request.delete(`/api/users/${newUser.id}`);
  });

  test('non-admin cannot list users', async ({ request, playwright, baseURL }) => {
    // Create a regular user
    const createRes = await request.post('/api/users', {
      data: { username: 'e2e-regular', password: 'pass', role: 'USER' },
    });
    const { id } = await createRes.json();

    // Make request as regular user
    const regularAuth = 'Basic ' + Buffer.from('e2e-regular:pass').toString('base64');
    const userCtx = await playwright.request.newContext({
      baseURL,
      extraHTTPHeaders: { Authorization: regularAuth },
    })
    const listRes = await userCtx.get('/api/users');
    expect(listRes.status()).toBe(403);
    await userCtx.dispose();

    // Clean up
    await request.delete(`/api/users/${id}`);
  });

  // ── Browser-level ───────────────────────────────────────────────────────────

  test('admin sees Users tab in Settings', async ({ page }) => {
    await page.goto('/settings');
    await expect(page.getByRole('button', { name: 'Users' })).toBeVisible();
  });

  test('admin can view users list in Settings', async ({ page }) => {
    await page.goto('/settings');
    await page.getByRole('button', { name: 'Users' }).click();
    // At least the bootstrap admin should be listed
    await expect(page.getByText('e2e-admin')).toBeVisible({ timeout: 5_000 });
  });

  test('admin can create a user via Settings UI', async ({ page, request }) => {
    await page.goto('/settings');
    await page.getByRole('button', { name: 'Users' }).click();

    await page.getByPlaceholder('Username').fill('e2e-ui-user');
    await page.getByPlaceholder('Password').fill('uipass123');
    await page.getByRole('button', { name: 'Create User' }).click();

    await expect(page.getByText('e2e-ui-user')).toBeVisible({ timeout: 5_000 });

    // Clean up via API
    const users = await (await request.get('/api/users')).json();
    const target = users.find(u => u.username === 'e2e-ui-user');
    if (target) await request.delete(`/api/users/${target.id}`);
  });

  test('mainAdmin row has no delete button', async ({ page }) => {
    await page.goto('/settings');
    await page.getByRole('button', { name: 'Users' }).click();

    // Wait for the user list to load
    await expect(page.getByText('e2e-admin')).toBeVisible({ timeout: 5_000 });

    // The admin row should not have a delete button (mainAdmin + self)
    const adminRow = page.locator('[class*="userRow"]').filter({ hasText: 'e2e-admin' });
    await expect(adminRow.getByRole('button', { name: /delete/i })).not.toBeVisible();
  });
});
