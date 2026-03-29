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
  const [providers, setProviders] = useState([])
  const [defaultModel, setDefaultModel] = useState(null)
  const [keyDraft, setKeyDraft] = useState('')
  const [keySaved, setKeySaved] = useState(false)
  const { supported, permission, requestPermission } = useNotification()

  useEffect(() => {
    fetch('/api/models')
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data) {
          setProviders(data.providers)
          setDefaultModel(data.defaultModel)
        }
      })
      .catch(() => {})
  }, [])

  // Sync key draft with saved setting
  useEffect(() => {
    if (settings.openaiApiKey) {
      setKeyDraft(settings.openaiApiKey)
    }
  }, [settings.openaiApiKey])

  const selectedModel = settings.model || defaultModel
  const hasOpenAiKey = settings.openaiApiKey != null && settings.openaiApiKey !== ''

  function handleSaveKey() {
    if (!keyDraft.trim()) return
    onUpdateSetting('openaiApiKey', keyDraft.trim())
    setKeySaved(true)
    setTimeout(() => setKeySaved(false), 2000)
    // Refresh providers to show OpenAI as configured
    fetch('/api/models').then(r => r.json()).then(data => {
      if (data) setProviders(data.providers)
    }).catch(() => {})
  }

  function handleRemoveKey() {
    onUpdateSetting('openaiApiKey', '')
    setKeyDraft('')
    // Refresh providers
    fetch('/api/models').then(r => r.json()).then(data => {
      if (data) setProviders(data.providers)
    }).catch(() => {})
  }

  return (
    <div className={styles.container}>
      <button className={styles.backBtn} onClick={onBack}>
        ← {STRINGS.SETTINGS_BACK}
      </button>

      <h2 className={styles.heading}>{STRINGS.SETTINGS_TITLE}</h2>

      {/* Summary length */}
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

      {/* API Keys */}
      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_API_KEYS_HEADING}</h3>
        <p className={styles.sectionDesc}>{STRINGS.SETTINGS_API_KEYS_SUBHEADING}</p>

        <div className={styles.apiKeyGroup}>
          <label className={styles.apiKeyLabel}>{STRINGS.SETTINGS_OPENAI_KEY_LABEL}</label>
          <div className={styles.apiKeyRow}>
            <input
              type="password"
              className={styles.apiKeyInput}
              value={keyDraft}
              onChange={e => setKeyDraft(e.target.value)}
              placeholder={STRINGS.SETTINGS_OPENAI_KEY_PLACEHOLDER}
            />
            {hasOpenAiKey ? (
              <button className={styles.apiKeyRemoveBtn} onClick={handleRemoveKey}>
                {STRINGS.SETTINGS_API_KEY_REMOVE}
              </button>
            ) : (
              <button
                className={styles.apiKeySaveBtn}
                onClick={handleSaveKey}
                disabled={!keyDraft.trim()}
              >
                {keySaved ? STRINGS.SETTINGS_API_KEY_SAVED : STRINGS.SETTINGS_API_KEY_SAVE}
              </button>
            )}
          </div>
        </div>
      </section>

      {/* Model selection grouped by provider */}
      {providers.length > 0 && (
        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_MODEL_HEADING}</h3>
          <p className={styles.sectionDesc}>{STRINGS.SETTINGS_MODEL_SUBHEADING}</p>

          {providers.map(provider => (
            <div key={provider.id} className={styles.providerGroup}>
              <div className={styles.providerHeader}>
                <span className={styles.providerName}>{provider.name}</span>
                {!provider.configured && (
                  <span className={styles.providerBadge}>{STRINGS.SETTINGS_API_KEY_ADD}</span>
                )}
              </div>

              <div className={styles.options}>
                {provider.models.map(m => {
                  const disabled = !provider.configured
                  return (
                    <label
                      key={m.id}
                      className={`${styles.option} ${selectedModel === m.id ? styles.optionSelected : ''} ${disabled ? styles.optionDisabled : ''}`}
                    >
                      <input
                        type="radio"
                        name="model"
                        value={m.id}
                        checked={selectedModel === m.id}
                        onChange={() => onUpdateSetting('model', m.id)}
                        className={styles.radio}
                        disabled={disabled}
                      />
                      <div className={styles.optionContent}>
                        <span className={styles.optionLabel}>{m.name}</span>
                        <span className={styles.optionDesc}>{m.description}</span>
                      </div>
                    </label>
                  )
                })}
              </div>
            </div>
          ))}
        </section>
      )}

      {/* Notifications */}
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
