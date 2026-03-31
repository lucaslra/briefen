import { STRINGS } from '../constants/strings'
import { formatElapsed } from '../hooks/useElapsedTime'
import styles from './LoadingSkeleton.module.css'

export function LoadingSkeleton({ elapsed = 0, onCancel }) {
  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <p className={styles.text}>{STRINGS.LOADING_TEXT}</p>
        <div className={styles.headerRight}>
          <span className={styles.timer}>{formatElapsed(elapsed)}</span>
          {onCancel && (
            <button className={styles.cancelBtn} onClick={onCancel}>
              {STRINGS.CANCEL}
            </button>
          )}
        </div>
      </div>
      <div className={styles.skeleton}>
        <div className={`${styles.line} ${styles.title}`} />
        <div className={styles.line} />
        <div className={styles.line} />
        <div className={`${styles.line} ${styles.short}`} />
        <div className={styles.line} />
        <div className={`${styles.line} ${styles.medium}`} />
      </div>
    </div>
  )
}
