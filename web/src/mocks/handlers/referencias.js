import { banco, buscar, mensagensNaoEncontrado, proximoId } from '../banco.js'
import {
  MENSAGEM_REQUISICAO_INVALIDA,
  criado,
  idDoCaminho,
  naoEncontrado,
  pagina,
  parametro,
  regraNegocio,
  validacao,
  validar,
} from '../respostas.js'

const ValorReferenciaDiariaRequisicao = {
  valor: { tipo: 'decimal', obrigatorio: true, digitos: [10, 2] },
  vigenciaInicio: { tipo: 'data', obrigatorio: true },
  vigenciaFim: { tipo: 'data' },
}

const NAO_ENCONTRADO = mensagensNaoEncontrado.referencias
const SOBREPOSICAO =
  'Já existe valor de referência da diária com vigência neste período. Informe um período sem sobreposição.'

/** Vigência com bordas inclusivas e fim opcional (RN17). */
export const vigenteEm = (data) =>
  banco.referencias.find(
    (referencia) =>
      referencia.vigenciaInicio <= data &&
      (referencia.vigenciaFim == null || data <= referencia.vigenciaFim),
  )

function validarVigencia(dto, ignorarId) {
  if (Number(dto.valor) <= 0)
    throw regraNegocio(
      'O valor de referência da diária deve ser maior que zero.',
    )
  if (dto.vigenciaFim && dto.vigenciaFim < dto.vigenciaInicio)
    throw regraNegocio('O fim da vigência não pode ser anterior ao início.')
  const fim = dto.vigenciaFim ?? '9999-12-31'
  if (
    banco.referencias.some(
      (referencia) =>
        referencia.id !== ignorarId &&
        referencia.vigenciaInicio <= fim &&
        dto.vigenciaInicio <= (referencia.vigenciaFim ?? '9999-12-31'),
    )
  )
    throw regraNegocio(SOBREPOSICAO)
}

const preencher = (referencia, dto) =>
  Object.assign(referencia, {
    valor: Number(dto.valor),
    vigenciaInicio: dto.vigenciaInicio,
    vigenciaFim: dto.vigenciaFim,
  })

export default [
  [
    'get',
    '/valores-referencia-diaria',
    ({ url }) => pagina(url, banco.referencias),
  ],
  [
    'post',
    '/valores-referencia-diaria',
    ({ corpo }) => {
      const dto = validar(corpo, ValorReferenciaDiariaRequisicao)
      validarVigencia(dto, null)
      const referencia = preencher({ id: proximoId('referencias') }, dto)
      banco.referencias.push(referencia)
      return criado(`/valores-referencia-diaria/${referencia.id}`, referencia)
    },
  ],
  // Antes de /:id, para "vigente" não ser lido como id.
  [
    'get',
    '/valores-referencia-diaria/vigente',
    ({ url }) => {
      const dataFato = parametro(url, 'dataFato', 'data')
      if (!dataFato) throw validacao([], MENSAGEM_REQUISICAO_INVALIDA)
      const referencia = vigenteEm(dataFato)
      if (!referencia)
        throw naoEncontrado(
          'Não há valor de referência da diária vigente nesta data. Cadastre uma referência para o período.',
        )
      return referencia
    },
  ],
  [
    'get',
    '/valores-referencia-diaria/:id',
    ({ params }) => buscar('referencias', idDoCaminho(params), NAO_ENCONTRADO),
  ],
  [
    'put',
    '/valores-referencia-diaria/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, ValorReferenciaDiariaRequisicao)
      const referencia = buscar('referencias', id, NAO_ENCONTRADO)
      validarVigencia(dto, id)
      return preencher(referencia, dto)
    },
  ],
]
