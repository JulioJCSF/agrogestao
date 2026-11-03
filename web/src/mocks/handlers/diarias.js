import {
  banco,
  buscar,
  existeEmissaoNoPeriodo,
  mensagensNaoEncontrado,
  proximoId,
  validarDataFato,
} from '../banco.js'
import {
  criado,
  dataBr,
  hoje,
  idDoCaminho,
  multiplicarHalfUp,
  pagina,
  regraNegocio,
  semConteudo,
  validar,
} from '../respostas.js'
import { vigenteEm } from './referencias.js'

const DiariaRequisicao = {
  natureza: { tipo: ['PAGA', 'FAMILIAR'], obrigatorio: true },
  quantidadeDias: { tipo: 'decimal', obrigatorio: true, digitos: [4, 2] },
  valorUnitario: { tipo: 'decimal', digitos: [10, 2] },
  dataFato: { tipo: 'data', obrigatorio: true },
}

const NAO_ENCONTRADO = mensagensNaoEncontrado.diarias
const MAIOR_VALOR = 9999999999.99

const despesaGerada = (diariaId) =>
  banco.despesas.find((despesa) => despesa.diariaId === diariaId)

/** RN11: a despesa da diária paga em período emitido não muda de data nem é excluída. */
function exigirDespesaNaoEmitida(despesa) {
  if (existeEmissaoNoPeriodo(despesa.produtorId, despesa.dataFato))
    throw regraNegocio(
      'A despesa desta diária já consta em relatório emitido. Não altere a data nem exclua a diária. Use o estorno da despesa.',
    )
}

function preencher(diaria, plantio, dto) {
  if (Number(dto.quantidadeDias) <= 0)
    throw regraNegocio('A quantidade de dias deve ser maior que zero.', [
      {
        campo: 'quantidadeDias',
        mensagem: 'Informe uma quantidade de dias maior que zero.',
      },
    ])
  validarDataFato(dto.dataFato, [plantio])
  if (dto.natureza === 'PAGA') {
    if (dto.valorUnitario == null || Number(dto.valorUnitario) <= 0)
      throw regraNegocio(
        'Informe um valor maior que zero para a diária paga.',
        [
          {
            campo: 'valorUnitario',
            mensagem: 'Informe o valor da diária paga, maior que zero.',
          },
        ],
      )
    const total = multiplicarHalfUp(dto.quantidadeDias, dto.valorUnitario)
    if (total <= 0 || total > MAIOR_VALOR)
      throw regraNegocio(
        'O valor total da diária paga deve estar entre R$ 0,01 e R$ 9.999.999.999,99.',
      )
    Object.assign(diaria, {
      valorUnitario: Number(dto.valorUnitario),
      valorReferenciaDiariaId: null,
    })
  } else if (diaria.id == null || dto.dataFato !== diaria.dataFato) {
    // RN13: a familiar copia a referência vigente na data do fato; o valor do corpo é ignorado.
    const referencia = vigenteEm(dto.dataFato)
    if (!referencia)
      throw regraNegocio(
        `Não há valor de referência da diária vigente em ${dataBr(dto.dataFato)}. Cadastre o valor de referência para essa data antes de lançar a diária familiar.`,
      )
    Object.assign(diaria, {
      valorUnitario: referencia.valor,
      valorReferenciaDiariaId: referencia.id,
    })
  }
  Object.assign(diaria, {
    natureza: dto.natureza,
    quantidadeDias: Number(dto.quantidadeDias),
    dataFato: dto.dataFato,
  })
  diaria.valorTotal = multiplicarHalfUp(
    String(diaria.quantidadeDias),
    String(diaria.valorUnitario),
  )
  return diaria
}

/** RN16: a despesa gerada acompanha a diária paga, com rateio integral ao plantio. */
function atualizarDespesa(despesa, diaria) {
  Object.assign(despesa, {
    descricao: `Diária paga de ${dataBr(diaria.dataFato)}`,
    dataFato: diaria.dataFato,
    valorTotal: diaria.valorTotal,
  })
  despesa.rateios.forEach((rateio) => (rateio.valorRateado = diaria.valorTotal))
}

export default [
  [
    'get',
    '/plantios/:plantioId/diarias',
    ({ params, url }) => {
      const plantioId = idDoCaminho(params, 'plantioId')
      buscar('plantios', plantioId, mensagensNaoEncontrado.plantios)
      return pagina(
        url,
        banco.diarias.filter((item) => item.plantioId === plantioId),
      )
    },
  ],
  [
    'post',
    '/plantios/:plantioId/diarias',
    ({ params, corpo }) => {
      const plantioId = idDoCaminho(params, 'plantioId')
      const dto = validar(corpo, DiariaRequisicao)
      const plantio = buscar(
        'plantios',
        plantioId,
        mensagensNaoEncontrado.plantios,
      )
      const diaria = preencher(
        { id: null, plantioId, dataColeta: hoje() },
        plantio,
        dto,
      )
      diaria.id = proximoId('diarias')
      banco.diarias.push(diaria)
      if (diaria.natureza === 'PAGA') {
        const categoria = banco.categorias.find(
          (item) => item.sistema && item.nome.toLowerCase() === 'diária',
        )
        if (!categoria)
          throw regraNegocio(
            'A categoria de sistema Diária não está cadastrada. Solicite a configuração ao administrador.',
          )
        const despesa = {
          id: proximoId('despesas'),
          produtorId: plantio.produtorId,
          categoriaDespesaId: categoria.id,
          diariaId: diaria.id,
          dataColeta: diaria.dataColeta,
          estornada: false,
          rateios: [{ plantioId, percentual: 1, valorRateado: 0 }],
        }
        atualizarDespesa(despesa, diaria)
        banco.despesas.push(despesa)
      }
      return criado(`/diarias/${diaria.id}`, diaria)
    },
  ],
  [
    'get',
    '/diarias/:id',
    ({ params }) => buscar('diarias', idDoCaminho(params), NAO_ENCONTRADO),
  ],
  [
    'put',
    '/diarias/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, DiariaRequisicao)
      const diaria = buscar('diarias', id, NAO_ENCONTRADO)
      if (diaria.natureza !== dto.natureza)
        throw regraNegocio(
          'A natureza da diária não pode ser alterada. Exclua a diária e registre outra com a natureza correta.',
          [
            {
              campo: 'natureza',
              mensagem: 'Exclua e registre outra diária para mudar a natureza.',
            },
          ],
        )
      if (diaria.natureza === 'PAGA' && dto.dataFato !== diaria.dataFato)
        exigirDespesaNaoEmitida(despesaGerada(id))
      // Valida numa cópia: um 422 não deixa a diária alterada pela metade.
      const alterada = preencher(
        { ...diaria },
        buscar('plantios', diaria.plantioId),
        dto,
      )
      Object.assign(diaria, alterada)
      if (diaria.natureza === 'PAGA')
        atualizarDespesa(despesaGerada(id), diaria)
      return diaria
    },
  ],
  [
    'delete',
    '/diarias/:id',
    ({ params }) => {
      const diaria = buscar('diarias', idDoCaminho(params), NAO_ENCONTRADO)
      if (diaria.natureza === 'PAGA') {
        const despesa = despesaGerada(diaria.id)
        exigirDespesaNaoEmitida(despesa)
        banco.despesas.splice(banco.despesas.indexOf(despesa), 1)
      }
      banco.diarias.splice(banco.diarias.indexOf(diaria), 1)
      return semConteudo()
    },
  ],
]
