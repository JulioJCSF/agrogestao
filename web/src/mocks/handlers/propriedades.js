import { banco, buscar, mensagensNaoEncontrado, proximoId } from '../banco.js'
import { criado, idDoCaminho, pagina, validar } from '../respostas.js'

const PropriedadeRequisicao = {
  nome: { tipo: 'texto', obrigatorio: true, max: 150 },
  areaTotal: { tipo: 'decimal', digitos: [9, 3] },
  localizacao: { tipo: 'texto', max: 255 },
}

// A API não tem regra de negócio (422) para propriedade: área total é opcional e sem mínimo.
function preencher(propriedade, dto) {
  return Object.assign(propriedade, {
    nome: dto.nome.trim(),
    areaTotal: dto.areaTotal == null ? null : Number(dto.areaTotal),
    localizacao: dto.localizacao,
  })
}

export default [
  [
    'get',
    '/produtores/:produtorId/propriedades',
    ({ params, url }) => {
      const produtorId = idDoCaminho(params, 'produtorId')
      buscar('produtores', produtorId, mensagensNaoEncontrado.produtores)
      return pagina(
        url,
        banco.propriedades.filter((item) => item.produtorId === produtorId),
      )
    },
  ],
  [
    'post',
    '/produtores/:produtorId/propriedades',
    ({ params, corpo }) => {
      const produtorId = idDoCaminho(params, 'produtorId')
      const dto = validar(corpo, PropriedadeRequisicao)
      buscar('produtores', produtorId, mensagensNaoEncontrado.produtores)
      const propriedade = preencher(
        { id: proximoId('propriedades'), produtorId },
        dto,
      )
      banco.propriedades.push(propriedade)
      return criado(`/propriedades/${propriedade.id}`, propriedade)
    },
  ],
  [
    'get',
    '/propriedades/:id',
    ({ params }) =>
      buscar(
        'propriedades',
        idDoCaminho(params),
        mensagensNaoEncontrado.propriedades,
      ),
  ],
  [
    'put',
    '/propriedades/:id',
    ({ params, corpo }) => {
      const id = idDoCaminho(params)
      const dto = validar(corpo, PropriedadeRequisicao)
      return preencher(
        buscar('propriedades', id, mensagensNaoEncontrado.propriedades),
        dto,
      )
    },
  ],
]
