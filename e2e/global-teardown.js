/**
 * Playwright global teardown — stops containers and processes started by global-setup.js.
 * No-op when E2E_MANAGED is not set.
 */

import { rmSync } from 'fs';

export default async function globalTeardown() {
  if (process.env.E2E_MANAGED !== 'true') return;

  console.log('[e2e] Tearing down managed E2E environment…');

  // Stop Spring Boot
  const backend = global.__E2E_BACKEND__;
  if (backend && !backend.killed) {
    backend.kill('SIGTERM');
    // Give it a moment to flush, then force-kill
    await new Promise(resolve => {
      const timer = setTimeout(() => { backend.kill('SIGKILL'); resolve(); }, 5000);
      backend.on('close', () => { clearTimeout(timer); resolve(); });
    });
    console.log('[e2e] Spring Boot stopped');
  }

  // Stop WireMock container
  const wiremock = global.__E2E_WIREMOCK__;
  if (wiremock) {
    await wiremock.stop();
    console.log('[e2e] WireMock container stopped');
  }

  // Remove test database
  const dbPath = global.__E2E_DB_PATH__;
  if (dbPath) {
    try {
      rmSync(dbPath, { force: true });
      console.log(`[e2e] Removed test database: ${dbPath}`);
    } catch { /* best-effort */ }
  }
}
