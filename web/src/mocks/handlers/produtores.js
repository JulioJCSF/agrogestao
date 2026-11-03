import { banco, buscar, mensagensNaoEncontrado, proximoId } from '../banco.js'
import {
  criado,
  idDoCaminho,
  pagina,
  regraNegocio,
  semConteudo,
  validar,
} from '../respostas.js'

const ProdutorRequisicao = {
  nome: { tipo: 'texto', obrigatorio: true, max: 150 },
  documento: {
    tipo: 'texto',
    obrigatorio: true,
    padrao: {
      regex: /^[0-9]{11}$/,
      mensagem: 'Informe os 11 dígitos do CPF, sem máscara.',
    },
    cpf: 'CPF inválido. Confira os dígitos.',
  },
  endereco: { tipo: 'texto', max: 255 },
  profissao: { tipo: 'texto', max: 100 },
  contato: { tipo: 'texto', max: 100 },
  consentimentoEm: {
    tipo: 'data',
    passadoOuPresente: 'A data do consentimento não pode ser futura.',
  },
  ativo: { tipo: 'booleano', obrigatorio: true },
}

const DOCUMENTO_DUPLICADO = 'Já existe produtor cadastrado com este CPF.'
const NAO_ENCONTRADO = mensagensNaoEncontrado.produtores

function preencher(produtor, dto) {
  if (
    banco.produtores.some(
      (item) => item.documento === dto.documento && item.id !== produtor.id,
    )
  )
    throw regraNegocio(DOCUMENTO_DUPLICADO, [
      { campo: 'documento', mensagem: DOCUMENTO_DUPLICADO },
    ])
  Object.assign(produtor, {
    ...dto,
    consentimentoPendente: dto.consentimentoEm == null,
  })
  return produtor
}

/** RF09: produtor com histórico é inativado, nunca excluído. */
function exigirExcluivel(id) {
  const plantios = banco.plantios
    .filter((plantio) => plantio.produtorId === id)
    .map((plantio) => plantio.id)
  if (
    banco.despesas.some((despesa) => despesa.produtorId === id) ||
    banco.vendas.some((venda) => venda.produtorId === id) ||
    banco.producoes.some((producao) => plantios.includes(producao.plantioId)) ||
    banco.diarias.some((diaria) => plantios.includes(diaria.plantioId))
  )
    throw regraNegocio(
      'Este produtor possui lançamentos e não pode ser excluído. Inative o cadastro.',
    )
  if (
    banco.propriedades.some((propriedade) => propriedade.produtorId === id) ||
    banco.relatorios.some((emissao) => emissao.produtorId === id)
  )
    throw regraNegocio(
      'Este produtor possui propriedades ou relatórios vinculados e não pode ser excluído. Inative o cadastro.',
    )
}

export default [
  [
    'get',
    '/produtores',
    ({ url }) => {
      const termo = (url.searchParams.get('busca') || '').trim().toLowerCase()
      const digitos = termo.replace(/\D/g, '')
      return pagina(
        url,
        banco.produtores.filter(
          (produtor) =>
            produtor.nome.toLowerCase().includes(termo) ||
            (digitos && produtor.documento.includes(digitos)),
        ),
      )
    },
  ],
  [
    'post',
    '/produtores',
    ({ corpo }) => {
      const dto = validar(corpo, ProdutorRequisicao)
      const produtor = preencher({ id: null }, dto)
      produtor.id = proximoId('produtores')
      banco.produtores.push(produtor)
      return criado(`/produtores/${produtor.id}`, produtor)
    },
  ],
  [
    'get',
    '/produtores/:id',
    ({ params }) => buscar('produtores', idDoCaminho(params), NAO_ENCONTRADO),
  ],
  [
    'put',
    '/produtores/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, ProdutorRequisicao)
      return preencher(buscar('produtores', id, NAO_ENCONTRADO), dto)
    },
  ],
  [
    'delete',
    '/produtores/:id',
    ({ params }) => {
      const produtor = buscar('produtores', idDoCaminho(params), NAO_ENCONTRADO)
      exigirExcluivel(produtor.id)
      banco.produtores.splice(banco.produtores.indexOf(produtor), 1)
      return semConteudo()
    },
  ],
]
