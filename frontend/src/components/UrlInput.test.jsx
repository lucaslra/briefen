import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { UrlInput } from './UrlInput'

// ReadeckBrowser is imported inside UrlInput; mock it to avoid pulling in its deps
vi.mock('./ReadeckBrowser', () => ({
  ReadeckBrowser: () => <div data-testid="readeck-browser" />,
}))

function renderUrlInput(props = {}) {
  const defaults = {
    onSubmitUrl: vi.fn(),
    onSubmitBatch: vi.fn(),
    onSubmitText: vi.fn(),
    loading: false,
    error: null,
    readeck: null,
    readeckConfigured: false,
    readeckKey: '',
  }
  return render(
    <MemoryRouter>
      <UrlInput {...defaults} {...props} />
    </MemoryRouter>
  )
}

describe('UrlInput', () => {
  beforeEach(() => {
    // Stub clipboard API via Object.defineProperty (jsdom doesn't expose it as writable)
    vi.stubGlobal('navigator', {
      ...navigator,
      clipboard: { readText: vi.fn().mockResolvedValue('') },
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shouldRenderUrlInputByDefault', () => {
    // Arrange / Act
    renderUrlInput()

    // Assert — the URL tab input is present
    const input = screen.getByPlaceholderText('Paste an article URL...')
    expect(input).toBeInTheDocument()
  })

  it('shouldShowSubmitButtonDisabledWhenEmpty', () => {
    // Arrange / Act
    renderUrlInput()

    // Assert
    const submitBtn = screen.getByRole('button', { name: 'Summarize' })
    expect(submitBtn).toBeDisabled()
  })

  it('shouldEnableSubmitButtonWhenValidHttpsUrlEntered', async () => {
    // Arrange
    const user = userEvent.setup()
    renderUrlInput()
    const input = screen.getByPlaceholderText('Paste an article URL...')

    // Act
    await user.type(input, 'https://example.com/article')

    // Assert
    const submitBtn = screen.getByRole('button', { name: 'Summarize' })
    expect(submitBtn).toBeEnabled()
  })

  it('shouldShowValidationErrorForNonHttpUrl', async () => {
    // Arrange
    const user = userEvent.setup()
    renderUrlInput()
    const input = screen.getByPlaceholderText('Paste an article URL...')

    // Act: type a non-http URL and submit
    await user.type(input, 'ftp://invalid.com')
    const submitBtn = screen.getByRole('button', { name: 'Summarize' })
    await user.click(submitBtn)

    // Assert
    expect(screen.getByText('Please enter a valid HTTP or HTTPS URL.')).toBeInTheDocument()
  })

  it('shouldCallOnSubmitWithValidUrl', async () => {
    // Arrange
    const user = userEvent.setup()
    const onSubmitUrl = vi.fn()
    renderUrlInput({ onSubmitUrl })
    const input = screen.getByPlaceholderText('Paste an article URL...')

    // Act
    await user.type(input, 'https://example.com/article')
    const submitBtn = screen.getByRole('button', { name: 'Summarize' })
    await user.click(submitBtn)

    // Assert
    expect(onSubmitUrl).toHaveBeenCalledOnce()
    expect(onSubmitUrl).toHaveBeenCalledWith('https://example.com/article')
  })

  it('shouldSwitchToPasteContentMode', async () => {
    // Arrange
    const user = userEvent.setup()
    renderUrlInput()

    // Act — click the "Paste Content" tab
    const pasteTab = screen.getByRole('button', { name: 'Paste Content' })
    await user.click(pasteTab)

    // Assert — textarea for pasted content should be visible
    expect(screen.getByPlaceholderText('Paste the article content here...')).toBeInTheDocument()
  })
})
