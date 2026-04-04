const DEFAULT_BRIEFEN_URL = 'http://localhost:8080';
const TIMEOUT_MS = 8_000;

async function getStoredUrl() {
  const result = await browser.storage.local.get('briefenUrl');
  return result.briefenUrl || DEFAULT_BRIEFEN_URL;
}

async function saveUrl(url) {
  await browser.storage.local.set({ briefenUrl: url });
}

function showFeedback(el, type, message) {
  el.textContent = message;
  el.className = `feedback feedback--${type}`;
}

function hideFeedback(el) {
  el.className = 'feedback hidden';
  el.textContent = '';
}

async function testConnection(url) {
  const normalizedUrl = url.replace(/\/$/, '');
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const response = await fetch(`${normalizedUrl}/api/health`, {
      method: 'GET',
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
  const saveBtn = document.getElementById('save-btn');
  const testBtn = document.getElementById('test-btn');
  const feedbackEl = document.getElementById('feedback');

  // Load saved URL
  urlInput.value = await getStoredUrl();

  // Clear feedback on input change
  urlInput.addEventListener('input', () => hideFeedback(feedbackEl));

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

    await saveUrl(url.replace(/\/$/, ''));
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

    const result = await testConnection(url);

    testBtn.disabled = false;
    showFeedback(feedbackEl, result.ok ? 'success' : 'error', result.message);
  });
});
