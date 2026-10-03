import { createContext, useContext, useEffect, useState } from 'react'
import { api } from '../services/api.js'
import { mockEnabled } from '../services/records.js'

const AuthContext = createContext(null)
const KEY = 'agrogestao-session'

function readSession() {
  try {
    return JSON.parse(sessionStorage.getItem(KEY)) || null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession)

  useEffect(() => {
    const onExpired = () => setSession(null)
    window.addEventListener('agrogestao-session-expired', onExpired)
    return () =>
      window.removeEventListener('agrogestao-session-expired', onExpired)
  }, [])

  useEffect(() => {
    if (!session?.token || session.demo || mockEnabled) return
    api
      .get('/auth/me')
      .then(({ data }) => {
        setSession((current) =>
          current ? { ...current, user: data } : current,
        )
      })
      .catch(() => {})
  }, [session?.token, session?.demo])

  function persist(next) {
    if (next) sessionStorage.setItem(KEY, JSON.stringify(next))
    else sessionStorage.removeItem(KEY)
    setSession(next)
  }

  async function login(loginName, senha) {
    const data = (await api.post('/auth/login', { login: loginName, senha }))
      .data
    const next = {
      token: data.token,
      expiraEm: data.expiraEm,
      user: data.usuario || data.user,
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

  const value = { session, login, enterDemo, logout: () => persist(null) }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}

export function usePerfil() {
  const { session } = useAuth()
  const perfil = session?.user?.perfil || 'CONSULTA'
  return {
    perfil,
    canWrite: perfil !== 'CONSULTA',
    isAdmin: perfil === 'ADMIN',
  }
}
