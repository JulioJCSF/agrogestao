import { test, expect } from '@playwright/test'
import { LoginPage } from '../pages/LoginPage.js'

const MINUTO = 60_000
const mensagem =
  'Sua sessão foi encerrada após 30 minutos sem atividade. Entre novamente.'

async function entrar(page) {
  await page.clock.install()
  const sessao = {
    token: 'token-sintetico-inatividade',
    expiraEm: new Date(Date.now() + 3_600_000).toISOString(),
    usuario: { id: 1, nome: 'Operador Fictício', perfil: 'LANCAMENTO' },
  }
  await page.route('**/api/v1/**', (route) => {
    const caminho = new URL(route.request().url()).pathname
    return route.fulfill({
      json: caminho.endsWith('/auth/login')
        ? sessao
        : caminho.endsWith('/auth/me')
          ? sessao.usuario
          : { itens: [], pagina: 0, tamanho: 100, total: 0 },
    })
  })
  const tela = new LoginPage(page)
  await tela.entrarComo()
  await page.clock.pauseAt(
    new Date(await page.evaluate(() => Date.now() + 1000)),
  )
  await page.evaluate(() => window.dispatchEvent(new Event('pointerdown')))
  return tela
}

test('deveEncerrarSessaoQuandoCompletarTrintaMinutosSemInteracao', async ({
  page,
}) => {
  const tela = await entrar(page)
  await page.clock.fastForward(30 * MINUTO - 1)
  await expect(tela.campo('profile-menu')).toBeVisible()
  await page.clock.fastForward(1)
  await expect(page).toHaveURL(/\/login$/)
  await expect(tela.campo('session-message')).toHaveText(mensagem)
  expect(await tela.sessao()).toBeNull()
  await tela.logar()
  await expect(tela.campo('profile-menu')).toBeVisible()
  await tela.sair()
  await expect(tela.campo('session-message')).toHaveCount(0)
})

for (const evento of [
  'pointerdown',
  'pointermove',
  'keydown',
  'scroll',
  'touchstart',
]) {
  test(`deveRenovarPrazoQuandoHouverInteracaoDe${evento}`, async ({ page }) => {
    const tela = await entrar(page)
    await page.clock.fastForward(29 * MINUTO)
    await page.evaluate((tipo) => window.dispatchEvent(new Event(tipo)), evento)
    await page.clock.fastForward(29 * MINUTO)
    await expect(tela.campo('profile-menu')).toBeVisible()
    await page.clock.fastForward(MINUTO)
    await expect(page).toHaveURL(/\/login$/)
    expect(await tela.sessao()).toBeNull()
  })
}

test('devePreservarUltimaAtividadeQuandoRecarregarPagina', async ({ page }) => {
  const tela = await entrar(page)
  await page.clock.fastForward(29 * MINUTO)
  await page.reload()
  await expect(tela.campo('profile-menu')).toBeVisible()
  await page.clock.fastForward(MINUTO)
  await expect(page).toHaveURL(/\/login$/)
  await expect(tela.campo('session-message')).toHaveText(mensagem)
})

test('deveRecusarRenovacaoQuandoVoltarDeSuspensaoAposPrazo', async ({
  page,
}) => {
  const tela = await entrar(page)
  await page.clock.setSystemTime(new Date(Date.now() + 31 * MINUTO))
  await page.evaluate(() => window.dispatchEvent(new Event('pointerdown')))
  await expect(page).toHaveURL(/\/login$/)
  await expect(tela.campo('session-message')).toHaveText(mensagem)
})

test('deveRecusarRestauracaoQuandoPrazoVencerDurantePaginaFechada', async ({
  page,
}) => {
  const tela = await entrar(page)
  await page.clock.setSystemTime(new Date(Date.now() + 31 * MINUTO))
  await page.reload()
  await expect(page).toHaveURL(/\/login$/)
  expect(await tela.sessao()).toBeNull()
  await expect(tela.campo('session-message')).toHaveText(mensagem)
})

test('deveExpirarTokenMesmoQuandoOperadorContinuaAtivo', async ({ page }) => {
  const tela = await entrar(page)
  for (let i = 0; i < 3; i++) {
    await page.clock.fastForward(19 * MINUTO)
    await page.evaluate(() => window.dispatchEvent(new Event('keydown')))
  }
  await expect(tela.campo('profile-menu')).toBeVisible()
  await page.clock.fastForward(3 * MINUTO)
  await expect(page).toHaveURL(/\/login$/)
  expect(await tela.sessao()).toBeNull()
  await expect(tela.campo('session-message')).toHaveCount(0)
})

test('devePreservarInteracaoQuandoRespostaDeRestauracaoChegarDepois', async ({
  page,
}) => {
  const tela = await entrar(page)
  await page.clock.fastForward(29 * MINUTO)
  let liberar
  const respostaPendente = new Promise((resolve) => {
    liberar = resolve
  })
  await page.route('**/api/v1/auth/me', async (route) => {
    await respostaPendente
    await route.fulfill({
      json: { id: 1, nome: 'Operador Fictício', perfil: 'LANCAMENTO' },
    })
  })
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.evaluate(() => window.dispatchEvent(new Event('keydown')))
  liberar()
  await expect(tela.campo('profile-menu')).toBeVisible()
  await page.clock.fastForward(29 * MINUTO)
  await expect(tela.campo('profile-menu')).toBeVisible()
  await page.clock.fastForward(MINUTO)
  await expect(page).toHaveURL(/\/login$/)
})

test('deveManterSessaoQuandoMouseMoverACadaCincoSegundos', async ({ page }) => {
  const tela = await entrar(page)
  for (let i = 0; i < (40 * MINUTO) / 5000; i++) {
    await page.clock.fastForward(5000)
    await page.mouse.move(10 + (i % 2), 10)
  }
  await expect(tela.campo('profile-menu')).toBeVisible()
  await page.clock.fastForward(30 * MINUTO)
  await expect(page).toHaveURL(/\/login$/)
  await expect(tela.campo('session-message')).toHaveText(mensagem)
})

test('deveGravarUltimaAtividadeQuandoSessaoSalvaNaoTiver', async ({
  page,
  context,
}) => {
  await entrar(page)
  await page.evaluate(() => {
    const sessao = JSON.parse(sessionStorage.getItem('agrogestao-session'))
    delete sessao.ultimaAtividade
    sessionStorage.setItem('agrogestao-session', JSON.stringify(sessao))
  })
  // Sem resposta do /auth/me, só a leitura da sessão pode gravar a última atividade.
  await page.route('**/api/v1/auth/me', () => {})
  await page.reload({ waitUntil: 'domcontentloaded' })
  const salva = await page.evaluate(() => ({
    sessao: sessionStorage.getItem('agrogestao-session'),
    agora: Date.now(),
  }))
  // Reabre a aba a partir do que ficou gravado, sem a página anterior encerrar a sessão ao sair.
  const reaberta = await context.newPage()
  await reaberta.clock.install({ time: salva.agora + 31 * MINUTO })
  await reaberta.addInitScript((sessao) => {
    sessionStorage.setItem('agrogestao-session', sessao)
  }, salva.sessao)
  await reaberta.goto('/')
  const tela = new LoginPage(reaberta)
  await expect(reaberta).toHaveURL(/\/login$/)
  await expect(tela.campo('session-message')).toHaveText(mensagem)
})
