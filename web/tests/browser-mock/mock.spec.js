import { test, expect } from '@playwright/test'
import { ConfiguracoesPage } from '../pages/ConfiguracoesPage.js'
import { LoginPage } from '../pages/LoginPage.js'

async function entrar(page, login) {
  const tela = new LoginPage(page)
  await tela.abrir()
  await expect(tela.avisoDoMock()).toBeVisible()
  await tela.logar(login, 'mock')
  await expect(tela.campo('profile-menu')).toBeVisible()
  return tela
}

test('operador cadastra cultura, vê o 422 da repetida e perde tudo ao recarregar', async ({
  page,
}) => {
  const nome = 'Cultura Fictícia Mock'
  const login = await entrar(page, 'operador.mock')
  await expect(login.faixaDoMock()).toHaveText(
    'Modo mock: dados fictícios em memória, apagados ao recarregar',
  )
  const configuracoes = new ConfiguracoesPage(page)
  await configuracoes.abrir()
  await expect(configuracoes.linhaCultura(1)).toContainText('Milho')
  await configuracoes.cadastrarCultura({ nome })
  await expect(configuracoes.linhaCultura(10)).toContainText(nome)

  await configuracoes.cadastrarCultura({ nome })
  await expect(configuracoes.erroGeral()).toHaveText(
    'Já existe uma cultura com este nome.',
  )
  await configuracoes.cancelar()

  await page.reload()
  await expect(configuracoes.linhaCultura(1)).toContainText('Milho')
  await expect(configuracoes.linhaCultura(10)).toHaveCount(0)
})

test('consulta não vê as ações de gravação', async ({ page }) => {
  await entrar(page, 'consulta.mock')
  await expect(page.getByTestId('new-record')).toHaveCount(0)
  const configuracoes = new ConfiguracoesPage(page)
  await configuracoes.abrir()
  await expect(configuracoes.linhaCultura(1)).toBeVisible()
  await expect(configuracoes.acaoCadastrar()).toHaveCount(0)
  await expect(configuracoes.acoesDeEdicao()).toHaveCount(0)
  await page.goto('/produtores')
  await expect(page.getByTestId('add-producer')).toHaveCount(0)
})
