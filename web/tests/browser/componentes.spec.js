import { test, expect } from '@playwright/test'
class ExemplosPage {
  constructor(page) {
    this.page = page
  }
  campo(id) {
    return this.page.getByTestId(id)
  }
  async abrir() {
    await this.page.goto('/tests/components.html')
  }
  async preencher() {
    await this.campo('example-money').fill('10025')
    await this.campo('example-date').fill('29/02/2024')
    await this.campo('example-cpf').fill('99900000188')
  }
}
test('máscaras preservam valores decimais, CPF e data de calendário', async ({
  page,
}) => {
  const tela = new ExemplosPage(page)
  await tela.abrir()
  await tela.preencher()
  await expect(tela.campo('example-money')).toHaveValue('R$ 100,25')
  await expect(tela.campo('example-date')).toHaveValue('29/02/2024')
  await expect(tela.campo('example-cpf')).toHaveValue('999.000.001-88')
  await tela.campo('example-submit').click()
  await expect(tela.campo('example-submitted')).toHaveText(
    '{"valor":"100.25","dataFato":"2024-02-29","documento":"99900000188"}',
  )
})
test('data inexistente impede envio e correção permite enviar', async ({
  page,
}) => {
  const tela = new ExemplosPage(page)
  await tela.abrir()
  await tela.preencher()
  await tela.campo('example-date').fill('31/02/2024')
  await tela.campo('example-submit').click()
  await expect(tela.campo('example-submitted')).toBeEmpty()
  await tela.campo('example-date').fill('28/02/2024')
  await tela.campo('example-submit').click()
  await expect(tela.campo('example-submitted')).toContainText('2024-02-28')
})
test('ErroResposta apresenta mensagem geral e erro no campo correto', async ({
  page,
}) => {
  const tela = new ExemplosPage(page)
  await tela.abrir()
  await tela.preencher()
  await tela.campo('example-api-error').check()
  await tela.campo('example-submit').click()
  await expect(tela.campo('example-form-error')).toHaveText(
    'Verifique os dados informados.',
  )
  await expect(tela.campo('example-cpf')).toHaveAttribute(
    'aria-invalid',
    'true',
  )
  await expect(tela.campo('example-cpf-error')).toHaveText('CPF inválido.')
  await expect(tela.campo('example-money')).toHaveAttribute(
    'aria-invalid',
    'false',
  )
})
test('tabela usa paginação de base zero, busca, vazio e carregamento', async ({
  page,
}) => {
  const tela = new ExemplosPage(page)
  await tela.abrir()
  await expect(tela.campo('example-table-linha-1')).toBeVisible()
  await tela.campo('example-table-proxima').click()
  await expect(tela.campo('example-table-linha-11')).toBeVisible()
  await tela.campo('example-table-busca').fill('nenhum')
  await expect(tela.campo('example-table-vazio')).toBeVisible()
  await expect(tela.campo('example-table-proxima')).toBeDisabled()
  await tela.campo('example-loading').click()
  await expect(tela.campo('example-table-carregando')).toBeVisible()
  await expect(tela.campo('example-success')).toHaveAttribute('role', 'status')
  await expect(tela.campo('example-error')).toHaveAttribute('role', 'alert')
})
test('confirmação cancela com Escape, restaura foco e confirma uma vez', async ({
  page,
}) => {
  const tela = new ExemplosPage(page)
  await tela.abrir()
  await tela.campo('example-confirm').click()
  await expect(tela.campo('cancel-confirm')).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(tela.campo('confirmacao')).toHaveCount(0)
  await expect(tela.campo('example-confirm')).toBeFocused()
  await tela.campo('example-confirm').click()
  await tela.campo('accept-confirm').click()
  await expect(tela.campo('example-confirmed')).toHaveText('Confirmado')
})
