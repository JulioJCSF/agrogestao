import { chromium } from '@playwright/test'
import { createServer } from 'vite'

// Smoke das rotas com o mock da API (F14): entra pelo login do mock e confere que cada tela
// renderiza. Usa o navegador porque os dados chegam pela rede, depois da renderização.
process.env.VITE_USAR_MOCK = 'true'
const vite = await createServer({
  server: { host: '127.0.0.1', port: 4175, strictPort: true },
})
await vite.listen()
const browser = await chromium.launch(
  process.env.PLAYWRIGHT_CHANNEL
    ? { channel: process.env.PLAYWRIGHT_CHANNEL }
    : {},
)
try {
  const page = await browser.newPage({ baseURL: 'http://127.0.0.1:4175' })
  async function entrar(login) {
    await page.goto('/login')
    await page.getByTestId('login-name').fill(login)
    await page.getByTestId('login-password').fill('mock')
    await page.getByTestId('login-submit').click()
    await page.getByTestId('profile-menu').waitFor()
  }
  await entrar('operador.mock')
  const expected = new Map([
    ['/', 'Visão geral'],
    ['/produtores', 'Produtores'],
    ['/produtores/1', 'Produtor Fictício Um'],
    ['/producao', 'Produção'],
    ['/plantios/1', 'Milho'],
    ['/financeiro', 'Financeiro'],
    ['/vendas', 'Vendas'],
    ['/relatorios', 'Relatórios'],
    ['/configuracoes', 'Configurações'],
  ])
  for (const [path, heading] of expected) {
    await page.goto(path)
    try {
      await page
        .locator('main')
        .getByText(heading)
        .first()
        .waitFor({ timeout: 10000 })
    } catch {
      throw new Error(`Falha ao renderizar ${path}: ${heading} ausente`)
    }
    console.log(`OK ${path}`)
  }
  await page.getByTestId('profile-menu').click()
  await page.getByTestId('logout').click()
  await entrar('admin.mock')
  await page.goto('/configuracoes')
  await page.getByTestId('tab-usuarios').waitFor()
  console.log('OK visualização ADMIN')
} finally {
  await browser.close()
  await vite.close()
}
