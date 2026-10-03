import axios from 'axios'

export const api = axios.create({ baseURL: '/api/v1', timeout: 12000 })

api.interceptors.request.use((config) => {
  const raw = sessionStorage.getItem('agrogestao-session')
  let token = null
  try {
    token = raw ? JSON.parse(raw).token : null
  } catch {
    sessionStorage.removeItem('agrogestao-session')
  }
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('agrogestao-session')
      window.dispatchEvent(new Event('agrogestao-session-expired'))
    }
    const message =
      error.response?.data?.mensagem ||
      error.message ||
      'Não foi possível concluir a operação.'
    const normalized = new Error(message)
    normalized.status = error.response?.status
    normalized.campos = error.response?.data?.campos || []
    return Promise.reject(normalized)
  },
)
