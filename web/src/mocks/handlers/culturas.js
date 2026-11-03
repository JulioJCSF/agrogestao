import { banco, buscar, mensagensNaoEncontrado, proximoId } from '../banco.js'
import {
  criado,
  idDoCaminho,
  pagina,
  regraNegocio,
  validar,
} from '../respostas.js'

export const TIPOS_CICLO = ['CURTO', 'LONGO']

const CulturaRequisicao = {
  nome: { tipo: 'texto', obrigatorio: true, max: 100 },
  tipoCicloPadrao: { tipo: TIPOS_CICLO, obrigatorio: true },
  unidadeMedida: { tipo: 'texto', obrigatorio: true, max: 10 },
}

const mesmaUnidade = (a, b) => a.trim().toLowerCase() === b.trim().toLowerCase()

function preencher(cultura, dto) {
  const nome = dto.nome.trim()
  if (
    banco.culturas.some(
      (item) =>
        item.nome.toLowerCase() === nome.toLowerCase() &&
        item.id !== cultura.id,
    )
  )
    throw regraNegocio('Já existe uma cultura com este nome.')
  // RN38: a unidade não muda depois que há produção nos plantios da cultura.
  if (
    cultura.id &&
    !mesmaUnidade(cultura.unidadeMedida, dto.unidadeMedida) &&
    banco.producoes.some(
      (producao) =>
        banco.plantios.find((plantio) => plantio.id === producao.plantioId)
          ?.culturaId === cultura.id,
    )
  )
    throw regraNegocio(
      'A unidade da cultura não pode mudar porque já há produção registrada.',
    )
  return Object.assign(cultura, {
    nome,
    tipoCicloPadrao: dto.tipoCicloPadrao,
    unidadeMedida: dto.unidadeMedida.trim(),
  })
}

export default [
  [
    'get',
    '/culturas',
    ({ url }) => {
      const termo = (url.searchParams.get('busca') || '').toLowerCase()
      return pagina(
        url,
        banco.culturas.filter((item) =>
          item.nome.toLowerCase().includes(termo),
        ),
      )
    },
  ],
  [
    'post',
    '/culturas',
    ({ corpo }) => {
      const cultura = preencher({ id: null }, validar(corpo, CulturaRequisicao))
      cultura.id = proximoId('culturas')
      banco.culturas.push(cultura)
      return criado(`/culturas/${cultura.id}`, cultura)
    },
  ],
  [
    'get',
    '/culturas/:id',
    ({ params }) =>
      buscar('culturas', idDoCaminho(params), mensagensNaoEncontrado.culturas),
  ],
  [
    'put',
    '/culturas/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, CulturaRequisicao)
      return preencher(
        buscar('culturas', id, mensagensNaoEncontrado.culturas),
        dto,
      )
    },
  ],
]
