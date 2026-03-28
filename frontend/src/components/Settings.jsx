import { STRINGS } from '../constants/strings'
import styles from './Settings.module.css'

const LENGTH_OPTIONS = [
  { value: 'shorter', label: STRINGS.SETTINGS_LENGTH_SHORT, description: STRINGS.SETTINGS_LENGTH_SHORT_DESC },
  { value: 'default', label: STRINGS.SETTINGS_LENGTH_DEFAULT, description: STRINGS.SETTINGS_LENGTH_DEFAULT_DESC },
  { value: 'longer', label: STRINGS.SETTINGS_LENGTH_LONG, description: STRINGS.SETTINGS_LENGTH_LONG_DESC },
]

export function Settings({ settings, onUpdateSetting, onBack }) {
  return (
    <div className={styles.container}>
      <button className={styles.backBtn} onClick={onBack}>
        ← {STRINGS.SETTINGS_BACK}
      </button>

      <h2 className={styles.heading}>{STRINGS.SETTINGS_TITLE}</h2>

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_LENGTH_HEADING}</h3>
        <p className={styles.sectionDesc}>{STRINGS.SETTINGS_LENGTH_SUBHEADING}</p>

        <div className={styles.options}>
          {LENGTH_OPTIONS.map(opt => (
            <label
              key={opt.value}
              className={`${styles.option} ${settings.defaultLength === opt.value ? styles.optionSelected : ''}`}
            >
              <input
                type="radio"
                name="defaultLength"
                value={opt.value}
                checked={settings.defaultLength === opt.value}
                onChange={() => onUpdateSetting('defaultLength', opt.value)}
                className={styles.radio}
              />
              <div className={styles.optionContent}>
                <span className={styles.optionLabel}>{opt.label}</span>
                <span className={styles.optionDesc}>{opt.description}</span>
              </div>
            </label>
          ))}
        </div>
      </section>
    </div>
  )
}
