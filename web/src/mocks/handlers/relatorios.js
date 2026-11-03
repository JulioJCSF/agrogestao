import { banco, buscar, mensagensNaoEncontrado, proximoId } from '../banco.js'
import {
  criado,
  idDoCaminho,
  pagina,
  regraNegocio,
  validar,
} from '../respostas.js'
import { resultadoFixo } from './resultado.js'

const EmissaoRelatorioRequisicao = {
  produtorId: { tipo: 'inteiro', obrigatorio: true, positivo: true },
  periodoInicio: { tipo: 'data', obrigatorio: true },
  periodoFim: { tipo: 'data', obrigatorio: true },
}

// A emissão ainda é 501 na API (A03a). Mensagem do 422 proposta em
// docs/qa/casos-de-teste-relatorio.md (RN37).
const SEM_CONSENTIMENTO =
  'O produtor ainda não tem consentimento registrado. Registre o consentimento antes de emitir o relatório.'
const NAO_ENCONTRADO = 'Relatório não encontrado.'

/** Totais fixos por produtor, coerentes com APURACAO_FIXA; o período não muda os números. */
const TOTAIS_FIXOS = {
  1: {
    receita: 125,
    custoDesembolso: 330,
    resultado: -205,
    valorMaoObraFamiliar: 120,
  },
  2: {
    receita: 360,
    custoDesembolso: 80,
    resultado: 280,
    valorMaoObraFamiliar: 0,
  },
}

/**
 * Conteúdo gravado na emissão (RN31/RN34): cópia dos cadastros e dos lançamentos do período,
 * com a apuração de exemplo. O mock não apura; os valores vêm de resultado.js.
 */
function conteudo(produtor, inicio, fim) {
  const plantios = banco.plantios.filter(
    (item) => item.produtorId === produtor.id,
  )
  const ids = plantios.map((item) => item.id)
  const noPeriodo = (item) => inicio <= item.dataFato && item.dataFato <= fim
  return structuredClone({
    produtor,
    propriedades: banco.propriedades.filter(
      (item) => item.produtorId === produtor.id,
    ),
    plantios: plantios.map(resultadoFixo),
    despesas: banco.despesas.filter(
      (item) => item.produtorId === produtor.id && noPeriodo(item),
    ),
    diarias: banco.diarias.filter(
      (item) => ids.includes(item.plantioId) && noPeriodo(item),
    ),
    ...(TOTAIS_FIXOS[produtor.id] ?? {
      receita: 0,
      custoDesembolso: 0,
      resultado: 0,
      valorMaoObraFamiliar: 0,
    }),
    ressalvaValorDeclarado:
      'Valores declarados pelo produtor, não comprovados por documento fiscal.',
    ressalvaFinalidade:
      'Este documento não constitui garantia de aceitação para qualquer finalidade externa.',
  })
}

export default [
  [
    'post',
    '/relatorios',
    ({ corpo, usuario }) => {
      const dto = validar(corpo, EmissaoRelatorioRequisicao)
      const produtor = buscar(
        'produtores',
        dto.produtorId,
        mensagensNaoEncontrado.produtores,
      )
      if (dto.periodoFim < dto.periodoInicio)
        throw regraNegocio('O fim do período não pode ser anterior ao início.')
      if (produtor.consentimentoEm == null)
        throw regraNegocio(SEM_CONSENTIMENTO)
      const emissao = {
        id: proximoId('relatorios'),
        produtorId: produtor.id,
        usuarioId: usuario.id,
        dataEmissao: new Date().toISOString(),
        periodoInicio: dto.periodoInicio,
        periodoFim: dto.periodoFim,
        conteudo: conteudo(produtor, dto.periodoInicio, dto.periodoFim),
      }
      banco.relatorios.push(emissao)
      return criado(`/relatorios/${emissao.id}`, emissao)
    },
  ],
  [
    'get',
    '/produtores/:produtorId/relatorios',
    ({ params, url }) => {
      const produtorId = idDoCaminho(params, 'produtorId')
      buscar('produtores', produtorId, mensagensNaoEncontrado.produtores)
      return pagina(
        url,
        banco.relatorios.filter((item) => item.produtorId === produtorId),
      )
    },
  ],
  [
    'get',
    '/relatorios/:id',
    ({ params }) => buscar('relatorios', idDoCaminho(params), NAO_ENCONTRADO),
  ],
]
