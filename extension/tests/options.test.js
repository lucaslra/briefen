const fs = require('fs');
const path = require('path');

const {
  DEFAULT_BRIEFEN_URL,
  TIMEOUT_MS,
  getStoredUrl,
  saveUrl,
  getStoredCredentials,
  saveCredentials,
  showFeedback,
  hideFeedback,
  testConnection,
} = require('../options');

// ---------------------------------------------------------------------------
// getStoredUrl
// ---------------------------------------------------------------------------

describe('getStoredUrl', () => {
  test('returns the stored URL when present', async () => {
    browser.storage.local.get.mockResolvedValueOnce({ briefenUrl: 'https://briefen.example.com' });
    await expect(getStoredUrl()).resolves.toBe('https://briefen.example.com');
  });

  test('returns the default URL when storage is empty', async () => {
    browser.storage.local.get.mockResolvedValueOnce({});
    await expect(getStoredUrl()).resolves.toBe(DEFAULT_BRIEFEN_URL);
  });
});

// ---------------------------------------------------------------------------
// saveUrl
// ---------------------------------------------------------------------------

describe('saveUrl', () => {
  test('persists the URL to local storage', async () => {
    browser.storage.local.set.mockResolvedValueOnce(undefined);
    await saveUrl('http://localhost:9090');
    expect(browser.storage.local.set).toHaveBeenCalledWith({ briefenUrl: 'http://localhost:9090' });
  });
});

// ---------------------------------------------------------------------------
// getStoredCredentials
// ---------------------------------------------------------------------------

describe('getStoredCredentials', () => {
  test('returns stored username and password', async () => {
    browser.storage.local.get.mockResolvedValueOnce({ briefenUsername: 'alice', briefenPassword: 'secret' });
    await expect(getStoredCredentials()).resolves.toEqual({ username: 'alice', password: 'secret' });
  });

  test('returns empty strings when nothing is stored', async () => {
    browser.storage.local.get.mockResolvedValueOnce({});
    await expect(getStoredCredentials()).resolves.toEqual({ username: '', password: '' });
  });
});

// ---------------------------------------------------------------------------
// saveCredentials
// ---------------------------------------------------------------------------

describe('saveCredentials', () => {
  test('persists username and password to local storage', async () => {
    browser.storage.local.set.mockResolvedValueOnce(undefined);
    await saveCredentials('alice', 'secret');
    expect(browser.storage.local.set).toHaveBeenCalledWith({ briefenUsername: 'alice', briefenPassword: 'secret' });
  });

  test('persists empty strings when credentials are cleared', async () => {
    browser.storage.local.set.mockResolvedValueOnce(undefined);
    await saveCredentials('', '');
    expect(browser.storage.local.set).toHaveBeenCalledWith({ briefenUsername: '', briefenPassword: '' });
  });
});

// ---------------------------------------------------------------------------
// showFeedback / hideFeedback
// ---------------------------------------------------------------------------

describe('showFeedback', () => {
  let el;
  beforeEach(() => { el = document.createElement('div'); });

  test('sets textContent and success class', () => {
    showFeedback(el, 'success', 'Settings saved.');
    expect(el.textContent).toBe('Settings saved.');
    expect(el.className).toBe('feedback feedback--success');
  });

  test('sets textContent and error class', () => {
    showFeedback(el, 'error', 'Could not connect.');
    expect(el.textContent).toBe('Could not connect.');
    expect(el.className).toBe('feedback feedback--error');
  });

  test('sets textContent and info class', () => {
    showFeedback(el, 'info', 'Testing connection…');
    expect(el.textContent).toBe('Testing connection…');
    expect(el.className).toBe('feedback feedback--info');
  });
});

describe('hideFeedback', () => {
  test('clears textContent and sets the hidden class', () => {
    const el = document.createElement('div');
    el.textContent = 'Some message';
    el.className = 'feedback feedback--success';

    hideFeedback(el);

    expect(el.textContent).toBe('');
    expect(el.className).toBe('feedback hidden');
  });
});

// ---------------------------------------------------------------------------
// testConnection
// ---------------------------------------------------------------------------

describe('testConnection', () => {
  test('returns ok: true and a "Connected" message on HTTP 200', async () => {
    fetch.mockResolvedValueOnce({ ok: true });
    const result = await testConnection('http://localhost:8080');
    expect(result.ok).toBe(true);
    expect(result.message).toMatch(/Connected to Briefen at http:\/\/localhost:8080/);
  });

  test('calls the /api/health endpoint', async () => {
    fetch.mockResolvedValueOnce({ ok: true });
    await testConnection('http://localhost:8080');
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/health',
      expect.any(Object),
    );
  });

  test('strips a trailing slash before building the URL', async () => {
    fetch.mockResolvedValueOnce({ ok: true });
    await testConnection('http://localhost:8080/');
    expect(fetch).toHaveBeenCalledWith('http://localhost:8080/api/health', expect.any(Object));
  });

  test('returns ok: false and includes the status code on non-2xx', async () => {
    fetch.mockResolvedValueOnce({ ok: false, status: 503 });
    const result = await testConnection('http://localhost:8080');
    expect(result.ok).toBe(false);
    expect(result.message).toContain('503');
  });

  test('returns ok: false with a timeout message on AbortError', async () => {
    fetch.mockRejectedValueOnce(new DOMException('Aborted', 'AbortError'));
    const result = await testConnection('http://localhost:8080');
    expect(result.ok).toBe(false);
    expect(result.message).toMatch(/timed out/i);
  });

  test('returns ok: false with the error message on a general network error', async () => {
    fetch.mockRejectedValueOnce(new Error('Connection refused'));
    const result = await testConnection('http://localhost:8080');
    expect(result.ok).toBe(false);
    expect(result.message).toContain('Connection refused');
  });

  test('aborts the fetch after TIMEOUT_MS', async () => {
    jest.useFakeTimers();
    fetch.mockImplementationOnce((_url, { signal }) =>
      new Promise((_res, rej) => {
        signal.addEventListener('abort', () =>
          rej(new DOMException('Aborted', 'AbortError')),
        );
      }),
    );

    const promise = testConnection('http://localhost:8080');
    jest.advanceTimersByTime(TIMEOUT_MS);
    const result = await promise;
    expect(result.ok).toBe(false);
    expect(result.message).toMatch(/timed out/i);
    jest.useRealTimers();
  });

  test('includes Authorization header when credentials are provided', async () => {
    fetch.mockResolvedValueOnce({ ok: true });
    await testConnection('http://localhost:8080', { username: 'alice', password: 'secret' });
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/health',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': 'Basic ' + btoa('alice:secret'),
        }),
      }),
    );
  });

  test('omits Authorization header when credentials are empty', async () => {
    fetch.mockResolvedValueOnce({ ok: true });
    await testConnection('http://localhost:8080', { username: '', password: '' });
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/health',
      expect.objectContaining({ headers: {} }),
    );
  });
});

// ---------------------------------------------------------------------------
// Options page DOM integration
// ---------------------------------------------------------------------------

describe('Options page DOM integration', () => {
  const HTML = fs.readFileSync(path.resolve(__dirname, '../options.html'), 'utf8');

  function loadOptions() {
    // codeql[js/bad-tag-filter,js/incomplete-multi-character-sanitization] - test-only: stripping script tags from fixture HTML for jsdom injection, not a security sanitizer
    document.documentElement.innerHTML =
      HTML.replace(/<script.*?<\/script>/gs, '');
    jest.isolateModules(() => { require('../options'); });
    document.dispatchEvent(new Event('DOMContentLoaded'));
  }

  test('pre-fills the URL input with the stored value', async () => {
    browser.storage.local.get
      .mockResolvedValueOnce({ briefenUrl: 'https://briefen.example.com' })
      .mockResolvedValueOnce({});
    loadOptions();
    await new Promise((r) => setTimeout(r, 0));
    expect(document.getElementById('briefen-url').value).toBe('https://briefen.example.com');
  });

  test('pre-fills with the default URL when nothing is stored', async () => {
    browser.storage.local.get
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({});
    loadOptions();
    await new Promise((r) => setTimeout(r, 0));
    expect(document.getElementById('briefen-url').value).toBe(DEFAULT_BRIEFEN_URL);
  });

  test('pre-fills username and password inputs with stored credentials', async () => {
    browser.storage.local.get
      .mockResolvedValueOnce({ briefenUrl: 'http://localhost:8080' })
      .mockResolvedValueOnce({ briefenUsername: 'alice', briefenPassword: 'secret' });
    loadOptions();
    await new Promise((r) => setTimeout(r, 0));
    expect(document.getElementById('briefen-username').value).toBe('alice');
    expect(document.getElementById('briefen-password').value).toBe('secret');
  });

  test('pre-fills credential inputs as empty when nothing is stored', async () => {
    browser.storage.local.get
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({});
    loadOptions();
    await new Promise((r) => setTimeout(r, 0));
    expect(document.getElementById('briefen-username').value).toBe('');
    expect(document.getElementById('briefen-password').value).toBe('');
  });

  test('shows success feedback after saving a valid URL', async () => {
    browser.storage.local.get
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({});
    browser.storage.local.set.mockResolvedValue(undefined);
    loadOptions();
    await new Promise((r) => setTimeout(r, 0));

    document.getElementById('briefen-url').value = 'http://localhost:9090';
    document.getElementById('save-btn').click();
    await new Promise((r) => setTimeout(r, 0));

    const feedback = document.getElementById('feedback');
    expect(feedback.textContent).toContain('saved');
    expect(feedback.className).toContain('success');
  });

  test('shows error feedback when saving an invalid URL', async () => {
    browser.storage.local.get
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({});
    loadOptions();
    await new Promise((r) => setTimeout(r, 0));

    document.getElementById('briefen-url').value = 'not a url';
    document.getElementById('save-btn').click();
    await new Promise((r) => setTimeout(r, 0));

    const feedback = document.getElementById('feedback');
    expect(feedback.className).toContain('error');
    expect(browser.storage.local.set).not.toHaveBeenCalled();
  });

  test('shows error feedback when saving an empty URL', async () => {
    browser.storage.local.get
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({});
    loadOptions();
    await new Promise((r) => setTimeout(r, 0));

    document.getElementById('briefen-url').value = '';
    document.getElementById('save-btn').click();
    await new Promise((r) => setTimeout(r, 0));

    expect(document.getElementById('feedback').className).toContain('error');
  });

  test('shows success feedback when connection test passes', async () => {
    browser.storage.local.get
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({});
    fetch.mockResolvedValueOnce({ ok: true });
    loadOptions();
    await new Promise((r) => setTimeout(r, 0));

    document.getElementById('briefen-url').value = 'http://localhost:8080';
    document.getElementById('test-btn').click();
    await new Promise((r) => setTimeout(r, 0));
    await new Promise((r) => setTimeout(r, 0));

    const feedback = document.getElementById('feedback');
    expect(feedback.className).toContain('success');
    expect(feedback.textContent).toContain('Connected');
  });

  test('shows error feedback when connection test fails', async () => {
    browser.storage.local.get
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({});
    fetch.mockRejectedValueOnce(new Error('ECONNREFUSED'));
    loadOptions();
    await new Promise((r) => setTimeout(r, 0));

    document.getElementById('briefen-url').value = 'http://localhost:8080';
    document.getElementById('test-btn').click();
    await new Promise((r) => setTimeout(r, 0));
    await new Promise((r) => setTimeout(r, 0));

    const feedback = document.getElementById('feedback');
    expect(feedback.className).toContain('error');
  });
});
