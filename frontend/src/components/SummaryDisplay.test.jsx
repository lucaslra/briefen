import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SummaryDisplay } from './SummaryDisplay'

const REALISTIC_SUMMARY = {
  id: 1,
  url: 'https://example.com/article',
  title: 'The Future of AI',
  summary: 'This article discusses AI advancements and their societal impact.',
  modelUsed: 'gemma3:4b',
  createdAt: new Date().toISOString(),
  isRead: false,
}

describe('SummaryDisplay', () => {
  beforeEach(() => {
    vi.stubGlobal('navigator', {
      ...navigator,
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shouldRenderTitle', () => {
    // Arrange / Act
    render(
      <SummaryDisplay
        data={REALISTIC_SUMMARY}
        onMakeShorter={vi.fn()}
        onMakeLonger={vi.fn()}
        onRegenerate={vi.fn()}
        onClear={vi.fn()}
        loading={false}
        elapsedMs={0}
      />
    )

    // Assert
    expect(screen.getByText('The Future of AI')).toBeInTheDocument()
  })

  it('shouldRenderSummaryContent', () => {
    // Arrange / Act
    render(
      <SummaryDisplay
        data={REALISTIC_SUMMARY}
        onMakeShorter={vi.fn()}
        onMakeLonger={vi.fn()}
        onRegenerate={vi.fn()}
        onClear={vi.fn()}
        loading={false}
        elapsedMs={0}
      />
    )

    // Assert
    expect(screen.getByText(/This article discusses AI advancements/)).toBeInTheDocument()
  })

  it('shouldShowCopyButton', () => {
    // Arrange / Act
    render(
      <SummaryDisplay
        data={REALISTIC_SUMMARY}
        onMakeShorter={vi.fn()}
        onMakeLonger={vi.fn()}
        onRegenerate={vi.fn()}
        onClear={vi.fn()}
        loading={false}
        elapsedMs={0}
      />
    )

    // Assert
    expect(screen.getByRole('button', { name: 'Copy summary' })).toBeInTheDocument()
  })

  it('shouldCallOnMakeShorterWhenButtonClicked', async () => {
    // Arrange
    const user = userEvent.setup()
    const onMakeShorter = vi.fn()
    render(
      <SummaryDisplay
        data={REALISTIC_SUMMARY}
        onMakeShorter={onMakeShorter}
        onMakeLonger={vi.fn()}
        onRegenerate={vi.fn()}
        onClear={vi.fn()}
        loading={false}
        elapsedMs={0}
      />
    )

    // Act
    await user.click(screen.getByRole('button', { name: 'Make Shorter' }))

    // Assert
    expect(onMakeShorter).toHaveBeenCalledOnce()
  })

  it('shouldCallOnMakeLongerWhenButtonClicked', async () => {
    // Arrange
    const user = userEvent.setup()
    const onMakeLonger = vi.fn()
    render(
      <SummaryDisplay
        data={REALISTIC_SUMMARY}
        onMakeShorter={vi.fn()}
        onMakeLonger={onMakeLonger}
        onRegenerate={vi.fn()}
        onClear={vi.fn()}
        loading={false}
        elapsedMs={0}
      />
    )

    // Act
    await user.click(screen.getByRole('button', { name: 'Make Longer' }))

    // Assert
    expect(onMakeLonger).toHaveBeenCalledOnce()
  })
})
