import { useState, useCallback } from 'react'

export function useNotification() {
  const supported = typeof Notification !== 'undefined'
  const [permission, setPermission] = useState(supported ? Notification.permission : 'denied')

  const requestPermission = useCallback(async () => {
    if (!supported) return 'denied'
    const result = await Notification.requestPermission()
    setPermission(result)
    return result
  }, [supported])

  const notify = useCallback((title, body) => {
    if (!supported || Notification.permission !== 'granted') return
    new Notification(title, { body, icon: '/favicon.svg' })
  }, [supported])

  return { supported, permission, requestPermission, notify }
}
