import { STRINGS } from '../constants/strings'
import { ThemeToggle } from './ThemeToggle'
import styles from './Header.module.css'

export function Header({ theme, onToggleTheme, onNavigate, currentPage }) {
  return (
    <header className={styles.header}>
      <div className={styles.brand} onClick={() => onNavigate('home')} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onNavigate('home') } }} role="button" tabIndex={0}>
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
            <line x1="3" y1="5" x2="17" y2="5" />
            <line x1="3" y1="10" x2="17" y2="10" />
            <line x1="3" y1="15" x2="17" y2="15" />
            <circle cx="7" cy="5" r="2.5" fill="currentColor" stroke="none" />
            <circle cx="13" cy="10" r="2.5" fill="currentColor" stroke="none" />
            <circle cx="9" cy="15" r="2.5" fill="currentColor" stroke="none" />
          </svg>
        </button>
        <ThemeToggle theme={theme} onToggle={onToggleTheme} />
      </div>
    </header>
  )
}
