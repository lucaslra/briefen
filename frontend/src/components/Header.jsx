import { useNavigate, useLocation } from 'react-router-dom'
import { STRINGS } from '../constants/strings'
import { ThemeToggle } from './ThemeToggle'
import styles from './Header.module.css'

export function Header({ theme, onToggleTheme, unreadCount, onLogout, username }) {
  const navigate = useNavigate()
  const { pathname } = useLocation()

  return (
    <header className={styles.header}>
      <div className={styles.brand} onClick={() => navigate('/')} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate('/') } }} role="button" tabIndex={0}>
        <h1 className={styles.title}>{STRINGS.APP_TITLE}</h1>
        <p className={styles.subtitle}>{STRINGS.APP_SUBTITLE}</p>
      </div>
      <div className={styles.actions}>
        <div className={styles.iconBtnWrapper}>
          <button
            className={`${styles.iconBtn} ${pathname === '/reading-list' ? styles.iconBtnActive : ''}`}
            onClick={() => navigate(pathname === '/reading-list' ? '/' : '/reading-list')}
            title={STRINGS.READING_LIST}
            aria-label={STRINGS.READING_LIST}
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 4h14" />
              <path d="M3 8h10" />
              <path d="M3 12h12" />
              <path d="M3 16h8" />
            </svg>
          </button>
          {unreadCount > 0 && (
            <span className={styles.badge}>{unreadCount > 99 ? '99+' : unreadCount}</span>
          )}
        </div>
        <button
          className={`${styles.iconBtn} ${pathname === '/settings' ? styles.iconBtnActive : ''}`}
          onClick={() => navigate(pathname === '/settings' ? '/' : '/settings')}
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
        {onLogout && (
          <>
            {username && <span className={styles.username}>{username}</span>}
            <button
              className={styles.iconBtn}
              onClick={onLogout}
              title={STRINGS.LOGOUT_BUTTON}
              aria-label={STRINGS.LOGOUT_BUTTON}
            >
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M13 15l4-5-4-5" />
                <path d="M17 10H7" />
                <path d="M7 3H4a1 1 0 00-1 1v12a1 1 0 001 1h3" />
              </svg>
            </button>
          </>
        )}
      </div>
    </header>
  )
}
