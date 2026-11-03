import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile, readdir } from 'node:fs/promises'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { rotas } from '../src/mocks/handlers/index.js'

// F14: todo mapeamento dos controllers da API precisa de handler no mock. Quando a API ganhar
// um endpoint, este teste falha até o mock acompanhar.
const CONTROLLERS = fileURLToPath(
  new URL(
    '../../api/src/main/java/com/agrogestao/api/controller/',
    import.meta.url,
  ),
)

async function arquivosJava(pasta) {
  const entradas = await readdir(pasta, { withFileTypes: true })
  const arquivos = await Promise.all(
    entradas.map((entrada) =>
      entrada.isDirectory()
        ? arquivosJava(join(pasta, entrada.name))
        : entrada.name.endsWith('.java')
          ? [join(pasta, entrada.name)]
          : [],
    ),
  )
  return arquivos.flat()
}

// {id} na API e :id no MSW viram o mesmo marcador.
const normalizar = (metodo, caminho) =>
  `${metodo} ${caminho.replace(/\{[^}]+\}|:[A-Za-z]+/g, '{}')}`

async function mapeamentosDaApi() {
  const mapeamentos = []
  for (const arquivo of await arquivosJava(CONTROLLERS)) {
    const codigo = await readFile(arquivo, 'utf8')
    const prefixo = /^@RequestMapping\("([^"]*)"\)/m.exec(codigo)?.[1] ?? ''
    for (const [, metodo, caminho] of codigo.matchAll(
      /@(Get|Post|Put|Delete)Mapping\("([^"]*)"\)/g,
    ))
      mapeamentos.push(normalizar(metodo.toUpperCase(), prefixo + caminho))
  }
  return mapeamentos
}

test('todo endpoint da API tem handler no mock', async () => {
  const api = await mapeamentosDaApi()
  const mock = new Set(
    rotas.map(({ metodo, caminho }) => normalizar(metodo, caminho)),
  )
  const faltando = api.filter((rota) => !mock.has(rota))
  console.log(
    `Mapeamentos da API: ${api.length}; cobertos pelo mock: ${api.length - faltando.length}`,
  )
  assert.ok(api.length > 0, 'Nenhum mapeamento encontrado nos controllers.')
  assert.deepEqual(
    faltando,
    [],
    `Endpoints sem handler no mock:\n${faltando.join('\n')}`,
  )
})

test('o mock não inventa endpoint que a API não tem', async () => {
  const api = new Set(await mapeamentosDaApi())
  const sobrando = rotas
    .map(({ metodo, caminho }) => normalizar(metodo, caminho))
    .filter((rota) => !api.has(rota))
  assert.deepEqual(
    sobrando,
    [],
    `Handlers sem endpoint na API:\n${sobrando.join('\n')}`,
  )
})
