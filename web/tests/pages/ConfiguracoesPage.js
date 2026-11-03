// Page Object de Configurações (QA 3.4). Por enquanto só a aba Culturas (C07).
export class ConfiguracoesPage {
  constructor(page) {
    this.page = page
  }
  campo(id) {
    return this.page.getByTestId(id)
  }
  async abrir() {
    await this.page.goto('/configuracoes')
  }
  async abrirAba(aba) {
    await this.campo(`tab-${aba}`).click()
  }
  async preencherCultura({ nome, ciclo, unidade }) {
    if (nome !== undefined) await this.campo('field-name').fill(nome)
    if (ciclo !== undefined)
      await this.campo('field-cycle').selectOption({ label: ciclo })
    if (unidade !== undefined) await this.campo('field-unit').fill(unidade)
  }
  async cadastrarCultura(dados) {
    await this.campo('add-setting').click()
    await this.preencherCultura(dados)
    await this.campo('save-modal').click()
  }
  async abrirEdicaoCultura(id) {
    await this.campo(`edit-culture-${id}`).click()
  }
  async editarCultura(id, dados) {
    await this.abrirEdicaoCultura(id)
    await this.preencherCultura(dados)
    await this.campo('save-modal').click()
  }
  /** Digita tecla a tecla, como o operador; o navegador aplica o maxLength. */
  async digitarNoCampo(nome, texto) {
    await this.campo(`field-${nome}`).fill('')
    await this.campo(`field-${nome}`).pressSequentially(texto)
  }
  valorDoCampo(nome) {
    return this.campo(`field-${nome}`)
  }
  async campoObrigatorioVazio(nome) {
    return this.campo(`field-${nome}`).evaluate(
      (input) => input.validity.valueMissing,
    )
  }
  linhaCultura(id) {
    return this.campo(`culture-row-${id}`)
  }
  linhasDeCultura() {
    return this.page.locator('[data-testid^="culture-row-"]')
  }
  async idDaCultura(nome) {
    const testId = await this.linhasDeCultura()
      .filter({ hasText: nome })
      .getAttribute('data-testid')
    return Number(testId.replace('culture-row-', ''))
  }
  formulario() {
    return this.page.getByRole('dialog')
  }
  erroGeral() {
    return this.campo('form-error')
  }
  erroDoCampo(nome) {
    return this.campo(`field-${nome}-error`)
  }
  acaoCadastrar() {
    return this.campo('add-setting')
  }
  acoesDeEdicao() {
    return this.page.locator('[data-testid^="edit-culture-"]')
  }
  async cancelar() {
    await this.campo('cancel-modal').click()
  }
}
