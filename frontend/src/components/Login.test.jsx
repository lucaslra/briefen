import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Login } from './Login'

function renderLogin(props = {}) {
  const defaults = {
    onLogin: vi.fn().mockResolvedValue(true),
  }
  return {
    ...render(<Login {...defaults} {...props} />),
    onLogin: props.onLogin || defaults.onLogin,
  }
}

describe('Login', () => {
  it('should render login form with username and password fields', () => {
    renderLogin()

    expect(screen.getByText('Briefen')).toBeInTheDocument()
    expect(screen.getByText('Sign in to continue')).toBeInTheDocument()
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('should disable submit button when fields are empty', () => {
    renderLogin()

    expect(screen.getByRole('button', { name: /sign in/i })).toBeDisabled()
  })

  it('should enable submit button when both fields have values', async () => {
    renderLogin()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/password/i), 'secret')

    expect(screen.getByRole('button', { name: /sign in/i })).toBeEnabled()
  })

  it('should call onLogin with username and password on submit', async () => {
    const onLogin = vi.fn().mockResolvedValue(true)
    renderLogin({ onLogin })
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/password/i), 'secret')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(onLogin).toHaveBeenCalledWith('admin', 'secret')
    })
  })

  it('should show error on failed login', async () => {
    const onLogin = vi.fn().mockResolvedValue(false)
    renderLogin({ onLogin })
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/password/i), 'wrong')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(screen.getByText(/invalid username or password/i)).toBeInTheDocument()
    })
  })

  it('should show loading state while signing in', async () => {
    let resolveLogin
    const onLogin = vi.fn().mockImplementation(() => {
      return new Promise(resolve => {
        resolveLogin = resolve
      })
    })
    renderLogin({ onLogin })
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/password/i), 'secret')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(screen.getByRole('button', { name: /signing in/i })).toBeDisabled()

    resolveLogin(true)

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /sign in/i })).toBeEnabled()
    })
  })

  it('should trim username before calling onLogin', async () => {
    const onLogin = vi.fn().mockResolvedValue(true)
    renderLogin({ onLogin })
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), '  admin  ')
    await user.type(screen.getByLabelText(/password/i), 'secret')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(onLogin).toHaveBeenCalledWith('admin', 'secret')
    })
  })

  it('should not submit when username is only whitespace', async () => {
    const onLogin = vi.fn().mockResolvedValue(true)
    renderLogin({ onLogin })
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), '   ')
    await user.type(screen.getByLabelText(/password/i), 'secret')

    expect(screen.getByRole('button', { name: /sign in/i })).toBeDisabled()
  })
})
