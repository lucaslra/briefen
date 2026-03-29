import { useState, useEffect } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './Settings.module.css'

const LENGTH_OPTIONS = [
  { value: 'shorter', label: STRINGS.SETTINGS_LENGTH_SHORT, description: STRINGS.SETTINGS_LENGTH_SHORT_DESC },
  { value: 'default', label: STRINGS.SETTINGS_LENGTH_DEFAULT, description: STRINGS.SETTINGS_LENGTH_DEFAULT_DESC },
  { value: 'longer', label: STRINGS.SETTINGS_LENGTH_LONG, description: STRINGS.SETTINGS_LENGTH_LONG_DESC },
]

export function Settings({ settings, onUpdateSetting, onBack }) {
  const [models, setModels] = useState([])
  const [defaultModel, setDefaultModel] = useState(null)

  useEffect(() => {
    fetch('/api/models')
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data) {
          setModels(data.models)
          setDefaultModel(data.defaultModel)
        }
      })
      .catch(() => {})
  }, [])

  // The selected model: use the user's setting, or fall back to the server default
  const selectedModel = settings.model || defaultModel

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

      {models.length > 0 && (
        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_MODEL_HEADING}</h3>
          <p className={styles.sectionDesc}>{STRINGS.SETTINGS_MODEL_SUBHEADING}</p>

          <div className={styles.options}>
            {models.map(m => (
              <label
                key={m.id}
                className={`${styles.option} ${selectedModel === m.id ? styles.optionSelected : ''}`}
              >
                <input
                  type="radio"
                  name="model"
                  value={m.id}
                  checked={selectedModel === m.id}
                  onChange={() => onUpdateSetting('model', m.id)}
                  className={styles.radio}
                />
                <div className={styles.optionContent}>
                  <span className={styles.optionLabel}>{m.name}</span>
                  <span className={styles.optionDesc}>{m.description}</span>
                </div>
              </label>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
