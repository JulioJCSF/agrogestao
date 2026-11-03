// Page Object de Configurações (QA 3.4): abas Culturas (C07) e Categorias de despesa (L12).
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

  async abrirCadastro() {
    await this.campo('add-setting').click()
  }
  async cadastrarCategoria(nome) {
    await this.abrirCadastro()
    await this.campo('field-name').fill(nome)
    await this.campo('save-modal').click()
  }
  async abrirEdicaoCategoria(id) {
    await this.campo(`edit-category-${id}`).click()
  }
  async editarCategoria(id, nome) {
    await this.abrirEdicaoCategoria(id)
    await this.campo('field-name').fill(nome)
    await this.campo('save-modal').click()
  }
  /** Só abre o diálogo de confirmação. */
  async pedirExclusaoCategoria(id) {
    await this.campo(`delete-category-${id}`).click()
  }
  async excluirCategoria(id) {
    await this.pedirExclusaoCategoria(id)
    await this.campo('confirmacao-confirmar').click()
  }
  linhaCategoria(id) {
    return this.campo(`category-row-${id}`)
  }
  linhasDeCategoria() {
    return this.page.locator('[data-testid^="category-row-"]')
  }
  async idDaCategoria(nome) {
    const testId = await this.linhasDeCategoria()
      .filter({ has: this.page.getByText(nome, { exact: true }) })
      .getAttribute('data-testid')
    return Number(testId.replace('category-row-', ''))
  }
  acaoEditarCategoria(id) {
    return this.campo(`edit-category-${id}`)
  }
  acaoExcluirCategoria(id) {
    return this.campo(`delete-category-${id}`)
  }
  acoesDeEdicaoCategoria() {
    return this.page.locator('[data-testid^="edit-category-"]')
  }
  acoesDeExclusaoCategoria() {
    return this.page.locator('[data-testid^="delete-category-"]')
  }
  dialogoConfirmacao() {
    return this.campo('confirmacao')
  }
  erroDaConfirmacao() {
    return this.campo('confirmacao-erro')
  }
  async cancelarConfirmacao() {
    await this.campo('confirmacao-cancelar').click()
  }
  /** Aviso de sucesso (toast) depois de uma gravação. */
  aviso() {
    // O toast do App.jsx não tem data-testid; a faixa do mock também é role=status.
    return this.page.locator('.toast')
  }
}
