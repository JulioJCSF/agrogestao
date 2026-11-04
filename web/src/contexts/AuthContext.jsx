import { createContext, useContext, useEffect, useState } from 'react'
import { api } from '../services/api.js'
const AuthContext = createContext(null)
const KEY = 'agrogestao-session'
const MESSAGE_KEY = 'agrogestao-session-message'
const INATIVIDADE_MS = 30 * 60 * 1000
const MENSAGEM_INATIVIDADE =
  'Sua sessão foi encerrada após 30 minutos sem atividade. Entre novamente.'

function encerrarPorInatividade() {
  sessionStorage.removeItem(KEY)
  sessionStorage.setItem(MESSAGE_KEY, MENSAGEM_INATIVIDADE)
  window.dispatchEvent(new Event('agrogestao-session-expired'))
}
function readSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(KEY))
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
    if (!Number.isFinite(session.ultimaAtividade)) {
      session.ultimaAtividade = Date.now()
      sessionStorage.setItem(KEY, JSON.stringify(session))
    }
    const { ultimaAtividade } = session
    if (Date.now() - ultimaAtividade >= INATIVIDADE_MS) {
      encerrarPorInatividade()
      return null
    }
    return { ...session, ultimaAtividade }
  } catch {
    sessionStorage.removeItem(KEY)
    return null
  }
}
export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession)
  const [sessionMessage, setSessionMessage] = useState(
    () => sessionStorage.getItem(MESSAGE_KEY) || '',
  )
  const [restoring, setRestoring] = useState(() =>
    Boolean(readSession()?.token),
  )
  useEffect(() => {
    const expired = () => {
      setSessionMessage(sessionStorage.getItem(MESSAGE_KEY) || '')
      setSession(null)
      setRestoring(false)
    }
    window.addEventListener('agrogestao-session-expired', expired)
    return () =>
      window.removeEventListener('agrogestao-session-expired', expired)
  }, [])
  useEffect(() => {
    if (!session?.token) return
    let active = true
    api
      .get('/auth/me')
      .then(({ data }) => {
        if (!active) return
        setSession((current) => {
          if (current?.token !== session.token) return current
          const persisted = JSON.parse(sessionStorage.getItem(KEY))
          const ultimaAtividade =
            persisted?.token === current.token &&
            Number.isFinite(persisted.ultimaAtividade)
              ? persisted.ultimaAtividade
              : current.ultimaAtividade
          const next = { ...current, user: data, ultimaAtividade }
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
  }, [session?.token, session?.expiraEm])
  useEffect(() => {
    if (!session?.token) return
    let ultimaAtividade = session.ultimaAtividade
    let timer
    function verificar() {
      window.clearTimeout(timer)
      const restante = INATIVIDADE_MS - (Date.now() - ultimaAtividade)
      if (restante <= 0) encerrarPorInatividade()
      else timer = window.setTimeout(verificar, restante)
    }
    function registrarAtividade() {
      if (Date.now() - ultimaAtividade >= INATIVIDADE_MS) {
        encerrarPorInatividade()
        return
      }
      const agora = Date.now()
      // pointermove dispara dezenas de vezes por segundo: grava e reagenda no máximo a cada 15 s.
      const gravar = agora - ultimaAtividade >= 15000
      ultimaAtividade = agora
      if (!gravar) return
      sessionStorage.setItem(
        KEY,
        JSON.stringify({ ...session, ultimaAtividade }),
      )
      verificar()
    }
    const eventos = [
      'pointerdown',
      'pointermove',
      'keydown',
      'scroll',
      'touchstart',
    ]
    verificar()
    eventos.forEach((evento) =>
      window.addEventListener(evento, registrarAtividade, {
        capture: true,
        passive: true,
      }),
    )
    window.addEventListener('focus', verificar)
    document.addEventListener('visibilitychange', verificar)
    return () => {
      window.clearTimeout(timer)
      eventos.forEach((evento) =>
        window.removeEventListener(evento, registrarAtividade, true),
      )
      window.removeEventListener('focus', verificar)
      document.removeEventListener('visibilitychange', verificar)
    }
  }, [session])
  function persist(next) {
    sessionStorage.removeItem(MESSAGE_KEY)
    setSessionMessage('')
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
      ultimaAtividade: Date.now(),
    }
    persist(next)
    return next
  }
  return (
    <AuthContext.Provider
      value={{
        session,
        restoring,
        sessionMessage,
        login,
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
