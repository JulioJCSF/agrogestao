import { http, HttpResponse } from 'msw/http'
import { exigirPerfil } from '../autenticacao.js'
import { ErroApi, erro } from '../respostas.js'
import auth from './auth.js'
import categorias from './categorias.js'
import clientes from './clientes.js'
import complementos from './complementos.js'
import culturas from './culturas.js'
import despesas from './despesas.js'
import diarias from './diarias.js'
import plantios from './plantios.js'
import producao from './producao.js'
import produtores from './produtores.js'
import propriedades from './propriedades.js'
import referencias from './referencias.js'
import relatorios from './relatorios.js'
import resultado from './resultado.js'
import usuarios from './usuarios.js'
import vendas from './vendas.js'

const definicoes = [
  auth,
  produtores,
  propriedades,
  culturas,
  plantios,
  producao,
  categorias,
  despesas,
  referencias,
  diarias,
  clientes,
  vendas,
  resultado,
  relatorios,
  usuarios,
  complementos,
].flat()

/** Método e caminho de cada handler, para o teste de cobertura comparar com os controllers. */
export const rotas = definicoes.map(([metodo, caminho]) => ({
  metodo: metodo.toUpperCase(),
  caminho: `/api/v1${caminho}`,
}))

async function lerCorpo(request) {
  const texto = await request.text()
  if (!texto) return undefined
  try {
    return JSON.parse(texto)
  } catch {
    // Texto que não é JSON chega ao validar() como corpo inválido (400).
    return texto
  }
}

/**
 * Cada rota segue a ordem da API: segurança (401/403), conversão do caminho e do corpo (400),
 * regra do serviço (404/422). O resolver devolve o DTO ou uma Response pronta.
 */
export const handlers = definicoes.map(([metodo, caminho, resolver]) =>
  http[metodo](`*/api/v1${caminho}`, async ({ request, params }) => {
    try {
      const usuario = exigirPerfil(request, metodo, caminho)
      const corpo = ['post', 'put'].includes(metodo)
        ? await lerCorpo(request)
        : undefined
      const resposta = await resolver({
        params,
        url: new URL(request.url),
        corpo,
        usuario,
      })
      return resposta instanceof Response
        ? resposta
        : HttpResponse.json(structuredClone(resposta))
    } catch (causa) {
      if (causa instanceof ErroApi)
        return erro(causa.status, causa.codigo, causa.message, causa.campos)
      throw causa
    }
  }),
)
