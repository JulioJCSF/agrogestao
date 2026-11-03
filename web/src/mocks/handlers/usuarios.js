import { banco, buscar, proximoId, usuarioResposta } from '../banco.js'
import {
  criado,
  idDoCaminho,
  pagina,
  regraNegocio,
  validar,
} from '../respostas.js'

const PERFIS = ['LANCAMENTO', 'CONSULTA', 'ADMIN']

const UsuarioRequisicao = {
  nome: { tipo: 'texto', obrigatorio: true, max: 150 },
  login: { tipo: 'texto', obrigatorio: true, max: 100 },
  senha: { tipo: 'texto', obrigatorio: true, min: 8, max: 72 },
  perfil: { tipo: PERFIS, obrigatorio: true },
}

const UsuarioAlteracaoRequisicao = {
  nome: { tipo: 'texto', obrigatorio: true, max: 150 },
  perfil: { tipo: PERFIS, obrigatorio: true },
  ativo: { tipo: 'booleano', obrigatorio: true },
}

// Usuários ainda são 501 na API; mensagens propostas pelo mock.
const LOGIN_REPETIDO = 'Já existe usuário com este login.'
const NAO_ENCONTRADO = 'Usuário não encontrado.'

export default [
  [
    'get',
    '/usuarios',
    ({ url }) => pagina(url, banco.usuarios.map(usuarioResposta)),
  ],
  [
    'post',
    '/usuarios',
    ({ corpo }) => {
      const dto = validar(corpo, UsuarioRequisicao)
      const login = dto.login.trim()
      if (
        banco.usuarios.some(
          (item) => item.login.toLowerCase() === login.toLowerCase(),
        )
      )
        throw regraNegocio(LOGIN_REPETIDO, [
          { campo: 'login', mensagem: LOGIN_REPETIDO },
        ])
      // A senha fica só em memória, para o novo usuário poder entrar no mock.
      const usuario = {
        id: proximoId('usuarios'),
        nome: dto.nome.trim(),
        login,
        perfil: dto.perfil,
        ativo: true,
        senha: dto.senha,
      }
      banco.usuarios.push(usuario)
      return criado(`/usuarios/${usuario.id}`, usuarioResposta(usuario))
    },
  ],
  [
    'put',
    '/usuarios/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, UsuarioAlteracaoRequisicao)
      const usuario = buscar('usuarios', id, NAO_ENCONTRADO)
      Object.assign(usuario, {
        nome: dto.nome.trim(),
        perfil: dto.perfil,
        ativo: dto.ativo,
      })
      return usuarioResposta(usuario)
    },
  ],
]
