import { createContext, useContext, useEffect, useState } from 'react'
import { useAuth } from './AuthContext.jsx'
import {
  listarTodos,
  mockEnabled,
  readMock,
  removerRegistro,
  salvarRegistro,
  encerrarPlantioRegistro,
} from '../services/registroService.js'

const DataContext = createContext(null)
const emptyData = {
  producers: [],
  properties: [],
  cultures: [],
  plantings: [],
  production: [],
  categories: [],
  expenses: [],
  dailies: [],
  references: [],
  clients: [],
  sales: [],
  reports: [],
  users: [],
  results: [],
  stock: [],
  dashboard: null,
  resultSummary: null,
}

export function DataProvider({ children }) {
  const { session } = useAuth()
  const [data, setData] = useState(() =>
    session?.demo || mockEnabled ? readMock() : emptyData,
  )
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    if (!session) return
    let active = true
    setLoading(true)
    listarTodos(session.demo, session.user?.perfil)
      .then((result) => {
        if (active) {
          setData(result)
          setError('')
        }
      })
      .catch((cause) => {
        if (active) setError(cause.message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [session])

  async function save(collection, item) {
    const value = await salvarRegistro(collection, item, session?.demo)
    setData(await listarTodos(session?.demo, session?.user?.perfil))
    return value
  }

  async function remove(collection, id) {
    await removerRegistro(collection, id, session?.demo)
    setData(await listarTodos(session?.demo, session?.user?.perfil))
  }

  async function encerrarPlantio(id, endedAt) {
    await encerrarPlantioRegistro(id, endedAt, session?.demo)
    setData(await listarTodos(session?.demo, session?.user?.perfil))
  }

  return (
    <DataContext.Provider
      value={{ data, save, remove, encerrarPlantio, loading, error }}
    >
      {children}
    </DataContext.Provider>
  )
}

export function useData() {
  return useContext(DataContext)
}

export const byId = (list, id) => list.find((row) => row.id === id)
export const money = (value) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
    Number(value) || 0,
  )
export const number = (value, digits = 0) =>
  new Intl.NumberFormat('pt-BR', { maximumFractionDigits: digits }).format(
    Number(value) || 0,
  )
export const dateText = (value) =>
  value ? new Date(`${value}T12:00:00`).toLocaleDateString('pt-BR') : '—'
export const totalVenda = (sale) => sale.total ?? 0
export const rotuloPlantio = (data, planting) =>
  planting
    ? `${byId(data.cultures, planting.cultureId)?.name || 'Cultura'} · ${byId(data.properties, planting.propertyId)?.name || 'Propriedade'}`
    : 'Plantio não encontrado'
export const quantidadeProduzida = (data, plantingId) =>
  data.stock.find((row) => row.plantingId === plantingId)?.produced ?? 0
export const quantidadeVendida = (data, plantingId) =>
  data.stock.find((row) => row.plantingId === plantingId)?.sold ?? 0
export const resultadoPlantio = (data, plantingId) =>
  data.results.find((row) => row.plantingId === plantingId) || {
    produced: 0,
    sold: 0,
    revenue: 0,
    expense: 0,
    paidLabor: 0,
    familyLabor: 0,
    cost: 0,
    result: 0,
  }
