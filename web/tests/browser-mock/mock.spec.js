import { test, expect } from '@playwright/test'

async function entrar(page, login) {
  await page.goto('/login')
  await expect(page.getByTestId('mock-logins')).toBeVisible()
  await page.getByTestId('login-name').fill(login)
  await page.getByTestId('login-password').fill('mock')
  await page.getByTestId('login-submit').click()
  await expect(page.getByTestId('profile-menu')).toBeVisible()
}

async function cadastrarCultura(page, nome) {
  await page.getByTestId('add-setting').click()
  await page.getByTestId('field-name').fill(nome)
  await page.getByTestId('save-modal').click()
}

test('operador cadastra cultura, vê o 422 da repetida e perde tudo ao recarregar', async ({
  page,
}) => {
  const nome = 'Cultura Fictícia Mock'
  await entrar(page, 'operador.mock')
  await expect(page.getByTestId('mock-banner')).toHaveText(
    'Modo mock: dados fictícios em memória, apagados ao recarregar',
  )
  await page.goto('/configuracoes')
  await expect(page.getByTestId('culture-row-1')).toContainText('Milho')
  await cadastrarCultura(page, nome)
  await expect(page.getByTestId('culture-row-10')).toContainText(nome)

  await cadastrarCultura(page, nome)
  await expect(page.getByTestId('form-error')).toHaveText(
    'Já existe uma cultura com este nome.',
  )
  await page.getByTestId('cancel-modal').click()

  await page.reload()
  await expect(page.getByTestId('culture-row-1')).toContainText('Milho')
  await expect(page.getByTestId('culture-row-10')).toHaveCount(0)
})

test('consulta não vê as ações de gravação', async ({ page }) => {
  await entrar(page, 'consulta.mock')
  await expect(page.getByTestId('new-record')).toHaveCount(0)
  await page.goto('/configuracoes')
  await expect(page.getByTestId('culture-row-1')).toBeVisible()
  await expect(page.getByTestId('add-setting')).toHaveCount(0)
  await expect(page.getByTestId('edit-culture-1')).toHaveCount(0)
  await page.goto('/produtores')
  await expect(page.getByTestId('add-producer')).toHaveCount(0)
})
