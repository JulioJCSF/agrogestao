export class PlantioPage {
  constructor(page) {
    this.page = page
  }
  campo(id) {
    return this.page.getByTestId(id)
  }
  async abrir(id = 1) {
    await this.page.goto(`/plantios/${id}`)
  }
  async abrirPelaLista(id = 1) {
    await this.page.goto('/producao')
    await this.campo(`open-planting-${id}`).click()
  }
  secao(nome) {
    return this.campo(`planting-${nome === 'result' ? 'result-section' : nome}`)
  }
  async informarEncerramento(data = '30/06/2026') {
    await this.campo('close-planting').click()
    await this.campo('field-endedAt').fill(data)
  }
  async pedirConfirmacao() {
    await this.campo('save-modal').click()
  }
  async confirmar() {
    await this.campo('close-planting-confirmation-confirmar').click()
  }
  async cancelarConfirmacao() {
    await this.campo('close-planting-confirmation-cancelar').click()
  }
}
