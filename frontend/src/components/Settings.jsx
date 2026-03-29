import { useState, useEffect } from 'react'
import { STRINGS } from '../constants/strings'
import styles from './Settings.module.css'
import { useNotification } from '../hooks/useNotification'

const LENGTH_OPTIONS = [
  { value: 'shorter', label: STRINGS.SETTINGS_LENGTH_SHORT, description: STRINGS.SETTINGS_LENGTH_SHORT_DESC },
  { value: 'default', label: STRINGS.SETTINGS_LENGTH_DEFAULT, description: STRINGS.SETTINGS_LENGTH_DEFAULT_DESC },
  { value: 'longer', label: STRINGS.SETTINGS_LENGTH_LONG, description: STRINGS.SETTINGS_LENGTH_LONG_DESC },
]

export function Settings({ settings, onUpdateSetting, onBack }) {
  const [models, setModels] = useState([])
  const [defaultModel, setDefaultModel] = useState(null)
  const { supported, permission, requestPermission } = useNotification()

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

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_NOTIFICATIONS_HEADING}</h3>
        <p className={styles.sectionDesc}>{STRINGS.SETTINGS_NOTIFICATIONS_SUBHEADING}</p>

        {!supported ? (
          <p className={styles.toggleDenied}>{STRINGS.SETTINGS_NOTIFICATIONS_UNSUPPORTED}</p>
        ) : permission === 'denied' ? (
          <p className={styles.toggleDenied}>{STRINGS.SETTINGS_NOTIFICATIONS_DENIED}</p>
        ) : (
          <label
            className={`${styles.toggle} ${settings.notificationsEnabled && permission === 'granted' ? styles.toggleChecked : ''}`}
          >
            <input
              type="checkbox"
              className={styles.toggleInput}
              checked={settings.notificationsEnabled && permission === 'granted'}
              onChange={async (e) => {
                if (e.target.checked) {
                  const result = await requestPermission()
                  if (result === 'granted') {
                    onUpdateSetting('notificationsEnabled', true)
                  }
                } else {
                  onUpdateSetting('notificationsEnabled', false)
                }
              }}
            />
            <span className={styles.toggleSwitch} />
            <span className={styles.toggleText}>{STRINGS.SETTINGS_NOTIFICATIONS_ENABLE}</span>
          </label>
        )}
      </section>
    </div>
  )
}
