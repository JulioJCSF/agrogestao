import { test, expect } from '@playwright/test'
import { LoginPage } from '../pages/LoginPage.js'
import { PlantioPage } from '../pages/PlantioPage.js'

test('mock: detalhe, resultado e encerramento com RN04 histórico', async ({
  page,
}) => {
  await new LoginPage(page).entrarComo('operador.mock', 'mock')
  const tela = new PlantioPage(page)
  await tela.abrirPelaLista(1)
  await expect(tela.campo('planting-result')).toContainText('155,00')
  await tela.informarEncerramento('31/07/2026')
  await tela.pedirConfirmacao()
  await tela.confirmar()
  await expect(tela.campo('planting-status')).toHaveText('Encerrado')
  await expect(tela.campo('close-planting')).toHaveCount(0)
  await tela.campo('add-production-planting').click()
  await tela.campo('field-amount').fill('2')
  await tela.campo('field-date').fill('01/08/2026')
  await tela.campo('save-modal').click()
  await expect(tela.campo('form-error')).toContainText('31/07/2026')
  await tela.campo('field-date').fill('31/07/2026')
  await tela.campo('save-modal').click()
  await expect(tela.campo('close-modal')).toHaveCount(0)
  await expect(tela.secao('production')).toContainText('2 kg')
})

test('mock: data inválida mantém aberto e CONSULTA não grava', async ({
  page,
}) => {
  const login = new LoginPage(page)
  await login.entrarComo('operador.mock', 'mock')
  const tela = new PlantioPage(page)
  await tela.abrir(1)
  await tela.informarEncerramento('01/01/2020')
  await tela.pedirConfirmacao()
  await tela.confirmar()
  await expect(tela.campo('close-planting-confirmation-erro')).toContainText(
    'anterior à data de início',
  )
  await tela.cancelarConfirmacao()
  await tela.campo('cancel-modal').click()
  await expect(tela.campo('planting-status')).toHaveText('Em andamento')
  await login.sair()
  await login.entrarComo('consulta.mock', 'mock')
  await tela.abrir(1)
  await expect(tela.campo('planting-result')).toBeVisible()
  await expect(tela.campo('close-planting')).toHaveCount(0)
  await expect(tela.campo('add-production-planting')).toHaveCount(0)
})
