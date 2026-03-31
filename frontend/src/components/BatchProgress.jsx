import { useNavigate } from 'react-router-dom'
import { STRINGS } from '../constants/strings'
import { useElapsedTime, formatElapsed } from '../hooks/useElapsedTime'
import styles from './BatchProgress.module.css'

function extractDomain(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}

const STATUS_ICONS = {
  queued: '○',
  processing: '',
  done: '✓',
  error: '✗',
}

function JobRow({ job }) {
  const label = job.title || extractDomain(job.url)

  return (
    <div className={`${styles.job} ${styles[`job_${job.status}`]}`}>
      <span className={styles.jobStatus}>
        {job.status === 'processing' ? (
          <span className={styles.spinner} />
        ) : (
          STATUS_ICONS[job.status]
        )}
      </span>
      <div className={styles.jobContent}>
        <span className={styles.jobLabel}>{label}</span>
        {job.status === 'queued' && <span className={styles.jobHint}>{STRINGS.BATCH_STATUS_QUEUED}</span>}
        {job.status === 'processing' && <span className={styles.jobHint}>{STRINGS.BATCH_STATUS_PROCESSING}</span>}
        {job.status === 'error' && <span className={styles.jobError}>{job.error || STRINGS.BATCH_STATUS_ERROR}</span>}
      </div>
    </div>
  )
}

export function BatchProgress({ jobs, isProcessing, isComplete, doneCount, errorCount, onDismiss, onCancel }) {
  const navigate = useNavigate()
  const elapsed = useElapsedTime(isProcessing)
  const total = jobs.length

  const heading = isComplete
    ? STRINGS.BATCH_COMPLETE.replace('{done}', doneCount).replace('{total}', total)
    : STRINGS.BATCH_PROGRESS_TITLE.replace('{n}', total)

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <p className={styles.heading}>{heading}</p>
        <div className={styles.headerRight}>
          {isProcessing && <span className={styles.timer}>{formatElapsed(elapsed)}</span>}
          {isProcessing && onCancel && (
            <button className={styles.cancelBtn} onClick={onCancel}>
              {STRINGS.CANCEL}
            </button>
          )}
        </div>
      </div>

      <div className={styles.progressBar}>
        <div
          className={styles.progressFill}
          style={{ width: `${((doneCount + errorCount) / total) * 100}%` }}
        />
      </div>

      <div className={styles.list}>
        {jobs.map((job, i) => (
          <JobRow key={i} job={job} />
        ))}
      </div>

      {isComplete && (
        <div className={styles.footer}>
          <button
            className={styles.viewBtn}
            onClick={() => navigate('/reading-list')}
          >
            {STRINGS.BATCH_VIEW_READING_LIST}
          </button>
          <button className={styles.dismissBtn} onClick={onDismiss}>
            {STRINGS.BATCH_DISMISS}
          </button>
        </div>
      )}
    </div>
  )
}
