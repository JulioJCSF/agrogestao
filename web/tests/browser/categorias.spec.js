import { test, expect } from '@playwright/test'
import { apiDaSessao, cpfSintetico, unico } from '../pages/apoioApiReal.js'
import { ConfiguracoesPage } from '../pages/ConfiguracoesPage.js'
import { LoginPage } from '../pages/LoginPage.js'

// L12 (#53) contra a API real (npm run test:api-real). Cada caso cria os próprios dados, com
// nomes únicos; da massa só usa o admin, o login CONSULTA e a categoria de sistema "Diária",
// que a migration insere (RN16).
test.skip(
  !process.env.AGROGESTAO_API_REAL,
  'Executar com npm run test:api-real',
)

const SISTEMA = 'Categoria de sistema não pode ser alterada nem removida.'

async function telaDeCategorias(page, login, senha) {
  await new LoginPage(page).entrarComo(login, senha)
  const configuracoes = new ConfiguracoesPage(page)
  await configuracoes.abrir()
  await configuracoes.abrirAba('categorias')
  return configuracoes
}

const comoAdmin = (page) =>
  telaDeCategorias(
    page,
    process.env.AGROGESTAO_TEST_LOGIN,
    process.env.AGROGESTAO_TEST_SENHA,
  )

/** Recarrega a lista depois de criar dados direto pela API. */
async function recarregarAba(configuracoes) {
  await configuracoes.abrir()
  await configuracoes.abrirAba('categorias')
}

test('admin cadastra e edita categoria e a API grava o que a tela mostra', async ({
  page,
}) => {
  const configuracoes = await comoAdmin(page)
  const nome = `Categoria Fictícia L12 ${unico()}`
  await configuracoes.cadastrarCategoria(nome)
  await expect(configuracoes.formulario()).toHaveCount(0)
  const id = await configuracoes.idDaCategoria(nome)
  await expect(configuracoes.linhaCategoria(id)).toContainText('Personalizada')
  const api = await apiDaSessao(page)
  expect(await api.get(`/categorias-despesa/${id}`)).toMatchObject({
    id,
    nome,
    sistema: false,
  })

  const editado = `${nome} Editada`
  await configuracoes.editarCategoria(id, editado)
  await expect(configuracoes.formulario()).toHaveCount(0)
  await expect(configuracoes.linhaCategoria(id)).toContainText(editado)
  expect((await api.get(`/categorias-despesa/${id}`)).nome).toBe(editado)
})

test('nome repetido com outra caixa mostra o 422 da API', async ({ page }) => {
  const configuracoes = await comoAdmin(page)
  const nome = `Categoria Fictícia L12 ${unico()}`
  await (await apiDaSessao(page)).post('/categorias-despesa', { nome })
  await configuracoes.cadastrarCategoria(nome.toUpperCase())
  await expect(configuracoes.erroGeral()).toHaveText(
    'Já existe uma categoria com este nome.',
  )
  await expect(configuracoes.formulario()).toBeVisible()
  await expect(configuracoes.valorDoCampo('name')).toHaveValue(
    nome.toUpperCase(),
  )
  await configuracoes.cancelar()
})

test('nome só com espaços mostra o 400 da API no campo', async ({ page }) => {
  const configuracoes = await comoAdmin(page)
  await configuracoes.cadastrarCategoria('   ')
  await expect(configuracoes.erroDoCampo('name')).toHaveText(
    'não deve estar em branco',
  )
  await expect(configuracoes.erroGeral()).toHaveText(
    'Há campos com valores inválidos. Verifique os dados informados.',
  )
  await expect(configuracoes.formulario()).toBeVisible()
  await configuracoes.cancelar()
})

test('Diária é de sistema: sem ações na tela e a API recusa PUT e DELETE', async ({
  page,
}) => {
  const configuracoes = await comoAdmin(page)
  const id = await configuracoes.idDaCategoria('Diária')
  await expect(configuracoes.linhaCategoria(id)).toContainText('Sistema')
  await expect(configuracoes.acaoEditarCategoria(id)).toHaveCount(0)
  await expect(configuracoes.acaoExcluirCategoria(id)).toHaveCount(0)

  const api = await apiDaSessao(page)
  const alteracao = await api.requisitar('PUT', `/categorias-despesa/${id}`, {
    nome: `Diária Fictícia ${unico()}`,
  })
  expect(alteracao.status).toBe(422)
  expect(alteracao.corpo.mensagem).toBe(SISTEMA)
  const exclusao = await api.requisitar('DELETE', `/categorias-despesa/${id}`)
  expect(exclusao.status).toBe(422)
  expect(exclusao.corpo.mensagem).toBe(SISTEMA)
  expect((await api.get(`/categorias-despesa/${id}`)).nome).toBe('Diária')
})

test('exclui pela tela uma categoria sem uso', async ({ page }) => {
  const configuracoes = await comoAdmin(page)
  const api = await apiDaSessao(page)
  const categoria = await api.post('/categorias-despesa', {
    nome: `Categoria Fictícia L12 ${unico()}`,
  })
  await recarregarAba(configuracoes)
  await configuracoes.excluirCategoria(categoria.id)
  await expect(configuracoes.dialogoConfirmacao()).toHaveCount(0)
  await expect(configuracoes.linhaCategoria(categoria.id)).toHaveCount(0)
  await expect(configuracoes.aviso()).toContainText('Categoria excluída.')
  const consulta = await api.requisitar(
    'GET',
    `/categorias-despesa/${categoria.id}`,
  )
  expect(consulta.status).toBe(404)
})

test('categoria usada em despesa não é excluída', async ({ page }) => {
  const configuracoes = await comoAdmin(page)
  const api = await apiDaSessao(page)
  const sufixo = unico()
  const categoria = await api.post('/categorias-despesa', {
    nome: `Categoria Fictícia L12 Em Uso ${sufixo}`,
  })
  const cultura = await api.post('/culturas', {
    nome: `Cultura Fictícia L12 ${sufixo}`,
    tipoCicloPadrao: 'CURTO',
    unidadeMedida: 'kg',
  })
  const produtor = await api.post('/produtores', {
    nome: `Produtor Fictício L12 ${sufixo}`,
    documento: cpfSintetico(),
    ativo: true,
  })
  const propriedade = await api.post(
    `/produtores/${produtor.id}/propriedades`,
    { nome: `Sítio Fictício L12 ${sufixo}` },
  )
  const plantio = await api.post('/plantios', {
    propriedadeId: propriedade.id,
    culturaId: cultura.id,
    area: 1,
    dataInicio: '2026-02-01',
  })
  await api.post('/despesas', {
    produtorId: produtor.id,
    categoriaDespesaId: categoria.id,
    descricao: `Despesa Fictícia L12 ${sufixo}`,
    valorTotal: 50,
    dataFato: '2026-03-01',
    plantioIds: [plantio.id],
  })

  await recarregarAba(configuracoes)
  await configuracoes.excluirCategoria(categoria.id)
  await expect(configuracoes.erroDaConfirmacao()).toHaveText(
    'Esta categoria já foi usada em despesas e não pode ser excluída.',
  )
  await expect(configuracoes.dialogoConfirmacao()).toBeVisible()
  await configuracoes.cancelarConfirmacao()
  await expect(configuracoes.linhaCategoria(categoria.id)).toBeVisible()
  expect((await api.get(`/categorias-despesa/${categoria.id}`)).nome).toBe(
    categoria.nome,
  )
})

test('CONSULTA vê as categorias sem cadastrar, editar nem excluir', async ({
  page,
}) => {
  // Login CONSULTA inserido pela migration de dados iniciais (senha de exemplo da massa).
  const login = await page.request.post('/api/v1/auth/login', {
    data: { login: 'consulta', senha: '12345678' },
  })
  test.skip(
    login.status() !== 200,
    'A massa da API não tem o login consulta; POST /usuarios ainda responde 501.',
  )
  const configuracoes = await telaDeCategorias(page, 'consulta', '12345678')
  const id = await configuracoes.idDaCategoria('Diária')
  await expect(configuracoes.linhaCategoria(id)).toContainText('Sistema')
  await expect(configuracoes.acaoCadastrar()).toHaveCount(0)
  await expect(configuracoes.acoesDeEdicaoCategoria()).toHaveCount(0)
  await expect(configuracoes.acoesDeExclusaoCategoria()).toHaveCount(0)
})
