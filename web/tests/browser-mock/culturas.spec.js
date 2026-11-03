import { test, expect } from '@playwright/test'
import { ConfiguracoesPage } from '../pages/ConfiguracoesPage.js'
import { LoginPage } from '../pages/LoginPage.js'

// C07 (#43) contra o mock (F14). A massa tem 9 culturas em kg; a nova recebe o id 10.
// Milho (id 1) já tem produção no plantio 1, o que dispara a RN38.
async function telaDeCulturas(page, login = 'operador.mock') {
  await new LoginPage(page).entrarComo(login, 'mock')
  const configuracoes = new ConfiguracoesPage(page)
  await configuracoes.abrir()
  await configuracoes.abrirAba('culturas')
  return configuracoes
}

test('cadastra e edita cultura com unidade livre', async ({ page }) => {
  const configuracoes = await telaDeCulturas(page)
  const nome = 'Cultura Fictícia C07'
  await configuracoes.cadastrarCultura({
    nome,
    ciclo: 'Longo',
    unidade: 'molho',
  })
  await expect(configuracoes.formulario()).toHaveCount(0)
  const linha = configuracoes.linhaCultura(10)
  await expect(linha).toContainText(nome)
  await expect(linha).toContainText('Longo')
  await expect(linha).toContainText('molho')

  await configuracoes.editarCultura(10, { ciclo: 'Curto', unidade: 'cento' })
  await expect(configuracoes.formulario()).toHaveCount(0)
  await expect(linha).toContainText('Curto')
  await expect(linha).toContainText('cento')

  await configuracoes.abrirEdicaoCultura(10)
  await expect(configuracoes.valorDoCampo('name')).toHaveValue(nome)
  await expect(configuracoes.valorDoCampo('unit')).toHaveValue('cento')
  await configuracoes.cancelar()
})

test('nome vazio é barrado e a unidade para em 10 caracteres', async ({
  page,
}) => {
  const configuracoes = await telaDeCulturas(page)
  await configuracoes.cadastrarCultura({ nome: '' })
  expect(await configuracoes.campoObrigatorioVazio('name')).toBe(true)
  await expect(configuracoes.formulario()).toBeVisible()
  await expect(configuracoes.linhaCultura(10)).toHaveCount(0)

  await configuracoes.digitarNoCampo('unit', 'unidadeslon')
  await expect(configuracoes.valorDoCampo('unit')).toHaveValue('unidadeslo')
  await configuracoes.cancelar()
})

test('nome repetido mostra o 422 e mantém o que foi digitado', async ({
  page,
}) => {
  const configuracoes = await telaDeCulturas(page)
  await configuracoes.cadastrarCultura({ nome: 'milho', unidade: 'saca' })
  await expect(configuracoes.erroGeral()).toHaveText(
    'Já existe uma cultura com este nome.',
  )
  await expect(configuracoes.formulario()).toBeVisible()
  await expect(configuracoes.valorDoCampo('name')).toHaveValue('milho')
  await expect(configuracoes.valorDoCampo('unit')).toHaveValue('saca')
  await configuracoes.cancelar()
})

test('RN38: unidade não muda com produção registrada', async ({ page }) => {
  const configuracoes = await telaDeCulturas(page)
  await configuracoes.editarCultura(1, { unidade: 'saca' })
  await expect(configuracoes.erroGeral()).toHaveText(
    'A unidade da cultura não pode mudar porque já há produção registrada.',
  )
  await expect(configuracoes.valorDoCampo('unit')).toHaveValue('saca')
  await configuracoes.cancelar()
  await expect(configuracoes.linhaCultura(1)).toContainText('kg')
  await expect(configuracoes.linhaCultura(1)).not.toContainText('saca')
})

test('CONSULTA vê as culturas sem cadastrar nem editar', async ({ page }) => {
  const configuracoes = await telaDeCulturas(page, 'consulta.mock')
  await expect(configuracoes.linhaCultura(1)).toContainText('Milho')
  await expect(configuracoes.acaoCadastrar()).toHaveCount(0)
  await expect(configuracoes.acoesDeEdicao()).toHaveCount(0)
})
