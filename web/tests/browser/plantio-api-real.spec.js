import { test, expect } from '@playwright/test'
import { apiDaSessao, cpfSintetico, unico } from '../pages/apoioApiReal.js'
import { LoginPage } from '../pages/LoginPage.js'
import { PlantioPage } from '../pages/PlantioPage.js'

test.skip(
  !process.env.AGROGESTAO_API_REAL,
  'Executar com npm run test:api-real',
)

async function preparar(page, tipoCiclo = 'LONGO') {
  await new LoginPage(page).entrarComo(
    process.env.AGROGESTAO_TEST_LOGIN,
    process.env.AGROGESTAO_TEST_SENHA,
  )
  const api = await apiDaSessao(page)
  const sufixo = unico()
  const produtor = await api.post('/produtores', {
    nome: `Produtor Fictício C08b ${sufixo}`,
    documento: cpfSintetico(),
    ativo: true,
  })
  const propriedade = await api.post(
    `/produtores/${produtor.id}/propriedades`,
    { nome: `Sítio Fictício C08b ${sufixo}` },
  )
  const cultura = await api.post('/culturas', {
    nome: `Cultura Fictícia C08b ${sufixo}`,
    tipoCicloPadrao: 'LONGO',
    unidadeMedida: 'kg',
  })
  const plantio = await api.post('/plantios', {
    propriedadeId: propriedade.id,
    culturaId: cultura.id,
    area: 1,
    tipoCiclo,
    dataInicio: '2026-02-01',
  })
  return { api, plantio, produtor, tela: new PlantioPage(page), sufixo }
}

test('API real: detalhe agrega lançamentos, encerra e aceita produção histórica pela tela', async ({
  page,
}) => {
  const { api, plantio, produtor, tela, sufixo } = await preparar(page)
  const categoria = await api.post('/categorias-despesa', {
    nome: `Categoria Fictícia C08b ${sufixo}`,
  })
  await api.post(`/plantios/${plantio.id}/producoes`, {
    quantidade: 500,
    dataFato: '2026-06-10',
  })
  await api.post('/despesas', {
    produtorId: produtor.id,
    categoriaDespesaId: categoria.id,
    descricao: 'Adubo Fictício C08b',
    valorTotal: 100,
    dataFato: '2026-03-10',
    plantioIds: [plantio.id],
  })
  await api.post(`/plantios/${plantio.id}/diarias`, {
    natureza: 'PAGA',
    quantidadeDias: 1,
    valorUnitario: 80,
    dataFato: '2026-03-11',
  })
  const cliente = await api.post('/clientes', {
    nome: `Cliente Fictício C08b ${sufixo}`,
    tipoCanal: 'FEIRA',
  })
  await api.post('/vendas', {
    produtorId: produtor.id,
    clienteId: cliente.id,
    dataFato: '2026-06-15',
    itens: [
      {
        plantioId: plantio.id,
        quantidade: 200,
        unidadeMedida: 'kg',
        valorUnitario: 4.5,
      },
    ],
  })
  await tela.abrir(plantio.id)
  await expect(tela.secao('production')).toContainText('500 kg')
  await expect(tela.secao('expenses')).toContainText('100,00')
  await expect(tela.secao('dailies')).toContainText('Paga')
  await expect(tela.secao('sales')).toContainText('900,00')
  await expect(tela.campo('planting-result')).toContainText('720,00')
  await tela.informarEncerramento('30/06/2026')
  await tela.pedirConfirmacao()
  await tela.confirmar()
  await expect(tela.campo('planting-status')).toHaveText('Encerrado')
  expect((await api.get(`/plantios/${plantio.id}`)).dataEncerramento).toBe(
    '2026-06-30',
  )
  await tela.campo('add-production-planting').click()
  await tela.campo('field-amount').fill('2')
  await tela.campo('field-date').fill('01/07/2026')
  await tela.campo('save-modal').click()
  await expect(tela.campo('form-error')).toContainText('30/06/2026')
  await tela.campo('field-date').fill('30/06/2026')
  await tela.campo('save-modal').click()
  await expect(tela.campo('close-modal')).toHaveCount(0)
  await expect(tela.campo('planting-produced')).toContainText('502 kg')
  await tela.campo('add-sale-planting').click()
  await expect(tela.campo('field-sale-planting-0')).toHaveValue(
    String(plantio.id),
  )
  await tela.campo('field-clientId').selectOption(String(cliente.id))
  await tela.campo('field-date').fill('01/07/2026')
  await tela.campo('field-sale-amount-0').fill('2')
  await tela.campo('field-sale-price-0').fill('500')
  await tela.campo('save-modal').click()
  await expect(tela.campo('form-error')).toContainText('30/06/2026')
  await tela.campo('field-date').fill('30/06/2026')
  await tela.campo('save-modal').click()
  await expect(tela.campo('close-modal')).toHaveCount(0)
  await expect(tela.campo('planting-result')).toContainText('730,00')
})

test('API real: data de encerramento inválida preserva plantio e mostra mensagem do servidor', async ({
  page,
}) => {
  const { api, plantio, tela } = await preparar(page)
  await tela.abrir(plantio.id)
  await tela.informarEncerramento('31/01/2026')
  await tela.pedirConfirmacao()
  await tela.confirmar()
  await expect(tela.campo('close-planting-confirmation-erro')).toContainText(
    'anterior à data de início',
  )
  expect((await api.get(`/plantios/${plantio.id}`)).dataEncerramento).toBeNull()
  await tela.cancelarConfirmacao()
  await expect(tela.campo('field-endedAt')).toHaveValue('31/01/2026')
})

test('API real: ciclo curto consulta resultado mensal e CONSULTA mantém somente leitura', async ({
  page,
}) => {
  const { api, plantio, tela, sufixo } = await preparar(page, 'CURTO')
  await api.post(`/plantios/${plantio.id}/producoes`, {
    quantidade: 75,
    dataFato: '2026-03-15',
  })
  await tela.abrir(plantio.id)
  await tela.campo('planting-result-month').fill('2026-03')
  await expect(tela.campo('planting-produced')).toContainText('75 kg')
  await expect(tela.secao('result')).toContainText('01/03/2026 a 31/03/2026')
  await tela.campo('planting-result-month').fill('2026-04')
  await expect(tela.campo('planting-produced')).toContainText('0 kg')
  const usuario = {
    nome: `Consulta Fictícia C08b ${sufixo}`,
    login: `consulta-c08b-${sufixo}`,
    senha: 'senha-sintetica-c08b',
    perfil: 'CONSULTA',
  }
  await api.post('/usuarios', usuario)
  const login = new LoginPage(page)
  await login.sair()
  await login.entrarComo(usuario.login, usuario.senha)
  await tela.abrir(plantio.id)
  await expect(tela.campo('planting-result')).toBeVisible()
  for (const acao of [
    'close-planting',
    'edit-planting',
    'add-production-planting',
    'add-expense-planting',
    'add-daily-planting',
    'add-sale-planting',
  ])
    await expect(tela.campo(acao)).toHaveCount(0)
})
