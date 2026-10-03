import { api } from './api.js'
import { sample } from './mockData.js'

export const mockEnabled = import.meta.env.VITE_USAR_MOCK === 'true'

export const resources = {
  producers: '/produtores',
  properties: '/propriedades',
  cultures: '/culturas',
  plantings: '/plantios',
  production: '/producoes',
  categories: '/categorias-despesa',
  expenses: '/despesas',
  dailies: '/diarias',
  references: '/valores-referencia',
  clients: '/clientes',
  sales: '/vendas',
  reports: '/relatorios',
  users: '/usuarios',
}

const names = {
  name: 'nome',
  phone: 'telefone',
  community: 'comunidade',
  status: 'situacao',
  consentAt: 'consentimentoEm',
  producerId: 'produtorId',
  propertyId: 'propriedadeId',
  cultureId: 'culturaId',
  area: 'area',
  location: 'localizacao',
  cycle: 'ciclo',
  unit: 'unidade',
  startedAt: 'dataPlantio',
  expectedAt: 'previsaoColheita',
  endedAt: 'dataEncerramento',
  plantingId: 'plantioId',
  amount: 'quantidade',
  date: 'data',
  categoryId: 'categoriaId',
  description: 'descricao',
  plantingIds: 'plantioIds',
  allocations: 'rateios',
  percent: 'percentual',
  nature: 'natureza',
  days: 'quantidadeDiarias',
  unitValue: 'valorUnitario',
  startsAt: 'inicioVigencia',
  clientId: 'clienteId',
  items: 'itens',
  unitPrice: 'precoUnitario',
  channel: 'canal',
  from: 'inicio',
  to: 'fim',
  issuedAt: 'emitidoEm',
  snapshot: 'conteudo',
  system: 'sistema',
}

const reverseNames = Object.fromEntries(
  Object.entries(names).map(([key, value]) => [value, key]),
)

function translate(value, dictionary) {
  if (Array.isArray(value))
    return value.map((entry) => translate(entry, dictionary))
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(
    Object.entries(value).map(([key, entry]) => [
      dictionary[key] || key,
      translate(entry, dictionary),
    ]),
  )
}

const fromApi = (value) => translate(value, reverseNames)
const toApi = (value) => translate(value, names)

export const readMock = () => structuredClone(sample)

function unavailableInPreview() {
  throw new Error(
    'Esta é uma prévia visual. O registro e as regras de negócio dependem da API.',
  )
}

export async function listAll(demo = false, perfil = 'CONSULTA') {
  if (demo || mockEnabled) return readMock()
  const entries = await Promise.all(
    Object.entries(resources)
      .filter(([collection]) => collection !== 'users' || perfil === 'ADMIN')
      .map(async ([collection, url]) => {
        const { data } = await api.get(url, {
          params: { pagina: 0, tamanho: 100 },
        })
        return [collection, (data.itens || data).map(fromApi)]
      }),
  )
  const [dashboard, results, stock, resultSummary] = await Promise.all([
    api.get('/dashboard'),
    api.get('/resultados'),
    api.get('/estoque'),
    api.get('/resultados/resumo'),
  ])
  return {
    ...emptyCollections(),
    ...Object.fromEntries(entries),
    dashboard: fromApi(dashboard.data),
    results: fromApi(results.data.itens || results.data),
    stock: fromApi(stock.data.itens || stock.data),
    resultSummary: fromApi(resultSummary.data),
  }
}

function emptyCollections() {
  return Object.fromEntries(Object.keys(resources).map((key) => [key, []]))
}

export async function saveRecord(collection, item, demo = false) {
  if (!resources[collection]) throw new Error('Tipo de registro desconhecido.')
  if (demo || mockEnabled) unavailableInPreview()
  const url = resources[collection]
  const { data } = item.id
    ? await api.put(`${url}/${item.id}`, toApi(item))
    : await api.post(url, toApi(item))
  return fromApi(data || item)
}

export async function removeRecord(collection, id, demo = false) {
  if (!resources[collection]) throw new Error('Tipo de registro desconhecido.')
  if (demo || mockEnabled) unavailableInPreview()
  await api.delete(`${resources[collection]}/${id}`)
}

export async function closePlantingRecord(id, endedAt, demo = false) {
  if (demo || mockEnabled) unavailableInPreview()
  await api.post(`/plantios/${id}/encerramento`, { dataEncerramento: endedAt })
}
