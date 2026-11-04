import { test, expect } from '@playwright/test'
import { LoginPage } from '../pages/LoginPage.js'
import { PlantioPage } from '../pages/PlantioPage.js'

// Exercita o React, o contexto e o serviço reais; só a rede externa é substituída.
async function preparar(
  page,
  { perfil = 'LANCAMENTO', ciclo = 'LONGO', vazio = false } = {},
) {
  const plantio = {
    id: 1,
    produtorId: 1,
    propriedadeId: 1,
    culturaId: 1,
    area: 1.25,
    tipoCiclo: ciclo,
    dataInicio: '2026-02-01',
    previsaoColheita: '2026-06-20',
    dataEncerramento: null,
    situacao: 'ABERTO',
  }
  const resultado = {
    plantioId: 1,
    culturaNome: 'Milho Fictício',
    propriedadeNome: 'Sítio Fictício',
    tipoCiclo: ciclo,
    periodoInicio: '2026-02-01',
    periodoFim: '2026-06-30',
    quantidadeProduzida: 500,
    quantidadeVendida: 200,
    unidadeMedida: 'kg',
    receita: 900,
    custoDesembolso: 600,
    valorMaoObraFamiliar: 80,
    custoComMaoObraFamiliar: 680,
    resultado: 300,
  }
  const dados = {
    produtores: [{ id: 1, nome: 'Produtor Fictício', ativo: true }],
    culturas: [
      {
        id: 1,
        nome: 'Milho Fictício',
        tipoCicloPadrao: 'LONGO',
        unidadeMedida: 'kg',
      },
    ],
    plantios: [plantio],
    propriedades: [{ id: 1, produtorId: 1, nome: 'Sítio Fictício' }],
    producoes: [
      {
        id: 1,
        plantioId: 1,
        quantidade: 500,
        unidadeMedida: 'kg',
        dataFato: '2026-06-10',
        dataColeta: '2026-06-11',
      },
    ],
    despesas: [
      {
        id: 1,
        produtorId: 1,
        categoriaDespesaId: 1,
        descricao: 'Adubo Fictício',
        valorTotal: 100,
        dataFato: '2026-03-10',
        dataColeta: '2026-03-11',
        estornada: true,
        diariaId: null,
        rateios: [{ plantioId: 1, percentual: 0.333333, valorRateado: 33.34 }],
      },
    ],
    diarias: [
      {
        id: 1,
        plantioId: 1,
        natureza: 'FAMILIAR',
        quantidadeDias: 1,
        valorUnitario: 80,
        valorTotal: 80,
        dataFato: '2026-03-10',
        dataColeta: '2026-03-11',
        valorReferenciaDiariaId: 1,
      },
    ],
    clientes: [{ id: 1, nome: 'Cliente Fictício', tipoCanal: 'FEIRA' }],
    vendas: [
      {
        id: 1,
        produtorId: 1,
        clienteId: 1,
        dataFato: '2026-06-15',
        dataColeta: '2026-06-16',
        valorTotal: 999,
        itens: [
          {
            id: 1,
            plantioId: 1,
            quantidade: 3.333,
            unidadeMedida: 'kg',
            valorUnitario: 4.5,
            valorTotal: 15,
          },
          {
            id: 2,
            plantioId: 2,
            quantidade: 1,
            unidadeMedida: 'kg',
            valorUnitario: 984,
            valorTotal: 984,
          },
        ],
        avisos: [],
      },
    ],
  }
  if (vazio)
    for (const chave of ['producoes', 'despesas', 'diarias', 'vendas'])
      dados[chave] = []
  const estado = {
    dados,
    plantio,
    resultado,
    encerramentos: [],
    erroEncerramento: null,
    erroCarga: false,
    erroResultado: false,
    meses: [],
  }
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const caminho = url.pathname.replace('/api/v1', '')
    const usuario = { id: 1, nome: 'Operador Fictício', perfil }
    if (caminho === '/auth/login')
      return route.fulfill({
        json: {
          token: 'token-sintetico',
          expiraEm: new Date(Date.now() + 3600000).toISOString(),
          usuario,
        },
      })
    if (caminho === '/auth/me') return route.fulfill({ json: usuario })
    if (caminho === '/plantios/1/resultado') {
      estado.meses.push(url.searchParams.get('mes'))
      if (estado.erroResultado)
        return route.fulfill({
          status: 501,
          json: {
            mensagem: 'Apuração indisponível. Tente novamente.',
            campos: [],
          },
        })
      return route.fulfill({ json: estado.resultado })
    }
    if (caminho === '/plantios/1/encerramento') {
      estado.encerramentos.push(request.postDataJSON())
      if (estado.erroEncerramento)
        return route.fulfill({ status: 422, json: estado.erroEncerramento })
      Object.assign(plantio, {
        dataEncerramento: request.postDataJSON().dataEncerramento,
        situacao: 'ENCERRADO',
      })
      return route.fulfill({ status: 201, json: plantio })
    }
    if (caminho === '/plantios' && estado.erroCarga)
      return route.fulfill({
        status: 500,
        json: {
          mensagem: 'Não foi possível consultar os plantios.',
          campos: [],
        },
      })
    const lista = dados[caminho.split('/').at(-1)] || []
    const pagina = Number(url.searchParams.get('pagina') || 0)
    const tamanho = Number(url.searchParams.get('tamanho') || 20)
    return route.fulfill({
      json: {
        itens: lista.slice(pagina * tamanho, (pagina + 1) * tamanho),
        pagina,
        tamanho,
        total: lista.length,
      },
    })
  })
  await new LoginPage(page).entrarComo('operador-ficticio', 'senha-sintetica')
  return { tela: new PlantioPage(page), estado }
}

test('detalhe pela lista mostra seções, datas e valores enviados pela API', async ({
  page,
}) => {
  const { tela } = await preparar(page)
  await tela.abrirPelaLista()
  await expect(tela.campo('planting-dates')).toContainText('01/02/2026')
  await expect(tela.campo('planting-dates')).toContainText('20/06/2026')
  await expect(tela.secao('production')).toContainText('500 kg')
  await expect(tela.secao('expenses')).toContainText('33,34')
  await expect(tela.secao('expenses')).toContainText('33,33% do total')
  await expect(tela.secao('expenses')).toContainText('Estornada')
  await expect(tela.secao('dailies')).toContainText('Familiar')
  await expect(tela.secao('sales')).toContainText('15,00')
  await expect(tela.secao('sales')).not.toContainText('984,00')
  await expect(tela.campo('planting-produced')).toContainText('500 kg')
  await expect(tela.campo('planting-sold')).toContainText('200 kg')
  await expect(tela.campo('planting-result')).toContainText('300,00')
  await expect(tela.secao('result')).toContainText('680,00')
  await expect(tela.campo('planting-result-month')).toHaveCount(0)
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.screenshot({
    path: 'test-results/plantio-desktop.png',
    fullPage: true,
  })
})

test('resultado negativo da API não é ocultado nem recalculado pelas listas', async ({
  page,
}) => {
  const { tela, estado } = await preparar(page)
  Object.assign(estado.resultado, {
    receita: 0,
    quantidadeVendida: 0,
    resultado: -600,
  })
  await tela.abrir()
  await expect(tela.campo('planting-result')).toContainText('-R$')
  await expect(tela.campo('planting-result')).toContainText('600,00')
  await expect(tela.campo('planting-sold')).toContainText('0 kg')
})

test('ciclo efetivo curto permite consultar outro mês e não mantém resultado em erro', async ({
  page,
}) => {
  const { tela, estado } = await preparar(page, { ciclo: 'CURTO' })
  await tela.abrir()
  await expect(tela.campo('planting-result')).toContainText('300,00')
  Object.assign(estado.resultado, {
    periodoInicio: '2026-03-01',
    periodoFim: '2026-03-31',
    resultado: -80,
  })
  await tela.campo('planting-result-month').fill('2026-03')
  await expect(tela.secao('result')).toContainText('01/03/2026 a 31/03/2026')
  await expect(tela.campo('planting-result')).toContainText('80,00')
  expect(estado.meses).toContain('2026-03')
  estado.erroResultado = true
  await tela.campo('planting-result-month').fill('2026-04')
  await expect(tela.campo('planting-result-error')).toHaveText(
    'Apuração indisponível. Tente novamente.',
  )
  await expect(tela.campo('planting-result')).toHaveCount(0)
  estado.erroResultado = false
  await tela.campo('planting-result-retry').click()
  await expect(tela.campo('planting-result')).toContainText('80,00')
})

test('carregamento não mostra inexistente e vazio não inventa lançamentos', async ({
  page,
}) => {
  const { tela } = await preparar(page, { vazio: true })
  let liberar
  const espera = new Promise((resolve) => {
    liberar = resolve
  })
  await page.route('**/api/v1/plantios?**', async (route) => {
    await espera
    await route.fallback()
  })
  await tela.abrir()
  await expect(tela.campo('planting-loading')).toBeVisible()
  await expect(
    page.getByText('Página não encontrada', { exact: true }),
  ).toHaveCount(0)
  liberar()
  for (const secao of ['production', 'expenses', 'dailies', 'sales']) {
    await expect(tela.secao(secao)).toContainText('Nenhum registro encontrado')
  }
})

test('erro de carga tem recuperação e plantio inexistente tem retorno', async ({
  page,
}) => {
  const { tela, estado } = await preparar(page)
  estado.erroCarga = true
  await tela.abrir()
  await expect(tela.campo('planting-error')).toHaveText(
    'Não foi possível consultar os plantios.',
  )
  await expect(tela.campo('close-planting')).toHaveCount(0)
  estado.erroCarga = false
  await tela.campo('planting-retry').click()
  await expect(tela.campo('close-planting')).toBeVisible()
  await tela.abrir(999)
  await expect(
    page.getByText('Página não encontrada', { exact: true }),
  ).toBeVisible()
  await expect(tela.campo('back-link')).toBeVisible()
})

test('histórico inclui produção e itens de venda depois da primeira página', async ({
  page,
}) => {
  const { tela, estado } = await preparar(page)
  estado.dados.producoes = Array.from({ length: 101 }, (_, i) => ({
    id: i + 1,
    plantioId: 1,
    quantidade: i === 100 ? 789.125 : 1,
    unidadeMedida: 'kg',
    dataFato: '2026-06-10',
  }))
  estado.dados.vendas = Array.from({ length: 101 }, (_, i) => ({
    id: i + 1,
    produtorId: 1,
    clienteId: 1,
    dataFato: '2026-06-15',
    valorTotal: i === 100 ? 456.78 : 1,
    itens: [
      {
        id: i + 1,
        plantioId: i === 100 ? 1 : 2,
        quantidade: 1,
        unidadeMedida: 'kg',
        valorUnitario: 1,
        valorTotal: i === 100 ? 456.78 : 1,
      },
    ],
    avisos: [],
  }))
  await tela.abrir()
  await expect(tela.secao('production')).toContainText('789,125 kg')
  await expect(tela.secao('sales')).toContainText('456,78')
})

test('encerrar exige data e confirmação, cancelar preserva data e não grava', async ({
  page,
}) => {
  const { tela, estado } = await preparar(page)
  await tela.abrir()
  await tela.informarEncerramento('')
  await tela.pedirConfirmacao()
  await expect(tela.campo('close-planting-confirmation')).toHaveCount(0)
  expect(estado.encerramentos).toHaveLength(0)
  await tela.campo('field-endedAt').fill('30/06/2026')
  await tela.pedirConfirmacao()
  await expect(tela.campo('close-planting-confirmation')).toContainText(
    '30/06/2026',
  )
  expect(estado.encerramentos).toHaveLength(0)
  await tela.cancelarConfirmacao()
  await expect(tela.campo('field-endedAt')).toHaveValue('30/06/2026')
  await tela.pedirConfirmacao()
  await tela.confirmar()
  await expect(tela.campo('planting-status')).toHaveText('Encerrado')
  await expect(tela.campo('planting-dates')).toContainText('30/06/2026')
  await expect(tela.campo('close-planting')).toHaveCount(0)
  expect(estado.encerramentos).toEqual([{ dataEncerramento: '2026-06-30' }])
  // RN04 permite lançamento histórico até o encerramento.
  await expect(tela.campo('add-production-planting')).toBeVisible()
})

test('422 no encerramento mostra a mensagem da API e preserva a data para corrigir', async ({
  page,
}) => {
  const { tela, estado } = await preparar(page)
  estado.erroEncerramento = {
    mensagem:
      'Há lançamento em 15/06/2026, posterior à data de encerramento informada.',
    campos: [],
  }
  await tela.abrir()
  await tela.informarEncerramento('31/05/2026')
  await tela.pedirConfirmacao()
  await tela.confirmar()
  await expect(tela.campo('close-planting-confirmation-erro')).toHaveText(
    estado.erroEncerramento.mensagem,
  )
  await tela.cancelarConfirmacao()
  await expect(tela.campo('field-endedAt')).toHaveValue('31/05/2026')
  await expect(tela.campo('planting-status')).toHaveText('Em andamento')
})

test('400 com erro de campo no encerramento volta ao formulário e mostra o erro na data', async ({
  page,
}) => {
  const { tela } = await preparar(page)
  await page.route('**/api/v1/plantios/1/encerramento', (route) =>
    route.fulfill({
      status: 400,
      json: {
        mensagem: 'Confira a data informada.',
        campos: [
          { campo: 'dataEncerramento', mensagem: 'Informe uma data válida.' },
        ],
      },
    }),
  )
  await tela.abrir()
  await tela.informarEncerramento('30/06/2026')
  await tela.pedirConfirmacao()
  await tela.confirmar()
  await expect(tela.campo('close-planting-confirmation')).toHaveCount(0)
  await expect(tela.campo('field-endedAt')).toBeVisible()
  await expect(tela.campo('field-endedAt-error')).toHaveText(
    'Informe uma data válida.',
  )
  await expect(tela.campo('field-endedAt')).toHaveValue('30/06/2026')
  await expect(tela.campo('planting-status')).toHaveText('Em andamento')
})

test('CONSULTA lê detalhe e resultado sem ações de gravação', async ({
  page,
}) => {
  const { tela } = await preparar(page, { perfil: 'CONSULTA' })
  await tela.abrir()
  await expect(tela.campo('planting-result')).toContainText('300,00')
  for (const id of [
    'edit-planting',
    'close-planting',
    'add-production-planting',
    'add-expense-planting',
    'add-daily-planting',
    'add-sale-planting',
  ]) {
    await expect(tela.campo(id)).toHaveCount(0)
  }
})

test('módulo 501 não é apresentado como lista vazia', async ({ page }) => {
  const { tela } = await preparar(page)
  await page.route('**/api/v1/plantios/1/diarias?**', (route) =>
    route.fulfill({
      status: 501,
      json: { mensagem: 'Diárias indisponíveis.', campos: [] },
    }),
  )
  await tela.abrir()
  await expect(tela.secao('dailies')).toContainText('indisponível')
  await expect(tela.secao('dailies')).not.toContainText(
    'Nenhum registro encontrado',
  )
  await expect(tela.campo('add-daily-planting')).toHaveCount(0)
  await expect(tela.secao('production')).toContainText('500 kg')
})

test('resposta antiga não substitui resultado do mês mais recente', async ({
  page,
}) => {
  const { tela, estado } = await preparar(page, { ciclo: 'CURTO' })
  await tela.abrir()
  await expect(tela.campo('planting-result')).toBeVisible()
  let liberar
  const espera = new Promise((resolve) => {
    liberar = resolve
  })
  let iniciou
  const inicio = new Promise((resolve) => {
    iniciou = resolve
  })
  await page.route(
    '**/api/v1/plantios/1/resultado?mes=2026-03',
    async (route) => {
      iniciou()
      await espera
      await route.fulfill({ json: { ...estado.resultado, resultado: 111 } })
    },
  )
  await tela.campo('planting-result-month').fill('2026-03')
  await inicio
  await expect(tela.campo('planting-result-loading')).toBeVisible()
  Object.assign(estado.resultado, {
    resultado: 222,
    periodoInicio: '2026-04-01',
    periodoFim: '2026-04-30',
  })
  await tela.campo('planting-result-month').fill('2026-04')
  await expect(tela.campo('planting-result')).toContainText('222,00')
  liberar()
  await expect(tela.campo('planting-result-month')).toHaveValue('2026-04')
  await expect(tela.campo('planting-result')).toContainText('222,00')
})

test('encerramento bloqueia envio duplicado e erro de campo volta à data', async ({
  page,
}) => {
  const { tela } = await preparar(page)
  let liberar
  const espera = new Promise((resolve) => {
    liberar = resolve
  })
  let envios = 0
  await page.route('**/api/v1/plantios/1/encerramento', async (route) => {
    envios++
    await espera
    await route.fulfill({
      status: 400,
      json: {
        mensagem: 'Confira a data informada.',
        campos: [
          {
            campo: 'dataEncerramento',
            mensagem: 'Data inválida enviada pela API.',
          },
        ],
      },
    })
  })
  await tela.abrir()
  await tela.informarEncerramento()
  await tela.pedirConfirmacao()
  await tela.confirmar()
  await expect(
    tela.campo('close-planting-confirmation-confirmar'),
  ).toBeDisabled()
  await expect(
    tela.campo('close-planting-confirmation-cancelar'),
  ).toBeDisabled()
  liberar()
  await expect(tela.campo('field-endedAt-error')).toHaveText(
    'Data inválida enviada pela API.',
  )
  await expect(tela.campo('field-endedAt')).toHaveValue('30/06/2026')
  expect(envios).toBe(1)
})

test('falha de conexão no encerramento mantém plantio aberto e permite tentar de novo', async ({
  page,
}) => {
  const { tela } = await preparar(page)
  await page.route('**/api/v1/plantios/1/encerramento', (route) =>
    route.abort('failed'),
  )
  await tela.abrir()
  await tela.informarEncerramento()
  await tela.pedirConfirmacao()
  await tela.confirmar()
  await expect(tela.campo('close-planting-confirmation-erro')).toContainText(
    'Não foi possível conectar ao servidor.',
  )
  await expect(tela.campo('planting-status')).toHaveText('Em andamento')
  await page.unroute('**/api/v1/plantios/1/encerramento')
  await tela.confirmar()
  await expect(tela.campo('planting-status')).toHaveText('Encerrado')
})

test('plantio encerrado permanece selecionado como origem para venda histórica', async ({
  page,
}) => {
  const { tela, estado } = await preparar(page)
  Object.assign(estado.plantio, {
    dataEncerramento: '2026-06-30',
    situacao: 'ENCERRADO',
  })
  let enviado
  await page.route('**/api/v1/vendas', async (route) => {
    enviado = route.request().postDataJSON()
    const venda = {
      ...enviado,
      id: 99,
      dataColeta: '2026-07-01',
      valorTotal: 10,
      itens: [{ ...enviado.itens[0], id: 99, valorTotal: 10 }],
      avisos: [],
    }
    estado.dados.vendas.push(venda)
    await route.fulfill({ status: 201, json: venda })
  })
  await tela.abrir()
  await tela.campo('add-sale-planting').click()
  await expect(tela.campo('field-sale-planting-0')).toHaveValue('1')
  await tela.campo('field-clientId').selectOption('1')
  await tela.campo('field-date').fill('30/06/2026')
  await tela.campo('field-sale-amount-0').fill('2')
  await tela.campo('field-sale-price-0').fill('500')
  await tela.campo('save-modal').click()
  await expect(tela.campo('close-modal')).toHaveCount(0)
  expect(enviado).toMatchObject({
    dataFato: '2026-06-30',
    itens: [{ plantioId: 1, quantidade: 2, valorUnitario: 5 }],
  })
  await expect(tela.secao('sales')).toContainText('10,00')
})
