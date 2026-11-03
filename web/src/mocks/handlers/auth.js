import { banco } from '../banco.js'
import { tokenDe } from '../autenticacao.js'
import { ErroApi, validar } from '../respostas.js'

const LoginRequisicao = {
  login: { tipo: 'texto', obrigatorio: true },
  senha: { tipo: 'texto', obrigatorio: true },
}

const usuarioLogado = ({ id, nome, perfil }) => ({ id, nome, perfil })

export default [
  [
    'post',
    '/auth/login',
    ({ corpo }) => {
      const { login, senha } = validar(corpo, LoginRequisicao)
      const usuario = banco.usuarios.find((item) => item.login === login)
      if (!usuario || usuario.senha !== senha || !usuario.ativo)
        throw new ErroApi(401, 'NAO_AUTENTICADO', 'Login ou senha inválidos.')
      const expiraEm = new Date(Date.now() + 60 * 60 * 1000)
      expiraEm.setMilliseconds(0)
      return {
        token: tokenDe(usuario.login),
        expiraEm: expiraEm.toISOString().replace('.000Z', 'Z'),
        usuario: usuarioLogado(usuario),
      }
    },
  ],
  ['get', '/auth/me', ({ usuario }) => usuarioLogado(usuario)],
]
