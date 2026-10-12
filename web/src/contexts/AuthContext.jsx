import { createContext, useContext, useEffect, useState } from 'react'
import { api } from '../services/api.js'
const AuthContext = createContext(null)
const KEY = 'agrogestao-session'
function readSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(KEY))
    if (session?.demo) return { demo: true, token: null, user: session.user }
    if (
      !session?.token ||
      !session?.user ||
      !session.expiraEm ||
      Date.parse(session.expiraEm) <= Date.now() ||
      Number.isNaN(Date.parse(session.expiraEm))
    ) {
      sessionStorage.removeItem(KEY)
      return null
    }
    return session
  } catch {
    sessionStorage.removeItem(KEY)
    return null
  }
}
export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession)
  const [restoring, setRestoring] = useState(() =>
    Boolean(readSession()?.token),
  )
  useEffect(() => {
    const expired = () => {
      setSession(null)
      setRestoring(false)
    }
    window.addEventListener('agrogestao-session-expired', expired)
    return () =>
      window.removeEventListener('agrogestao-session-expired', expired)
  }, [])
  useEffect(() => {
    if (!session?.token || session.demo) return
    let active = true
    api
      .get('/auth/me')
      .then(({ data }) => {
        if (!active) return
        setSession((current) => {
          if (current?.token !== session.token) return current
          const next = { ...current, user: data }
          sessionStorage.setItem(KEY, JSON.stringify(next))
          return next
        })
      })
      .catch(() => {})
      .finally(() => {
        if (active) setRestoring(false)
      })
    const remaining = Date.parse(session.expiraEm) - Date.now()
    const timer = window.setTimeout(
      () => {
        sessionStorage.removeItem(KEY)
        window.dispatchEvent(new Event('agrogestao-session-expired'))
      },
      Math.max(0, Math.min(remaining, 2147483647)),
    )
    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [session?.token, session?.demo, session?.expiraEm])
  function persist(next) {
    if (next) sessionStorage.setItem(KEY, JSON.stringify(next))
    else sessionStorage.removeItem(KEY)
    setSession(next)
    setRestoring(false)
  }
  async function login(loginName, senha) {
    const { data } = await api.post('/auth/login', { login: loginName, senha })
    const next = {
      token: data.token,
      expiraEm: data.expiraEm,
      user: data.usuario,
      demo: false,
    }
    persist(next)
    return next
  }
  function enterDemo() {
    persist({
      token: null,
      demo: true,
      user: { nome: 'Prévia AgroGestão', perfil: 'ADMIN' },
    })
  }
  return (
    <AuthContext.Provider
      value={{
        session,
        restoring,
        login,
        enterDemo,
        logout: () => persist(null),
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
export function useAuth() {
  return useContext(AuthContext)
}
export function usePerfil() {
  const { session } = useAuth()
  const perfil = session?.user?.perfil || 'CONSULTA'
  return {
    perfil,
    canWrite: ['ADMIN', 'LANCAMENTO'].includes(perfil),
    isAdmin: perfil === 'ADMIN',
  }
}
