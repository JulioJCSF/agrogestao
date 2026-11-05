import { expect } from '@playwright/test'
import { LoginPage } from './LoginPage.js'
import { cpfSintetico as cpfSinteticoComGerador } from '../../scripts/sintetico.mjs'

// Apoio aos specs que rodam contra a API real (npm run test:api-real): dados sintéticos
// (RNF10) e chamadas diretas com o Bearer da sessão aberta pela tela.

export const unico = () => crypto.randomUUID().slice(0, 8)

/** CPF sintético válido com prefixo 999 (RNF10). */
export const cpfSintetico = () => cpfSinteticoComGerador(Math.random)

/** Chamadas diretas à API com o Bearer da sessão aberta pela tela. */
export async function apiDaSessao(page) {
  const { token } = await new LoginPage(page).sessao()
  const headers = { Authorization: `Bearer ${token}` }
  return {
    async get(caminho) {
      const resposta = await page.request.get(`/api/v1${caminho}`, { headers })
      expect(resposta.status()).toBe(200)
      return resposta.json()
    },
    async post(caminho, data) {
      const resposta = await page.request.post(`/api/v1${caminho}`, {
        headers,
        data,
      })
      expect(resposta.status(), await resposta.text()).toBe(201)
      return resposta.json()
    },
    /** Sem conferir o status: para os casos em que a API deve recusar. */
    async requisitar(metodo, caminho, data) {
      const resposta = await page.request.fetch(`/api/v1${caminho}`, {
        method: metodo,
        headers,
        data,
      })
      const texto = await resposta.text()
      return {
        status: resposta.status(),
        corpo: texto ? JSON.parse(texto) : undefined,
      }
    },
  }
}
