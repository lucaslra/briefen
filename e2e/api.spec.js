import { test, expect } from './fixtures.js';

test.describe('API endpoints', () => {
  test('GET /api/summaries returns a list', async ({ request }) => {
    const response = await request.get('/api/summaries');
    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body).toHaveProperty('content');
    expect(Array.isArray(body.content)).toBeTruthy();
  });

  test('GET /api/summaries/unread-count returns a count', async ({ request }) => {
    const response = await request.get('/api/summaries/unread-count');
    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(typeof body).toBe('object');
  });

  test('GET /api/models returns providers', async ({ request }) => {
    const response = await request.get('/api/models');
    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body).toHaveProperty('providers');
    expect(Array.isArray(body.providers)).toBeTruthy();
    expect(body.providers.length).toBeGreaterThan(0);
  });

  test('GET /api/settings returns settings', async ({ request }) => {
    const response = await request.get('/api/settings');
    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body).toHaveProperty('defaultLength');
  });

  test('POST /api/summarize rejects invalid URL', async ({ request }) => {
    const response = await request.post('/api/summarize', {
      data: { url: 'not-a-url' },
    });
    expect(response.status()).toBe(400);
  });
});
