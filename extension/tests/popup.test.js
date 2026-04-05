const fs = require('fs');
const path = require('path');

const {
  DEFAULT_BRIEFEN_URL,
  TIMEOUT_MS,
  getStoredUrl,
  getCurrentTab,
  isUnsupportedUrl,
  truncateUrl,
  sendToBriefen,
} = require('../popup');

// ---------------------------------------------------------------------------
// truncateUrl
// ---------------------------------------------------------------------------

describe('truncateUrl', () => {
  test('strips scheme and shows hostname + path for short URLs', () => {
    expect(truncateUrl('https://example.com/article')).toBe('example.com/article');
  });

  test('root path collapses to just hostname + "/"', () => {
    expect(truncateUrl('https://example.com/')).toBe('example.com/');
  });

  test('truncates to maxLen - 1 chars + ellipsis when display is too long', () => {
    const url = 'https://example.com/' + 'a'.repeat(60);
    const result = truncateUrl(url, 48);
    expect(result).toHaveLength(48);
    expect(result.endsWith('…')).toBe(true);
  });

  test('does not truncate when display is exactly maxLen', () => {
    // hostname (11) + path (1 "/") + 36 "a"s = 48 chars
    const url = 'https://example.com/' + 'a'.repeat(36);
    const result = truncateUrl(url, 48);
    expect(result).not.toContain('…');
  });

  test('respects a custom maxLen', () => {
    const url = 'https://example.com/some/deep/path/to/article';
    const result = truncateUrl(url, 20);
    expect(result.length).toBeLessThanOrEqual(20);
  });

  test('falls back to raw string truncation for invalid URLs', () => {
    const long = 'x'.repeat(60);
    const result = truncateUrl(long, 48);
    expect(result).toHaveLength(48);
    expect(result.endsWith('…')).toBe(true);
  });

  test('returns short invalid string unchanged', () => {
    expect(truncateUrl('short', 48)).toBe('short');
  });
});

// ---------------------------------------------------------------------------
// isUnsupportedUrl
// ---------------------------------------------------------------------------

describe('isUnsupportedUrl', () => {
  test.each([
    [null],
    [undefined],
    [''],
    ['about:blank'],
    ['about:newtab'],
    ['moz-extension://1234-abcd/popup.html'],
    ['chrome://newtab/'],
    ['file:///home/user/file.html'],
  ])('returns true for unsupported URL: %s', (url) => {
    expect(isUnsupportedUrl(url)).toBe(true);
  });

  test.each([
    ['https://example.com/article'],
    ['http://localhost:8080/some-page'],
    ['https://www.nytimes.com/2024/01/01/tech/article.html'],
    ['https://slatestarcodex.com/2014/07/30/meditations-on-moloch/'],
  ])('returns false for supported URL: %s', (url) => {
    expect(isUnsupportedUrl(url)).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// getStoredUrl
// ---------------------------------------------------------------------------

describe('getStoredUrl', () => {
  test('returns the stored URL when one is set', async () => {
    browser.storage.local.get.mockResolvedValueOnce({ briefenUrl: 'http://myserver:9090' });
    await expect(getStoredUrl()).resolves.toBe('http://myserver:9090');
  });

  test('returns the default URL when nothing is stored', async () => {
    browser.storage.local.get.mockResolvedValueOnce({});
    await expect(getStoredUrl()).resolves.toBe(DEFAULT_BRIEFEN_URL);
  });

  test('strips a trailing slash from the stored URL', async () => {
    browser.storage.local.get.mockResolvedValueOnce({ briefenUrl: 'http://myserver:9090/' });
    await expect(getStoredUrl()).resolves.toBe('http://myserver:9090');
  });
});

// ---------------------------------------------------------------------------
// getCurrentTab
// ---------------------------------------------------------------------------

describe('getCurrentTab', () => {
  test('returns the first active tab', async () => {
    const tab = { id: 1, url: 'https://example.com/' };
    browser.tabs.query.mockResolvedValueOnce([tab]);
    await expect(getCurrentTab()).resolves.toEqual(tab);
    expect(browser.tabs.query).toHaveBeenCalledWith({ active: true, currentWindow: true });
  });

  test('returns undefined when no tabs are found', async () => {
    browser.tabs.query.mockResolvedValueOnce([]);
    await expect(getCurrentTab()).resolves.toBeUndefined();
  });
});

// ---------------------------------------------------------------------------
// sendToBriefen
// ---------------------------------------------------------------------------

describe('sendToBriefen', () => {
  const TAB_URL = 'https://example.com/article';
  const BRIEFEN_URL = 'http://localhost:8080';

  function mockOk(body) {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(body),
    });
  }

  function mockError(status, body = {}) {
    fetch.mockResolvedValueOnce({
      ok: false,
      status,
      json: () => Promise.resolve(body),
    });
  }

  test('POSTs to /api/articles with the tab URL', async () => {
    mockOk({ id: 'abc', status: 'QUEUED', message: 'Queued' });
    await sendToBriefen(TAB_URL, BRIEFEN_URL);
    expect(fetch).toHaveBeenCalledWith(
      `${BRIEFEN_URL}/api/articles`,
      expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: TAB_URL }),
      }),
    );
  });

  test('returns the parsed response body on success', async () => {
    const payload = { id: 'xyz', status: 'QUEUED', message: 'Article queued' };
    mockOk(payload);
    await expect(sendToBriefen(TAB_URL, BRIEFEN_URL)).resolves.toEqual(payload);
  });

  test('throws "Endpoint not found" on 404', async () => {
    mockError(404);
    await expect(sendToBriefen(TAB_URL, BRIEFEN_URL)).rejects.toThrow('Endpoint not found');
  });

  test('throws "Endpoint not found" on 405', async () => {
    mockError(405);
    await expect(sendToBriefen(TAB_URL, BRIEFEN_URL)).rejects.toThrow('Endpoint not found');
  });

  test('includes status code and server error field on 500', async () => {
    mockError(500, { error: 'An unexpected error occurred.' });
    await expect(sendToBriefen(TAB_URL, BRIEFEN_URL))
      .rejects.toThrow('Server error (500): An unexpected error occurred.');
  });

  test('falls back to server message field when error field is absent', async () => {
    mockError(503, { message: 'Service Unavailable' });
    await expect(sendToBriefen(TAB_URL, BRIEFEN_URL))
      .rejects.toThrow('Server error (503): Service Unavailable');
  });

  test('shows generic log-hint message when 5xx body is not parseable JSON', async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      json: () => Promise.reject(new SyntaxError('Unexpected token')),
    });
    await expect(sendToBriefen(TAB_URL, BRIEFEN_URL))
      .rejects.toThrow('Server error (500). Check your Briefen logs.');
  });

  test('throws timeout message on AbortError', async () => {
    fetch.mockRejectedValueOnce(new DOMException('The user aborted a request.', 'AbortError'));
    await expect(sendToBriefen(TAB_URL, BRIEFEN_URL)).rejects.toThrow('Request timed out');
  });

  test('throws "Could not reach Briefen" on "Failed to fetch" network error', async () => {
    fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));
    await expect(sendToBriefen(TAB_URL, BRIEFEN_URL)).rejects.toThrow('Could not reach Briefen');
  });

  test('throws "Could not reach Briefen" on Firefox NetworkError message', async () => {
    fetch.mockRejectedValueOnce(new TypeError('NetworkError when attempting to fetch resource.'));
    await expect(sendToBriefen(TAB_URL, BRIEFEN_URL)).rejects.toThrow('Could not reach Briefen');
  });

  test('aborts the request after TIMEOUT_MS via the AbortController signal', async () => {
    jest.useFakeTimers();
    fetch.mockImplementationOnce((_url, { signal }) =>
      new Promise((_res, rej) => {
        signal.addEventListener('abort', () =>
          rej(new DOMException('The user aborted a request.', 'AbortError')),
        );
      }),
    );

    const promise = sendToBriefen(TAB_URL, BRIEFEN_URL);
    jest.advanceTimersByTime(TIMEOUT_MS);
    await expect(promise).rejects.toThrow('Request timed out');
    jest.useRealTimers();
  });
});

// ---------------------------------------------------------------------------
// Popup DOM integration
// ---------------------------------------------------------------------------

describe('Popup DOM integration', () => {
  const HTML = fs.readFileSync(path.resolve(__dirname, '../popup.html'), 'utf8');

  function loadPopup() {
    // jsdom parses innerHTML as a full document when assigned to documentElement
    document.documentElement.innerHTML =
      HTML.replace(/<script.*?<\/script>/gs, ''); // strip script tags; we load manually
    // Re-run the module in isolation so DOMContentLoaded wires up fresh each test
    jest.isolateModules(() => { require('../popup'); });
    document.dispatchEvent(new Event('DOMContentLoaded'));
  }

  test('shows "unsupported" state for an about:blank tab', async () => {
    browser.tabs.query.mockResolvedValueOnce([{ url: 'about:blank' }]);
    loadPopup();
    await new Promise((r) => setTimeout(r, 0)); // flush microtasks
    expect(document.getElementById('state-unsupported').classList.contains('hidden')).toBe(false);
    expect(document.getElementById('state-idle').classList.contains('hidden')).toBe(true);
  });

  test('shows "idle" state and displays the URL for a valid tab', async () => {
    browser.tabs.query.mockResolvedValueOnce([{ url: 'https://example.com/article' }]);
    loadPopup();
    await new Promise((r) => setTimeout(r, 0));
    expect(document.getElementById('state-idle').classList.contains('hidden')).toBe(false);
    expect(document.getElementById('tab-url').textContent).toContain('example.com');
  });

  test('transitions to "success" state after a successful send', async () => {
    browser.tabs.query.mockResolvedValueOnce([{ url: 'https://example.com/article' }]);
    browser.storage.local.get.mockResolvedValueOnce({});
    fetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ id: '1', status: 'QUEUED', message: 'Queued' }),
    });

    loadPopup();
    await new Promise((r) => setTimeout(r, 0)); // idle state ready

    document.getElementById('send-btn').click();
    await new Promise((r) => setTimeout(r, 0)); // loading
    await new Promise((r) => setTimeout(r, 0)); // success

    expect(document.getElementById('state-success').classList.contains('hidden')).toBe(false);
  });

  test('transitions to "error" state and shows message on failure', async () => {
    browser.tabs.query.mockResolvedValueOnce([{ url: 'https://example.com/article' }]);
    browser.storage.local.get.mockResolvedValueOnce({});
    fetch.mockResolvedValueOnce({ ok: false, status: 404, json: () => Promise.resolve({}) });

    loadPopup();
    await new Promise((r) => setTimeout(r, 0));

    document.getElementById('send-btn').click();
    await new Promise((r) => setTimeout(r, 0));
    await new Promise((r) => setTimeout(r, 0));

    expect(document.getElementById('state-error').classList.contains('hidden')).toBe(false);
    expect(document.getElementById('error-message').textContent).toContain('Endpoint not found');
  });
});
