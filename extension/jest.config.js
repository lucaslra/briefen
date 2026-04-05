/** @type {import('jest').Config} */
module.exports = {
  testEnvironment: 'jest-environment-jsdom',
  setupFilesAfterEnv: ['./tests/setup.js'],
  testMatch: ['**/tests/**/*.test.js'],
  clearMocks: true,
  coverageDirectory: 'coverage',
  collectCoverageFrom: ['popup.js', 'options.js'],
};
