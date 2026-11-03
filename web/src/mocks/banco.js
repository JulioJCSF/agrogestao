import {
  dataBr,
  digitosCpf,
  hoje,
  naoEncontrado,
  regraNegocio,
} from './respostas.js'

/**
 * Estado em memória do mock, semeado com dados fictícios (RNF10). Recarregar a página reinicia.
 * Os registros têm o formato das respostas da API (DTOs da F13); o que é só interno, como a
 * senha do usuário, fica em campo próprio e não sai nas respostas.
 */
export const banco = {}
const sequencias = {}

/** CPF fictício com prefixo 999 e dígitos verificadores válidos, no padrão dos *IT da API. */
export function cpfFicticio(sequencial) {
  const base = `999${String(sequencial).padStart(6, '0')}`
  return base + digitosCpf(base)
}

export function proximoId(colecao) {
  sequencias[colecao] = (sequencias[colecao] ?? 0) + 1
  return sequencias[colecao]
}

function semear(colecao, registros) {
  banco[colecao] = registros.map((registro, indice) => ({
    id: indice + 1,
    ...registro,
  }))
  sequencias[colecao] = registros.length
}

const produtor = (
  nome,
  sequencial,
  endereco,
  profissao,
  contato,
  consentimentoEm,
) => ({
  nome,
  documento: cpfFicticio(sequencial),
  endereco,
  profissao,
  contato,
  consentimentoEm,
  ativo: true,
  consentimentoPendente: consentimentoEm == null,
})

export function reiniciar() {
  semear('usuarios', [
    {
      nome: 'Administrador Fictício',
      login: 'admin.mock',
      perfil: 'ADMIN',
      ativo: true,
      senha: 'mock',
    },
    {
      nome: 'Operador Fictício',
      login: 'operador.mock',
      perfil: 'LANCAMENTO',
      ativo: true,
      senha: 'mock',
    },
    {
      nome: 'Consulta Fictícia',
      login: 'consulta.mock',
      perfil: 'CONSULTA',
      ativo: true,
      senha: 'mock',
    },
  ])
  semear('produtores', [
    produtor(
      'Produtor Fictício Um',
      1,
      'Comunidade Fictícia Norte',
      'Agricultor',
      'Contato Fictício Um',
      '2026-01-10',
    ),
    produtor(
      'Produtora Fictícia Dois',
      2,
      'Comunidade Fictícia Sul',
      'Agricultora',
      'Contato Fictício Dois',
      '2026-02-02',
    ),
    // Sem consentimento: a emissão de relatório responde 422 (RN37).
    produtor(
      'Produtor Fictício Três',
      3,
      'Comunidade Fictícia Leste',
      'Agricultor',
      null,
      null,
    ),
  ])
  semear('propriedades', [
    {
      produtorId: 1,
      nome: 'Sítio Fictício Alto',
      areaTotal: 5.5,
      localizacao: 'Localização fictícia norte',
    },
    {
      produtorId: 2,
      nome: 'Sítio Fictício Baixo',
      areaTotal: 3.25,
      localizacao: 'Localização fictícia sul',
    },
    {
      produtorId: 3,
      nome: 'Sítio Fictício Médio',
      areaTotal: 2,
      localizacao: null,
    },
  ])
  // Culturas e categorias da migration de dados iniciais.
  semear(
    'culturas',
    [
      ['Milho', 'LONGO'],
      ['Feijão', 'LONGO'],
      ['Fava', 'LONGO'],
      ['Mandioca', 'LONGO'],
      ['Urucum', 'LONGO'],
      ['Castanha', 'LONGO'],
      ['Caju', 'LONGO'],
      ['Banana', 'CURTO'],
      ['Hortaliça', 'CURTO'],
    ].map(([nome, tipoCicloPadrao]) => ({
      nome,
      tipoCicloPadrao,
      unidadeMedida: 'kg',
    })),
  )
  semear(
    'categorias',
    [
      ['Diária', true],
      ['Sementes e mudas', false],
      ['Adubo e fertilizantes', false],
      ['Defensivos', false],
      ['Combustível', false],
      ['Manutenção de equipamentos', false],
      ['Transporte', false],
      ['Embalagem', false],
      ['Energia e água', false],
      ['Outros', false],
    ].map(([nome, sistema]) => ({ nome, sistema })),
  )
  semear('plantios', [
    {
      produtorId: 1,
      propriedadeId: 1,
      culturaId: 1,
      area: 2,
      tipoCiclo: 'LONGO',
      dataInicio: '2026-02-01',
      previsaoColheita: '2026-08-30',
      dataEncerramento: null,
      situacao: 'ABERTO',
    },
    {
      produtorId: 1,
      propriedadeId: 1,
      culturaId: 2,
      area: 1,
      tipoCiclo: 'LONGO',
      dataInicio: '2026-03-01',
      previsaoColheita: '2026-09-15',
      dataEncerramento: null,
      situacao: 'ABERTO',
    },
    {
      produtorId: 2,
      propriedadeId: 2,
      culturaId: 8,
      area: 1.5,
      tipoCiclo: 'CURTO',
      dataInicio: '2026-01-15',
      previsaoColheita: '2026-05-30',
      dataEncerramento: '2026-06-30',
      situacao: 'ENCERRADO',
    },
  ])
  semear('producoes', [
    {
      plantioId: 1,
      quantidade: 120.5,
      unidadeMedida: 'kg',
      dataFato: '2026-07-10',
      dataColeta: '2026-07-11',
    },
    {
      plantioId: 3,
      quantidade: 300,
      unidadeMedida: 'kg',
      dataFato: '2026-05-20',
      dataColeta: '2026-05-21',
    },
    {
      plantioId: 2,
      quantidade: 80,
      unidadeMedida: 'kg',
      dataFato: '2026-08-05',
      dataColeta: '2026-08-06',
    },
  ])
  semear('referencias', [
    { valor: 80, vigenciaInicio: '2026-01-01', vigenciaFim: null },
  ])
  semear('diarias', [
    {
      plantioId: 1,
      valorReferenciaDiariaId: null,
      natureza: 'PAGA',
      quantidadeDias: 2,
      valorUnitario: 90,
      valorTotal: 180,
      dataFato: '2026-07-10',
      dataColeta: '2026-07-11',
    },
    {
      plantioId: 2,
      valorReferenciaDiariaId: 1,
      natureza: 'FAMILIAR',
      quantidadeDias: 1.5,
      valorUnitario: 80,
      valorTotal: 120,
      dataFato: '2026-08-05',
      dataColeta: '2026-08-06',
    },
  ])
  semear('despesas', [
    {
      produtorId: 1,
      categoriaDespesaId: 2,
      diariaId: null,
      descricao: 'Sementes fictícias',
      valorTotal: 150,
      dataFato: '2026-02-05',
      dataColeta: '2026-02-06',
      estornada: false,
      rateios: [
        { plantioId: 1, percentual: 0.666667, valorRateado: 100 },
        { plantioId: 2, percentual: 0.333333, valorRateado: 50 },
      ],
    },
    {
      produtorId: 2,
      categoriaDespesaId: 3,
      diariaId: null,
      descricao: 'Adubo fictício',
      valorTotal: 80,
      dataFato: '2026-02-20',
      dataColeta: '2026-02-20',
      estornada: false,
      rateios: [{ plantioId: 3, percentual: 1, valorRateado: 80 }],
    },
    {
      // Gerada pela diária paga 1 (RN16).
      produtorId: 1,
      categoriaDespesaId: 1,
      diariaId: 1,
      descricao: 'Diária paga de 10/07/2026',
      valorTotal: 180,
      dataFato: '2026-07-10',
      dataColeta: '2026-07-11',
      estornada: false,
      rateios: [{ plantioId: 1, percentual: 1, valorRateado: 180 }],
    },
  ])
  semear(
    'clientes',
    [
      ['Mercearia Fictícia', 'COMERCIO'],
      ['Feirante Fictício', 'FEIRA'],
      ['Atacadista Fictício', 'CEASA'],
      ['Escola Fictícia', 'MERENDA'],
      ['Cooperativa Fictícia', 'COOPERATIVA'],
      ['Comprador Fictício', 'OUTRO'],
    ].map(([nome, tipoCanal]) => ({ nome, tipoCanal })),
  )
  semear('itensVenda', [])
  sequencias.itensVenda = 2
  semear('vendas', [
    {
      produtorId: 1,
      clienteId: 2,
      dataFato: '2026-07-20',
      dataColeta: '2026-07-20',
      valorTotal: 125,
      avisos: [],
      itens: [
        {
          id: 1,
          plantioId: 1,
          quantidade: 50,
          unidadeMedida: 'kg',
          valorUnitario: 2.5,
          valorTotal: 125,
        },
      ],
    },
    {
      produtorId: 2,
      clienteId: 3,
      dataFato: '2026-06-01',
      dataColeta: '2026-06-02',
      valorTotal: 360,
      avisos: [],
      itens: [
        {
          id: 2,
          plantioId: 3,
          quantidade: 200,
          unidadeMedida: 'kg',
          valorUnitario: 1.8,
          valorTotal: 360,
        },
      ],
    },
  ])
  semear('relatorios', [])
}

reiniciar()

export function buscar(colecao, id, mensagem) {
  const registro = banco[colecao].find((item) => item.id === Number(id))
  if (!registro) throw naoEncontrado(mensagem)
  return registro
}

export const mensagensNaoEncontrado = {
  produtores: 'Produtor não encontrado.',
  propriedades: 'Propriedade não encontrada.',
  culturas: 'Cultura não encontrada.',
  plantios: 'Plantio não encontrado.',
  producoes: 'Produção não encontrada.',
  categorias: 'Categoria de despesa não encontrada.',
  despesas: 'Despesa não encontrada.',
  referencias: 'Valor de referência da diária não encontrado.',
  diarias: 'Diária não encontrada.',
  clientes: 'Cliente não encontrado.',
}

/** LancamentoService.validarDataFato: RN35 (não futura) e RN04 (não após o encerramento). */
export function validarDataFato(dataFato, plantios) {
  if (dataFato > hoje())
    throw regraNegocio('A data do fato não pode ser posterior a hoje.')
  const encerramentos = plantios
    .map((plantio) => plantio.dataEncerramento)
    .filter((data) => data && dataFato > data)
    .sort()
  if (encerramentos.length)
    throw regraNegocio(
      `O plantio foi encerrado em ${dataBr(encerramentos[0])}. A data do fato não pode ser posterior ao encerramento.`,
    )
}

/** Relatório emitido para o produtor cobrindo a data (RN11). */
export const existeEmissaoNoPeriodo = (produtorId, data) =>
  banco.relatorios.some(
    (emissao) =>
      emissao.produtorId === produtorId &&
      emissao.periodoInicio <= data &&
      data <= emissao.periodoFim,
  )

/** Leitura sem expor campos internos (a senha do usuário). */
export const usuarioResposta = ({ id, nome, login, perfil, ativo }) => ({
  id,
  nome,
  login,
  perfil,
  ativo,
})
