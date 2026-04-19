import i18n from '../i18n'

const MINUTE = 60_000
const HOUR = 3_600_000
const DAY = 86_400_000

export function formatRelativeDate(isoString) {
  if (!isoString) return ''
  const diff = Date.now() - new Date(isoString).getTime()

  if (diff < MINUTE) return 'just now'
  if (diff < HOUR) return `${Math.floor(diff / MINUTE)}m ago`
  if (diff < DAY) return `${Math.floor(diff / HOUR)}h ago`
  if (diff < DAY * 30) return `${Math.floor(diff / DAY)}d ago`

  return new Date(isoString).toLocaleDateString(i18n.language, { month: 'short', day: 'numeric' })
}
