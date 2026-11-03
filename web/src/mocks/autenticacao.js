import { banco } from './banco.js'
import { ErroApi } from './respostas.js'

// Mensagens do entry point e do access denied handler da API.
const NAO_AUTENTICADO = 'Você não está autenticado. Faça login para continuar.'
const ACESSO_NEGADO = 'Você não tem permissão para realizar esta operação.'

/** Token fictício por usuário; não é JWT e só vale dentro do mock. */
export const tokenDe = (login) => `mock-${login}`

function usuarioDoToken(request) {
  const cabecalho = request.headers.get('Authorization') || ''
  const token = cabecalho.startsWith('Bearer ') ? cabecalho.slice(7) : null
  return banco.usuarios.find(
    (usuario) => usuario.ativo && token === tokenDe(usuario.login),
  )
}

export function exigirSessao(request) {
  const usuario = usuarioDoToken(request)
  if (!usuario) throw new ErroApi(401, 'NAO_AUTENTICADO', NAO_AUTENTICADO)
  return usuario
}

/**
 * Mesma ordem da SecurityConfig: login livre; /auth/me exige sessão; /usuarios/** só ADMIN;
 * leitura para os três perfis; escrita para LANCAMENTO e ADMIN.
 */
export function exigirPerfil(request, metodo, caminho) {
  if (metodo === 'post' && caminho === '/auth/login') return null
  const usuario = exigirSessao(request)
  const permitidos = caminho.startsWith('/usuarios')
    ? ['ADMIN']
    : metodo === 'get'
      ? ['CONSULTA', 'LANCAMENTO', 'ADMIN']
      : ['LANCAMENTO', 'ADMIN']
  if (!permitidos.includes(usuario.perfil))
    throw new ErroApi(403, 'ACESSO_NEGADO', ACESSO_NEGADO)
  return usuario
}
