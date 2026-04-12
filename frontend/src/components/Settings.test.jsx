import { describe, it, expect, vi, beforeAll, afterAll, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Settings } from './Settings'

vi.mock('../hooks/useNotification', () => ({
  useNotification: () => ({
    supported: true,
    permission: 'default',
    requestPermission: vi.fn().mockResolvedValue('granted'),
  }),
}))

vi.mock('../hooks/useUsers', () => ({
  useUsers: () => ({
    users: [
      { id: '1', username: 'admin', role: 'ADMIN', mainAdmin: true, createdAt: new Date().toISOString() },
      { id: '2', username: 'user1', role: 'USER', mainAdmin: false, createdAt: new Date().toISOString() },
    ],
    loading: false,
    createUser: vi.fn().mockResolvedValue(undefined),
    deleteUser: vi.fn().mockResolvedValue(undefined),
  }),
}))

const MOCK_MODELS = {
  defaultModel: 'gemma3:4b',
  providers: [
    {
      id: 'ollama',
      name: 'Ollama',
      models: [
        { id: 'gemma3:4b', name: 'Gemma 3 4B', description: 'Lightweight model' },
        { id: 'llama3.2:3b', name: 'Llama 3.2 3B', description: 'Meta model' },
      ],
    },
  ],
}

const server = setupServer(
  http.get('/api/models', () => {
    return HttpResponse.json(MOCK_MODELS)
  }),
  http.get('/api/version', () => {
    return HttpResponse.json({ version: '1.2.3' })
  })
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

const DEFAULT_SETTINGS = {
  defaultLength: 'default',
  model: 'gemma3:4b',
  notificationsEnabled: false,
  openaiApiKey: null,
  anthropicApiKey: null,
  readeckApiKey: null,
  readeckUrl: null,
  webhookUrl: null,
  customPrompt: null,
}

function renderSettings(props = {}) {
  const defaults = {
    settings: DEFAULT_SETTINGS,
    onUpdateSetting: vi.fn(),
    onUpdateSettings: vi.fn().mockResolvedValue(undefined),
    isAdmin: false,
    currentUserId: null,
  }
  const merged = { ...defaults, ...props }
  return {
    ...render(
      <MemoryRouter>
        <Settings {...merged} />
      </MemoryRouter>
    ),
    onUpdateSetting: merged.onUpdateSetting,
    onUpdateSettings: merged.onUpdateSettings,
  }
}

describe('Settings', () => {
  it('should render the settings title', () => {
    renderSettings()

    expect(screen.getByText('Settings')).toBeInTheDocument()
  })

  it('should render summarization tab by default with length options', async () => {
    renderSettings()

    expect(screen.getByText('Summary Length')).toBeInTheDocument()
    expect(screen.getByText('Short')).toBeInTheDocument()
    expect(screen.getByText('Standard')).toBeInTheDocument()
    expect(screen.getByText('Detailed')).toBeInTheDocument()
  })

  it('should call onUpdateSetting when changing length', async () => {
    const onUpdateSetting = vi.fn()
    renderSettings({ onUpdateSetting })
    const user = userEvent.setup()

    await user.click(screen.getByText('Short'))

    expect(onUpdateSetting).toHaveBeenCalledWith('defaultLength', 'shorter')
  })

  it('should show model options after loading', async () => {
    renderSettings()

    await waitFor(() => {
      expect(screen.getByText('Gemma 3 4B')).toBeInTheDocument()
    })
    expect(screen.getByText('Llama 3.2 3B')).toBeInTheDocument()
  })

  it('should switch to integrations tab', async () => {
    renderSettings()
    const user = userEvent.setup()

    await user.click(screen.getByText('Integrations'))

    expect(screen.getByText('API Keys')).toBeInTheDocument()
    expect(screen.getByText(/OpenAI API Key/i)).toBeInTheDocument()
    expect(screen.getByText(/Anthropic API Key/i)).toBeInTheDocument()
  })

  it('should switch to preferences tab', async () => {
    renderSettings()
    const user = userEvent.setup()

    await user.click(screen.getByText('Preferences'))

    expect(screen.getByText('Notifications')).toBeInTheDocument()
  })

  it('should show users tab when isAdmin', async () => {
    renderSettings({ isAdmin: true, currentUserId: '1' })
    const user = userEvent.setup()

    expect(screen.getByText('Users')).toBeInTheDocument()

    await user.click(screen.getByText('Users'))

    expect(screen.getByText('User Accounts')).toBeInTheDocument()
    expect(screen.getByText('admin')).toBeInTheDocument()
    expect(screen.getByText('user1')).toBeInTheDocument()
  })

  it('should not show users tab when not admin', () => {
    renderSettings({ isAdmin: false })

    expect(screen.queryByText('Users')).not.toBeInTheDocument()
  })

  it('should display version in footer', async () => {
    renderSettings()

    await waitFor(() => {
      expect(screen.getByText(/v1\.2\.3/)).toBeInTheDocument()
    })
  })

  it('should save OpenAI API key', async () => {
    const onUpdateSetting = vi.fn()
    renderSettings({ onUpdateSetting })
    const user = userEvent.setup()

    await user.click(screen.getByText('Integrations'))

    const keyInput = screen.getByPlaceholderText('sk-...')
    await user.type(keyInput, 'sk-test-key-123')

    // Multiple "Save" buttons on this tab; the first one is the OpenAI save button
    const saveButtons = screen.getAllByText('Save')
    await user.click(saveButtons[0])

    expect(onUpdateSetting).toHaveBeenCalledWith('openaiApiKey', 'sk-test-key-123')
  })

  it('should show remove button when OpenAI key is set', async () => {
    renderSettings({
      settings: { ...DEFAULT_SETTINGS, openaiApiKey: 'sk-existing' },
    })
    const user = userEvent.setup()

    await user.click(screen.getByText('Integrations'))

    // There should be Remove buttons
    const removeButtons = screen.getAllByText('Remove')
    expect(removeButtons.length).toBeGreaterThan(0)
  })

  it('should render custom prompt section', async () => {
    renderSettings()

    expect(screen.getByText('Custom Prompt')).toBeInTheDocument()
    expect(screen.getByPlaceholderText(/custom summarization/i)).toBeInTheDocument()
  })

  it('should show back button that navigates', () => {
    renderSettings()

    expect(screen.getByText(/Back/)).toBeInTheDocument()
  })
})
