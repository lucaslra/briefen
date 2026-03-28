import { STRINGS } from '../constants/strings'
import styles from './LoadingSkeleton.module.css'

export function LoadingSkeleton() {
  return (
    <div className={styles.container}>
      <p className={styles.text}>{STRINGS.LOADING_TEXT}</p>
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
