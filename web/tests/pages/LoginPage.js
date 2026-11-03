// Page Object do login (QA 3.4): ações de negócio; seletor só aqui.
export class LoginPage {
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
    for (const [id, value] of [
      ['login-name', login],
      ['login-password', senha],
    ]) {
      await this.campo(id).press('ControlOrMeta+A')
      await this.campo(id).press('Backspace')
      await this.campo(id).pressSequentially(value)
    }
    await this.campo('login-submit').click()
  }
  /** Abre o login, entra e espera o menu do perfil. */
  async entrarComo(login, senha) {
    await this.abrir()
    await this.logar(login, senha)
    await this.campo('profile-menu').waitFor()
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
  avisoDoMock() {
    return this.campo('mock-logins')
  }
  faixaDoMock() {
    return this.campo('mock-banner')
  }
}
