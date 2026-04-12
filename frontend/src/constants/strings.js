import i18n from '../i18n'

/**
 * Proxy-based STRINGS object that delegates to i18next.
 * All existing component code (STRINGS.KEY_NAME) continues to work
 * without changes, but now respects the selected language.
 */
export const STRINGS = new Proxy({}, {
  get(_, key) {
    return i18n.t(key)
  }
})

/**
 * Maximum number of times each length adjustment button can be clicked.
 * Set to a higher value to allow multiple adjustments.
 */
export const MAX_LENGTH_ADJUSTMENTS = 1
