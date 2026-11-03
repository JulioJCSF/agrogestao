import { expect } from '@playwright/test'
import { LoginPage } from './LoginPage.js'

// Apoio aos specs que rodam contra a API real (npm run test:api-real): dados sintéticos
// (RNF10) e chamadas diretas com o Bearer da sessão aberta pela tela.

export const unico = () => crypto.randomUUID().slice(0, 8)

/** CPF sintético válido com prefixo 999 (RNF10). */
export function cpfSintetico() {
  const base = [9, 9, 9]
  while (base.length < 9) base.push(Math.floor(Math.random() * 10))
  for (const tamanho of [9, 10]) {
    const soma = base
      .slice(0, tamanho)
      .reduce((total, digito, i) => total + digito * (tamanho + 1 - i), 0)
    const resto = (soma * 10) % 11
    base.push(resto === 10 ? 0 : resto)
  }
  return base.join('')
}

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
