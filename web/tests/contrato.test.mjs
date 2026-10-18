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
  const { readCurrency, displayCurrency } = await server.ssrLoadModule(
    '/src/components/formularioValores.js',
  )
  globalThis.sessionStorage = { getItem: () => null, removeItem() {} }
  test('moeda normaliza zeros e mantém precisão decimal', () => {
    assert.equal(readCurrency('0'), '')
    assert.equal(readCurrency('000'), '')
    assert.equal(displayCurrency(readCurrency('000512')), 'R$ 5,12')
    assert.equal(readCurrency('900719925474099312'), '9007199254740993.12')
  })
  test('erros sem resposta usam mensagens em português e preservam erros da API', async () => {
    const originalAdapter = api.defaults.adapter
    try {
      for (const [code, message, expected] of [
        [
          'ERR_NETWORK',
          'Network Error',
          'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.',
        ],
        [
          'ECONNABORTED',
          'timeout of 12000ms exceeded',
          'A conexão demorou mais que o esperado. Tente novamente.',
        ],
        [
          'ETIMEDOUT',
          'timeout of 12000ms exceeded',
          'A conexão demorou mais que o esperado. Tente novamente.',
        ],
      ]) {
        api.defaults.adapter = async () => {
          throw Object.assign(new Error(message), { code })
        }
        await assert.rejects(api.get('/culturas'), { message: expected })
      }
      api.defaults.adapter = async () => {
        throw {
          response: {
            status: 422,
            data: {
              mensagem: 'Nome já cadastrado.',
              campos: [{ campo: 'nome', mensagem: 'Duplicado.' }],
            },
          },
        }
      }
      await assert.rejects(api.get('/culturas'), (error) => {
        assert.equal(error.message, 'Nome já cadastrado.')
        assert.equal(error.status, 422)
        assert.deepEqual(error.campos, [
          { campo: 'nome', mensagem: 'Duplicado.' },
        ])
        return true
      })
    } finally {
      api.defaults.adapter = originalAdapter
    }
  })
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
