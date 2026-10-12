import test from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
const server = await createServer({
  server: { middlewareMode: true },
  appType: 'custom',
})
try {
  const { toApi, fromApi, campoDoFormulario, salvarRegistro } =
    await server.ssrLoadModule('/src/services/registroService.js')
  const { api } = await server.ssrLoadModule('/src/services/api.js')
  globalThis.sessionStorage = { getItem: () => null, removeItem() {} }
  test('contratos usam campo monetário do domínio sem confundir quantidade', () => {
    assert.deepEqual(
      toApi('expenses', {
        producerId: '1',
        categoryId: '2',
        amount: '100.25',
        date: '2026-02-01',
        plantingIds: ['3', '4'],
      }),
      {
        produtorId: 1,
        categoriaDespesaId: 2,
        valorTotal: 100.25,
        dataFato: '2026-02-01',
        plantioIds: [3, 4],
      },
    )
    assert.equal(
      toApi('production', { plantingId: '3', amount: '12.345', unit: 'kg' })
        .quantidade,
      12.345,
    )
    assert.equal(
      toApi('producers', { cpf: '999.000.001-88', status: 'Ativo' }).documento,
      '99900000188',
    )
  })
  test('valores de cultura e erros por campo usam o contrato publicado', () => {
    assert.deepEqual(
      toApi('cultures', { name: 'Fictícia', cycle: 'Longo', unit: 'kg' }),
      { nome: 'Fictícia', tipoCicloPadrao: 'LONGO', unidadeMedida: 'kg' },
    )
    assert.equal(
      fromApi('cultures', {
        id: 1,
        nome: 'Fictícia',
        tipoCicloPadrao: 'CURTO',
        unidadeMedida: 'kg',
      }).id,
      '1',
    )
    assert.equal(campoDoFormulario('culture', 'tipoCicloPadrao'), 'cycle')
    assert.equal(campoDoFormulario('expense', 'valorTotal'), 'amount')
    assert.equal(
      campoDoFormulario('sale', 'itens[0].valorUnitario'),
      'sale-price-0',
    )
  })
  test('criação de propriedade e diária usa as URLs aninhadas', async () => {
    const calls = []
    api.defaults.adapter = async (config) => {
      calls.push({ url: config.url, body: JSON.parse(config.data) })
      return {
        data: { id: 1, nome: 'Fictício', produtorId: 1 },
        status: 201,
        headers: {},
        config,
      }
    }
    await salvarRegistro('properties', {
      producerId: '1',
      name: 'Sítio Fictício',
      area: '',
    })
    await salvarRegistro('dailies', {
      plantingId: '2',
      nature: 'Familiar',
      days: '1.50',
      unitValue: '90.00',
      date: '2026-02-01',
    })
    assert.equal(calls[0].url, '/produtores/1/propriedades')
    assert.deepEqual(calls[0].body, { nome: 'Sítio Fictício', areaTotal: null })
    assert.equal(calls[1].url, '/plantios/2/diarias')
    assert.deepEqual(calls[1].body, {
      natureza: 'FAMILIAR',
      quantidadeDias: 1.5,
      dataFato: '2026-02-01',
    })
  })
} finally {
  await server.close()
}
