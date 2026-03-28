import { STRINGS } from '../constants/strings'
import { ThemeToggle } from './ThemeToggle'
import styles from './Header.module.css'

export function Header({ theme, onToggleTheme }) {
  return (
    <header className={styles.header}>
      <div className={styles.brand}>
        <h1 className={styles.title}>{STRINGS.APP_TITLE}</h1>
        <p className={styles.subtitle}>{STRINGS.APP_SUBTITLE}</p>
      </div>
      <ThemeToggle theme={theme} onToggle={onToggleTheme} />
    </header>
  )
}
