import { renderToString } from 'react-dom/server'
import { createServer } from 'vite'
import assert from 'node:assert/strict'

const data = new Map()
globalThis.sessionStorage = {
  getItem: (key) => data.get(key) ?? null,
  setItem: (key, value) => data.set(key, value),
  removeItem: (key) => data.delete(key),
}
const vite = await createServer({
  server: { middlewareMode: true },
  appType: 'custom',
})
try {
  const { smokeTree } = await vite.ssrLoadModule('/tests/smoke-entry.jsx')
  globalThis.sessionStorage.setItem(
    'agrogestao-session',
    JSON.stringify({ demo: true, user: { nome: 'Teste', perfil: 'OPERADOR' } }),
  )
  const expected = new Map([
    ['/', 'Visão geral'],
    ['/produtores', 'Produtores'],
    ['/produtores/p1', 'Antônio Ferreira'],
    ['/producao', 'Produção'],
    ['/plantios/pl1', 'Tomate'],
    ['/financeiro', 'Financeiro'],
    ['/vendas', 'Vendas'],
    ['/relatorios', 'Relatórios'],
    ['/configuracoes', 'Configurações'],
  ])
  for (const [path, heading] of expected) {
    const html = renderToString(smokeTree(path))
    if (!html.includes(heading))
      throw new Error(`Falha ao renderizar ${path}: ${heading} ausente`)
    console.log(`OK ${path}`)
  }
  const { readMock, salvarRegistro, removerRegistro, encerrarPlantioRegistro } =
    await vite.ssrLoadModule('/src/services/registroService.js')
  assert.equal(readMock().dashboard.result, 5144)
  await assert.rejects(
    salvarRegistro('producers', { name: 'Teste' }, true),
    /prévia visual/,
  )
  await assert.rejects(
    removerRegistro('producers', 'p1', true),
    /prévia visual/,
  )
  await assert.rejects(
    encerrarPlantioRegistro('pl1', '2026-10-03', true),
    /prévia visual/,
  )
  assert.equal(readMock().producers.length, 4)
  globalThis.sessionStorage.setItem(
    'agrogestao-session',
    JSON.stringify({ demo: true, user: { nome: 'Admin', perfil: 'ADMIN' } }),
  )
  assert.match(renderToString(smokeTree('/configuracoes')), /tab-usuarios/)
  console.log('OK prévia somente leitura e visualização ADMIN')
} finally {
  await vite.close()
}
