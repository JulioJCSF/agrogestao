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
  references: '/valores-referencia-diaria',
  clients: '/clientes',
  sales: '/vendas',
  reports: '/relatorios',
  users: '/usuarios',
}
const fields = {
  producers: {
    name: 'nome',
    cpf: 'documento',
    phone: 'contato',
    community: 'endereco',
    consentAt: 'consentimentoEm',
  },
  properties: {
    producerId: 'produtorId',
    name: 'nome',
    area: 'areaTotal',
    location: 'localizacao',
  },
  cultures: { name: 'nome', cycle: 'tipoCicloPadrao', unit: 'unidadeMedida' },
  plantings: {
    producerId: 'produtorId',
    propertyId: 'propriedadeId',
    cultureId: 'culturaId',
    area: 'area',
    cycle: 'tipoCiclo',
    startedAt: 'dataInicio',
    expectedAt: 'previsaoColheita',
    endedAt: 'dataEncerramento',
  },
  production: {
    plantingId: 'plantioId',
    amount: 'quantidade',
    unit: 'unidadeMedida',
    date: 'dataFato',
  },
  categories: { name: 'nome', system: 'sistema' },
  expenses: {
    producerId: 'produtorId',
    categoryId: 'categoriaDespesaId',
    description: 'descricao',
    amount: 'valorTotal',
    date: 'dataFato',
    plantingIds: 'plantioIds',
  },
  dailies: {
    plantingId: 'plantioId',
    nature: 'natureza',
    days: 'quantidadeDias',
    unitValue: 'valorUnitario',
    amount: 'valorTotal',
    date: 'dataFato',
  },
  references: {
    value: 'valor',
    startsAt: 'vigenciaInicio',
    endsAt: 'vigenciaFim',
  },
  clients: { name: 'nome', channel: 'tipoCanal' },
  sales: {
    producerId: 'produtorId',
    clientId: 'clienteId',
    date: 'dataFato',
    total: 'valorTotal',
  },
  reports: {
    producerId: 'produtorId',
    from: 'periodoInicio',
    to: 'periodoFim',
    issuedAt: 'dataEmissao',
    snapshot: 'conteudo',
  },
}
const tipos = {
  producer: 'producers',
  property: 'properties',
  culture: 'cultures',
  planting: 'plantings',
  production: 'production',
  category: 'categories',
  expense: 'expenses',
  daily: 'dailies',
  reference: 'references',
  client: 'clients',
  sale: 'sales',
  report: 'reports',
  user: 'users',
  closePlanting: 'plantings',
}
const ciclos = { CURTO: 'Curto', LONGO: 'Longo' }
const naturezas = { PAGA: 'Paga', FAMILIAR: 'Familiar' }
const canais = {
  COMERCIO: 'Comércio local',
  FEIRA: 'Feira',
  CEASA: 'CEASA',
  MERENDA: 'Merenda escolar',
  COOPERATIVA: 'Cooperativa',
  OUTRO: 'Outros',
}
const inverse = (obj) =>
  Object.fromEntries(Object.entries(obj).map(([key, value]) => [value, key]))
const decimalKeys = new Set([
  'areaTotal',
  'area',
  'quantidade',
  'valor',
  'valorTotal',
  'valorUnitario',
  'quantidadeDias',
])
export function campoDoFormulario(type, campo) {
  const item = /^itens\[(\d+)\]\.(.+)$/.exec(campo)
  if (item)
    return `sale-${{ plantioId: 'planting', quantidade: 'amount', unidadeMedida: 'unit', valorUnitario: 'price' }[item[2]] || item[2]}-${item[1]}`
  return inverse(fields[tipos[type]] || {})[campo] || campo
}
function idsAsStrings(value) {
  return Object.fromEntries(
    Object.entries(value).map(([key, entry]) => [
      key,
      entry != null && (key === 'id' || key.endsWith('Id'))
        ? String(entry)
        : entry,
    ]),
  )
}
export function fromApi(collection, value) {
  const reverse = inverse(fields[collection] || {})
  const result = idsAsStrings(
    Object.fromEntries(
      Object.entries(value).map(([key, entry]) => [reverse[key] || key, entry]),
    ),
  )
  if (collection === 'producers')
    result.status = value.ativo ? 'Ativo' : 'Inativo'
  if (result.cycle) result.cycle = ciclos[result.cycle] || result.cycle
  if (collection === 'plantings')
    result.status = value.dataEncerramento ? 'Encerrado' : 'Em andamento'
  if (result.nature) result.nature = naturezas[result.nature] || result.nature
  if (result.channel) result.channel = canais[result.channel] || result.channel
  if (collection === 'expenses') {
    result.allocations = (value.rateios || []).map((row) => ({
      plantingId: String(row.plantioId),
      percent: row.percentual * 100,
      amount: row.valorRateado,
    }))
    result.plantingIds = result.allocations.map((row) => row.plantingId)
  }
  if (collection === 'sales')
    result.items = (value.itens || []).map((row) => ({
      plantingId: String(row.plantioId),
      amount: row.quantidade,
      unit: row.unidadeMedida,
      unitPrice: row.valorUnitario,
      total: row.valorTotal,
    }))
  return result
}
export function toApi(collection, value) {
  const result = Object.fromEntries(
    Object.entries(value)
      .filter(
        ([key]) =>
          !['id', 'status', 'system', 'producerId'].includes(key) ||
          (key === 'producerId' &&
            !['properties', 'plantings'].includes(collection)),
      )
      .map(([key, entry]) => {
        const campo = fields[collection]?.[key] || key
        const converted =
          campo.endsWith('Id') && entry
            ? Number(entry)
            : decimalKeys.has(campo) && entry !== '' && entry != null
              ? Number(entry)
              : entry === ''
                ? null
                : entry
        return [campo, converted]
      }),
  )
  if (collection === 'producers') {
    result.documento = String(value.cpf || '').replace(/\D/g, '')
    result.ativo = value.status !== 'Inativo'
  }
  if (collection === 'cultures')
    result.tipoCicloPadrao = inverse(ciclos)[value.cycle] || value.cycle
  if (collection === 'plantings')
    result.tipoCiclo = inverse(ciclos)[value.cycle] || value.cycle
  if (collection === 'expenses')
    result.plantioIds = (value.plantingIds || []).map(Number)
  if (collection === 'dailies') {
    result.natureza = inverse(naturezas)[value.nature] || value.nature
    if (result.natureza === 'FAMILIAR') delete result.valorUnitario
    delete result.plantioId
  }
  if (collection === 'production') delete result.plantioId
  if (collection === 'clients')
    result.tipoCanal = inverse(canais)[value.channel] || value.channel
  if (collection === 'sales') {
    delete result.items
    result.itens = value.items.map((row) => ({
      plantioId: Number(row.plantingId),
      quantidade: Number(row.amount),
      unidadeMedida: row.unit,
      valorUnitario: Number(row.unitPrice),
    }))
  }
  return result
}
export const readMock = () => structuredClone(sample)
function unavailableInPreview() {
  throw new Error(
    'Esta é uma prévia visual. O registro e as regras de negócio dependem da API.',
  )
}
async function listarPaginas(url, params = {}) {
  const rows = []
  for (let pagina = 0; ; pagina++) {
    const { data } = await api.get(url, {
      params: { ...params, pagina, tamanho: 100 },
    })
    rows.push(...data.itens)
    if (rows.length >= data.total || !data.itens.length) return rows
  }
}
export async function listarTodos(demo = false, perfil = 'CONSULTA') {
  if (demo) return readMock()
  const result = {
    ...Object.fromEntries(Object.keys(resources).map((key) => [key, []])),
    results: [],
    stock: [],
    dashboard: null,
    resultSummary: null,
    indisponiveis: [],
  }
  async function carregar(collection, url, params) {
    try {
      return (await listarPaginas(url, params)).map((row) =>
        fromApi(collection, row),
      )
    } catch (cause) {
      if (cause.status !== 501) throw cause
      result.indisponiveis.push(collection)
      return []
    }
  }
  await Promise.all(
    [
      'producers',
      'cultures',
      'plantings',
      'categories',
      'expenses',
      'references',
      'clients',
      'sales',
      ...(perfil === 'ADMIN' ? ['users'] : []),
    ].map(async (key) => {
      result[key] = await carregar(key, resources[key])
    }),
  )
  result.properties = (
    await Promise.all(
      result.producers.map((row) =>
        carregar('properties', `/produtores/${row.id}/propriedades`),
      ),
    )
  ).flat()
  result.reports = (
    await Promise.all(
      result.producers.map((row) =>
        carregar('reports', `/produtores/${row.id}/relatorios`),
      ),
    )
  ).flat()
  result.production = (
    await Promise.all(
      result.plantings.map((row) =>
        carregar('production', `/plantios/${row.id}/producoes`),
      ),
    )
  ).flat()
  result.dailies = (
    await Promise.all(
      result.plantings.map((row) =>
        carregar('dailies', `/plantios/${row.id}/diarias`),
      ),
    )
  ).flat()
  return result
}
export async function salvarRegistro(collection, item, demo = false) {
  if (!resources[collection]) throw new Error('Tipo de registro desconhecido.')
  if (demo) unavailableInPreview()
  let url = resources[collection]
  if (!item.id && collection === 'properties')
    url = `/produtores/${item.producerId}/propriedades`
  if (!item.id && ['production', 'dailies'].includes(collection))
    url = `/plantios/${item.plantingId}${url}`
  const payload = toApi(collection, item)
  const { data } = item.id
    ? await api.put(`${url}/${item.id}`, payload)
    : await api.post(url, payload)
  return fromApi(collection, data)
}
export async function removerRegistro(collection, id, demo = false) {
  if (!resources[collection]) throw new Error('Tipo de registro desconhecido.')
  if (demo) unavailableInPreview()
  await api.delete(`${resources[collection]}/${id}`)
}
export async function encerrarPlantioRegistro(id, endedAt, demo = false) {
  if (demo) unavailableInPreview()
  await api.post(`/plantios/${id}/encerramento`, { dataEncerramento: endedAt })
}
