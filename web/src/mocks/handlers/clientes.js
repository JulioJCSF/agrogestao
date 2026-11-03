import { banco, buscar, mensagensNaoEncontrado, proximoId } from '../banco.js'
import { criado, idDoCaminho, pagina, validar } from '../respostas.js'

const ClienteRequisicao = {
  nome: { tipo: 'texto', obrigatorio: true, max: 150 },
  tipoCanal: {
    tipo: ['COMERCIO', 'FEIRA', 'CEASA', 'MERENDA', 'COOPERATIVA', 'OUTRO'],
    obrigatorio: true,
  },
}

const NAO_ENCONTRADO = mensagensNaoEncontrado.clientes

// A API não tem regra de negócio (422) para cliente; o 422 do módulo fica na venda.
const preencher = (cliente, dto) =>
  Object.assign(cliente, { nome: dto.nome.trim(), tipoCanal: dto.tipoCanal })

export default [
  [
    'get',
    '/clientes',
    ({ url }) => {
      const termo = (url.searchParams.get('busca') || '').trim().toLowerCase()
      return pagina(
        url,
        banco.clientes.filter((item) =>
          item.nome.toLowerCase().includes(termo),
        ),
      )
    },
  ],
  [
    'post',
    '/clientes',
    ({ corpo }) => {
      const cliente = preencher({ id: null }, validar(corpo, ClienteRequisicao))
      cliente.id = proximoId('clientes')
      banco.clientes.push(cliente)
      return criado(`/clientes/${cliente.id}`, cliente)
    },
  ],
  [
    'get',
    '/clientes/:id',
    ({ params }) => buscar('clientes', idDoCaminho(params), NAO_ENCONTRADO),
  ],
  [
    'put',
    '/clientes/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, ClienteRequisicao)
      return preencher(buscar('clientes', id, NAO_ENCONTRADO), dto)
    },
  ],
]
