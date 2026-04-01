import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { STRINGS } from '../constants/strings'
import styles from './Settings.module.css'
import { useNotification } from '../hooks/useNotification'

const LENGTH_OPTIONS = [
  { value: 'shorter', label: STRINGS.SETTINGS_LENGTH_SHORT, description: STRINGS.SETTINGS_LENGTH_SHORT_DESC },
  { value: 'default', label: STRINGS.SETTINGS_LENGTH_DEFAULT, description: STRINGS.SETTINGS_LENGTH_DEFAULT_DESC },
  { value: 'longer', label: STRINGS.SETTINGS_LENGTH_LONG, description: STRINGS.SETTINGS_LENGTH_LONG_DESC },
]

const TABS = [
  { id: 'summarization', label: STRINGS.SETTINGS_TAB_SUMMARIZATION },
  { id: 'integrations', label: STRINGS.SETTINGS_TAB_INTEGRATIONS },
  { id: 'preferences', label: STRINGS.SETTINGS_TAB_PREFERENCES },
]

const TAB_IDS = TABS.map(t => t.id)

export function Settings({ settings, onUpdateSetting, onUpdateSettings }) {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const initialTab = TAB_IDS.includes(searchParams.get('tab')) ? searchParams.get('tab') : 'summarization'
  const [tab, setTab] = useState(initialTab)

  function handleSetTab(t) {
    setTab(t)
    setSearchParams(t === 'summarization' ? {} : { tab: t }, { replace: true })
  }
  const [providers, setProviders] = useState([])
  const [defaultModel, setDefaultModel] = useState(null)
  const [keyDraft, setKeyDraft] = useState('')
  const [keySaved, setKeySaved] = useState(false)
  const [readeckUrlDraft, setReadeckUrlDraft] = useState('')
  const [readeckKeyDraft, setReadeckKeyDraft] = useState('')
  const [readeckSaved, setReadeckSaved] = useState(false)
  const [modelsLoading, setModelsLoading] = useState(true)
  const { supported, permission, requestPermission } = useNotification()

  useEffect(() => {
    // modelsLoading is already initialized to true
    fetch('/api/models')
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data) {
          setProviders(data.providers)
          setDefaultModel(data.defaultModel)
        }
      })
      .catch(() => {})
      .finally(() => setModelsLoading(false))
  }, [])

  // Sync draft fields with settings (adjust state during render)
  const [prevOpenaiKey, setPrevOpenaiKey] = useState(settings.openaiApiKey)
  if (prevOpenaiKey !== settings.openaiApiKey) {
    setPrevOpenaiKey(settings.openaiApiKey)
    if (settings.openaiApiKey) setKeyDraft(settings.openaiApiKey)
  }

  const [prevReadeckKey, setPrevReadeckKey] = useState(settings.readeckApiKey)
  const [prevReadeckUrl, setPrevReadeckUrl] = useState(settings.readeckUrl)
  if (prevReadeckKey !== settings.readeckApiKey || prevReadeckUrl !== settings.readeckUrl) {
    setPrevReadeckKey(settings.readeckApiKey)
    setPrevReadeckUrl(settings.readeckUrl)
    if (settings.readeckApiKey) setReadeckKeyDraft(settings.readeckApiKey)
    if (settings.readeckUrl) setReadeckUrlDraft(settings.readeckUrl)
  }

  const selectedModel = settings.model || defaultModel
  const hasOpenAiKey = settings.openaiApiKey != null && settings.openaiApiKey !== ''

  // Filter out OpenAI providers when no key is configured
  const visibleProviders = providers.filter(p =>
    p.id === 'ollama' || (p.id === 'openai' && hasOpenAiKey)
  )

  function handleSaveKey() {
    if (!keyDraft.trim()) return
    onUpdateSetting('openaiApiKey', keyDraft.trim())
    setKeySaved(true)
    setTimeout(() => setKeySaved(false), 2000)
    fetch('/api/models').then(r => r.json()).then(data => {
      if (data) setProviders(data.providers)
    }).catch(() => {})
  }

  function handleRemoveKey() {
    if (!window.confirm(STRINGS.CONFIRM_REMOVE_OPENAI)) return
    onUpdateSetting('openaiApiKey', '')
    setKeyDraft('')
    // If the user had an OpenAI model selected, reset to default
    if (selectedModel && (selectedModel.startsWith('gpt-') || selectedModel.startsWith('o'))) {
      onUpdateSetting('model', defaultModel)
    }
    fetch('/api/models').then(r => r.json()).then(data => {
      if (data) setProviders(data.providers)
    }).catch(() => {})
  }

  const hasReadeck = settings.readeckApiKey != null && settings.readeckApiKey !== ''
      && settings.readeckUrl != null && settings.readeckUrl !== ''

  async function handleSaveReadeck() {
    if (!readeckUrlDraft.trim() || !readeckKeyDraft.trim()) return
    try {
      await onUpdateSettings({
        readeckUrl: readeckUrlDraft.trim().replace(/\/+$/, ''),
        readeckApiKey: readeckKeyDraft.trim(),
      })
      setReadeckSaved(true)
      setTimeout(() => setReadeckSaved(false), 2000)
    } catch {
      alert('Failed to save Readeck settings. Please check the URL and try again.')
    }
  }

  async function handleRemoveReadeck() {
    if (!window.confirm(STRINGS.CONFIRM_REMOVE_READECK)) return
    await onUpdateSettings({ readeckUrl: '', readeckApiKey: '' })
    setReadeckUrlDraft('')
    setReadeckKeyDraft('')
  }

  return (
    <div className={styles.container}>
      <button className={styles.backBtn} onClick={() => navigate('/')}>
        ← {STRINGS.SETTINGS_BACK}
      </button>

      <h2 className={styles.heading}>{STRINGS.SETTINGS_TITLE}</h2>

      {/* Tab bar */}
      <div className={styles.tabBar}>
        {TABS.map(t => (
          <button
            key={t.id}
            className={`${styles.tabBtn} ${tab === t.id ? styles.tabBtnActive : ''}`}
            onClick={() => handleSetTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* ── Summarization tab ── */}
      {tab === 'summarization' && (
        <>
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

          {(visibleProviders.length > 0 || modelsLoading) && (
            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_MODEL_HEADING}</h3>
              <p className={styles.sectionDesc}>{STRINGS.SETTINGS_MODEL_SUBHEADING}</p>

              {modelsLoading && visibleProviders.length === 0 && (
                <p className={styles.providerHint}>{STRINGS.SETTINGS_MODELS_LOADING}</p>
              )}

              {visibleProviders.map(provider => (
                <div key={provider.id} className={styles.providerGroup}>
                  <div className={styles.providerHeader}>
                    <span className={styles.providerName}>{provider.name}</span>
                  </div>

                  <div className={styles.options}>
                    {provider.models.map(m => (
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
                </div>
              ))}

              {!hasOpenAiKey && (
                <p className={styles.providerHint}>
                  Add an OpenAI API key in the Integrations tab to unlock cloud models.
                </p>
              )}
            </section>
          )}
        </>
      )}

      {/* ── Integrations tab ── */}
      {tab === 'integrations' && (
        <>
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
                <button
                  className={styles.apiKeySaveBtn}
                  onClick={handleSaveKey}
                  disabled={!keyDraft.trim() || keyDraft === settings.openaiApiKey}
                >
                  {keySaved ? STRINGS.SETTINGS_API_KEY_SAVED : (hasOpenAiKey ? STRINGS.SETTINGS_API_KEY_UPDATE : STRINGS.SETTINGS_API_KEY_SAVE)}
                </button>
                {hasOpenAiKey && (
                  <button className={styles.apiKeyRemoveBtn} onClick={handleRemoveKey}>
                    {STRINGS.SETTINGS_API_KEY_REMOVE}
                  </button>
                )}
              </div>
            </div>
          </section>

          <section className={styles.section}>
            <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_READECK_HEADING}</h3>
            <p className={styles.sectionDesc}>{STRINGS.SETTINGS_READECK_SUBHEADING}</p>

            <div className={styles.apiKeyGroup}>
              <label className={styles.apiKeyLabel}>{STRINGS.SETTINGS_READECK_URL_LABEL}</label>
              <div className={styles.apiKeyRow}>
                <input
                  type="url"
                  className={styles.apiKeyInput}
                  value={readeckUrlDraft}
                  onChange={e => setReadeckUrlDraft(e.target.value)}
                  placeholder={STRINGS.SETTINGS_READECK_URL_PLACEHOLDER}
                />
              </div>
            </div>

            <div className={styles.apiKeyGroup}>
              <label className={styles.apiKeyLabel}>{STRINGS.SETTINGS_READECK_KEY_LABEL}</label>
              <div className={styles.apiKeyRow}>
                <input
                  type="password"
                  className={styles.apiKeyInput}
                  value={readeckKeyDraft}
                  onChange={e => setReadeckKeyDraft(e.target.value)}
                  placeholder={STRINGS.SETTINGS_READECK_KEY_PLACEHOLDER}
                />
                <button
                  className={styles.apiKeySaveBtn}
                  onClick={handleSaveReadeck}
                  disabled={!readeckUrlDraft.trim() || !readeckKeyDraft.trim()
                    || (readeckKeyDraft === settings.readeckApiKey && readeckUrlDraft.trim().replace(/\/+$/, '') === settings.readeckUrl)}
                >
                  {readeckSaved ? STRINGS.SETTINGS_API_KEY_SAVED : (hasReadeck ? STRINGS.SETTINGS_API_KEY_UPDATE : STRINGS.SETTINGS_API_KEY_SAVE)}
                </button>
                {hasReadeck && (
                  <button className={styles.apiKeyRemoveBtn} onClick={handleRemoveReadeck}>
                    {STRINGS.SETTINGS_API_KEY_REMOVE}
                  </button>
                )}
              </div>
            </div>
          </section>
        </>
      )}

      {/* ── Preferences tab ── */}
      {tab === 'preferences' && (
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
      )}
    </div>
  )
}
