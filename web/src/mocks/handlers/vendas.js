import {
  banco,
  buscar,
  mensagensNaoEncontrado,
  proximoId,
  validarDataFato,
} from '../banco.js'
import {
  criado,
  escalar,
  hoje,
  idDoCaminho,
  multiplicarHalfUp,
  numero,
  pagina,
  parametro,
  regraNegocio,
  semConteudo,
  validar,
} from '../respostas.js'

const ItemVendaRequisicao = {
  plantioId: { tipo: 'inteiro', obrigatorio: true, positivo: true },
  quantidade: { tipo: 'decimal', obrigatorio: true, digitos: [9, 3] },
  unidadeMedida: { tipo: 'texto', obrigatorio: true, max: 10 },
  valorUnitario: { tipo: 'decimal', obrigatorio: true, digitos: [10, 2] },
}

const VendaRequisicao = {
  produtorId: { tipo: 'inteiro', obrigatorio: true, positivo: true },
  clienteId: { tipo: 'inteiro', obrigatorio: true, positivo: true },
  dataFato: { tipo: 'data', obrigatorio: true },
  itens: { lista: true, naoVazio: true, item: ItemVendaRequisicao },
}

// A venda ainda é 501 na API (L09b). Mensagens propostas em docs/qa/casos-de-teste-venda.md.
const NAO_ENCONTRADA = 'Venda não encontrada.'

/** RN30: unidade da produção registrada no plantio; sem produção, a da cultura. */
function validarUnidade(item, indice, plantio) {
  const producao = banco.producoes.find((p) => p.plantioId === plantio.id)
  const referencia = (
    producao?.unidadeMedida ??
    buscar('culturas', plantio.culturaId).unidadeMedida
  ).trim()
  if (referencia.toLowerCase() === item.unidadeMedida.trim().toLowerCase())
    return
  const mensagem = `A unidade do item (${item.unidadeMedida.trim()}) é diferente da unidade ${
    producao ? 'da produção registrada no plantio' : 'da cultura do plantio'
  } (${referencia}).`
  throw regraNegocio(mensagem, [
    { campo: `itens[${indice}].unidadeMedida`, mensagem },
  ])
}

function preencher(venda, dto) {
  buscar('produtores', dto.produtorId, mensagensNaoEncontrado.produtores)
  buscar('clientes', dto.clienteId, mensagensNaoEncontrado.clientes)
  const plantios = dto.itens.map((item) =>
    buscar('plantios', item.plantioId, mensagensNaoEncontrado.plantios),
  )
  validarDataFato(dto.dataFato, plantios)
  dto.itens.forEach((item, indice) =>
    validarUnidade(item, indice, plantios[indice]),
  )
  // Total do item com HALF_UP em 2 casas; o da venda soma os itens já arredondados.
  const itens = dto.itens.map((item) => ({
    id: proximoId('itensVenda'),
    plantioId: item.plantioId,
    quantidade: Number(item.quantidade),
    unidadeMedida: item.unidadeMedida.trim(),
    valorUnitario: Number(item.valorUnitario),
    valorTotal: multiplicarHalfUp(item.quantidade, item.valorUnitario),
  }))
  return Object.assign(venda, {
    produtorId: dto.produtorId,
    clienteId: dto.clienteId,
    dataFato: dto.dataFato,
    valorTotal: numero(
      itens.reduce((soma, item) => soma + escalar(item.valorTotal, 2), 0n),
      2,
    ),
    itens,
    // RN29 (saldo informativo) é cálculo da API; o mock não gera avisos.
    avisos: [],
  })
}

export default [
  [
    'get',
    '/vendas',
    ({ url }) => {
      const produtorId = parametro(url, 'produtorId', 'inteiro')
      const periodoInicio = parametro(url, 'periodoInicio', 'data')
      const periodoFim = parametro(url, 'periodoFim', 'data')
      return pagina(
        url,
        banco.vendas.filter(
          (venda) =>
            (produtorId == null || venda.produtorId === produtorId) &&
            (periodoInicio == null || venda.dataFato >= periodoInicio) &&
            (periodoFim == null || venda.dataFato <= periodoFim),
        ),
      )
    },
  ],
  [
    'post',
    '/vendas',
    ({ corpo }) => {
      const dto = validar(corpo, VendaRequisicao)
      const venda = preencher({ id: null, dataColeta: hoje() }, dto)
      venda.id = proximoId('vendas')
      banco.vendas.push(venda)
      return criado(`/vendas/${venda.id}`, venda)
    },
  ],
  [
    'get',
    '/vendas/:id',
    ({ params }) => buscar('vendas', idDoCaminho(params), NAO_ENCONTRADA),
  ],
  [
    'put',
    '/vendas/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, VendaRequisicao)
      const venda = buscar('vendas', id, NAO_ENCONTRADA)
      return Object.assign(venda, preencher({ ...venda }, dto))
    },
  ],
  [
    'delete',
    '/vendas/:id',
    ({ params }) => {
      const venda = buscar('vendas', idDoCaminho(params), NAO_ENCONTRADA)
      banco.vendas.splice(banco.vendas.indexOf(venda), 1)
      return semConteudo()
    },
  ],
]
