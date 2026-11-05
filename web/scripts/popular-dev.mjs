// Popula o banco local de desenvolvimento com uma carga sintética (RNF10), só pela API, para ver
// as telas e as regras juntas. Uso: npm run popular:dev [-- --semente=<n>]
//
// Só aceita localhost: a emissão de relatório é imutável (RN34) e deixaria lixo permanente em
// qualquer outro banco. Credenciais do ambiente ou do .env da raiz; nenhuma senha no código.

import { readFileSync } from 'node:fs'
import { performance } from 'node:perf_hooks'
import { criarAleatorio, cpfSintetico } from './sintetico.mjs'

const HOSTS_PERMITIDOS = ['localhost', '127.0.0.1']
const PREFIXO_PRODUTOR = 'Produtor Fictício Carga'
const INICIO_CARGA = '2025-08-01'
const ULTIMO_INICIO_PLANTIO = '2026-05-31'
const FIM_PRIMEIRA_EMISSAO = '2026-03-31'
const INICIO_SEGUNDA_EMISSAO = '2026-04-01'
const CANAIS = {
  COMERCIO: 'Comércio',
  FEIRA: 'Feira',
  CEASA: 'Ceasa',
  MERENDA: 'Merenda',
  COOPERATIVA: 'Cooperativa',
  OUTRO: 'Outro',
}

function sair(mensagem, codigo = 1) {
  console.error(mensagem)
  process.exit(codigo)
}

// --- Entrada e proteções, antes de qualquer requisição ---

const urlApi = process.env.AGROGESTAO_API_URL || 'http://localhost:8080'
let host
try {
  host = new URL(urlApi).hostname
} catch {
  sair(`AGROGESTAO_API_URL inválida: ${urlApi}`)
}
if (!HOSTS_PERMITIDOS.includes(host))
  sair(
    `Recusado: ${urlApi} não é local. O popular:dev só roda contra localhost ou 127.0.0.1, porque` +
      ' a emissão de relatório é imutável (RN34).',
  )

const argSemente = process.argv.find((arg) => arg.startsWith('--semente='))
const semente = argSemente ? Number(argSemente.split('=')[1]) : 20261105
if (!Number.isInteger(semente)) sair(`Semente inválida: ${argSemente}`)

function lerEnvDaRaiz() {
  let texto = ''
  try {
    texto = readFileSync(new URL('../../.env', import.meta.url), 'utf8')
  } catch {
    return {}
  }
  const valores = {}
  for (const linha of texto.split(/\r?\n/)) {
    const par = linha.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/)
    if (par) valores[par[1]] = par[2].replace(/^(['"])(.*)\1$/, '$2')
  }
  return valores
}
const envRaiz = lerEnvDaRaiz()
const config = (chave) => process.env[chave] ?? envRaiz[chave] ?? ''

// --- Datas (dias UTC desde 1970) e sorteios ---

const hoje = new Date().toLocaleDateString('sv-SE')
const dia = (data) => Date.parse(`${data}T00:00:00Z`) / 86400000
const data = (numero) => new Date(numero * 86400000).toISOString().slice(0, 10)
const maior = (...datas) => datas.reduce((a, b) => (a > b ? a : b))
const menor = (...datas) => datas.reduce((a, b) => (a < b ? a : b))

const aleatorio = criarAleatorio(semente)
const inteiro = (min, max) => min + Math.floor(aleatorio() * (max - min + 1))
const decimal = (min, max, casas) =>
  Number((min + aleatorio() * (max - min)).toFixed(casas))
const escolher = (lista) => lista[Math.floor(aleatorio() * lista.length)]
const dataEntre = (inicio, fim) => data(inteiro(dia(inicio), dia(fim)))
function embaralhar(lista) {
  const copia = [...lista]
  for (let i = copia.length - 1; i > 0; i--) {
    const j = Math.floor(aleatorio() * (i + 1))
    ;[copia[i], copia[j]] = [copia[j], copia[i]]
  }
  return copia
}

// --- Chamadas à API: resposta fora do esperado mostra a chamada e para ---

async function chamar(token, metodo, caminho, corpo, esperados = [200, 201]) {
  const resposta = await fetch(`${urlApi}/api/v1${caminho}`, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  })
  const texto = await resposta.text()
  if (!esperados.includes(resposta.status))
    sair(
      `\nFalha inesperada: ${metodo} ${caminho} -> ${resposta.status}\n${texto}`,
    )
  return {
    status: resposta.status,
    corpo: texto ? JSON.parse(texto) : undefined,
  }
}

async function entrar(login, senha) {
  const { status, corpo } = await chamar(
    null,
    'POST',
    '/auth/login',
    { login, senha },
    [200, 401],
  )
  return status === 200 ? corpo.token : null
}

function sessao(token) {
  return {
    get: async (caminho) => (await chamar(token, 'GET', caminho)).corpo,
    post: async (caminho, corpo) =>
      (await chamar(token, 'POST', caminho, corpo)).corpo,
    put: async (caminho, corpo) =>
      (await chamar(token, 'PUT', caminho, corpo)).corpo,
    tentar: (metodo, caminho, corpo, esperados) =>
      chamar(token, metodo, caminho, corpo, esperados),
  }
}

// --- Execução ---

const inicioExecucao = performance.now()
const adminLogin = config('ADMIN_LOGIN')
const adminSenha = config('ADMIN_SENHA')
if (!adminLogin || !adminSenha)
  sair('Defina ADMIN_LOGIN e ADMIN_SENHA no ambiente ou no .env da raiz.')

let tokenAdmin
try {
  tokenAdmin = await entrar(adminLogin, adminSenha)
} catch (erro) {
  sair(`A API não respondeu em ${urlApi}: ${erro.cause?.code ?? erro.message}`)
}
if (!tokenAdmin)
  sair(`Login recusado para ${adminLogin}. Confira ADMIN_LOGIN e ADMIN_SENHA.`)
const admin = sessao(tokenAdmin)

const jaPopulado = await admin.get(
  `/produtores?busca=${encodeURIComponent(PREFIXO_PRODUTOR)}&tamanho=1`,
)
if (jaPopulado.total > 0)
  sair(
    `O banco já foi populado (há "${PREFIXO_PRODUTOR} …"); nada foi criado.\n` +
      'Para recomeçar, derrube o volume do banco: docker compose down -v e docker compose up -d db.' +
      ' As emissões são imutáveis e não há outra forma de apagá-las.',
    0,
  )

const senhaDev = config('DEV_SENHA_USUARIOS')
const tokenOperador = senhaDev ? await entrar('operador.dev', senhaDev) : null
if (!tokenOperador)
  console.warn(
    'Aviso: sem login do operador.dev (DEV_SENHA_USUARIOS vazia ou recusada); os lançamentos' +
      ' saem como admin.',
  )
const operador = tokenOperador ? sessao(tokenOperador) : admin

const contagem = {
  'Referências de diária': 0,
  Clientes: 0,
  Produtores: 0,
  '  com consentimento': 0,
  Propriedades: 0,
  Plantios: 0,
  Despesas: 0,
  '  compartilhadas': 0,
  'Diárias familiares': 0,
  'Diárias pagas': 0,
  Produções: 0,
  Vendas: 0,
  '  itens': 0,
  '  acima do produzido': 0,
  Estornos: 0,
  Encerramentos: 0,
  Emissões: 0,
  'Emissão recusada (RN37)': 0,
  'Produtores inativados': 0,
}

console.log(`Populando ${urlApi} com a semente ${semente} (hoje: ${hoje})…`)

// 1. Referências de diária: só o que falta, sem sobrepor vigência.
const referencias = (await admin.get('/valores-referencia-diaria?tamanho=100'))
  .itens
const sobrepoe = (inicio, fim) =>
  referencias.some(
    (r) =>
      r.vigenciaInicio <= (fim ?? '9999-12-31') &&
      inicio <= (r.vigenciaFim ?? '9999-12-31'),
  )
for (const desejada of [
  { valor: 75.0, vigenciaInicio: '2025-01-01', vigenciaFim: '2025-12-31' },
  { valor: 80.0, vigenciaInicio: '2026-01-01', vigenciaFim: null },
]) {
  if (sobrepoe(desejada.vigenciaInicio, desejada.vigenciaFim)) continue
  referencias.push(await admin.post('/valores-referencia-diaria', desejada))
  contagem['Referências de diária']++
}
for (let d = dia(INICIO_CARGA); d <= dia(hoje); d++)
  if (!sobrepoe(data(d), data(d)))
    sair(
      `Não há referência de diária vigente em ${data(d)}; a diária familiar seria recusada.`,
    )

// Catálogos.
const culturas = (await admin.get('/culturas?tamanho=100')).itens
const categorias = (
  await admin.get('/categorias-despesa?tamanho=100')
).itens.filter((c) => !c.sistema)

// 2. Clientes: um por canal e mais dois.
const canais = Object.keys(CANAIS)
const porCanal = {}
const clientes = []
for (const canal of [...canais, escolher(canais), escolher(canais)]) {
  porCanal[canal] = (porCanal[canal] ?? 0) + 1
  const nome = `Cliente Fictício Carga ${CANAIS[canal]} ${porCanal[canal]}`
  clientes.push(await admin.post('/clientes', { nome, tipoCanal: canal }))
  contagem.Clientes++
}

// 3. Produtores, propriedades e plantios.
const semConsentimento = new Set(embaralhar([...Array(12).keys()]).slice(0, 2))
const cpfs = new Set()
const produtores = []
for (let i = 0; i < 12; i++) {
  const numero = String(i + 1).padStart(2, '0')
  let documento = cpfSintetico(aleatorio)
  while (cpfs.has(documento)) documento = cpfSintetico(aleatorio)
  cpfs.add(documento)
  const requisicao = {
    nome: `${PREFIXO_PRODUTOR} ${numero}`,
    documento,
    endereco: `Endereço Fictício Carga ${numero}`,
    profissao: escolher([
      'Agricultor fictício',
      'Agricultora fictícia',
      'Horticultor fictício',
    ]),
    contato: `contato.carga${numero}@exemplo.invalid`,
    consentimentoEm: semConsentimento.has(i)
      ? null
      : dataEntre('2025-01-01', '2025-07-31'),
    ativo: true,
  }
  const produtor = await admin.post('/produtores', requisicao)
  contagem.Produtores++
  if (requisicao.consentimentoEm) contagem['  com consentimento']++

  const propriedades = []
  for (let p = 1; p <= inteiro(1, 2); p++) {
    propriedades.push(
      await admin.post(`/produtores/${produtor.id}/propriedades`, {
        nome: `Propriedade Fictícia Carga ${numero}-${p}`,
        areaTotal: decimal(4, 40, 3),
        localizacao: `Localidade Fictícia ${numero}-${p}`,
      }),
    )
    contagem.Propriedades++
  }

  const escolhidas = embaralhar(culturas).slice(0, inteiro(2, 4))
  const destinos = escolhidas.map(
    (_, n) => propriedades[n % propriedades.length],
  )
  const plantios = []
  for (const [n, cultura] of escolhidas.entries()) {
    const propriedade = destinos[n]
    const naMesma = destinos.filter((d) => d === propriedade).length
    const inicio = dataEntre(INICIO_CARGA, menor(ULTIMO_INICIO_PLANTIO, hoje))
    const plantio = await admin.post('/plantios', {
      propriedadeId: propriedade.id,
      culturaId: cultura.id,
      area: Number(
        Math.max(
          0.1,
          decimal(0.5, 0.9, 3) * ((propriedade.areaTotal * 0.9) / naMesma),
        ).toFixed(3),
      ),
      dataInicio: inicio,
    })
    contagem.Plantios++
    // Cerca de 1 em 4 é encerrado depois dos lançamentos; os lançamentos já respeitam a data.
    const encerra = dia(hoje) - dia(inicio) >= 60 && aleatorio() < 0.25
    const encerramento = encerra
      ? dataEntre(data(dia(inicio) + 45), hoje)
      : null
    plantios.push({
      ...plantio,
      cultura,
      inicio,
      fim: encerramento ?? hoje,
      encerramento,
      producoes: [],
      saldo: 0,
    })
  }
  produtores.push({ ...produtor, numero, requisicao, plantios })
}

// 4. Lançamentos, como operador.dev quando houver.
const despesas = []
const estouros = new Set(embaralhar([...Array(12).keys()]).slice(0, 2))
const vendasAcima = []
for (const [i, produtor] of produtores.entries()) {
  const { plantios } = produtor

  for (let n = 1; n <= inteiro(6, 12); n++) {
    let grupo = [escolher(plantios)]
    if (plantios.length > 1 && aleatorio() < 0.4) {
      const candidato = embaralhar(plantios).slice(
        0,
        inteiro(2, Math.min(3, plantios.length)),
      )
      const inicio = maior(...candidato.map((p) => p.inicio))
      const fim = menor(...candidato.map((p) => p.fim))
      if (inicio <= fim) grupo = candidato
    }
    const categoria = escolher(categorias)
    const despesa = await operador.post('/despesas', {
      produtorId: produtor.id,
      categoriaDespesaId: categoria.id,
      descricao: `${categoria.nome} · lançamento fictício ${produtor.numero}-${n}`,
      valorTotal: decimal(80, 2500, 2),
      dataFato: dataEntre(
        maior(...grupo.map((p) => p.inicio)),
        menor(...grupo.map((p) => p.fim)),
      ),
      plantioIds: grupo.map((p) => p.id),
    })
    despesas.push(despesa)
    contagem.Despesas++
    if (grupo.length > 1) contagem['  compartilhadas']++
  }

  for (const plantio of plantios) {
    for (let n = inteiro(1, 3); n > 0; n--) {
      await operador.post(`/plantios/${plantio.id}/diarias`, {
        natureza: 'FAMILIAR',
        quantidadeDias: inteiro(1, 8) / 2,
        dataFato: dataEntre(plantio.inicio, plantio.fim),
      })
      contagem['Diárias familiares']++
    }
    for (let n = inteiro(0, 2); n > 0; n--) {
      await operador.post(`/plantios/${plantio.id}/diarias`, {
        natureza: 'PAGA',
        quantidadeDias: inteiro(1, 6) / 2,
        valorUnitario: decimal(60, 120, 2),
        dataFato: dataEntre(plantio.inicio, plantio.fim),
      })
      contagem['Diárias pagas']++
    }
    for (let n = inteiro(1, 3); n > 0; n--) {
      const producao = await operador.post(
        `/plantios/${plantio.id}/producoes`,
        {
          quantidade: decimal(20, 1500, 3),
          dataFato: dataEntre(plantio.inicio, plantio.fim),
        },
      )
      plantio.producoes.push(producao.dataFato)
      plantio.saldo += Number(producao.quantidade)
      contagem.Produções++
    }
  }

  // Vendas: data depois da primeira produção de cada item e até o fim do plantio.
  const primeiraProducao = (p) => menor(...p.producoes)
  for (let n = 1; n <= inteiro(2, 6); n++) {
    const acima = n === 1 && estouros.has(i)
    let itens = embaralhar(plantios.filter((p) => p.saldo > 1)).slice(
      0,
      inteiro(1, 3),
    )
    if (itens.length === 0) continue
    let inicio = maior(...itens.map(primeiraProducao))
    let fim = menor(...itens.map((p) => p.fim))
    if (inicio > fim) {
      itens = itens.slice(0, 1)
      inicio = primeiraProducao(itens[0])
      fim = itens[0].fim
    }
    const quantidades = itens.map((p, k) =>
      acima && k === 0
        ? Number((p.saldo + decimal(10, 100, 3)).toFixed(3))
        : Number((p.saldo * decimal(0.1, 0.4, 3)).toFixed(3)),
    )
    const venda = await operador.post('/vendas', {
      produtorId: produtor.id,
      clienteId: escolher(clientes).id,
      dataFato: dataEntre(inicio, fim),
      itens: itens.map((p, k) => ({
        plantioId: p.id,
        quantidade: quantidades[k],
        unidadeMedida: p.cultura.unidadeMedida,
        valorUnitario: decimal(1.5, 25, 2),
      })),
    })
    itens.forEach((p, k) => (p.saldo -= quantidades[k]))
    contagem.Vendas++
    contagem['  itens'] += itens.length
    if (acima) {
      if (!venda.avisos?.length)
        sair(
          `A venda ${venda.id}, acima do produzido, voltou sem o aviso de saldo (RN29).`,
        )
      vendasAcima.push({
        venda: venda.id,
        produtor: produtor.nome,
        aviso: venda.avisos[0],
      })
      contagem['  acima do produzido']++
    }
  }
}

// 5. Estornos.
for (const despesa of embaralhar(despesas).slice(0, 2)) {
  await operador.post(`/despesas/${despesa.id}/estorno`)
  contagem.Estornos++
}

// 6. Encerramentos.
for (const produtor of produtores)
  for (const plantio of produtor.plantios.filter((p) => p.encerramento)) {
    await operador.post(`/plantios/${plantio.id}/encerramento`, {
      dataEncerramento: plantio.encerramento,
    })
    contagem.Encerramentos++
  }

// 7. Emissões, depois de todos os lançamentos; o produtor sem consentimento recebe 422 (RN37).
const comConsentimento = produtores.filter((p) => p.requisicao.consentimentoEm)
const emitidos = embaralhar(comConsentimento).slice(0, 6)
for (const produtor of emitidos) {
  await admin.post('/relatorios', {
    produtorId: produtor.id,
    periodoInicio: INICIO_CARGA,
    periodoFim: FIM_PRIMEIRA_EMISSAO,
  })
  contagem.Emissões++
}
if (hoje >= INICIO_SEGUNDA_EMISSAO) {
  await admin.post('/relatorios', {
    produtorId: emitidos[0].id,
    periodoInicio: INICIO_SEGUNDA_EMISSAO,
    periodoFim: hoje,
  })
  contagem.Emissões++
}
const semConsentir = produtores.find((p) => !p.requisicao.consentimentoEm)
const recusa = await admin.tentar(
  'POST',
  '/relatorios',
  {
    produtorId: semConsentir.id,
    periodoInicio: INICIO_CARGA,
    periodoFim: hoje,
  },
  [422],
)
if (!/consentimento/i.test(recusa.corpo?.mensagem ?? ''))
  sair(
    `A emissão sem consentimento voltou 422 por outro motivo: ${JSON.stringify(recusa.corpo)}`,
  )
contagem['Emissão recusada (RN37)']++

// 8. Um produtor com consentimento e sem emissão fica inativo.
const inativo = comConsentimento.find((p) => !emitidos.includes(p))
await admin.put(`/produtores/${inativo.id}`, {
  ...inativo.requisicao,
  ativo: false,
})
contagem['Produtores inativados']++

// --- Resumo ---

const dinheiro = (valor) =>
  Number(valor).toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
const linha = (rotulo, valor) => console.log(`  ${rotulo.padEnd(34)} ${valor}`)

console.log('\nCriado')
for (const [rotulo, valor] of Object.entries(contagem)) linha(rotulo, valor)

console.log(`\nPainel de ${INICIO_CARGA} a ${hoje}`)
for (const produtor of emitidos.slice(0, 3)) {
  const painel = await admin.get(
    `/painel?produtorId=${produtor.id}&periodoInicio=${INICIO_CARGA}&periodoFim=${hoje}`,
  )
  console.log(
    `  ${produtor.nome}: receita ${dinheiro(painel.receita)}, custo ${dinheiro(painel.custoDesembolso)},` +
      ` resultado ${dinheiro(painel.resultado)}`,
  )
  for (const plantio of painel.plantios)
    linha(
      `    ${plantio.culturaNome} (plantio ${plantio.plantioId})`,
      dinheiro(plantio.resultado),
    )
}

console.log('\nPlantios com saldo negativo')
for (const produtor of produtores)
  for (const plantio of produtor.plantios) {
    const saldo = await admin.get(`/plantios/${plantio.id}/saldo`)
    if (Number(saldo.saldo) < 0)
      linha(
        `  plantio ${plantio.id} (${produtor.nome.slice(-2)}, ${plantio.cultura.nome})`,
        `${saldo.saldo} ${saldo.unidadeMedida}`,
      )
  }
for (const { venda, aviso } of vendasAcima)
  console.log(`  venda ${venda}: ${aviso}`)

console.log('\nRelatórios emitidos')
for (const produtor of emitidos) {
  const pagina = await admin.get(`/produtores/${produtor.id}/relatorios`)
  const periodos = pagina.itens
    .map((r) => `${r.periodoInicio} a ${r.periodoFim}`)
    .join('; ')
  linha(`  ${produtor.nome}`, `${pagina.total}: ${periodos}`)
}
linha(`  ${semConsentir.nome}`, `recusado (${recusa.status})`)
linha(`  ${inativo.nome}`, 'inativo')

const logins = new Set(
  (await admin.get('/usuarios?tamanho=100')).itens.map((u) => u.login),
)
console.log('\nLogins para o front (senhas no .env)')
console.log(`  ${adminLogin} (ADMIN)`)
for (const [login, perfil] of [
  ['operador.dev', 'LANCAMENTO'],
  ['consulta.dev', 'CONSULTA'],
])
  if (logins.has(login)) console.log(`  ${login} (${perfil})`)

console.log(
  `\nTempo total: ${((performance.now() - inicioExecucao) / 1000).toFixed(1)} s`,
)
