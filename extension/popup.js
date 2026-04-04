const DEFAULT_BRIEFEN_URL = 'http://localhost:8080';
const TIMEOUT_MS = 10_000;

// Unsupported URL schemes — can't summarize browser internals
const UNSUPPORTED_SCHEMES = ['about:', 'moz-extension:', 'chrome:', 'file:'];

async function getStoredUrl() {
  const result = await browser.storage.local.get('briefenUrl');
  return (result.briefenUrl || DEFAULT_BRIEFEN_URL).replace(/\/$/, '');
}

async function getCurrentTab() {
  const [tab] = await browser.tabs.query({ active: true, currentWindow: true });
  return tab;
}

function isUnsupportedUrl(url) {
  if (!url) return true;
  return UNSUPPORTED_SCHEMES.some(scheme => url.startsWith(scheme));
}

function truncateUrl(url, maxLen = 48) {
  try {
    const parsed = new URL(url);
    const display = parsed.hostname + parsed.pathname;
    return display.length > maxLen ? display.slice(0, maxLen - 1) + '…' : display;
  } catch {
    return url.length > maxLen ? url.slice(0, maxLen - 1) + '…' : url;
  }
}

async function sendToBriefen(tabUrl, briefenUrl) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const response = await fetch(`${briefenUrl}/api/articles`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url: tabUrl }),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      let message;
      if (response.status === 404 || response.status === 405) {
        message = 'Endpoint not found. Make sure your Briefen backend is up to date.';
      } else {
        let serverMsg = '';
        try {
          const body = await response.json();
          serverMsg = body.error || body.message || '';
        } catch { /* ignore parse errors */ }
        message = serverMsg
          ? `Server error (${response.status}): ${serverMsg}`
          : `Server error (${response.status}). Check your Briefen logs.`;
      }
      throw new Error(message);
    }

    return await response.json();
  } catch (err) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') {
      throw new Error('Request timed out. Is Briefen running?');
    }
    if (err.message.includes('NetworkError') || err.message.includes('Failed to fetch')) {
      throw new Error('Could not reach Briefen. Check the URL in Settings.');
    }
    throw err;
  }
}

// --- UI helpers ---

function showState(name) {
  for (const el of document.querySelectorAll('.state')) {
    el.classList.toggle('hidden', el.id !== `state-${name}`);
  }
}

// --- Init ---

document.addEventListener('DOMContentLoaded', async () => {
  const sendBtn = document.getElementById('send-btn');
  const retryBtn = document.getElementById('retry-btn');
  const tabUrlEl = document.getElementById('tab-url');
  const errorMsg = document.getElementById('error-message');
  const settingsLink = document.getElementById('settings-link');

  settingsLink.addEventListener('click', (e) => {
    e.preventDefault();
    browser.runtime.openOptionsPage();
  });

  let tabUrl = null;

  try {
    const tab = await getCurrentTab();
    tabUrl = tab?.url;

    if (isUnsupportedUrl(tabUrl)) {
      showState('unsupported');
      return;
    }

    tabUrlEl.textContent = truncateUrl(tabUrl);
    tabUrlEl.title = tabUrl;
    showState('idle');
  } catch {
    showState('unsupported');
    return;
  }

  async function handleSend() {
    showState('loading');
    sendBtn.disabled = true;

    try {
      const briefenUrl = await getStoredUrl();
      await sendToBriefen(tabUrl, briefenUrl);
      showState('success');
    } catch (err) {
      errorMsg.textContent = err.message || 'Something went wrong.';
      showState('error');
      sendBtn.disabled = false;
    }
  }

  sendBtn.addEventListener('click', handleSend);
  retryBtn.addEventListener('click', handleSend);
});
