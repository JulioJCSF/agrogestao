import { banco, buscar, mensagensNaoEncontrado } from '../banco.js'
import { escalar, hoje, idDoCaminho, numero, parametro } from '../respostas.js'
import { resultadoFixo } from './resultado.js'

// O painel é exemplo fixo, coerente com a massa; o cálculo pertence à API.

const PAINEL_FIXO = { receita: 485, custoDesembolso: 410, resultado: 75 }

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
    ({ url }) => ({
      periodoInicio: parametro(url, 'periodoInicio', 'data') ?? '2026-01-01',
      periodoFim: parametro(url, 'periodoFim', 'data') ?? hoje(),
      ...PAINEL_FIXO,
      plantios: banco.plantios.map(resultadoFixo),
    }),
  ],
]
