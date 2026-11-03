import { test, expect } from '@playwright/test'
import { ConfiguracoesPage } from '../pages/ConfiguracoesPage.js'
import { LoginPage } from '../pages/LoginPage.js'

// L12 (#53) contra o mock (F14). A massa tem 10 categorias: 1 "Diária" (sistema),
// 2 "Sementes e mudas", 3 "Adubo e fertilizantes", 4 "Defensivos" … 7 "Transporte"; a nova
// recebe o id 11. As despesas da massa usam as categorias 1, 2 e 3.
async function telaDeCategorias(page, login = 'operador.mock') {
  await new LoginPage(page).entrarComo(login, 'mock')
  const configuracoes = new ConfiguracoesPage(page)
  await configuracoes.abrir()
  await configuracoes.abrirAba('categorias')
  return configuracoes
}

test('categoria de sistema sem ações e personalizada com Editar e Excluir', async ({
  page,
}) => {
  const configuracoes = await telaDeCategorias(page)
  await expect(configuracoes.linhaCategoria(1)).toContainText('Diária')
  await expect(configuracoes.linhaCategoria(1)).toContainText('Sistema')
  await expect(configuracoes.acoesDeEdicaoCategoria()).toHaveCount(9)
  await expect(configuracoes.acoesDeExclusaoCategoria()).toHaveCount(9)
  await expect(configuracoes.acaoEditarCategoria(1)).toHaveCount(0)
  await expect(configuracoes.acaoExcluirCategoria(1)).toHaveCount(0)
  await expect(configuracoes.linhaCategoria(4)).toContainText('Personalizada')
  await expect(configuracoes.acaoEditarCategoria(4)).toBeVisible()
  await expect(configuracoes.acaoExcluirCategoria(4)).toBeVisible()
})

test('cadastra, edita e exclui categoria personalizada', async ({ page }) => {
  const configuracoes = await telaDeCategorias(page)
  await configuracoes.cadastrarCategoria('Categoria Fictícia L12')
  await expect(configuracoes.formulario()).toHaveCount(0)
  const linha = configuracoes.linhaCategoria(11)
  await expect(linha).toContainText('Categoria Fictícia L12')
  await expect(linha).toContainText('Personalizada')

  await configuracoes.editarCategoria(11, 'Categoria Fictícia L12 Editada')
  await expect(configuracoes.formulario()).toHaveCount(0)
  await expect(linha).toContainText('Categoria Fictícia L12 Editada')

  await configuracoes.pedirExclusaoCategoria(11)
  await expect(configuracoes.dialogoConfirmacao()).toBeVisible()
  await configuracoes.cancelarConfirmacao()
  await expect(configuracoes.dialogoConfirmacao()).toHaveCount(0)
  await expect(linha).toBeVisible()

  await configuracoes.excluirCategoria(11)
  await expect(configuracoes.dialogoConfirmacao()).toHaveCount(0)
  await expect(linha).toHaveCount(0)
  await expect(configuracoes.aviso()).toContainText('Categoria excluída.')
})

test('nome repetido com outra caixa mostra o 422 e mantém o texto', async ({
  page,
}) => {
  const configuracoes = await telaDeCategorias(page)
  await configuracoes.cadastrarCategoria('transporte')
  await expect(configuracoes.erroGeral()).toHaveText(
    'Já existe uma categoria com este nome.',
  )
  await expect(configuracoes.formulario()).toBeVisible()
  await expect(configuracoes.valorDoCampo('name')).toHaveValue('transporte')
  await configuracoes.cancelar()
})

test('nome para em 80 caracteres', async ({ page }) => {
  const configuracoes = await telaDeCategorias(page)
  await configuracoes.abrirCadastro()
  await configuracoes.digitarNoCampo('name', 'a'.repeat(81))
  await expect(configuracoes.valorDoCampo('name')).toHaveValue('a'.repeat(80))
  await configuracoes.cancelar()
})

test('categoria usada em despesa não é excluída', async ({ page }) => {
  const configuracoes = await telaDeCategorias(page)
  await configuracoes.excluirCategoria(2)
  await expect(configuracoes.erroDaConfirmacao()).toHaveText(
    'Esta categoria já foi usada em despesas e não pode ser excluída.',
  )
  await expect(configuracoes.dialogoConfirmacao()).toBeVisible()
  await configuracoes.cancelarConfirmacao()
  await expect(configuracoes.linhaCategoria(2)).toContainText(
    'Sementes e mudas',
  )
})

test('CONSULTA vê as categorias sem cadastrar, editar nem excluir', async ({
  page,
}) => {
  const configuracoes = await telaDeCategorias(page, 'consulta.mock')
  await expect(configuracoes.linhaCategoria(1)).toContainText('Sistema')
  await expect(configuracoes.linhaCategoria(2)).toContainText('Personalizada')
  await expect(configuracoes.acaoCadastrar()).toHaveCount(0)
  await expect(configuracoes.acoesDeEdicaoCategoria()).toHaveCount(0)
  await expect(configuracoes.acoesDeExclusaoCategoria()).toHaveCount(0)
})
