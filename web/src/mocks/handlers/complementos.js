import { banco, buscar, mensagensNaoEncontrado } from '../banco.js'
import {
  escalar,
  hoje,
  idDoCaminho,
  numero,
  parametro,
  regraNegocio,
  validacao,
} from '../respostas.js'
import { resultadoFixo } from './resultado.js'

// O painel usa os resultados fixos de resultado.js; o cálculo pertence à API. Os totais são a
// soma dos plantios devolvidos, como na API, para a tela não mostrar total que não bate.

export default [
  [
    'get',
    '/plantios/:id/saldo',
    ({ params }) => {
      const plantio = buscar(
        'plantios',
        idDoCaminho(params),
        mensagensNaoEncontrado.plantios,
      )
      // Exceção à regra dos cálculos fixos (M05): a M06 exibe o saldo e testa contra o mock.
      const somar = (quantidades) =>
        quantidades.reduce((soma, q) => soma + escalar(q, 3), 0n)
      const produzido = somar(
        banco.producoes
          .filter((p) => p.plantioId === plantio.id)
          .map((p) => p.quantidade),
      )
      const vendido = somar(
        banco.vendas
          .flatMap((v) => v.itens)
          .filter((i) => i.plantioId === plantio.id)
          .map((i) => i.quantidade),
      )
      return {
        plantioId: plantio.id,
        quantidadeProduzida: numero(produzido, 3),
        quantidadeVendida: numero(vendido, 3),
        saldo: numero(produzido - vendido, 3),
        unidadeMedida: buscar('culturas', plantio.culturaId).unidadeMedida,
      }
    },
  ],
  [
    'get',
    '/painel',
    ({ url }) => {
      const produtorId = parametro(url, 'produtorId', 'inteiro')
      if (produtorId != null && produtorId <= 0)
        throw validacao([
          { campo: 'produtorId', mensagem: 'deve ser maior que 0' },
        ])
      // Decisão de 04/11: sem período, de 01/01 do ano corrente até hoje; só início, até hoje;
      // só fim, desde 01/01 do ano do fim.
      const periodoFim = parametro(url, 'periodoFim', 'data') ?? hoje()
      const periodoInicio =
        parametro(url, 'periodoInicio', 'data') ??
        `${periodoFim.slice(0, 4)}-01-01`
      if (periodoFim < periodoInicio)
        throw regraNegocio('O fim do período não pode ser anterior ao início.')
      if (produtorId != null)
        buscar('produtores', produtorId, mensagensNaoEncontrado.produtores)
      // Sem produtor, o painel geral; entram os plantios ativos em algum dia do período.
      const plantios = banco.plantios
        .filter(
          (plantio) =>
            (produtorId == null || plantio.produtorId === produtorId) &&
            plantio.dataInicio <= periodoFim &&
            (plantio.dataEncerramento == null ||
              plantio.dataEncerramento >= periodoInicio),
        )
        .map((plantio) => ({
          ...resultadoFixo(plantio),
          periodoInicio,
          periodoFim,
        }))
      const somar = (campo) =>
        numero(
          plantios.reduce(
            (soma, plantio) => soma + escalar(plantio[campo], 2),
            0n,
          ),
          2,
        )
      return {
        periodoInicio,
        periodoFim,
        receita: somar('receita'),
        custoDesembolso: somar('custoDesembolso'),
        resultado: somar('resultado'),
        plantios,
      }
    },
  ],
]
