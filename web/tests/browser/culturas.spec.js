import { test, expect } from '@playwright/test'
import { apiDaSessao, cpfSintetico, unico } from '../pages/apoioApiReal.js'
import { ConfiguracoesPage } from '../pages/ConfiguracoesPage.js'
import { LoginPage } from '../pages/LoginPage.js'

// C07 (#43) contra a API real (npm run test:api-real). Cada caso cria os próprios dados, com
// nomes únicos; da massa só usa o admin e o login CONSULTA que a migration insere.
test.skip(
  !process.env.AGROGESTAO_API_REAL,
  'Executar com npm run test:api-real',
)

async function telaDeCulturas(page, login, senha) {
  await new LoginPage(page).entrarComo(login, senha)
  const configuracoes = new ConfiguracoesPage(page)
  await configuracoes.abrir()
  await configuracoes.abrirAba('culturas')
  return configuracoes
}

const comoAdmin = (page) =>
  telaDeCulturas(
    page,
    process.env.AGROGESTAO_TEST_LOGIN,
    process.env.AGROGESTAO_TEST_SENHA,
  )

test('admin cadastra e edita cultura e a API grava o que a tela mostra', async ({
  page,
}) => {
  const configuracoes = await comoAdmin(page)
  const nome = `Cultura Fictícia C07 ${unico()}`
  await configuracoes.cadastrarCultura({
    nome,
    ciclo: 'Longo',
    unidade: 'molho',
  })
  await expect(configuracoes.formulario()).toHaveCount(0)
  const id = await configuracoes.idDaCultura(nome)
  const api = await apiDaSessao(page)
  expect(await api.get(`/culturas/${id}`)).toMatchObject({
    id,
    nome,
    tipoCicloPadrao: 'LONGO',
    unidadeMedida: 'molho',
  })

  await configuracoes.editarCultura(id, { ciclo: 'Curto', unidade: 'cento' })
  await expect(configuracoes.formulario()).toHaveCount(0)
  await expect(configuracoes.linhaCultura(id)).toContainText('cento')
  expect(await api.get(`/culturas/${id}`)).toMatchObject({
    nome,
    tipoCicloPadrao: 'CURTO',
    unidadeMedida: 'cento',
  })
})

test('nome repetido com outra caixa mostra o 422 da API', async ({ page }) => {
  const configuracoes = await comoAdmin(page)
  const nome = `Cultura Fictícia C07 ${unico()}`
  await (
    await apiDaSessao(page)
  ).post('/culturas', {
    nome,
    tipoCicloPadrao: 'CURTO',
    unidadeMedida: 'kg',
  })
  await configuracoes.cadastrarCultura({ nome: nome.toUpperCase() })
  await expect(configuracoes.erroGeral()).toHaveText(
    'Já existe uma cultura com este nome.',
  )
  await expect(configuracoes.formulario()).toBeVisible()
  await expect(configuracoes.valorDoCampo('name')).toHaveValue(
    nome.toUpperCase(),
  )
  await configuracoes.cancelar()
})

test('nome só com espaços mostra o 400 da API no campo', async ({ page }) => {
  const configuracoes = await comoAdmin(page)
  await configuracoes.cadastrarCultura({ nome: '   ' })
  await expect(configuracoes.erroDoCampo('name')).toHaveText(
    'não deve estar em branco',
  )
  await expect(configuracoes.erroGeral()).toHaveText(
    'Há campos com valores inválidos. Verifique os dados informados.',
  )
  await expect(configuracoes.formulario()).toBeVisible()
  await configuracoes.cancelar()
})

test('RN38: a unidade não muda depois de registrada a produção', async ({
  page,
}) => {
  const configuracoes = await comoAdmin(page)
  const api = await apiDaSessao(page)
  const sufixo = unico()
  const cultura = await api.post('/culturas', {
    nome: `Cultura Fictícia C07 RN38 ${sufixo}`,
    tipoCicloPadrao: 'CURTO',
    unidadeMedida: 'kg',
  })
  const produtor = await api.post('/produtores', {
    nome: `Produtor Fictício C07 ${sufixo}`,
    documento: cpfSintetico(),
    ativo: true,
  })
  const propriedade = await api.post(
    `/produtores/${produtor.id}/propriedades`,
    { nome: `Sítio Fictício C07 ${sufixo}` },
  )
  const plantio = await api.post('/plantios', {
    propriedadeId: propriedade.id,
    culturaId: cultura.id,
    area: 1,
    dataInicio: '2026-02-01',
  })
  await api.post(`/plantios/${plantio.id}/producoes`, {
    quantidade: 10,
    dataFato: '2026-03-01',
  })

  await configuracoes.abrir()
  await configuracoes.abrirAba('culturas')
  await configuracoes.editarCultura(cultura.id, { unidade: 'saca' })
  await expect(configuracoes.erroGeral()).toHaveText(
    'A unidade da cultura não pode mudar porque já há produção registrada.',
  )
  await expect(configuracoes.valorDoCampo('unit')).toHaveValue('saca')
  await configuracoes.cancelar()
  await expect(configuracoes.linhaCultura(cultura.id)).toContainText('kg')
  expect((await api.get(`/culturas/${cultura.id}`)).unidadeMedida).toBe('kg')
})

test('CONSULTA vê as culturas sem cadastrar nem editar', async ({ page }) => {
  // Login CONSULTA inserido pela migration de dados iniciais (senha de exemplo da massa).
  const login = await page.request.post('/api/v1/auth/login', {
    data: { login: 'consulta', senha: '12345678' },
  })
  test.skip(
    login.status() !== 200,
    'A massa da API não tem o login consulta; POST /usuarios ainda responde 501.',
  )
  const configuracoes = await telaDeCulturas(page, 'consulta', '12345678')
  await expect(configuracoes.linhasDeCultura().first()).toBeVisible()
  await expect(configuracoes.acaoCadastrar()).toHaveCount(0)
  await expect(configuracoes.acoesDeEdicao()).toHaveCount(0)
})
