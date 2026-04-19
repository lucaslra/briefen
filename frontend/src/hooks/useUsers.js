import { useState, useEffect, useCallback } from 'react'
import { apiFetch } from '../apiFetch.js'
import { STRINGS } from '../constants/strings'

export function useUsers() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await apiFetch('/api/users')
      if (res.ok) {
        setUsers(await res.json())
      } else {
        setError(STRINGS.USERS_LOAD_ERROR)
      }
    } catch {
      setError(STRINGS.USERS_NETWORK_ERROR)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])

  const createUser = useCallback(async (username, password, role) => {
    const res = await apiFetch('/api/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password, role }),
    })
    if (res.status === 409) throw new Error('taken')
    if (!res.ok) throw new Error('generic')
    await refresh()
  }, [refresh])

  const deleteUser = useCallback(async (id) => {
    const res = await apiFetch(`/api/users/${id}`, { method: 'DELETE' })
    if (!res.ok) throw new Error('delete')
    await refresh()
  }, [refresh])

  return { users, loading, error, createUser, deleteUser }
}
