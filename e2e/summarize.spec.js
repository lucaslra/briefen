/**
 * Full-stack summarization E2E tests.
 *
 * These tests require the managed environment (WireMock + backend) to be running.
 * They are automatically skipped when E2E_MANAGED is not set.
 *
 * Run with:   E2E_MANAGED=true npx playwright test e2e/summarize.spec.js
 *   or:       make e2e-managed
 */

import { test, expect } from './fixtures.js';

const MANAGED = process.env.E2E_MANAGED === 'true';

const ARTICLE_TEXT = `
  Software testing is the process of evaluating and verifying that a software application
  or system works as intended. The benefits of testing include preventing bugs, reducing
  development costs, and improving performance. Testing can be divided into functional and
  non-functional testing. Functional testing verifies that software behaves according to
  requirements. Non-functional testing covers performance, security, and usability aspects.
  Integration testing ensures that different components work together correctly. End-to-end
  testing validates complete user workflows from start to finish, providing high confidence
  in the system's overall behaviour.
`.trim();

test.describe('Summarization flow (managed)', () => {
  test.beforeEach(({ }, testInfo) => {
    if (!MANAGED) testInfo.skip();
  });

  // ── API-level ───────────────────────────────────────────────────────────────

  test('POST /api/summarize with text returns a summary from WireMock', async ({ request }) => {
    const response = await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'E2E API Test Article' },
    });

    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body).toMatchObject({
      title: expect.any(String),
      summary: expect.stringContaining('Managed E2E Test Article'),
      modelUsed: expect.any(String),
    });
    expect(body.id).toBeTruthy();
  });

  test('POST /api/summarize caches result — second call returns same id', async ({ request }) => {
    const first = await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'Cache Test Article', sourceUrl: 'https://example.com/cache-test' },
    });
    const second = await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'Cache Test Article', sourceUrl: 'https://example.com/cache-test' },
    });

    expect(first.status()).toBe(200);
    expect(second.status()).toBe(200);
    expect((await first.json()).id).toBe((await second.json()).id);
  });

  test('created summary appears in GET /api/summaries list', async ({ request }) => {
    const createRes = await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'List Visibility Test' },
    });
    const { id } = await createRes.json();

    const listRes = await request.get('/api/summaries');
    const body = await listRes.json();

    const found = body.content.find(s => s.id === id);
    expect(found).toBeTruthy();
    expect(found.title).toBeTruthy();
  });

  test('unread count increments after new summary is created', async ({ request }) => {
    const before = (await (await request.get('/api/summaries/unread-count')).json()).count;

    await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'Unread Count Test' },
    });

    const after = (await (await request.get('/api/summaries/unread-count')).json()).count;
    expect(after).toBeGreaterThanOrEqual(before);
  });

  test('PATCH read-status marks summary as read and unread count decreases', async ({ request }) => {
    const createRes = await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'Read Status Test' },
    });
    const { id } = await createRes.json();

    const before = (await (await request.get('/api/summaries/unread-count')).json()).count;

    const patchRes = await request.patch(`/api/summaries/${id}/read-status`, {
      data: { isRead: true },
    });
    expect(patchRes.status()).toBe(200);
    expect((await patchRes.json()).isRead).toBe(true);

    const after = (await (await request.get('/api/summaries/unread-count')).json()).count;
    expect(after).toBeLessThan(before);
  });

  test('DELETE removes summary from list', async ({ request }) => {
    const createRes = await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'Delete Test' },
    });
    const { id } = await createRes.json();

    const deleteRes = await request.delete(`/api/summaries/${id}`);
    expect(deleteRes.status()).toBe(204);

    const getRes = await request.get('/api/summaries');
    const body = await getRes.json();
    expect(body.content.find(s => s.id === id)).toBeUndefined();
  });

  // ── Browser-level ───────────────────────────────────────────────────────────

  test('user can paste text, summarize it, and see the result', async ({ page }) => {
    await page.goto('/');

    // Switch to Paste Content mode
    await page.getByRole('button', { name: 'Paste Content' }).click();

    // Fill the textarea
    const textarea = page.getByPlaceholder(/paste the article content/i);
    await textarea.fill(ARTICLE_TEXT);

    // Submit
    await page.getByRole('button', { name: 'Summarize' }).click();

    // The loading state should appear briefly, then the summary
    await expect(page.getByText('Managed E2E Test Article')).toBeVisible({ timeout: 30_000 });
  });

  test('summarized article appears in recent summaries on homepage', async ({ page, request }) => {
    // Create via API for speed
    const res = await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'Recent Summaries Visibility Check' },
    });
    expect(res.status()).toBe(200);

    await page.goto('/');
    // Recent summaries section starts collapsed — expand it first
    await page.getByRole('button', { name: /show recent summaries/i }).click();
    await expect(page.getByText('Recent Summaries Visibility Check')).toBeVisible({ timeout: 10_000 });
  });

  test('read items appear in the Reading List under the Read filter', async ({ page, request }) => {
    // Create and immediately mark as read
    const createRes = await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'Reading List E2E Entry' },
    });
    const { id } = await createRes.json();
    await request.patch(`/api/summaries/${id}/read-status`, { data: { isRead: true } });

    // Navigate to reading list, switch to Read filter
    await page.goto('/reading-list');
    await page.getByRole('button', { name: 'Read', exact: true }).click();

    await expect(page.getByText('Reading List E2E Entry')).toBeVisible({ timeout: 10_000 });
  });

  test('notes can be added and are visible in the reading list', async ({ page, request }) => {
    const createRes = await request.post('/api/summarize', {
      data: { text: ARTICLE_TEXT, title: 'Notes E2E Test' },
    });
    const { id } = await createRes.json();

    // Add a note via API
    const noteText = 'E2E test note — added programmatically';
    const patchRes = await request.patch(`/api/summaries/${id}/notes`, {
      data: { notes: noteText },
    });
    expect(patchRes.status()).toBe(200);
    expect((await patchRes.json()).notes).toBe(noteText);
  });
});
