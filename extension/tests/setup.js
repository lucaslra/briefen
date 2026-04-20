// Mock the WebExtension browser API used throughout popup.js and options.js.
// These globals are injected by Firefox at extension runtime; in Jest we stub them.

global.browser = {
  storage: {
    local: {
      get: jest.fn(),
      set: jest.fn(),
    },
    session: {
      get: jest.fn(),
      set: jest.fn(),
    },
  },
  tabs: {
    query: jest.fn(),
  },
  runtime: {
    openOptionsPage: jest.fn(),
  },
};

// fetch is not available in jsdom by default.
global.fetch = jest.fn();

// ---------------------------------------------------------------------------
// DOMContentLoaded listener cleanup
//
// Integration tests use jest.isolateModules + document.dispatchEvent to drive
// the popup/options init handlers. Without cleanup, each loadPopup/loadOptions
// call adds a new DOMContentLoaded listener on the *same* document object,
// causing prior-test listeners to fire in subsequent tests and consume mocks
// out of order. We track every listener added to `document` and remove them
// all after each test.
// ---------------------------------------------------------------------------

const _domContentListeners = [];
const _origAddEventListener = document.addEventListener.bind(document);

document.addEventListener = function (type, listener, ...args) {
  if (type === 'DOMContentLoaded') {
    _domContentListeners.push(listener);
  }
  return _origAddEventListener(type, listener, ...args);
};

afterEach(() => {
  _domContentListeners.forEach((l) =>
    document.removeEventListener('DOMContentLoaded', l),
  );
  _domContentListeners.length = 0;
});
