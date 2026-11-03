import { banco, buscar, proximoId, usuarioResposta } from '../banco.js'
import {
  criado,
  idDoCaminho,
  pagina,
  regraNegocio,
  semConteudo,
  validacao,
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

const UsuarioSenhaRequisicao = {
  senha: { tipo: 'texto', obrigatorio: true, min: 8, max: 72 },
}

// Mensagens do UsuarioService (M01).
const LOGIN_REPETIDO = 'Já existe um usuário com este login.'
const NAO_ENCONTRADO = 'Usuário não encontrado.'
const ULTIMO_ADMIN = 'O sistema precisa de pelo menos um administrador ativo.'
const SENHA_LONGA = 'A senha deve ter no máximo 72 bytes.'

/** O BCrypt da API só considera 72 bytes: senha acentuada pode passar no tamanho e não aqui. */
function exigirSenhaNoLimite(senha) {
  if (new TextEncoder().encode(senha).length > 72)
    throw validacao([{ campo: 'senha', mensagem: SENHA_LONGA }], SENHA_LONGA)
}

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
      exigirSenhaNoLimite(dto.senha)
      // Decisão de 03/11: login aparado, em minúsculas e único sem diferenciar caixa.
      const login = dto.login.trim().toLowerCase()
      if (banco.usuarios.some((item) => item.login.toLowerCase() === login))
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
      // Decisão de 03/11: nenhuma alteração deixa o sistema sem ADMIN ativo.
      const deixaDeSerAdminAtivo =
        usuario.perfil === 'ADMIN' &&
        usuario.ativo &&
        (dto.perfil !== 'ADMIN' || !dto.ativo)
      if (
        deixaDeSerAdminAtivo &&
        !banco.usuarios.some(
          (item) => item.id !== id && item.perfil === 'ADMIN' && item.ativo,
        )
      )
        throw regraNegocio(ULTIMO_ADMIN)
      Object.assign(usuario, {
        nome: dto.nome.trim(),
        perfil: dto.perfil,
        ativo: dto.ativo,
      })
      return usuarioResposta(usuario)
    },
  ],
  [
    'put',
    '/usuarios/:id/senha',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, UsuarioSenhaRequisicao)
      exigirSenhaNoLimite(dto.senha)
      const usuario = buscar('usuarios', id, NAO_ENCONTRADO)
      usuario.senha = dto.senha
      return semConteudo()
    },
  ],
]
