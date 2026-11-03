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
  numero,
  pagina,
  parametro,
  regraNegocio,
  semConteudo,
  validar,
} from '../respostas.js'

const DespesaRequisicao = {
  produtorId: { tipo: 'inteiro', obrigatorio: true, positivo: true },
  categoriaDespesaId: { tipo: 'inteiro', obrigatorio: true, positivo: true },
  descricao: { tipo: 'texto', obrigatorio: true, max: 255 },
  valorTotal: { tipo: 'decimal', obrigatorio: true, digitos: [10, 2] },
  dataFato: { tipo: 'data', obrigatorio: true },
  plantioIds: { lista: true, tipo: 'inteiro', positivo: true },
}

const NAO_ENCONTRADO = mensagensNaoEncontrado.despesas

/**
 * Rateio da RN07–RN09, o único cálculo que o mock refaz, porque a tela de despesa mostra os
 * rateios logo depois de gravar. Mesmo truncamento do RateioDespesaService: cada parcela é
 * truncada (DOWN) em centavos, o residual vai para o plantio de maior área e, no empate, para o
 * de menor id. O percentual é informativo, em 6 casas com HALF_UP. Valores em inteiros
 * (centavos e milésimos de área) para não arredondar em ponto flutuante.
 */
export function ratear(valorTotal, plantios) {
  const total = escalar(valorTotal, 2)
  const areas = plantios.map((plantio) => escalar(plantio.area, 3))
  const soma = areas.reduce((acumulado, area) => acumulado + area, 0n)
  const provisorios = areas.map((area) => (total * area) / soma)
  const residual =
    total - provisorios.reduce((acumulado, valor) => acumulado + valor, 0n)
  const destino = plantios.reduce((maior, plantio, indice) =>
    areas[indice] > areas[plantios.indexOf(maior)] ||
    (areas[indice] === areas[plantios.indexOf(maior)] && plantio.id < maior.id)
      ? plantio
      : maior,
  )
  return plantios
    .map((plantio, indice) => ({
      plantioId: plantio.id,
      percentual: numero((areas[indice] * 2000000n + soma) / (2n * soma), 6),
      valorRateado: numero(
        provisorios[indice] + (plantio === destino ? residual : 0n),
        2,
      ),
    }))
    .sort((a, b) => a.plantioId - b.plantioId)
}

/** Validações 2 a 9 da L04a, na ordem do DespesaService. */
function validarVinculos(dto) {
  if (Number(dto.valorTotal) <= 0)
    throw regraNegocio('O valor da despesa deve ser maior que zero.')
  const ids = dto.plantioIds
  if (!ids?.length)
    throw regraNegocio(
      'A despesa precisa estar vinculada a pelo menos um plantio.',
    )
  if (new Set(ids).size !== ids.length)
    throw regraNegocio('O mesmo plantio foi informado mais de uma vez.')
  buscar('produtores', dto.produtorId, mensagensNaoEncontrado.produtores)
  const categoria = buscar(
    'categorias',
    dto.categoriaDespesaId,
    mensagensNaoEncontrado.categorias,
  )
  const plantios = ids.map((id) =>
    buscar('plantios', id, mensagensNaoEncontrado.plantios),
  )
  if (categoria.sistema)
    throw regraNegocio(
      'A categoria Diária é lançada automaticamente pela diária paga e não pode ser usada em despesa manual.',
    )
  if (plantios.some((plantio) => plantio.produtorId !== dto.produtorId))
    throw regraNegocio(
      'Todos os plantios da despesa devem ser do produtor informado.',
    )
  if (escalar(dto.valorTotal, 2) < BigInt(ids.length))
    throw regraNegocio(
      'O valor da despesa não cobre um centavo por plantio. Vincule menos plantios.',
    )
  validarDataFato(dto.dataFato, plantios)
  return plantios
}

/** RN16: a despesa gerada por diária só muda pela diária. */
function exigirDespesaManual(despesa) {
  if (despesa.diariaId != null)
    throw regraNegocio(
      'Esta despesa foi gerada por uma diária paga. Altere ou exclua a diária.',
    )
}

function preencher(despesa, dto) {
  return Object.assign(despesa, {
    produtorId: dto.produtorId,
    categoriaDespesaId: dto.categoriaDespesaId,
    descricao: dto.descricao,
    valorTotal: Number(dto.valorTotal),
    dataFato: dto.dataFato,
  })
}

export default [
  [
    'get',
    '/despesas',
    ({ url }) => {
      const produtorId = parametro(url, 'produtorId', 'inteiro')
      const periodoInicio = parametro(url, 'periodoInicio', 'data')
      const periodoFim = parametro(url, 'periodoFim', 'data')
      return pagina(
        url,
        banco.despesas.filter(
          (despesa) =>
            (produtorId == null || despesa.produtorId === produtorId) &&
            (periodoInicio == null || despesa.dataFato >= periodoInicio) &&
            (periodoFim == null || despesa.dataFato <= periodoFim),
        ),
      )
    },
  ],
  [
    'post',
    '/despesas',
    ({ corpo }) => {
      const dto = validar(corpo, DespesaRequisicao)
      const plantios = validarVinculos(dto)
      const despesa = preencher(
        { id: proximoId('despesas'), diariaId: null },
        dto,
      )
      Object.assign(despesa, {
        dataColeta: hoje(),
        estornada: false,
        rateios: ratear(dto.valorTotal, plantios),
      })
      banco.despesas.push(despesa)
      return criado(`/despesas/${despesa.id}`, despesa)
    },
  ],
  [
    'get',
    '/despesas/:id',
    ({ params }) => buscar('despesas', idDoCaminho(params), NAO_ENCONTRADO),
  ],
  [
    'put',
    '/despesas/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, DespesaRequisicao)
      const despesa = buscar('despesas', id, NAO_ENCONTRADO)
      exigirDespesaManual(despesa)
      const plantios = validarVinculos(dto)
      // RN05: o rateio só é recalculado se mudarem o valor ou o conjunto de plantios.
      const atuais = despesa.rateios.map((rateio) => rateio.plantioId).sort()
      const recalcular =
        escalar(despesa.valorTotal, 2) !== escalar(dto.valorTotal, 2) ||
        atuais.join() !== [...dto.plantioIds].sort().join()
      preencher(despesa, dto)
      if (recalcular) despesa.rateios = ratear(dto.valorTotal, plantios)
      return despesa
    },
  ],
  [
    'delete',
    '/despesas/:id',
    ({ params }) => {
      const despesa = buscar('despesas', idDoCaminho(params), NAO_ENCONTRADO)
      exigirDespesaManual(despesa)
      // Exclusão manual ainda é 501 na API (L04b); o mock remove a despesa e seus rateios (RN11).
      banco.despesas.splice(banco.despesas.indexOf(despesa), 1)
      return semConteudo()
    },
  ],
  [
    'get',
    '/plantios/:plantioId/despesas',
    ({ params, url }) => {
      const plantioId = idDoCaminho(params, 'plantioId')
      const periodoInicio = parametro(url, 'periodoInicio', 'data')
      const periodoFim = parametro(url, 'periodoFim', 'data')
      buscar('plantios', plantioId, mensagensNaoEncontrado.plantios)
      return pagina(
        url,
        banco.despesas
          .filter(
            (despesa) =>
              (periodoInicio == null || despesa.dataFato >= periodoInicio) &&
              (periodoFim == null || despesa.dataFato <= periodoFim),
          )
          .flatMap((despesa) =>
            despesa.rateios
              .filter((rateio) => rateio.plantioId === plantioId)
              .map(({ percentual, valorRateado }) => ({
                id: despesa.id,
                produtorId: despesa.produtorId,
                categoriaDespesaId: despesa.categoriaDespesaId,
                diariaId: despesa.diariaId,
                descricao: despesa.descricao,
                dataFato: despesa.dataFato,
                estornada: despesa.estornada,
                percentual,
                valorRateado,
              })),
          ),
      )
    },
  ],
  [
    'post',
    '/despesas/:id/estorno',
    ({ params }) => {
      const despesa = buscar('despesas', idDoCaminho(params), NAO_ENCONTRADO)
      // A03b ainda decide o efeito do estorno; o mock só marca a despesa (RN19).
      despesa.estornada = true
      return criado(`/despesas/${despesa.id}`, despesa)
    },
  ],
]
