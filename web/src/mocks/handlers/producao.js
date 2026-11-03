import {
  banco,
  buscar,
  mensagensNaoEncontrado,
  proximoId,
  validarDataFato,
} from '../banco.js'
import {
  criado,
  hoje,
  idDoCaminho,
  pagina,
  regraNegocio,
  semConteudo,
  validar,
} from '../respostas.js'

const ProducaoRequisicao = {
  quantidade: { tipo: 'decimal', obrigatorio: true, digitos: [9, 3] },
  unidadeMedida: {
    tipo: 'texto',
    max: 10,
    padrao: {
      regex: /\S/,
      mensagem: 'Informe uma unidade de medida não vazia.',
    },
  },
  dataFato: { tipo: 'data', obrigatorio: true },
}

const NAO_ENCONTRADO = mensagensNaoEncontrado.producoes

/** RN38: unidade omitida recebe a da cultura; informada, deve ser a mesma, sem caixa e espaços. */
function preencher(producao, plantio, dto) {
  if (Number(dto.quantidade) <= 0)
    throw regraNegocio('A quantidade produzida deve ser maior que zero.')
  validarDataFato(dto.dataFato, [plantio])
  const unidade = buscar('culturas', plantio.culturaId).unidadeMedida.trim()
  if (
    dto.unidadeMedida != null &&
    unidade.toLowerCase() !== dto.unidadeMedida.trim().toLowerCase()
  )
    throw regraNegocio(
      `A unidade da produção deve ser a mesma da cultura do plantio (${unidade}).`,
      [
        {
          campo: 'unidadeMedida',
          mensagem: `Use a unidade da cultura: ${unidade}.`,
        },
      ],
    )
  return Object.assign(producao, {
    quantidade: Number(dto.quantidade),
    unidadeMedida: unidade,
    dataFato: dto.dataFato,
  })
}

export default [
  [
    'get',
    '/plantios/:plantioId/producoes',
    ({ params, url }) => {
      const plantioId = idDoCaminho(params, 'plantioId')
      buscar('plantios', plantioId, mensagensNaoEncontrado.plantios)
      return pagina(
        url,
        banco.producoes.filter((item) => item.plantioId === plantioId),
      )
    },
  ],
  [
    'post',
    '/plantios/:plantioId/producoes',
    ({ params, corpo }) => {
      const plantioId = idDoCaminho(params, 'plantioId')
      const dto = validar(corpo, ProducaoRequisicao)
      const plantio = buscar(
        'plantios',
        plantioId,
        mensagensNaoEncontrado.plantios,
      )
      // dataColeta é sempre a de hoje no registro (RN36); o corpo não a informa.
      const producao = preencher(
        { id: null, plantioId, dataColeta: hoje() },
        plantio,
        dto,
      )
      producao.id = proximoId('producoes')
      banco.producoes.push(producao)
      return criado(`/producoes/${producao.id}`, producao)
    },
  ],
  [
    'get',
    '/producoes/:id',
    ({ params }) => buscar('producoes', idDoCaminho(params), NAO_ENCONTRADO),
  ],
  [
    'put',
    '/producoes/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, ProducaoRequisicao)
      const producao = buscar('producoes', id, NAO_ENCONTRADO)
      return preencher(producao, buscar('plantios', producao.plantioId), dto)
    },
  ],
  [
    'delete',
    '/producoes/:id',
    ({ params }) => {
      const producao = buscar('producoes', idDoCaminho(params), NAO_ENCONTRADO)
      banco.producoes.splice(banco.producoes.indexOf(producao), 1)
      return semConteudo()
    },
  ],
]
