import { buscar, mensagensNaoEncontrado } from '../banco.js'
import {
  MENSAGEM_VALOR_INVALIDO,
  hoje,
  idDoCaminho,
  validacao,
} from '../respostas.js'

/**
 * Exemplos fixos de apuração, coerentes com a massa do banco.js: o mock não refaz a apuração
 * (RN19–RN24), que pertence à API. Plantio criado em memória responde com valores zerados, e
 * o filtro por mês é aceito mas não muda os números.
 */
export const APURACAO_FIXA = {
  1: {
    quantidadeProduzida: 120.5,
    quantidadeVendida: 50,
    receita: 125,
    custoDesembolso: 280,
    valorMaoObraFamiliar: 0,
    custoComMaoObraFamiliar: 280,
    resultado: -155,
  },
  2: {
    quantidadeProduzida: 80,
    quantidadeVendida: 0,
    receita: 0,
    custoDesembolso: 50,
    valorMaoObraFamiliar: 120,
    custoComMaoObraFamiliar: 170,
    resultado: -50,
  },
  3: {
    quantidadeProduzida: 300,
    quantidadeVendida: 200,
    receita: 360,
    custoDesembolso: 80,
    valorMaoObraFamiliar: 0,
    custoComMaoObraFamiliar: 80,
    resultado: 280,
  },
}

const ZERADO = {
  quantidadeProduzida: 0,
  quantidadeVendida: 0,
  receita: 0,
  custoDesembolso: 0,
  valorMaoObraFamiliar: 0,
  custoComMaoObraFamiliar: 0,
  resultado: 0,
}

export function resultadoFixo(plantio) {
  const cultura = buscar('culturas', plantio.culturaId)
  const { quantidadeProduzida, quantidadeVendida, ...valores } =
    APURACAO_FIXA[plantio.id] ?? ZERADO
  return {
    plantioId: plantio.id,
    culturaNome: cultura.nome,
    propriedadeNome: buscar('propriedades', plantio.propriedadeId).nome,
    tipoCiclo: plantio.tipoCiclo,
    periodoInicio: plantio.dataInicio,
    periodoFim: plantio.dataEncerramento ?? hoje(),
    quantidadeProduzida,
    quantidadeVendida,
    unidadeMedida: cultura.unidadeMedida,
    ...valores,
  }
}

export default [
  [
    'get',
    '/plantios/:id/resultado',
    ({ params, url }) => {
      const id = idDoCaminho(params)
      const mes = url.searchParams.get('mes')
      if (mes && !/^\d{4}-(0[1-9]|1[0-2])$/.test(mes))
        throw validacao([{ campo: 'mes', mensagem: MENSAGEM_VALOR_INVALIDO }])
      return resultadoFixo(
        buscar('plantios', id, mensagensNaoEncontrado.plantios),
      )
    },
  ],
]
