import { after, before, beforeEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { setupServer } from 'msw/node'
import { handlers } from '../src/mocks/handlers/index.js'
import { banco, reiniciar } from '../src/mocks/banco.js'

const server = setupServer(...handlers)
before(() => server.listen())
after(() => server.close())
beforeEach(() => reiniciar())
const consultar = async (caminho) => {
  const response = await fetch(`http://localhost/api/v1${caminho}`, {
    headers: { Authorization: 'Bearer mock-operador.mock' },
  })
  return { status: response.status, data: await response.json() }
}

test('mock de ciclo curto devolve mês-calendário consultado, inclusive fevereiro bissexto', async () => {
  banco.plantios[0].tipoCiclo = 'CURTO'
  const { status, data } = await consultar('/plantios/1/resultado?mes=2024-02')
  assert.equal(status, 200)
  assert.equal(data.periodoInicio, '2024-02-01')
  assert.equal(data.periodoFim, '2024-02-29')
  assert.equal(data.resultado, -155)
})

test('mock de ciclo longo ignora mês e usa início e encerramento', async () => {
  Object.assign(banco.plantios[0], {
    tipoCiclo: 'LONGO',
    dataInicio: '2026-02-01',
    dataEncerramento: '2026-06-30',
  })
  const { data } = await consultar('/plantios/1/resultado?mes=2024-02')
  assert.equal(data.periodoInicio, '2026-02-01')
  assert.equal(data.periodoFim, '2026-06-30')
})

test('mock de resultado recusa mês inválido e plantio inexistente', async () => {
  assert.equal(
    (await consultar('/plantios/1/resultado?mes=2026-13')).status,
    400,
  )
  assert.equal((await consultar('/plantios/999/resultado')).status, 404)
})
