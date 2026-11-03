import { HttpResponse } from 'msw/http'

// Mensagens do ApiExceptionHandler, para o mock responder com o mesmo texto da API.
export const MENSAGEM_VALIDACAO =
  'Há campos com valores inválidos. Verifique os dados informados.'
export const MENSAGEM_CORPO_INVALIDO =
  'O corpo da requisição não está em um formato válido.'
export const MENSAGEM_VALOR_INVALIDO = 'Valor em formato inválido.'
export const MENSAGEM_REQUISICAO_INVALIDA =
  'A requisição não pôde ser processada. Verifique os dados informados.'

/** Equivale às exceções da API: o handler lança e a rota converte em ErroResposta. */
export class ErroApi extends Error {
  constructor(status, codigo, mensagem, campos = []) {
    super(mensagem)
    this.status = status
    this.codigo = codigo
    this.campos = campos
  }
}

export const naoEncontrado = (mensagem) =>
  new ErroApi(404, 'NAO_ENCONTRADO', mensagem)
export const regraNegocio = (mensagem, campos = []) =>
  new ErroApi(422, 'REGRA_NEGOCIO', mensagem, campos)
export const validacao = (campos, mensagem = MENSAGEM_VALIDACAO) =>
  new ErroApi(400, 'VALIDACAO', mensagem, campos)

export function erro(status, codigo, mensagem, campos = []) {
  return HttpResponse.json(
    {
      timestamp: new Date().toISOString(),
      status,
      erro: codigo,
      mensagem,
      campos,
    },
    { status },
  )
}

export function criado(caminho, corpo) {
  return HttpResponse.json(corpo, {
    status: 201,
    headers: { Location: `/api/v1${caminho}` },
  })
}

export const semConteudo = () => new HttpResponse(null, { status: 204 })

const inteiro = (valor) => (/^-?\d+$/.test(valor) ? Number(valor) : NaN)

/** PaginacaoConsulta: pagina >= 0, tamanho de 1 a 100 (padrão 20), ordenada por id. */
export function pagina(url, itens) {
  const campos = []
  const numero = inteiro(url.searchParams.get('pagina') ?? '0')
  const tamanho = inteiro(url.searchParams.get('tamanho') ?? '20')
  if (Number.isNaN(numero))
    campos.push({ campo: 'pagina', mensagem: MENSAGEM_VALOR_INVALIDO })
  else if (numero < 0)
    campos.push({
      campo: 'pagina',
      mensagem: 'deve ser maior que ou igual à 0',
    })
  if (Number.isNaN(tamanho))
    campos.push({ campo: 'tamanho', mensagem: MENSAGEM_VALOR_INVALIDO })
  else if (tamanho < 1)
    campos.push({
      campo: 'tamanho',
      mensagem: 'deve ser maior que ou igual à 1',
    })
  else if (tamanho > 100)
    campos.push({
      campo: 'tamanho',
      mensagem: 'deve ser menor que ou igual à 100',
    })
  if (campos.length) throw validacao(campos)
  const ordenados = [...itens].sort((a, b) => a.id - b.id)
  return {
    itens: ordenados.slice(numero * tamanho, (numero + 1) * tamanho),
    pagina: numero,
    tamanho,
    total: ordenados.length,
  }
}

/** Parâmetro de consulta opcional, convertido como o Spring faz; formato inválido é 400. */
export function parametro(url, nome, tipo) {
  const valor = url.searchParams.get(nome)
  if (valor == null || valor === '') return null
  const convertido = converter(valor, tipo)
  if (convertido === undefined)
    throw validacao([{ campo: nome, mensagem: MENSAGEM_VALOR_INVALIDO }])
  return convertido
}

export function idDoCaminho(params, nome = 'id') {
  const id = inteiro(params[nome])
  if (Number.isNaN(id))
    throw validacao([{ campo: nome, mensagem: MENSAGEM_VALOR_INVALIDO }])
  return id
}

const DATA = /^\d{4}-\d{2}-\d{2}$/
function dataValida(texto) {
  if (!DATA.test(texto)) return false
  const data = new Date(`${texto}T00:00:00Z`)
  return !Number.isNaN(data.getTime()) && data.toISOString().startsWith(texto)
}

/** Conversão do Jackson: undefined quando o valor não é do tipo esperado. */
function converter(valor, tipo) {
  if (Array.isArray(tipo)) return tipo.includes(valor) ? valor : undefined
  if (tipo === 'texto')
    return ['string', 'number', 'boolean'].includes(typeof valor)
      ? String(valor)
      : undefined
  if (tipo === 'inteiro') {
    const numero = typeof valor === 'number' ? valor : inteiro(String(valor))
    return Number.isInteger(numero) ? numero : undefined
  }
  if (tipo === 'decimal') {
    const texto = typeof valor === 'number' ? String(valor) : valor
    return typeof texto === 'string' && /^-?\d+(\.\d+)?$/.test(texto.trim())
      ? texto.trim()
      : undefined
  }
  if (tipo === 'data')
    return typeof valor === 'string' && dataValida(valor) ? valor : undefined
  if (tipo === 'booleano')
    return typeof valor === 'boolean'
      ? valor
      : valor === 'true' || valor === 'false'
        ? valor === 'true'
        : undefined
  throw new Error(`Tipo desconhecido no mock: ${tipo}`)
}

/**
 * Valida o corpo como Jackson + Bean Validation da API: primeiro o formato de cada campo (um
 * erro por vez, como o HttpMessageNotReadableException), depois as anotações do DTO.
 * Decimais voltam como texto, para os cálculos não sofrerem com ponto flutuante.
 */
export function validar(corpo, esquema, prefixo = '') {
  if (corpo == null || typeof corpo !== 'object' || Array.isArray(corpo))
    throw validacao([], MENSAGEM_CORPO_INVALIDO)
  const convertido = {}
  for (const [campo, regra] of Object.entries(esquema)) {
    const valor = corpo[campo]
    if (valor == null) {
      convertido[campo] = null
      continue
    }
    if (regra.lista) {
      if (!Array.isArray(valor))
        throw validacao([
          { campo: `${prefixo}${campo}`, mensagem: MENSAGEM_VALOR_INVALIDO },
        ])
      convertido[campo] = valor.map((item, indice) => {
        if (item == null) return null
        if (regra.item)
          return validarFormato(
            item,
            regra.item,
            `${prefixo}${campo}[${indice}].`,
          )
        const valorItem = converter(item, regra.tipo)
        if (valorItem === undefined)
          throw validacao([
            {
              campo: `${prefixo}${campo}[${indice}]`,
              mensagem: MENSAGEM_VALOR_INVALIDO,
            },
          ])
        return valorItem
      })
      continue
    }
    const valorConvertido = converter(valor, regra.tipo)
    if (valorConvertido === undefined)
      throw validacao([
        { campo: `${prefixo}${campo}`, mensagem: MENSAGEM_VALOR_INVALIDO },
      ])
    convertido[campo] = valorConvertido
  }
  const campos = restricoes(convertido, esquema, prefixo)
  if (campos.length) throw validacao(campos)
  return convertido
}

function validarFormato(corpo, esquema, prefixo) {
  if (typeof corpo !== 'object' || Array.isArray(corpo))
    throw validacao([
      { campo: prefixo.slice(0, -1), mensagem: MENSAGEM_VALOR_INVALIDO },
    ])
  const convertido = {}
  for (const [campo, regra] of Object.entries(esquema)) {
    const valor = corpo[campo]
    if (valor == null) {
      convertido[campo] = null
      continue
    }
    const valorConvertido = converter(valor, regra.tipo)
    if (valorConvertido === undefined)
      throw validacao([
        { campo: `${prefixo}${campo}`, mensagem: MENSAGEM_VALOR_INVALIDO },
      ])
    convertido[campo] = valorConvertido
  }
  return convertido
}

function digitos(texto) {
  const [inteiros, fracao = ''] = texto.replace('-', '').split('.')
  return [
    inteiros.replace(/^0+(?=\d)/, '').length,
    fracao.replace(/0+$/, '').length,
  ]
}

// Mensagens padrão do Hibernate Validator em pt-BR, as mesmas que a API devolve em `campos`.
function restricoes(corpo, esquema, prefixo) {
  const campos = []
  const falha = (campo, mensagem) =>
    campos.push({ campo: `${prefixo}${campo}`, mensagem })
  for (const [campo, regra] of Object.entries(esquema)) {
    const valor = corpo[campo]
    if (valor == null) {
      if (regra.obrigatorio || regra.naoVazio)
        falha(
          campo,
          regra.naoVazio
            ? 'não deve estar vazio'
            : regra.tipo === 'texto'
              ? 'não deve estar em branco'
              : 'não deve ser nulo',
        )
      continue
    }
    if (regra.lista) {
      if (regra.naoVazio && !valor.length) falha(campo, 'não deve estar vazio')
      valor.forEach((item, indice) => {
        if (item == null) falha(`${campo}[${indice}]`, 'não deve ser nulo')
        else if (regra.item)
          campos.push(
            ...restricoes(item, regra.item, `${prefixo}${campo}[${indice}].`),
          )
        else if (regra.positivo && Number(item) <= 0)
          falha(`${campo}[${indice}]`, 'deve ser maior que 0')
      })
      continue
    }
    if (regra.tipo === 'texto') {
      if (regra.obrigatorio && !valor.trim())
        falha(campo, 'não deve estar em branco')
      if (regra.padrao && !regra.padrao.regex.test(valor))
        falha(campo, regra.padrao.mensagem)
      if (regra.max && valor.length > regra.max)
        falha(campo, `tamanho deve ser entre ${regra.min ?? 0} e ${regra.max}`)
      else if (regra.min && valor.length < regra.min)
        falha(campo, `tamanho deve ser entre ${regra.min} e ${regra.max}`)
      if (regra.cpf && /^\d{11}$/.test(valor) && !cpfValido(valor))
        falha(campo, regra.cpf)
    }
    if (regra.positivo && Number(valor) <= 0)
      falha(campo, 'deve ser maior que 0')
    if (regra.digitos) {
      const [inteiros, fracao] = digitos(valor)
      if (inteiros > regra.digitos[0] || fracao > regra.digitos[1])
        falha(
          campo,
          `valor numérico fora do limite (<${regra.digitos[0]} dígitos>.<${regra.digitos[1]} dígitos> esperado)`,
        )
    }
    if (regra.passadoOuPresente && valor > hoje())
      falha(campo, regra.passadoOuPresente)
  }
  return campos
}

export function cpfValido(cpf) {
  if (/^(\d)\1{10}$/.test(cpf)) return false
  return cpf.slice(0, 9) + digitosCpf(cpf.slice(0, 9)) === cpf
}

export function digitosCpf(base) {
  const dv = (numeros) => {
    const soma = [...numeros].reduce(
      (total, digito, indice) =>
        total + Number(digito) * (numeros.length + 1 - indice),
      0,
    )
    const resto = (soma * 10) % 11
    return resto === 10 ? 0 : resto
  }
  const primeiro = dv(base)
  return `${primeiro}${dv(base + primeiro)}`
}

/** Hoje no fuso da API (America/Fortaleza), em AAAA-MM-DD. */
export function hoje() {
  return new Date().toLocaleDateString('en-CA', {
    timeZone: 'America/Fortaleza',
  })
}

export const dataBr = (data) => data.split('-').reverse().join('/')

// Dinheiro e quantidades em inteiros escalados, para não arredondar em ponto flutuante.
export function escalar(texto, casas) {
  const [inteiros, fracao = ''] = String(texto).split('.')
  const negativo = inteiros.startsWith('-')
  const valor = BigInt(
    inteiros.replace('-', '') + fracao.padEnd(casas, '0').slice(0, casas),
  )
  return negativo ? -valor : valor
}

export function numero(valor, casas) {
  const negativo = valor < 0n
  const texto = (negativo ? -valor : valor).toString().padStart(casas + 1, '0')
  const resultado = Number(`${texto.slice(0, -casas)}.${texto.slice(-casas)}`)
  return negativo ? -resultado : resultado
}

/** quantidade × valor unitário, arredondado em 2 casas com HALF_UP (RN16 e RN30). */
export function multiplicarHalfUp(quantidade, valorUnitario) {
  const produto = escalar(quantidade, 3) * escalar(valorUnitario, 2)
  return numero((produto + 500n) / 1000n, 2)
}
