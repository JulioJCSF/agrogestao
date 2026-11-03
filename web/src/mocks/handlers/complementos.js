import { banco, buscar, mensagensNaoEncontrado } from '../banco.js'
import { hoje, idDoCaminho, parametro } from '../respostas.js'
import { APURACAO_FIXA, resultadoFixo } from './resultado.js'

// Saldo e painel são exemplos fixos, coerentes com a massa; o cálculo pertence à API.
const SALDO_FIXO = { 1: 70.5, 2: 80, 3: 100 }

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
      const fixo = APURACAO_FIXA[plantio.id]
      return {
        plantioId: plantio.id,
        quantidadeProduzida: fixo?.quantidadeProduzida ?? 0,
        quantidadeVendida: fixo?.quantidadeVendida ?? 0,
        saldo: SALDO_FIXO[plantio.id] ?? 0,
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
