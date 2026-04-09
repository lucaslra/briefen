import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Setup } from './Setup'

function renderSetup(props = {}) {
  const defaults = {
    onComplete: vi.fn().mockResolvedValue({ ok: true }),
  }
  return {
    ...render(<Setup {...defaults} {...props} />),
    onComplete: props.onComplete || defaults.onComplete,
  }
}

describe('Setup', () => {
  it('should render setup form with all fields', () => {
    renderSetup()

    expect(screen.getByText('Welcome to Briefen')).toBeInTheDocument()
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument()
  })

  it('should disable submit button when fields are empty', () => {
    renderSetup()

    expect(screen.getByRole('button', { name: /create account/i })).toBeDisabled()
  })

  it('should show password mismatch error', async () => {
    renderSetup()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/^password$/i), 'Str0ng!Pass')
    await user.type(screen.getByLabelText(/confirm password/i), 'Different!1')
    // Blur to trigger touched state
    fireEvent.blur(screen.getByLabelText(/confirm password/i))

    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument()
  })

  it('should show password validation rules after typing', async () => {
    renderSetup()
    const user = userEvent.setup()

    const passwordInput = screen.getByLabelText(/^password$/i)
    await user.type(passwordInput, 'weak')
    fireEvent.blur(passwordInput)

    expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument()
    expect(screen.getByText(/at least one uppercase/i)).toBeInTheDocument()
    expect(screen.getByText(/at least one digit/i)).toBeInTheDocument()
    expect(screen.getByText(/at least one special/i)).toBeInTheDocument()
  })

  it('should show username validation on blur', async () => {
    renderSetup()
    const user = userEvent.setup()

    const usernameInput = screen.getByLabelText(/username/i)
    await user.type(usernameInput, 'ab')
    fireEvent.blur(usernameInput)

    expect(screen.getByText(/username must be at least 3 characters/i)).toBeInTheDocument()
  })

  it('should enable submit when all fields are valid', async () => {
    renderSetup()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/^password$/i), 'Str0ng!Pass1')
    await user.type(screen.getByLabelText(/confirm password/i), 'Str0ng!Pass1')

    expect(screen.getByRole('button', { name: /create account/i })).toBeEnabled()
  })

  it('should call onComplete with username and password on submit', async () => {
    const onComplete = vi.fn().mockResolvedValue({ ok: true })
    renderSetup({ onComplete })
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/^password$/i), 'Str0ng!Pass1')
    await user.type(screen.getByLabelText(/confirm password/i), 'Str0ng!Pass1')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(onComplete).toHaveBeenCalledWith('admin', 'Str0ng!Pass1')
    })
  })

  it('should show error on 409 conflict', async () => {
    const onComplete = vi.fn().mockResolvedValue({ ok: false, status: 409, error: 'conflict' })
    renderSetup({ onComplete })
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/^password$/i), 'Str0ng!Pass1')
    await user.type(screen.getByLabelText(/confirm password/i), 'Str0ng!Pass1')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(screen.getByText(/already been completed/i)).toBeInTheDocument()
    })
  })

  it('should show generic error on other failures', async () => {
    const onComplete = vi.fn().mockResolvedValue({ ok: false, status: 500, error: '' })
    renderSetup({ onComplete })
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/^password$/i), 'Str0ng!Pass1')
    await user.type(screen.getByLabelText(/confirm password/i), 'Str0ng!Pass1')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(screen.getByText(/something went wrong/i)).toBeInTheDocument()
    })
  })
})
