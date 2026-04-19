import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { STRINGS } from '../constants/strings'
import styles from './Settings.module.css'
import { useNotification } from '../hooks/useNotification'
import { apiFetch } from '../apiFetch.js'
import { useUsers } from '../hooks/useUsers'
import { formatRelativeDate } from '../utils/relativeDate'

function UsersTab({ currentUserId }) {
  const { users, loading: usersLoading, createUser, deleteUser } = useUsers()
  const [newUsername, setNewUsername] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newRole, setNewRole] = useState('USER')
  const [createError, setCreateError] = useState(null)
  const [createLoading, setCreateLoading] = useState(false)

  async function handleCreateUser(e) {
    e.preventDefault()
    if (!newUsername.trim() || !newPassword.trim()) return
    setCreateError(null)
    setCreateLoading(true)
    try {
      await createUser(newUsername.trim(), newPassword, newRole)
      setNewUsername('')
      setNewPassword('')
      setNewRole('USER')
    } catch (err) {
      setCreateError(err.message === 'taken' ? STRINGS.USERS_CREATE_ERROR_TAKEN : STRINGS.USERS_CREATE_ERROR_GENERIC)
    } finally {
      setCreateLoading(false)
    }
  }

  async function handleDeleteUser(id, uname) {
    if (!window.confirm(STRINGS.USERS_DELETE_CONFIRM.replace('{username}', uname))) return
    try {
      await deleteUser(id)
    } catch {
      alert(STRINGS.USERS_DELETE_ERROR)
    }
  }

  return (
    <>
      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>{STRINGS.USERS_HEADING}</h3>
        <p className={styles.sectionDesc}>{STRINGS.USERS_SUBHEADING}</p>

        {usersLoading ? (
          <p className={styles.providerHint}>{STRINGS.USERS_LOADING}</p>
        ) : users.length === 0 ? (
          <p className={styles.providerHint}>{STRINGS.USERS_EMPTY}</p>
        ) : (
          <div className={styles.userList}>
            {users.map(u => (
              <div key={u.id} className={styles.userRow}>
                <div className={styles.userMeta}>
                  <span className={styles.userUsername}>{u.username}</span>
                  <span className={styles.userBadge}>{u.role}</span>
                  {u.createdAt && (
                    <span className={styles.userDate}>{formatRelativeDate(u.createdAt)}</span>
                  )}
                </div>
                {u.id !== currentUserId && !u.mainAdmin && (
                  <button
                    className={styles.apiKeyRemoveBtn}
                    onClick={() => handleDeleteUser(u.id, u.username)}
                  >
                    {STRINGS.USERS_DELETE_BUTTON}
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>{STRINGS.USERS_CREATE_HEADING}</h3>
        <form onSubmit={handleCreateUser}>
          <div className={styles.apiKeyGroup}>
            <label className={styles.apiKeyLabel}>{STRINGS.USERS_USERNAME_LABEL}</label>
            <div className={styles.apiKeyRow}>
              <input
                type="text"
                className={styles.apiKeyInput}
                value={newUsername}
                onChange={e => setNewUsername(e.target.value)}
                placeholder={STRINGS.USERS_USERNAME_PLACEHOLDER}
                autoComplete="off"
              />
            </div>
          </div>
          <div className={`${styles.apiKeyGroup} ${styles.apiKeyGroupSpaced}`}>
            <label className={styles.apiKeyLabel}>{STRINGS.USERS_PASSWORD_LABEL}</label>
            <div className={styles.apiKeyRow}>
              <input
                type="password"
                className={styles.apiKeyInput}
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
                placeholder={STRINGS.USERS_PASSWORD_PLACEHOLDER}
                autoComplete="new-password"
              />
            </div>
          </div>
          <div className={`${styles.apiKeyGroup} ${styles.apiKeyGroupSpaced}`}>
            <label className={styles.apiKeyLabel}>{STRINGS.USERS_ROLE_LABEL}</label>
            <div className={styles.apiKeyRow}>
              <select
                className={styles.apiKeyInput}
                value={newRole}
                onChange={e => setNewRole(e.target.value)}
              >
                <option value="USER">{STRINGS.USERS_ROLE_USER}</option>
                <option value="ADMIN">{STRINGS.USERS_ROLE_ADMIN}</option>
              </select>
            </div>
          </div>
          {createError && (
            <p className={styles.createError}>{createError}</p>
          )}
          <div className={styles.createSubmit}>
            <button
              type="submit"
              className={styles.apiKeySaveBtn}
              disabled={!newUsername.trim() || !newPassword.trim() || createLoading}
            >
              {STRINGS.USERS_CREATE_BUTTON}
            </button>
          </div>
        </form>
      </section>
    </>
  )
}

const LANGUAGE_OPTIONS = [
  { value: 'en', label: 'SETTINGS_LANGUAGE_EN' },
  { value: 'pt-BR', label: 'SETTINGS_LANGUAGE_PT_BR' },
]

export function Settings({ settings, onUpdateSetting, onUpdateSettings, isAdmin = false, currentUserId = null }) {
  const { i18n } = useTranslation()

  const LENGTH_OPTIONS = [
    { value: 'shorter', label: STRINGS.SETTINGS_LENGTH_SHORT, description: STRINGS.SETTINGS_LENGTH_SHORT_DESC },
    { value: 'default', label: STRINGS.SETTINGS_LENGTH_DEFAULT, description: STRINGS.SETTINGS_LENGTH_DEFAULT_DESC },
    { value: 'longer', label: STRINGS.SETTINGS_LENGTH_LONG, description: STRINGS.SETTINGS_LENGTH_LONG_DESC },
  ]

  const BASE_TABS = [
    { id: 'summarization', label: STRINGS.SETTINGS_TAB_SUMMARIZATION },
    { id: 'integrations', label: STRINGS.SETTINGS_TAB_INTEGRATIONS },
    { id: 'preferences', label: STRINGS.SETTINGS_TAB_PREFERENCES },
  ]
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const TABS = isAdmin
    ? [...BASE_TABS, { id: 'users', label: STRINGS.SETTINGS_TAB_USERS }]
    : BASE_TABS
  const TAB_IDS = TABS.map(t => t.id)
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
  const [anthropicKeyDraft, setAnthropicKeyDraft] = useState('')
  const [anthropicKeySaved, setAnthropicKeySaved] = useState(false)
  const [readeckUrlDraft, setReadeckUrlDraft] = useState('')
  const [readeckKeyDraft, setReadeckKeyDraft] = useState('')
  const [readeckSaved, setReadeckSaved] = useState(false)
  const [webhookUrlDraft, setWebhookUrlDraft] = useState('')
  const [webhookSaved, setWebhookSaved] = useState(false)
  const [promptDraft, setPromptDraft] = useState('')
  const [promptSaved, setPromptSaved] = useState(false)
  const [modelsLoading, setModelsLoading] = useState(true)
  const [modelsError, setModelsError] = useState(false)
  const [appVersion, setAppVersion] = useState(null)
  const { supported, permission, requestPermission } = useNotification()

  useEffect(() => {
    apiFetch('/api/version')
      .then(res => res.ok ? res.json() : null)
      .then(data => { if (data) setAppVersion(data.version) })
      .catch(() => {})
  }, [])

  useEffect(() => {
    // modelsLoading is already initialized to true
    apiFetch('/api/models')
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data) {
          setProviders(data.providers)
          setDefaultModel(data.defaultModel)
        }
      })
      .catch(() => { setModelsError(true) })
      .finally(() => setModelsLoading(false))
  }, [])

  // Sync draft fields with settings (adjust state during render)
  const [prevOpenaiKey, setPrevOpenaiKey] = useState(settings.openaiApiKey)
  if (prevOpenaiKey !== settings.openaiApiKey) {
    setPrevOpenaiKey(settings.openaiApiKey)
    if (settings.openaiApiKey) setKeyDraft(settings.openaiApiKey)
  }

  const [prevAnthropicKey, setPrevAnthropicKey] = useState(settings.anthropicApiKey)
  if (prevAnthropicKey !== settings.anthropicApiKey) {
    setPrevAnthropicKey(settings.anthropicApiKey)
    if (settings.anthropicApiKey) setAnthropicKeyDraft(settings.anthropicApiKey)
  }

  const [prevReadeckKey, setPrevReadeckKey] = useState(settings.readeckApiKey)
  const [prevReadeckUrl, setPrevReadeckUrl] = useState(settings.readeckUrl)
  if (prevReadeckKey !== settings.readeckApiKey || prevReadeckUrl !== settings.readeckUrl) {
    setPrevReadeckKey(settings.readeckApiKey)
    setPrevReadeckUrl(settings.readeckUrl)
    if (settings.readeckApiKey) setReadeckKeyDraft(settings.readeckApiKey)
    if (settings.readeckUrl) setReadeckUrlDraft(settings.readeckUrl)
  }

  const [prevWebhookUrl, setPrevWebhookUrl] = useState(settings.webhookUrl)
  if (prevWebhookUrl !== settings.webhookUrl) {
    setPrevWebhookUrl(settings.webhookUrl)
    if (settings.webhookUrl) setWebhookUrlDraft(settings.webhookUrl)
  }

  const [prevCustomPrompt, setPrevCustomPrompt] = useState(settings.customPrompt)
  if (prevCustomPrompt !== settings.customPrompt) {
    setPrevCustomPrompt(settings.customPrompt)
    setPromptDraft(settings.customPrompt || '')
  }

  const selectedModel = settings.model || defaultModel
  const hasOpenAiKey = settings.openaiApiKey != null && settings.openaiApiKey !== ''
  const hasAnthropicKey = settings.anthropicApiKey != null && settings.anthropicApiKey !== ''

  // Filter out cloud providers when no key is configured
  const visibleProviders = providers.filter(p =>
    p.id === 'ollama' || (p.id === 'openai' && hasOpenAiKey) || (p.id === 'anthropic' && hasAnthropicKey)
  )

  function handleSaveKey() {
    if (!keyDraft.trim()) return
    onUpdateSetting('openaiApiKey', keyDraft.trim())
    setKeySaved(true)
    setTimeout(() => setKeySaved(false), 2000)
    apiFetch('/api/models').then(r => r.ok ? r.json() : null).then(data => {
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
    apiFetch('/api/models').then(r => r.ok ? r.json() : null).then(data => {
      if (data) setProviders(data.providers)
    }).catch(() => {})
  }

  function handleSaveAnthropicKey() {
    if (!anthropicKeyDraft.trim()) return
    onUpdateSetting('anthropicApiKey', anthropicKeyDraft.trim())
    setAnthropicKeySaved(true)
    setTimeout(() => setAnthropicKeySaved(false), 2000)
    apiFetch('/api/models').then(r => r.ok ? r.json() : null).then(data => {
      if (data) setProviders(data.providers)
    }).catch(() => {})
  }

  function handleRemoveAnthropicKey() {
    if (!window.confirm(STRINGS.CONFIRM_REMOVE_ANTHROPIC)) return
    onUpdateSetting('anthropicApiKey', '')
    setAnthropicKeyDraft('')
    // If the user had an Anthropic model selected, reset to default
    if (selectedModel && selectedModel.startsWith('claude-')) {
      onUpdateSetting('model', defaultModel)
    }
    apiFetch('/api/models').then(r => r.ok ? r.json() : null).then(data => {
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
      alert(STRINGS.SETTINGS_READECK_SAVE_ERROR)
    }
  }

  async function handleRemoveReadeck() {
    if (!window.confirm(STRINGS.CONFIRM_REMOVE_READECK)) return
    await onUpdateSettings({ readeckUrl: '', readeckApiKey: '' })
    setReadeckUrlDraft('')
    setReadeckKeyDraft('')
  }

  const hasWebhookUrl = settings.webhookUrl != null && settings.webhookUrl !== ''

  async function handleSaveWebhook() {
    if (!webhookUrlDraft.trim()) return
    try {
      await onUpdateSettings({ webhookUrl: webhookUrlDraft.trim() })
      setWebhookSaved(true)
      setTimeout(() => setWebhookSaved(false), 2000)
    } catch {
      alert(STRINGS.SETTINGS_WEBHOOK_SAVE_ERROR)
    }
  }

  async function handleRemoveWebhook() {
    if (!window.confirm(STRINGS.CONFIRM_REMOVE_WEBHOOK)) return
    await onUpdateSettings({ webhookUrl: '' })
    setWebhookUrlDraft('')
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

          {(visibleProviders.length > 0 || modelsLoading || modelsError) && (
            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_MODEL_HEADING}</h3>
              <p className={styles.sectionDesc}>{STRINGS.SETTINGS_MODEL_SUBHEADING}</p>

              {modelsLoading && visibleProviders.length === 0 && (
                <p className={styles.providerHint}>{STRINGS.SETTINGS_MODELS_LOADING}</p>
              )}
              {modelsError && !modelsLoading && (
                <p className={styles.modelsError}>{STRINGS.SETTINGS_MODELS_ERROR}</p>
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

              {!hasOpenAiKey && !hasAnthropicKey && (
                <p className={styles.providerHint}>
                  Add an OpenAI or Anthropic API key in the Integrations tab to unlock cloud models.
                </p>
              )}
            </section>
          )}

          <section className={styles.section}>
            <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_PROMPT_HEADING}</h3>
            <p className={styles.sectionDesc}>{STRINGS.SETTINGS_PROMPT_SUBHEADING}</p>

            <div className={styles.apiKeyGroup}>
              <textarea
                className={styles.promptTextarea}
                value={promptDraft}
                onChange={e => setPromptDraft(e.target.value)}
                placeholder={STRINGS.SETTINGS_PROMPT_PLACEHOLDER}
                rows={6}
              />
              <div className={styles.apiKeyRow}>
                <button
                  className={styles.apiKeySaveBtn}
                  onClick={() => {
                    onUpdateSetting('customPrompt', promptDraft.trim() || '')
                    setPromptSaved(true)
                    setTimeout(() => setPromptSaved(false), 2000)
                  }}
                  disabled={promptDraft === (settings.customPrompt || '')}
                >
                  {promptSaved ? STRINGS.SETTINGS_PROMPT_SAVED : STRINGS.SETTINGS_PROMPT_SAVE}
                </button>
                {(settings.customPrompt != null && settings.customPrompt !== '') && (
                  <button
                    className={styles.apiKeyRemoveBtn}
                    onClick={() => {
                      if (!window.confirm(STRINGS.SETTINGS_PROMPT_CONFIRM_RESET)) return
                      onUpdateSetting('customPrompt', '')
                      setPromptDraft('')
                    }}
                  >
                    {STRINGS.SETTINGS_PROMPT_RESET}
                  </button>
                )}
              </div>
            </div>
          </section>
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

            <div className={styles.apiKeyGroup}>
              <label className={styles.apiKeyLabel}>{STRINGS.SETTINGS_ANTHROPIC_KEY_LABEL}</label>
              <div className={styles.apiKeyRow}>
                <input
                  type="password"
                  className={styles.apiKeyInput}
                  value={anthropicKeyDraft}
                  onChange={e => setAnthropicKeyDraft(e.target.value)}
                  placeholder={STRINGS.SETTINGS_ANTHROPIC_KEY_PLACEHOLDER}
                />
                <button
                  className={styles.apiKeySaveBtn}
                  onClick={handleSaveAnthropicKey}
                  disabled={!anthropicKeyDraft.trim() || anthropicKeyDraft === settings.anthropicApiKey}
                >
                  {anthropicKeySaved ? STRINGS.SETTINGS_API_KEY_SAVED : (hasAnthropicKey ? STRINGS.SETTINGS_API_KEY_UPDATE : STRINGS.SETTINGS_API_KEY_SAVE)}
                </button>
                {hasAnthropicKey && (
                  <button className={styles.apiKeyRemoveBtn} onClick={handleRemoveAnthropicKey}>
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
          <section className={styles.section}>
            <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_WEBHOOK_HEADING}</h3>
            <p className={styles.sectionDesc}>{STRINGS.SETTINGS_WEBHOOK_SUBHEADING}</p>

            <div className={styles.apiKeyGroup}>
              <label className={styles.apiKeyLabel}>{STRINGS.SETTINGS_WEBHOOK_URL_LABEL}</label>
              <div className={styles.apiKeyRow}>
                <input
                  type="url"
                  className={styles.apiKeyInput}
                  value={webhookUrlDraft}
                  onChange={e => setWebhookUrlDraft(e.target.value)}
                  placeholder={STRINGS.SETTINGS_WEBHOOK_URL_PLACEHOLDER}
                />
                <button
                  className={styles.apiKeySaveBtn}
                  onClick={handleSaveWebhook}
                  disabled={!webhookUrlDraft.trim() || webhookUrlDraft.trim() === settings.webhookUrl}
                >
                  {webhookSaved ? STRINGS.SETTINGS_API_KEY_SAVED : (hasWebhookUrl ? STRINGS.SETTINGS_API_KEY_UPDATE : STRINGS.SETTINGS_API_KEY_SAVE)}
                </button>
                {hasWebhookUrl && (
                  <button className={styles.apiKeyRemoveBtn} onClick={handleRemoveWebhook}>
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
        <>
          <section className={styles.section}>
            <h3 className={styles.sectionTitle}>{STRINGS.SETTINGS_LANGUAGE}</h3>
            <p className={styles.sectionDesc}>{STRINGS.SETTINGS_LANGUAGE_SUBHEADING}</p>

            <div className={styles.options}>
              {LANGUAGE_OPTIONS.map(opt => (
                <label
                  key={opt.value}
                  className={`${styles.option} ${i18n.language === opt.value ? styles.optionSelected : ''}`}
                >
                  <input
                    type="radio"
                    name="language"
                    value={opt.value}
                    checked={i18n.language === opt.value}
                    onChange={() => {
                      i18n.changeLanguage(opt.value)
                      localStorage.setItem('briefen-language', opt.value)
                    }}
                    className={styles.radio}
                  />
                  <div className={styles.optionContent}>
                    <span className={styles.optionLabel}>{STRINGS[opt.label]}</span>
                  </div>
                </label>
              ))}
            </div>
          </section>

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
        </>
      )}

      {/* ── Users tab (admin only) ── */}
      {isAdmin && tab === 'users' && <UsersTab currentUserId={currentUserId} />}

      {appVersion && (
        <p className={styles.versionFooter}>{STRINGS.SETTINGS_VERSION_PREFIX} v{appVersion}</p>
      )}
    </div>
  )
}
