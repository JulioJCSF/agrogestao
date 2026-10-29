# AgroGestão — Casos de Teste: Produção (CT060–CT061)

Complemento da L01 (#50), decidido em 29/10/2026. Demais requisitos: RF15–17, RN04, RN35, RN36, CT007/CT008 e CT057. Dados exclusivamente sintéticos.

### CT060 — Unidade da cultura preenchida e normalizada

| Campo | Conteúdo |
|---|---|
| Regra | RN38 |
| Requisito | RF15, RF16 |
| Tipo | Integração |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição:** plantio de Milho, unidade `kg`.

**Passos:** registrar colheita de 250,125 sem unidade; consultar; alterar para 12,345 com unidade `" KG "`; registrar outra colheita de 0,001; paginar por id.

**Resultado esperado:** 201 com Location na criação, 200 na alteração/consulta; unidade `kg` em todos os registros, quantidades exatas, múltiplas colheitas isoladas por plantio; coleta e autor originais preservados na alteração.

**Automação:** `ProducaoIT` e `ProducaoServiceTest`.

### CT061 — Unidade divergente e alterações incompatíveis rejeitadas

| Campo | Conteúdo |
|---|---|
| Regra | RN38 |
| Requisito | RF15, RF16 |
| Tipo | Integração |
| Prioridade | P2 |
| Natureza | Negativo |

**Pré-condição:** plantio de Milho em `kg`, com uma colheita registrada.

**Passos:** tentar criar/alterar produção em `saca`; mudar a unidade da cultura para `saca`; trocar o plantio para cultura em `saca`.

**Resultado esperado:** 422 REGRA_NEGOCIO em todos; produção, cultura e plantio preservados. Branco e mais de 10 caracteres são erros de formato (400).

**Automação:** `ProducaoIT.deveRejeitarUnidadeDiferenteQuandoCriarOuAlterarColheita` e `devePreservarUnidadeQuandoCulturaOuPlantioJaTiverProducao`.

`ProducaoConcorrenciaIT` também confirma que registrar colheita em 01/07 e encerrar o mesmo plantio em 30/06, concorrentemente, não podem ambos ter sucesso (RN03/RN04).
