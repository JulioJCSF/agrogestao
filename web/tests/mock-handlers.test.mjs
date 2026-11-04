import { after, before, beforeEach, describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { setupServer } from 'msw/node'
import { handlers } from '../src/mocks/handlers/index.js'
import { reiniciar } from '../src/mocks/banco.js'

// F14: os handlers do mock respondem como a API (status, erro, mensagem e campos).
const BASE = 'http://localhost/api/v1'
const FUTURO = '2999-01-01'
const VALIDACAO =
  'Há campos com valores inválidos. Verifique os dados informados.'
const server = setupServer(...handlers)

before(() => server.listen({ onUnhandledFrame: 'error' }))
after(() => server.close())
beforeEach(() => reiniciar())

async function chamar(metodo, caminho, { login, corpo } = {}) {
  const resposta = await fetch(BASE + caminho, {
    method: metodo,
    headers: {
      'content-type': 'application/json',
      ...(login ? { Authorization: `Bearer mock-${login}` } : {}),
    },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  })
  const texto = await resposta.text()
  return {
    status: resposta.status,
    location: resposta.headers.get('Location'),
    corpo: texto ? JSON.parse(texto) : null,
  }
}

const operador = (metodo, caminho, corpo) =>
  chamar(metodo, caminho, { login: 'operador.mock', corpo })

function erroEsperado(resposta, status, erro, mensagem, campos = []) {
  assert.equal(resposta.status, status, JSON.stringify(resposta.corpo))
  assert.equal(resposta.corpo.status, status)
  assert.equal(resposta.corpo.erro, erro)
  assert.equal(resposta.corpo.mensagem, mensagem)
  assert.deepEqual(resposta.corpo.campos, campos)
  assert.ok(resposta.corpo.timestamp)
}

const validacao = (resposta, campos) =>
  erroEsperado(resposta, 400, 'VALIDACAO', VALIDACAO, campos)
const regra = (resposta, mensagem, campos) =>
  erroEsperado(resposta, 422, 'REGRA_NEGOCIO', mensagem, campos)

const produtorValido = {
  nome: 'Produtor Fictício Teste',
  documento: '99900000420',
  ativo: true,
}
const plantioValido = {
  propriedadeId: 1,
  culturaId: 1,
  area: 1,
  dataInicio: '2026-03-01',
}

describe('autenticação e permissões', () => {
  test('os três perfis entram e /auth/me lê o Bearer', async () => {
    for (const [login, perfil] of [
      ['admin.mock', 'ADMIN'],
      ['operador.mock', 'LANCAMENTO'],
      ['consulta.mock', 'CONSULTA'],
    ]) {
      const { status, corpo } = await chamar('POST', '/auth/login', {
        corpo: { login, senha: 'mock' },
      })
      assert.equal(status, 200)
      assert.equal(corpo.token, `mock-${login}`)
      assert.equal(corpo.usuario.perfil, perfil)
      assert.ok(Date.parse(corpo.expiraEm) > Date.now() + 59 * 60 * 1000)
      const me = await chamar('GET', '/auth/me', { login })
      assert.deepEqual(me.corpo, corpo.usuario)
    }
  })

  test('senha errada e requisição sem token respondem 401', async () => {
    erroEsperado(
      await chamar('POST', '/auth/login', {
        corpo: { login: 'admin.mock', senha: 'errada' },
      }),
      401,
      'NAO_AUTENTICADO',
      'Login ou senha inválidos.',
    )
    const mensagem = 'Você não está autenticado. Faça login para continuar.'
    erroEsperado(
      await chamar('GET', '/culturas'),
      401,
      'NAO_AUTENTICADO',
      mensagem,
    )
    erroEsperado(
      await chamar('GET', '/culturas', { login: 'desconhecido' }),
      401,
      'NAO_AUTENTICADO',
      mensagem,
    )
  })

  test('CONSULTA não grava e só ADMIN acessa /usuarios', async () => {
    const mensagem = 'Você não tem permissão para realizar esta operação.'
    erroEsperado(
      await chamar('POST', '/culturas', {
        login: 'consulta.mock',
        corpo: {
          nome: 'Cultura Fictícia',
          tipoCicloPadrao: 'CURTO',
          unidadeMedida: 'kg',
        },
      }),
      403,
      'ACESSO_NEGADO',
      mensagem,
    )
    assert.equal(
      (await chamar('GET', '/culturas', { login: 'consulta.mock' })).status,
      200,
    )
    erroEsperado(
      await operador('GET', '/usuarios'),
      403,
      'ACESSO_NEGADO',
      mensagem,
    )
    assert.equal(
      (await chamar('GET', '/usuarios', { login: 'admin.mock' })).status,
      200,
    )
  })
})

describe('formato e paginação', () => {
  test('POST responde 201 com Location e o registro entra na listagem', async () => {
    const criado = await operador('POST', '/produtores', produtorValido)
    assert.equal(criado.status, 201)
    assert.equal(criado.location, `/api/v1/produtores/${criado.corpo.id}`)
    assert.equal(criado.corpo.consentimentoPendente, true)
    const lista = await operador('GET', '/produtores?busca=teste')
    assert.deepEqual(
      lista.corpo.itens.map((item) => item.id),
      [criado.corpo.id],
    )
  })

  test('tamanho acima de 100 é 400 e a página traz os itens certos', async () => {
    validacao(await operador('GET', '/culturas?tamanho=101'), [
      { campo: 'tamanho', mensagem: 'deve ser menor que ou igual à 100' },
    ])
    const { corpo } = await operador('GET', '/culturas?pagina=1&tamanho=2')
    assert.deepEqual(
      corpo.itens.map((item) => item.id),
      [3, 4],
    )
    assert.equal(corpo.pagina, 1)
    assert.equal(corpo.tamanho, 2)
    assert.equal(corpo.total, 9)
  })

  test('produção grava a data de coleta de hoje e DELETE responde 204', async () => {
    const criada = await operador('POST', '/plantios/1/producoes', {
      quantidade: 10,
      dataFato: '2026-07-20',
    })
    assert.equal(criada.status, 201)
    assert.equal(criada.corpo.unidadeMedida, 'kg')
    assert.match(criada.corpo.dataColeta, /^\d{4}-\d{2}-\d{2}$/)
    const removida = await operador('DELETE', `/producoes/${criada.corpo.id}`)
    assert.equal(removida.status, 204)
  })
})

describe('400 e 422 por módulo', () => {
  test('produtores', async () => {
    validacao(
      await operador('POST', '/produtores', { ...produtorValido, nome: '' }),
      [{ campo: 'nome', mensagem: 'não deve estar em branco' }],
    )
    const duplicado = 'Já existe produtor cadastrado com este CPF.'
    regra(
      await operador('POST', '/produtores', {
        ...produtorValido,
        documento: '99900000188',
      }),
      duplicado,
      [{ campo: 'documento', mensagem: duplicado }],
    )
    erroEsperado(
      await operador('GET', '/produtores/999'),
      404,
      'NAO_ENCONTRADO',
      'Produtor não encontrado.',
    )
  })

  test('propriedades', async () => {
    validacao(
      await operador('POST', '/produtores/1/propriedades', { nome: '' }),
      [{ campo: 'nome', mensagem: 'não deve estar em branco' }],
    )
    erroEsperado(
      await operador('POST', '/produtores/999/propriedades', {
        nome: 'Sítio Fictício',
      }),
      404,
      'NAO_ENCONTRADO',
      'Produtor não encontrado.',
    )
  })

  test('culturas', async () => {
    validacao(
      await operador('POST', '/culturas', {
        nome: '',
        tipoCicloPadrao: 'CURTO',
        unidadeMedida: 'kg',
      }),
      [{ campo: 'nome', mensagem: 'não deve estar em branco' }],
    )
    regra(
      await operador('POST', '/culturas', {
        nome: 'MILHO',
        tipoCicloPadrao: 'CURTO',
        unidadeMedida: 'kg',
      }),
      'Já existe uma cultura com este nome.',
      [],
    )
    regra(
      await operador('PUT', '/culturas/1', {
        nome: 'Milho',
        tipoCicloPadrao: 'LONGO',
        unidadeMedida: 'saca',
      }),
      'A unidade da cultura não pode mudar porque já há produção registrada.',
      [],
    )
  })

  test('plantios', async () => {
    validacao(
      await operador('POST', '/plantios', { ...plantioValido, area: null }),
      [{ campo: 'area', mensagem: 'não deve ser nulo' }],
    )
    regra(
      await operador('POST', '/plantios', {
        ...plantioValido,
        dataInicio: FUTURO,
      }),
      'A data de início não pode ser posterior a hoje.',
      [],
    )
    regra(
      await operador('PUT', '/plantios/1', {
        ...plantioValido,
        propriedadeId: 2,
      }),
      'O plantio não pode ser transferido para propriedade de outro produtor.',
      [],
    )
    regra(
      await operador('POST', '/plantios/3/encerramento', {
        dataEncerramento: '2026-07-01',
      }),
      'O plantio já foi encerrado em 30/06/2026.',
      [],
    )
    regra(
      await operador('POST', '/plantios/2/encerramento', {
        dataEncerramento: '2026-02-01',
      }),
      'A data de encerramento não pode ser anterior à data de início (01/03/2026).',
      [],
    )
  })

  test('produção', async () => {
    validacao(
      await operador('POST', '/plantios/1/producoes', {
        dataFato: '2026-07-01',
      }),
      [{ campo: 'quantidade', mensagem: 'não deve ser nulo' }],
    )
    regra(
      await operador('POST', '/plantios/1/producoes', {
        quantidade: 0,
        dataFato: '2026-07-01',
      }),
      'A quantidade produzida deve ser maior que zero.',
      [],
    )
    regra(
      await operador('POST', '/plantios/1/producoes', {
        quantidade: 5,
        unidadeMedida: 'saca',
        dataFato: '2026-07-01',
      }),
      'A unidade da produção deve ser a mesma da cultura do plantio (kg).',
      [{ campo: 'unidadeMedida', mensagem: 'Use a unidade da cultura: kg.' }],
    )
    regra(
      await operador('POST', '/plantios/1/producoes', {
        quantidade: 5,
        dataFato: FUTURO,
      }),
      'A data do fato não pode ser posterior a hoje.',
      [],
    )
  })

  test('categorias', async () => {
    validacao(await operador('POST', '/categorias-despesa', { nome: '' }), [
      { campo: 'nome', mensagem: 'não deve estar em branco' },
    ])
    const sistema = 'Categoria de sistema não pode ser alterada nem removida.'
    regra(
      await operador('PUT', '/categorias-despesa/1', { nome: 'Outra' }),
      sistema,
      [],
    )
    regra(await operador('DELETE', '/categorias-despesa/1'), sistema, [])
    regra(
      await operador('DELETE', '/categorias-despesa/2'),
      'Esta categoria já foi usada em despesas e não pode ser excluída.',
      [],
    )
  })

  test('despesas', async () => {
    const despesa = {
      produtorId: 1,
      categoriaDespesaId: 2,
      descricao: 'Despesa Fictícia',
      valorTotal: 10,
      dataFato: '2026-04-01',
      plantioIds: [1],
    }
    validacao(
      await operador('POST', '/despesas', { ...despesa, valorTotal: null }),
      [{ campo: 'valorTotal', mensagem: 'não deve ser nulo' }],
    )
    regra(
      await operador('POST', '/despesas', { ...despesa, plantioIds: [] }),
      'A despesa precisa estar vinculada a pelo menos um plantio.',
      [],
    )
    regra(
      await operador('POST', '/despesas', {
        ...despesa,
        categoriaDespesaId: 1,
      }),
      'A categoria Diária é lançada automaticamente pela diária paga e não pode ser usada em despesa manual.',
      [],
    )
    const gerada =
      'Esta despesa foi gerada por uma diária paga. Altere ou exclua a diária.'
    regra(await operador('PUT', '/despesas/3', despesa), gerada, [])
    regra(await operador('DELETE', '/despesas/3'), gerada, [])
  })

  test('rateio de R$ 100,00 em três plantios iguais dá o centavo ao menor id', async () => {
    const ids = []
    for (let i = 0; i < 3; i++)
      ids.push((await operador('POST', '/plantios', plantioValido)).corpo.id)
    const { status, corpo } = await operador('POST', '/despesas', {
      produtorId: 1,
      categoriaDespesaId: 2,
      descricao: 'Rateio Fictício',
      valorTotal: 100,
      dataFato: '2026-04-01',
      plantioIds: [...ids].reverse(),
    })
    assert.equal(status, 201)
    assert.deepEqual(corpo.rateios, [
      { plantioId: ids[0], percentual: 0.333333, valorRateado: 33.34 },
      { plantioId: ids[1], percentual: 0.333333, valorRateado: 33.33 },
      { plantioId: ids[2], percentual: 0.333333, valorRateado: 33.33 },
    ])
  })

  test('referências', async () => {
    validacao(
      await operador('POST', '/valores-referencia-diaria', {
        vigenciaInicio: '2027-01-01',
      }),
      [{ campo: 'valor', mensagem: 'não deve ser nulo' }],
    )
    regra(
      await operador('POST', '/valores-referencia-diaria', {
        valor: 90,
        vigenciaInicio: '2026-06-01',
      }),
      'Já existe valor de referência da diária com vigência neste período. Informe um período sem sobreposição.',
      [],
    )
    erroEsperado(
      await operador(
        'GET',
        '/valores-referencia-diaria/vigente?dataFato=2025-06-01',
      ),
      404,
      'NAO_ENCONTRADO',
      'Não há valor de referência da diária vigente nesta data. Cadastre uma referência para o período.',
    )
  })

  test('diárias', async () => {
    validacao(
      await operador('POST', '/plantios/1/diarias', {
        quantidadeDias: 1,
        dataFato: '2026-07-01',
      }),
      [{ campo: 'natureza', mensagem: 'não deve ser nulo' }],
    )
    regra(
      await operador('POST', '/plantios/1/diarias', {
        natureza: 'FAMILIAR',
        quantidadeDias: 1,
        dataFato: '2025-06-01',
      }),
      'Não há valor de referência da diária vigente em 01/06/2025. Cadastre o valor de referência para essa data antes de lançar a diária familiar.',
      [],
    )
    regra(
      await operador('PUT', '/diarias/1', {
        natureza: 'FAMILIAR',
        quantidadeDias: 2,
        dataFato: '2026-07-10',
      }),
      'A natureza da diária não pode ser alterada. Exclua a diária e registre outra com a natureza correta.',
      [
        {
          campo: 'natureza',
          mensagem: 'Exclua e registre outra diária para mudar a natureza.',
        },
      ],
    )
  })

  test('diária paga gera a despesa Diária com rateio de 100%', async () => {
    const { status, corpo } = await operador('POST', '/plantios/2/diarias', {
      natureza: 'PAGA',
      quantidadeDias: 1.5,
      valorUnitario: 33.33,
      dataFato: '2026-08-10',
    })
    assert.equal(status, 201)
    assert.equal(corpo.valorTotal, 50)
    const despesas = (await operador('GET', '/despesas?produtorId=1')).corpo
      .itens
    const gerada = despesas.find((item) => item.diariaId === corpo.id)
    assert.equal(gerada.categoriaDespesaId, 1)
    assert.equal(gerada.descricao, 'Diária paga de 10/08/2026')
    assert.deepEqual(gerada.rateios, [
      { plantioId: 2, percentual: 1, valorRateado: 50 },
    ])
  })

  test('clientes e vendas', async () => {
    validacao(
      await operador('POST', '/clientes', {
        nome: 'Cliente Fictício',
        tipoCanal: 'LOJA',
      }),
      [{ campo: 'tipoCanal', mensagem: 'Valor em formato inválido.' }],
    )
    erroEsperado(
      await operador('GET', '/clientes/999'),
      404,
      'NAO_ENCONTRADO',
      'Cliente não encontrado.',
    )
    const venda = {
      produtorId: 1,
      clienteId: 1,
      dataFato: '2026-08-01',
      itens: [
        {
          plantioId: 1,
          quantidade: 1.005,
          unidadeMedida: 'KG',
          valorUnitario: 2.5,
        },
      ],
    }
    validacao(await operador('POST', '/vendas', { ...venda, itens: [] }), [
      { campo: 'itens', mensagem: 'não deve estar vazio' },
    ])
    regra(
      await operador('POST', '/vendas', { ...venda, dataFato: FUTURO }),
      'A data do fato não pode ser posterior a hoje.',
      [],
    )
    const unidade =
      'A unidade do item (saca) é diferente da unidade da produção registrada no plantio (kg).'
    regra(
      await operador('POST', '/vendas', {
        ...venda,
        itens: [{ ...venda.itens[0], unidadeMedida: 'saca' }],
      }),
      unidade,
      [{ campo: 'itens[0].unidadeMedida', mensagem: unidade }],
    )
    const criada = await operador('POST', '/vendas', venda)
    assert.equal(criada.status, 201)
    // 1,005 × 2,50 = 2,5125 → 2,51 (HALF_UP em 2 casas).
    assert.equal(criada.corpo.itens[0].valorTotal, 2.51)
    assert.equal(criada.corpo.valorTotal, 2.51)
  })

  test('relatórios', async () => {
    validacao(
      await operador('POST', '/relatorios', {
        produtorId: 1,
        periodoFim: '2026-06-30',
      }),
      [{ campo: 'periodoInicio', mensagem: 'não deve ser nulo' }],
    )
    regra(
      await operador('POST', '/relatorios', {
        produtorId: 3,
        periodoInicio: '2026-01-01',
        periodoFim: '2026-06-30',
      }),
      'O produtor ainda não tem consentimento registrado. Registre o consentimento antes de emitir o relatório.',
      [],
    )
    const emitido = await operador('POST', '/relatorios', {
      produtorId: 1,
      periodoInicio: '2026-01-01',
      periodoFim: '2026-12-31',
    })
    assert.equal(emitido.status, 201)
    assert.equal(emitido.location, `/api/v1/relatorios/${emitido.corpo.id}`)
    assert.equal(emitido.corpo.conteudo.produtor.id, 1)
    // RN11: a diária paga em período emitido não pode ser excluída.
    regra(
      await operador('DELETE', '/diarias/1'),
      'A despesa desta diária já consta em relatório emitido. Não altere a data nem exclua a diária. Use o estorno da despesa.',
      [],
    )
  })

  test('usuários', async () => {
    const admin = (metodo, caminho, corpo) =>
      chamar(metodo, caminho, { login: 'admin.mock', corpo })
    const usuario = {
      nome: 'Usuário Fictício',
      login: 'novo.mock',
      senha: 'senha-ficticia',
      perfil: 'CONSULTA',
    }
    validacao(await admin('POST', '/usuarios', { ...usuario, login: '' }), [
      { campo: 'login', mensagem: 'não deve estar em branco' },
    ])
    const repetido = 'Já existe um usuário com este login.'
    regra(
      await admin('POST', '/usuarios', { ...usuario, login: ' ADMIN.Mock ' }),
      repetido,
      [{ campo: 'login', mensagem: repetido }],
    )
    const criado = await admin('POST', '/usuarios', {
      ...usuario,
      login: '  Novo.Mock ',
    })
    assert.equal(criado.status, 201)
    assert.equal(criado.corpo.login, 'novo.mock')
    assert.equal(criado.corpo.senha, undefined)
    const login = await chamar('POST', '/auth/login', {
      corpo: { login: 'NOVO.MOCK', senha: 'senha-ficticia' },
    })
    assert.equal(login.corpo.usuario.perfil, 'CONSULTA')
  })

  test('usuários: redefinição de senha e último admin (M01)', async () => {
    const admin = (metodo, caminho, corpo) =>
      chamar(metodo, caminho, { login: 'admin.mock', corpo })
    const senha = await admin('PUT', '/usuarios/3/senha', {
      senha: 'senha-nova-ficticia',
    })
    assert.equal(senha.status, 204)
    assert.equal(senha.corpo, null)
    const entrar = (s) =>
      chamar('POST', '/auth/login', {
        corpo: { login: 'consulta.mock', senha: s },
      })
    assert.equal((await entrar('mock')).status, 401)
    assert.equal((await entrar('senha-nova-ficticia')).status, 200)
    validacao(await admin('PUT', '/usuarios/3/senha', { senha: '1234567' }), [
      {
        campo: 'senha',
        mensagem: 'tamanho deve ser entre 8 e 72',
      },
    ])
    const longa = 'A senha deve ter no máximo 72 bytes.'
    erroEsperado(
      await admin('PUT', '/usuarios/3/senha', { senha: 'á'.repeat(40) }),
      400,
      'VALIDACAO',
      longa,
      [{ campo: 'senha', mensagem: longa }],
    )
    erroEsperado(
      await admin('PUT', '/usuarios/999/senha', {
        senha: 'senha-nova-ficticia',
      }),
      404,
      'NAO_ENCONTRADO',
      'Usuário não encontrado.',
    )
    assert.equal(
      (
        await operador('PUT', '/usuarios/3/senha', {
          senha: 'senha-nova-ficticia',
        })
      ).status,
      403,
    )
    const ultimo = 'O sistema precisa de pelo menos um administrador ativo.'
    const semAdmin = { nome: 'Administrador Fictício', perfil: 'ADMIN' }
    regra(
      await admin('PUT', '/usuarios/1', { ...semAdmin, ativo: false }),
      ultimo,
      [],
    )
    regra(
      await admin('PUT', '/usuarios/1', {
        ...semAdmin,
        perfil: 'LANCAMENTO',
        ativo: true,
      }),
      ultimo,
      [],
    )
    // Com outro ADMIN ativo, o primeiro pode se desativar.
    await admin('PUT', '/usuarios/2', {
      nome: 'Operador Fictício',
      perfil: 'ADMIN',
      ativo: true,
    })
    const desativado = await admin('PUT', '/usuarios/1', {
      ...semAdmin,
      ativo: false,
    })
    assert.equal(desativado.status, 200)
    assert.equal(desativado.corpo.ativo, false)
  })
})

describe('saldo do plantio (M05)', () => {
  test('soma produção e vendas da massa, com sinal, e responde 404', async () => {
    const inicial = await operador('GET', '/plantios/1/saldo')
    assert.equal(inicial.status, 200)
    assert.deepEqual(inicial.corpo, {
      plantioId: 1,
      quantidadeProduzida: 120.5,
      quantidadeVendida: 50,
      saldo: 70.5,
      unidadeMedida: 'kg',
    })

    // RN29: vender acima do produzido é aceito, e o saldo fica negativo.
    const venda = await operador('POST', '/vendas', {
      produtorId: 1,
      clienteId: 1,
      dataFato: '2026-08-01',
      itens: [
        {
          plantioId: 1,
          quantidade: 100.001,
          unidadeMedida: 'kg',
          valorUnitario: 2,
        },
      ],
    })
    assert.equal(venda.status, 201)
    const depois = await operador('GET', '/plantios/1/saldo')
    assert.equal(depois.corpo.quantidadeVendida, 150.001)
    assert.equal(depois.corpo.saldo, -29.501)

    erroEsperado(
      await operador('GET', '/plantios/999/saldo'),
      404,
      'NAO_ENCONTRADO',
      'Plantio não encontrado.',
    )
  })
})

test('reiniciar() descarta o que foi gravado', async () => {
  await operador('POST', '/culturas', {
    nome: 'Cultura Fictícia Temporária',
    tipoCicloPadrao: 'CURTO',
    unidadeMedida: 'kg',
  })
  reiniciar()
  assert.equal((await operador('GET', '/culturas')).corpo.total, 9)
})
