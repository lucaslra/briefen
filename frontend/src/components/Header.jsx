import { STRINGS } from '../constants/strings'
import { ThemeToggle } from './ThemeToggle'
import styles from './Header.module.css'

export function Header({ theme, onToggleTheme, onNavigate, currentPage }) {
  return (
    <header className={styles.header}>
      <div className={styles.brand} onClick={() => onNavigate('home')} role="button" tabIndex={0}>
        <h1 className={styles.title}>{STRINGS.APP_TITLE}</h1>
        <p className={styles.subtitle}>{STRINGS.APP_SUBTITLE}</p>
      </div>
      <div className={styles.actions}>
        <button
          className={`${styles.iconBtn} ${currentPage === 'settings' ? styles.iconBtnActive : ''}`}
          onClick={() => onNavigate(currentPage === 'settings' ? 'home' : 'settings')}
          title={STRINGS.SETTINGS_TITLE}
          aria-label={STRINGS.SETTINGS_TITLE}
        >
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="10" cy="10" r="3" />
            <path d="M10 1.5v2M10 16.5v2M1.5 10h2M16.5 10h2M3.4 3.4l1.4 1.4M15.2 15.2l1.4 1.4M3.4 16.6l1.4-1.4M15.2 4.8l1.4-1.4" />
          </svg>
        </button>
        <ThemeToggle theme={theme} onToggle={onToggleTheme} />
      </div>
    </header>
  )
}
