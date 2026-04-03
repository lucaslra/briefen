import { defineConfig } from '@playwright/test';

const managed = process.env.E2E_MANAGED === 'true';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  // Managed mode runs serially: one backend process shared across all tests.
  // Non-managed mode runs in parallel as before.
  fullyParallel: !managed,
  retries: 0,
  reporter: 'list',

  globalSetup: './e2e/global-setup.js',
  globalTeardown: './e2e/global-teardown.js',

  use: {
    // global-setup.js overwrites BASE_URL when managed; falls back to running app otherwise.
    baseURL: process.env.BASE_URL || 'http://localhost:8080',
    headless: true,
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
});
