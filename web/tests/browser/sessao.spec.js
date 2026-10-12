import { test, expect } from '@playwright/test'
class LoginPage {
  constructor(page) {
    this.page = page
  }
  campo(id) {
    return this.page.getByTestId(id)
  }
  async abrir() {
    await this.page.goto('/login')
  }
  async logar(login = 'operador-ficticio', senha = 'senha-sintetica') {
    await this.campo('login-name').fill(login)
    await this.campo('login-password').fill(senha)
    await this.campo('login-submit').click()
  }
  async sair() {
    await this.campo('profile-menu').click()
    await this.campo('logout').click()
  }
  async sessao() {
    return this.page.evaluate(() =>
      JSON.parse(sessionStorage.getItem('agrogestao-session')),
    )
  }
}
function resposta(perfil = 'LANCAMENTO') {
  return {
    token: 'token-sintetico',
    expiraEm: new Date(Date.now() + 3600000).toISOString(),
    usuario: { id: 1, nome: 'Operador Fictício', perfil },
  }
}
async function apiFicticia(page, perfil) {
  const sessao = resposta(perfil)
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.endsWith('/auth/login'))
      await route.fulfill({ json: sessao })
    else if (url.pathname.endsWith('/auth/me'))
      await route.fulfill({ json: sessao.usuario })
    else
      await route.fulfill({
        json: { itens: [], pagina: 0, tamanho: 100, total: 0 },
      })
  })
}
test('login restaura a sessão e sair apaga o token', async ({ page }) => {
  await apiFicticia(page, 'LANCAMENTO')
  const tela = new LoginPage(page)
  await tela.abrir()
  await tela.logar()
  await expect(tela.campo('profile-menu')).toContainText('Operador Fictício')
  expect((await tela.sessao()).token).toBe('token-sintetico')
  await page.reload()
  await expect(tela.campo('profile-menu')).toContainText('Operador Fictício')
  await tela.sair()
  await expect(page).toHaveURL(/\/login$/)
  expect(await tela.sessao()).toBeNull()
})
test('CONSULTA oculta gravações e rota sem sessão vai para login', async ({
  page,
}) => {
  const tela = new LoginPage(page)
  await page.goto('/produtores')
  await expect(page).toHaveURL(/\/login$/)
  await apiFicticia(page, 'CONSULTA')
  await tela.logar()
  await expect(tela.campo('new-record')).toHaveCount(0)
  await page.goto('/produtores')
  await expect(tela.campo('add-producer')).toHaveCount(0)
})
test('401 limpa a sessão e exibe a mensagem da API no login', async ({
  page,
}) => {
  await apiFicticia(page, 'LANCAMENTO')
  const tela = new LoginPage(page)
  await tela.abrir()
  await tela.logar()
  await expect(tela.campo('profile-menu')).toBeVisible()
  await page.route('**/api/v1/auth/me', (route) =>
    route.fulfill({
      status: 401,
      json: { mensagem: 'Faça login para continuar.', campos: [] },
    }),
  )
  await page.reload()
  await expect(page).toHaveURL(/\/login$/)
  expect(await tela.sessao()).toBeNull()
  await page.route('**/api/v1/auth/login', (route) =>
    route.fulfill({
      status: 401,
      json: { mensagem: 'Login ou senha inválidos.', campos: [] },
    }),
  )
  await tela.logar()
  await expect(page.getByRole('alert')).toHaveText('Login ou senha inválidos.')
})
test('token expirado não restaura sessão', async ({ page }) => {
  await page.goto('/login')
  await page.evaluate(() =>
    sessionStorage.setItem(
      'agrogestao-session',
      JSON.stringify({
        token: 'expirado',
        expiraEm: '2000-01-01T00:00:00Z',
        user: { perfil: 'ADMIN' },
      }),
    ),
  )
  await page.goto('/configuracoes')
  await expect(page).toHaveURL(/\/login$/)
})
test('login, Bearer, catálogo e erro usam a API real com mock desligado', async ({
  page,
}) => {
  test.skip(
    !process.env.AGROGESTAO_API_REAL,
    'Executar com npm run test:api-real',
  )
  const tela = new LoginPage(page)
  const requests = []
  page.on('request', (request) => {
    if (request.url().includes('/api/v1/culturas'))
      requests.push(request.headers())
  })
  await tela.abrir()
  await tela.logar(
    process.env.AGROGESTAO_TEST_LOGIN,
    process.env.AGROGESTAO_TEST_SENHA,
  )
  await expect(tela.campo('profile-menu')).toContainText('Administrador')
  await page.goto('/configuracoes')
  await expect(tela.campo('culture-row-1')).toBeVisible()
  const sessao = await tela.sessao()
  expect(
    requests.some(
      (headers) => headers.authorization === `Bearer ${sessao.token}`,
    ),
  ).toBe(true)
  await tela.sair()
  await tela.logar('login-inexistente-sintetico', 'senha-errada')
  await expect(page.getByRole('alert')).toHaveText('Login ou senha inválidos.')
  await page.screenshot({
    path: 'test-results/login-api-real.png',
    fullPage: true,
  })
})
