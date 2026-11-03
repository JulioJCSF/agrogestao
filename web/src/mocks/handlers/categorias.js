import { banco, buscar, mensagensNaoEncontrado, proximoId } from '../banco.js'
import {
  criado,
  idDoCaminho,
  pagina,
  regraNegocio,
  semConteudo,
  validar,
} from '../respostas.js'

const CategoriaDespesaRequisicao = {
  nome: { tipo: 'texto', obrigatorio: true, max: 80 },
}

const NAO_ENCONTRADO = mensagensNaoEncontrado.categorias

function validarEditavel(categoria) {
  if (categoria.sistema)
    throw regraNegocio(
      'Categoria de sistema não pode ser alterada nem removida.',
    )
}

function validarNome(nome, id) {
  if (
    banco.categorias.some(
      (item) =>
        item.nome.toLowerCase() === nome.toLowerCase() && item.id !== id,
    )
  )
    throw regraNegocio('Já existe uma categoria com este nome.')
}

export default [
  [
    'get',
    '/categorias-despesa',
    ({ url }) => {
      const termo = (url.searchParams.get('busca') || '').toLowerCase()
      return pagina(
        url,
        banco.categorias.filter((item) =>
          item.nome.toLowerCase().includes(termo),
        ),
      )
    },
  ],
  [
    'post',
    '/categorias-despesa',
    ({ corpo }) => {
      const nome = validar(corpo, CategoriaDespesaRequisicao).nome.trim()
      validarNome(nome, null)
      const categoria = { id: proximoId('categorias'), nome, sistema: false }
      banco.categorias.push(categoria)
      return criado(`/categorias-despesa/${categoria.id}`, categoria)
    },
  ],
  [
    'get',
    '/categorias-despesa/:id',
    ({ params }) => buscar('categorias', idDoCaminho(params), NAO_ENCONTRADO),
  ],
  [
    'put',
    '/categorias-despesa/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const nome = validar(corpo, CategoriaDespesaRequisicao).nome.trim()
      const categoria = buscar('categorias', id, NAO_ENCONTRADO)
      validarEditavel(categoria)
      validarNome(nome, id)
      categoria.nome = nome
      return categoria
    },
  ],
  [
    'delete',
    '/categorias-despesa/:id',
    ({ params }) => {
      const categoria = buscar(
        'categorias',
        idDoCaminho(params),
        NAO_ENCONTRADO,
      )
      validarEditavel(categoria)
      if (
        banco.despesas.some((item) => item.categoriaDespesaId === categoria.id)
      )
        throw regraNegocio(
          'Esta categoria já foi usada em despesas e não pode ser excluída.',
        )
      banco.categorias.splice(banco.categorias.indexOf(categoria), 1)
      return semConteudo()
    },
  ],
]
