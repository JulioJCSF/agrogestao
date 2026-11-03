import { banco, buscar, mensagensNaoEncontrado, proximoId } from '../banco.js'
import {
  criado,
  dataBr,
  hoje,
  idDoCaminho,
  pagina,
  parametro,
  regraNegocio,
  validar,
} from '../respostas.js'
import { TIPOS_CICLO } from './culturas.js'

const PlantioRequisicao = {
  propriedadeId: { tipo: 'inteiro', obrigatorio: true, positivo: true },
  culturaId: { tipo: 'inteiro', obrigatorio: true, positivo: true },
  area: { tipo: 'decimal', obrigatorio: true, digitos: [9, 3] },
  tipoCiclo: { tipo: TIPOS_CICLO },
  dataInicio: { tipo: 'data', obrigatorio: true },
  previsaoColheita: { tipo: 'data' },
}

const EncerramentoPlantioRequisicao = {
  dataEncerramento: { tipo: 'data', obrigatorio: true },
}

const NAO_ENCONTRADO = mensagensNaoEncontrado.plantios

/** RN03, com a mesma mensagem no encerramento e na alteração do início. */
function exigirEncerramentoAposInicio(inicio, encerramento) {
  if (encerramento < inicio)
    throw regraNegocio(
      `A data de encerramento não pode ser anterior à data de início (${dataBr(inicio)}).`,
    )
}

/** Maior data do fato entre produção, despesa (pelo rateio), diária e item de venda. */
function ultimoLancamento(plantioId) {
  return [
    ...banco.producoes.filter((item) => item.plantioId === plantioId),
    ...banco.despesas.filter((item) =>
      item.rateios.some((rateio) => rateio.plantioId === plantioId),
    ),
    ...banco.diarias.filter((item) => item.plantioId === plantioId),
    ...banco.vendas.filter((item) =>
      item.itens.some((itemVenda) => itemVenda.plantioId === plantioId),
    ),
  ]
    .map((item) => item.dataFato)
    .sort()
    .at(-1)
}

/** produtorAtual é nulo na criação; na alteração, trava o plantio no mesmo produtor. */
function preencher(plantio, dto, produtorAtual) {
  if (Number(dto.area) <= 0)
    throw regraNegocio('A área do plantio deve ser maior que zero.')
  if (dto.dataInicio > hoje())
    throw regraNegocio('A data de início não pode ser posterior a hoje.')
  if (plantio.dataEncerramento)
    exigirEncerramentoAposInicio(dto.dataInicio, plantio.dataEncerramento)
  const propriedade = buscar(
    'propriedades',
    dto.propriedadeId,
    mensagensNaoEncontrado.propriedades,
  )
  if (produtorAtual != null && produtorAtual !== propriedade.produtorId)
    throw regraNegocio(
      'O plantio não pode ser transferido para propriedade de outro produtor.',
    )
  const cultura = buscar(
    'culturas',
    dto.culturaId,
    mensagensNaoEncontrado.culturas,
  )
  // RN38: com produção registrada, a nova cultura precisa ter a mesma unidade.
  if (
    plantio.id &&
    banco.producoes.some((producao) => producao.plantioId === plantio.id) &&
    buscar('culturas', plantio.culturaId).unidadeMedida.trim().toLowerCase() !==
      cultura.unidadeMedida.trim().toLowerCase()
  )
    throw regraNegocio(
      'A cultura do plantio deve manter a unidade da produção já registrada.',
    )
  return Object.assign(plantio, {
    produtorId: propriedade.produtorId,
    propriedadeId: propriedade.id,
    culturaId: cultura.id,
    area: Number(dto.area),
    tipoCiclo: dto.tipoCiclo ?? cultura.tipoCicloPadrao,
    dataInicio: dto.dataInicio,
    previsaoColheita: dto.previsaoColheita,
  })
}

export default [
  [
    'get',
    '/plantios',
    ({ url }) => {
      const produtorId = parametro(url, 'produtorId', 'inteiro')
      const culturaId = parametro(url, 'culturaId', 'inteiro')
      const situacao = parametro(url, 'situacao', ['ABERTO', 'ENCERRADO'])
      const periodoInicio = parametro(url, 'periodoInicio', 'data')
      const periodoFim = parametro(url, 'periodoFim', 'data')
      if (periodoInicio && periodoFim && periodoFim < periodoInicio)
        throw regraNegocio('O fim do período não pode ser anterior ao início.')
      return pagina(
        url,
        banco.plantios.filter(
          (plantio) =>
            (produtorId == null || plantio.produtorId === produtorId) &&
            (culturaId == null || plantio.culturaId === culturaId) &&
            (situacao == null || plantio.situacao === situacao) &&
            (periodoInicio == null || plantio.dataInicio >= periodoInicio) &&
            (periodoFim == null || plantio.dataInicio <= periodoFim),
        ),
      )
    },
  ],
  [
    'post',
    '/plantios',
    ({ corpo }) => {
      const dto = validar(corpo, PlantioRequisicao)
      const plantio = preencher({ id: null }, dto, null)
      Object.assign(plantio, {
        id: proximoId('plantios'),
        dataEncerramento: null,
        situacao: 'ABERTO',
      })
      banco.plantios.push(plantio)
      return criado(`/plantios/${plantio.id}`, plantio)
    },
  ],
  [
    'get',
    '/plantios/:id',
    ({ params }) => buscar('plantios', idDoCaminho(params), NAO_ENCONTRADO),
  ],
  [
    'put',
    '/plantios/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, PlantioRequisicao)
      const plantio = buscar('plantios', id, NAO_ENCONTRADO)
      return preencher(plantio, dto, plantio.produtorId)
    },
  ],
  [
    'post',
    '/plantios/:id/encerramento',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const { dataEncerramento } = validar(corpo, EncerramentoPlantioRequisicao)
      const plantio = buscar('plantios', id, NAO_ENCONTRADO)
      if (plantio.dataEncerramento)
        throw regraNegocio(
          `O plantio já foi encerrado em ${dataBr(plantio.dataEncerramento)}.`,
        )
      if (dataEncerramento > hoje())
        throw regraNegocio(
          'A data de encerramento não pode ser posterior a hoje.',
        )
      exigirEncerramentoAposInicio(plantio.dataInicio, dataEncerramento)
      const ultimo = ultimoLancamento(plantio.id)
      if (ultimo && ultimo > dataEncerramento)
        throw regraNegocio(
          `Há lançamento em ${dataBr(ultimo)}, posterior à data de encerramento informada.`,
        )
      Object.assign(plantio, { dataEncerramento, situacao: 'ENCERRADO' })
      return criado(`/plantios/${plantio.id}`, plantio)
    },
  ],
]
