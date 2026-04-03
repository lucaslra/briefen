import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Header } from './Header'

// ThemeToggle is a presentational button — we can let it render naturally
// but mock it if it causes import issues
vi.mock('./ThemeToggle', () => ({
  ThemeToggle: ({ onToggle }) => (
    <button onClick={onToggle} aria-label="Toggle theme">Toggle theme</button>
  ),
}))

function renderHeader(props = {}) {
  const defaults = {
    theme: 'light',
    onToggleTheme: vi.fn(),
    unreadCount: 0,
  }
  return render(
    <MemoryRouter>
      <Header {...defaults} {...props} />
    </MemoryRouter>
  )
}

describe('Header', () => {
  it('shouldRenderAppTitle', () => {
    // Arrange / Act
    renderHeader()

    // Assert
    expect(screen.getByText('Briefen')).toBeInTheDocument()
  })

  it('shouldRenderReadingListLink', () => {
    // Arrange / Act
    renderHeader()

    // Assert — the reading list button has aria-label "Reading list"
    expect(screen.getByRole('button', { name: 'Reading list' })).toBeInTheDocument()
  })

  it('shouldShowUnreadBadgeWhenCountIsPositive', () => {
    // Arrange / Act
    renderHeader({ unreadCount: 5 })

    // Assert
    expect(screen.getByText('5')).toBeInTheDocument()
  })

  it('shouldHideUnreadBadgeWhenCountIsZero', () => {
    // Arrange / Act
    renderHeader({ unreadCount: 0 })

    // Assert — the badge element should not be in the DOM
    expect(screen.queryByText('0')).not.toBeInTheDocument()
  })
})
