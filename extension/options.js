const DEFAULT_BRIEFEN_URL = 'http://localhost:8080';
const TEST_TIMEOUT_MS = 8_000; // health check should respond quickly; shorter than send

async function getStoredUrl() {
  const result = await browser.storage.local.get('briefenUrl');
  return result.briefenUrl || DEFAULT_BRIEFEN_URL;
}

async function saveUrl(url) {
  await browser.storage.local.set({ briefenUrl: url });
}

async function getStoredCredentials() {
  const result = await browser.storage.session.get(['briefenUsername', 'briefenPassword']);
  return {
    username: result.briefenUsername || '',
    password: result.briefenPassword || '',
  };
}

async function saveCredentials(username, password) {
  await browser.storage.session.set({ briefenUsername: username, briefenPassword: password });
}

function showFeedback(el, type, message) {
  el.textContent = message;
  el.className = `feedback feedback--${type}`;
}

function hideFeedback(el) {
  el.className = 'feedback hidden';
  el.textContent = '';
}

async function testConnection(url, credentials) {
  const normalizedUrl = url.replace(/\/$/, '');
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TEST_TIMEOUT_MS);

  const headers = {};
  if (credentials?.username && credentials?.password) {
    headers['Authorization'] = 'Basic ' + btoa(`${credentials.username}:${credentials.password}`);
  }

  try {
    const response = await fetch(`${normalizedUrl}/api/health`, {
      method: 'GET',
      headers,
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error(`Server returned ${response.status}`);
    }

    return { ok: true, message: `Connected to Briefen at ${normalizedUrl}` };
  } catch (err) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') {
      return { ok: false, message: 'Connection timed out. Is Briefen running?' };
    }
    return { ok: false, message: `Could not connect: ${err.message}` };
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const urlInput = document.getElementById('briefen-url');
  const usernameInput = document.getElementById('briefen-username');
  const passwordInput = document.getElementById('briefen-password');
  const saveBtn = document.getElementById('save-btn');
  const testBtn = document.getElementById('test-btn');
  const feedbackEl = document.getElementById('feedback');

  // Load saved settings
  urlInput.value = await getStoredUrl();
  const stored = await getStoredCredentials();
  usernameInput.value = stored.username;
  passwordInput.value = stored.password;

  // Clear feedback on any input change
  for (const input of [urlInput, usernameInput, passwordInput]) {
    input.addEventListener('input', () => hideFeedback(feedbackEl));
  }

  // Save
  saveBtn.addEventListener('click', async () => {
    const url = urlInput.value.trim();

    if (!url) {
      showFeedback(feedbackEl, 'error', 'Please enter a URL.');
      return;
    }

    try {
      new URL(url); // validate format
    } catch {
      showFeedback(feedbackEl, 'error', 'Enter a valid URL, e.g. http://localhost:8080');
      return;
    }

    if (usernameInput.value.trim().includes(':')) {
      showFeedback(feedbackEl, 'error', 'Username must not contain a colon (:).');
      return;
    }

    await saveUrl(url.replace(/\/$/, ''));
    await saveCredentials(usernameInput.value.trim(), passwordInput.value);
    showFeedback(feedbackEl, 'success', 'Settings saved.');

    setTimeout(() => hideFeedback(feedbackEl), 3000);
  });

  // Test connection
  testBtn.addEventListener('click', async () => {
    const url = urlInput.value.trim();

    if (!url) {
      showFeedback(feedbackEl, 'error', 'Enter a URL to test.');
      return;
    }

    testBtn.disabled = true;
    showFeedback(feedbackEl, 'info', 'Testing connection…');

    const credentials = {
      username: usernameInput.value.trim(),
      password: passwordInput.value,
    };
    const result = await testConnection(url, credentials);

    testBtn.disabled = false;
    showFeedback(feedbackEl, result.ok ? 'success' : 'error', result.message);
  });
});

// Export pure/async functions for unit testing.
// `module` is undefined in the browser extension context, so this block is a no-op at runtime.
if (typeof module !== 'undefined') {
  module.exports = { DEFAULT_BRIEFEN_URL, TEST_TIMEOUT_MS, getStoredUrl, saveUrl, getStoredCredentials, saveCredentials, showFeedback, hideFeedback, testConnection };
}
