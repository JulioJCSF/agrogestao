# AgroGestão — Casos de Teste: Diária e Valor de Referência (CT021–CT031)

Versão 1.1

Especificação dos casos de diária e valor de referência no padrão da QA 3.3. Destrava L07 e documenta o que a L06 já automatizou (CT029 e CT030).

**Documentos relacionados**

- `agrogestao-fase-1-regras-negocio.md` — RN12 a RN18 (RN13, RN15 e RN16 completadas na versão 1.5)
- `agrogestao-fase-1-requisitos.md` — RF24 a RF28
- `agrogestao-fase-3-qa.md` — 3.2 (matriz) e 3.3 (padrão)
- `agrogestao-fase-2-der.md` — tabelas `diaria`, `valor_referencia_diaria`, `despesa`, `rateio_despesa` e `categoria_despesa`; seção 5 (RN16 fica no serviço)
- `agrogestao-fase-2-padroes-tecnicos.md` — 2.4, contratos de diária, referência e despesa; formato de erro
- `agrogestao-l06-referencia-diaria.md` — o que a L06 implementou para RN17 e RN18
- `api/src/main/java/com/agrogestao/api/dto/` — `DiariaRequisicao` e `DiariaResposta`, contrato da F13
- `api/src/test/java/com/agrogestao/api/controller/referencia/ValorReferenciaDiariaIT.java` e `api/src/test/java/com/agrogestao/api/service/referencia/ValorReferenciaDiariaServiceTest.java` — automação de CT029 e CT030

---

## Visão geral

| Caso | Título | Regra | Requisito | Tipo | Prior. | Natureza | Issue |
|---|---|---|---|---|---|---|---|
| CT021 | Diária paga ou familiar é registrada com a natureza | RN12 | RF24 | Integração | P3 | Positivo | L07 |
| CT022 | Diária sem natureza ou com natureza inválida é rejeitada | RN12 | RF24 | Integração | P3 | Negativo | L07 |
| CT023 | Familiar recebe a referência vigente na data do fato | RN13 | RF26 | Unitário | **P1** | Positivo | L07 |
| CT024 | Familiar sem referência vigente é rejeitada | RN14 | RF25 | Unitário | P2 | Negativo | L07 |
| CT025 | Paga usa o valor informado, não a referência | RN15 | RF24 | Unitário | P2 | Positivo | L07 |
| CT026 | Paga gera despesa na mesma transação | RN16 | RF27 | Integração | **P1** | Positivo | L07 |
| CT027 | Alterar ou excluir a diária paga leva a despesa junto | RN16 | RF27 | Integração | **P1** | Positivo | L07 |
| CT028 | Familiar não gera despesa e a despesa gerada não é editável | RN16 | RF27 | Integração | **P1** | Negativo | L07 |
| CT029 | Vigências adjacentes são aceitas | RN17 | RF25 | Integração | P2 | Positivo | L06 |
| CT030 | Vigências sobrepostas são rejeitadas | RN17 | RF25 | Integração | P2 | Negativo | L06 |
| CT031 | Alterar a referência não muda diária já lançada | RN18 | RF26 | Unitário | **P1** | Positivo | L07 |

**Sobre a regra do par.** RN12 (CT021 positivo, CT022 negativo), RN16 (CT026 e CT027 positivos, CT028 negativo) e RN17 (CT029 positivo, CT030 negativo) têm par. RN13 e RN14 formam par entre si: o CT023 é o positivo da valoração da familiar e o CT024 é o negativo — a familiar sem referência vigente é recusada. RN15 descreve de onde vem o valor da paga, sem violação própria; o CT025 exclui o valor errado com número concreto (190,00, não 160,50) e cobre a rejeição da paga sem valor como borda. RN18 descreve ausência de efeito: não há entrada a rejeitar, e o CT031 exclui o valor errado (120,00 preservado, não 142,50), no mesmo raciocínio do CT009.

---

## Convenções

| Item | Decisão | Por quê |
|---|---|---|
| Natureza ausente ou `null` | **400** `VALIDACAO`, com `natureza` em `campos` | `DiariaRequisicao.natureza` é `@NotNull` |
| Natureza fora do enum (`"VOLUNTARIA"`, `"paga"`) | **400** `VALIDACAO`, com `natureza` em `campos` e a mensagem do campo "Valor em formato inválido." | `ApiExceptionHandler.handleHttpMessageNotReadable`: o Jackson recusa o valor e o caminho do campo vai para `campos`. Enum diferencia maiúsculas (padrão do Spring Boot) |
| `quantidadeDias` ausente ou com mais de 2 casas | **400** `VALIDACAO`, com `quantidadeDias` em `campos` | `@NotNull @Digits(integer = 4, fraction = 2)` |
| `quantidadeDias` ou `valorUnitario` zero ou negativo | **422** `REGRA_NEGOCIO` | Proposta, na mesma linha da área na Q00a e do valor da referência na L06. Ver ponto 6 |
| Diária paga sem `valorUnitario` | **422** `REGRA_NEGOCIO`, com `valorUnitario` em `campos` | Decisão de 2026-10-26 (decisão 8a), registrada na RN15 (regras v1.5). A regra é condicional à natureza e fica no serviço, como a RN06 na Q00b |
| Diária familiar com `valorUnitario` informado | O valor informado é ignorado; grava-se o da referência vigente, e a resposta mostra o valor aplicado | Decisão de 2026-10-26 (decisão 8b), registrada na RN13 (regras v1.5). Mesmo tratamento que a L00 dá à data de coleta enviada |
| Referência aplicada à familiar | A vigente na **data do fato** da diária, com as duas pontas da vigência inclusivas. Nunca a vigente hoje nem na data de coleta | RN13; intervalo fechado `'[]'` do `daterange` da L06 |
| Familiar sem referência vigente | **422** `REGRA_NEGOCIO`, mensagem orientando o cadastro. A consulta da L06 devolve 404 quando não acha; a L07 traduz para 422 | RN14; `agrogestao-l06-referencia-diaria.md` |
| Valor total da diária | `quantidade_dias × valor_unitario`, arredondado em 2 casas com `RoundingMode.HALF_UP`. A tabela `diaria` não tem coluna de total: o valor sai calculado em `DiariaResposta.valorTotal` e gravado na despesa gerada | Decisão de 2026-10-26 (decisão 7), registrada na RN16 (regras v1.5); DER |
| Despesa gerada pela paga | Categoria "Diária" (`sistema = TRUE`), `diaria_id` = id da diária, `produtor_id` do produtor do plantio, `valor_total` = total da diária, `data_fato` e `data_coleta` iguais às da diária, `estornada = FALSE`; um rateio, ao plantio da diária, com percentual 1,000000 e valor igual ao total | RN16, critério de pronto da L07; data do fato pela decisão de 2026-10-26, registrada na RN16 (regras v1.5) |
| Transação | Diária, despesa e rateio são gravados na mesma transação: falha em qualquer um desfaz os três | Critério de pronto da L07 |
| Despesa gerada pela API de despesa | `PUT` e `DELETE /api/v1/despesas/{id}` de despesa com `diaria_id` → **422** `REGRA_NEGOCIO`; `POST /api/v1/despesas` na categoria "Diária" → **422** | RN16 e RF28; critérios de pronto da L04b |
| RN04, RN35 e RN36 na diária | Validadas pela L07 com os validadores comuns da L00. Os casos são CT007/CT008 (Q00a) e CT055/CT056/CT057 (Q00f); não se repetem aqui | Issue L00 |
| Datas nos casos de integração | 2024. A migration de dados iniciais grava referências de 70,00 (2025) e 80,00 (de 2026-01-01 em diante, sem fim) em qualquer banco, inclusive no de teste | Mesma escolha da `ValorReferenciaDiariaIT`: em 2024 nenhuma referência vem pré-carregada |
| "Hoje" nos unitários | `LocalDate.now(clock)` com `Clock.fixed(2026-10-11T12:00:00-03:00, America/Fortaleza)` | Mesma convenção da Q00a |
| Mensagens | As citadas são propostas. O teste verifica `status`, `erro` e `campos`; a mensagem exata é confirmada na L07 | Mesma linha da Q00a e da Q00b |

---

## Massa de dados base

Sintética, conforme `RNF10`. Criada por fixture em cada teste e removida no final.

| Item | Valor |
|---|---|
| Usuário | operador de teste, perfil `LANCAMENTO` |
| Produtor | "Produtor Teste Diária", CPF `99900001230` |
| Propriedade | "Sítio Teste", área total 10,000 ha, do produtor acima |
| Cultura | Milho, do catálogo da migration |
| Plantio | P1: 1,000 ha, aberto. `data_inicio` 2024-02-01 nos casos de integração; 2026-02-01 nos unitários |
| Categorias | "Diária" (`sistema = TRUE`) e "Adubo e fertilizantes" (`sistema = FALSE`), da migration de dados iniciais |
| Referência nos casos de integração | VR24: 80,00, vigência de 2024-01-01 a 2024-12-31, criada pela fixture |
| Referências nos unitários | VR1: 80,25, de 2026-01-01 a 2026-06-30. VR2: 90,50, de 2026-07-01 sem fim |
| "Hoje" nos unitários | 2026-10-11 |

Nos unitários, o mock do repositório de referência responde pela vigência: devolve VR1 para datas de 2026-01-01 a 2026-06-30, VR2 a partir de 2026-07-01 e vazio antes de 2026-01-01. O repositório de diária captura a entidade recebida em `save`.

---

## RN12 — Natureza obrigatória

### CT021 — Diária paga ou familiar é registrada com a natureza

| Campo | Conteúdo |
|---|---|
| Regra | RN12 |
| Requisito | RF24 |
| Tipo | Integração |
| Prioridade | P3 |
| Natureza | Positivo |

**Pré-condição**
P1 e VR24 da massa base. Banco efêmero (Testcontainers).

**Passos**
1. `POST /api/v1/plantios/{P1}/diarias` com:
   ```json
   {"natureza": "PAGA", "quantidadeDias": 1.00, "valorUnitario": 90.00, "dataFato": "2024-03-10"}
   ```
2. `POST /api/v1/plantios/{P1}/diarias` com:
   ```json
   {"natureza": "FAMILIAR", "quantidadeDias": 1.00, "dataFato": "2024-03-11"}
   ```
3. Ler as duas linhas de `diaria` por SQL.

**Resultado esperado**
- Passo 1: **201**, `Location` terminando em `/api/v1/diarias/{id}`, `natureza` = `"PAGA"`, `valorUnitario` = 90.00, `valorTotal` = 90.00, `valorReferenciaDiariaId` = `null`.
- Passo 2: **201**, `natureza` = `"FAMILIAR"`, `valorUnitario` = 80.00, `valorTotal` = 80.00, `valorReferenciaDiariaId` = id de VR24.
- Passo 3: `natureza` gravada como `'PAGA'` e `'FAMILIAR'`.

### CT022 — Diária sem natureza ou com natureza inválida é rejeitada

| Campo | Conteúdo |
|---|---|
| Regra | RN12 |
| Requisito | RF24 |
| Tipo | Integração |
| Prioridade | P3 |
| Natureza | Negativo |

**Pré-condição**
P1 e VR24 da massa base. Nenhuma diária nem despesa do produtor. Banco efêmero.

**Passos**
1. `POST /api/v1/plantios/{P1}/diarias` sem `natureza`:
   ```json
   {"quantidadeDias": 1.00, "valorUnitario": 90.00, "dataFato": "2024-03-10"}
   ```
2. Repetir com `"natureza": null`.
3. Repetir com `"natureza": "VOLUNTARIA"`.
4. Repetir com `"natureza": "paga"`.
5. Contar as linhas de `diaria` e `despesa` do produtor.
6. Inserir por SQL direto, fora do serviço, uma diária com `natureza = 'OUTRA'`.
7. Inserir por SQL direto uma diária com `natureza = NULL`.
8. Inserir por SQL direto uma diária `'FAMILIAR'` com `valor_referencia_diaria_id = NULL`.

**Resultado esperado**
- Passos 1 a 4: **400**, `erro: "VALIDACAO"`, `campos` com um elemento de `campo` = `"natureza"`. Nos passos 3 e 4, a mensagem do campo é "Valor em formato inválido."
- Passo 5: zero diárias e zero despesas.
- Passo 6: violação da restrição `ck_diaria_natureza`.
- Passo 7: violação de `NOT NULL` na coluna `natureza`.
- Passo 8: violação da restrição `ck_diaria_ref`.

O passo 3 pega a implementação que transforma valor desconhecido em `PAGA` por padrão; o passo 5 garante que nenhuma despesa nasceu de uma diária recusada.

---

## RN13 — Valoração da diária familiar

### CT023 — Familiar recebe a referência vigente na data do fato

| Campo | Conteúdo |
|---|---|
| Regra | RN13 |
| Requisito | RF26 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
P1, VR1 e VR2 da massa base; hoje = 2026-10-11, dentro da vigência de VR2. Repositórios mockados.

**Passos**
1. Registrar diária familiar de 1,00 dia com data do fato 2026-01-01 (primeiro dia de VR1).
2. Registrar diária familiar de 1,00 dia com data do fato 2026-06-30 (último dia de VR1).
3. Registrar diária familiar de 1,25 dia com data do fato 2026-07-01 (primeiro dia de VR2).

**Resultado esperado**

| Passo | `valor_unitario` gravado | `valor_referencia_diaria_id` | `valorTotal` na resposta |
|---|---|---|---|
| 1 | 80,25 | VR1 | 80,25 |
| 2 | **80,25** | VR1 | 80,25 |
| 3 | 90,50 | VR2 | **113,13** |

- A consulta da referência recebe a data do fato de cada diária: 2026-01-01, 2026-06-30 e 2026-07-01. Nunca recebe 2026-10-11.
- Nenhuma despesa é criada (`despesaRepository.save` não é chamado).

Valores que indicam defeito:
- 90,50 no passo 2 — usou a referência vigente hoje (VR2), não a da data do fato
- 113,12 no passo 3 — arredondou com `HALF_EVEN` ou truncou 113,125

**Borda a cobrir no mesmo teste:** diária familiar de 1,00 dia em 2026-03-10 com `valorUnitario` = 50,00 no corpo grava 80,25 e VR1; a resposta traz `valorUnitario` 80,25. O valor informado é ignorado (decisão 8b).

---

## RN14 — Ausência de referência vigente

### CT024 — Familiar sem referência vigente é rejeitada

| Campo | Conteúdo |
|---|---|
| Regra | RN14 |
| Requisito | RF25 |
| Tipo | Unitário |
| Prioridade | P2 |
| Natureza | Negativo |

**Pré-condição**
P1 da massa base. A consulta de referência não encontra vigente: o repositório mockado devolve `Optional.empty()` para qualquer data. Se a L07 usar `ValorReferenciaDiariaService.vigente`, o mock lança `RecursoNaoEncontradoException`, como a L06 faz.

**Passos**
1. Registrar diária familiar de 1,00 dia com data do fato 2026-03-15.
2. Registrar diária familiar de 1,00 dia com data do fato 2025-12-31 (um dia antes de VR1, com o mock de vigência da massa base).

**Resultado esperado**
- Passos 1 e 2: `RegraNegocioException` com a mensagem proposta "Não há valor de referência da diária vigente em 15/03/2026. Cadastre o valor de referência para essa data antes de lançar a diária familiar." — com a data do fato da diária. Na API: **422**, `erro: "REGRA_NEGOCIO"`, não 404.
- `diariaRepository.save` e `despesaRepository.save` não são chamados: nem diária, nem despesa.

**Borda a cobrir no mesmo teste:** diária **paga** de 2,00 × 90,00 em 2026-03-15, com a mesma ausência de referência, é aceita: `valor_unitario` 90,00 e `valor_referencia_diaria_id` nulo. A falta de referência só bloqueia a familiar.

Repassar o 404 da consulta da L06 seria o defeito mais provável: o operador receberia "não encontrado" num lançamento cujo plantio existe.

---

## RN15 — Valor da diária paga

### CT025 — Paga usa o valor informado, não a referência

| Campo | Conteúdo |
|---|---|
| Regra | RN15 |
| Requisito | RF24 |
| Tipo | Unitário |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
P1, VR1 e VR2 da massa base. Em 2026-03-10 a referência vigente é VR1, de 80,25. Repositórios mockados.

**Passos**
1. Registrar diária paga de 2,00 dias a R$ 95,00, data do fato 2026-03-10.
2. Registrar diária paga de 0,75 dia a R$ 90,50, data do fato 2026-03-11.

**Resultado esperado**
- Passo 1: `valor_unitario` gravado = **95,00**; `valor_referencia_diaria_id` nulo; `valorTotal` = **190,00**. Falha: 80,25 e 160,50 (usou a referência).
- Passo 2: `valor_unitario` = 90,50; `valorTotal` = **67,88** (67,875 arredondado com `HALF_UP`; 67,87 indica truncamento).

**Borda a cobrir no mesmo teste:** diária paga de 1,00 dia em 2026-03-10 **sem** `valorUnitario` → `RegraNegocioException` com a mensagem proposta "Informe o valor da diária paga." e `valorUnitario` em `campos`; na API, **422** `REGRA_NEGOCIO`. `diariaRepository.save` e `despesaRepository.save` não são chamados (decisão 8a).

---

## RN16 — Geração de despesa

Os três casos rodam contra o banco (Testcontainers), pela API. CT026 e CT027 são os positivos; CT028 é o negativo.

### CT026 — Paga gera despesa na mesma transação

| Campo | Conteúdo |
|---|---|
| Regra | RN16 |
| Requisito | RF27 |
| Tipo | Integração |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
P1 e VR24 da massa base. Nenhuma diária nem despesa do produtor.

**Passos**
1. `POST /api/v1/plantios/{P1}/diarias` com:
   ```json
   {"natureza": "PAGA", "quantidadeDias": 2.50, "valorUnitario": 80.25, "dataFato": "2024-03-10"}
   ```
2. Ler por SQL as despesas com `diaria_id` = id da diária do passo 1, e os rateios dessas despesas.
3. Inserir por SQL direto uma segunda despesa com o mesmo `diaria_id`.
4. Com o repositório de rateio configurado como spy (`@MockitoSpyBean`) para lançar `DataIntegrityViolationException` no `save`, repetir o passo 1 com data do fato 2024-03-11.
5. Contar as linhas de `diaria`, `despesa` e `rateio_despesa` do produtor.

**Resultado esperado**
- Passo 1: **201**, `valorTotal` = **200.63** (2,50 × 80,25 = 200,625, arredondado com `HALF_UP`).
- Passo 2: exatamente **uma** despesa, com:
  - categoria "Diária" (`sistema = TRUE`)
  - `produtor_id` = produtor de P1
  - `valor_total` = 200,63
  - `data_fato` = 2024-03-10 e `data_coleta` = `data_coleta` da diária
  - `estornada = FALSE`

  E exatamente **um** rateio: P1, percentual 1,000000, `valor_rateado` 200,63.
- Passo 3: violação da restrição de unicidade de `despesa.diaria_id`, que a migration declara sem nome (o PostgreSQL a chama de `despesa_diaria_id_key`). Uma diária gera no máximo uma despesa.
- Passo 4: **422**, `erro: "REGRA_NEGOCIO"`.
- Passo 5: uma diária, uma despesa e um rateio — só os do passo 1. A diária do passo 4 não ficou gravada sem a sua despesa.

Valores que indicam defeito no passo 2: 200,62 (`HALF_EVEN` ou truncamento); duas despesas; despesa em categoria de operador; rateio com percentual diferente de 1,000000. O passo 4 falha no último dos três `save`, para provar que a diária e a despesa voltam atrás juntas.

### CT027 — Alterar ou excluir a diária paga leva a despesa junto

| Campo | Conteúdo |
|---|---|
| Regra | RN16 |
| Requisito | RF27 |
| Tipo | Integração |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
P1 da massa base, com:
- Diária paga DP de 2,00 dias × R$ 80,00 = R$ 160,00, data do fato 2024-03-10, lançada pela API; despesa gerada DG de 160,00 com rateio 1,000000 / 160,00 a P1.
- Despesa manual D2 "Adubo e fertilizantes" de R$ 30,00 em 2024-03-10, só em P1.

Guardar o id e o `criado_em` de DG.

**Passos**
1. `PUT /api/v1/diarias/{DP}` com:
   ```json
   {"natureza": "PAGA", "quantidadeDias": 3.00, "valorUnitario": 85.50, "dataFato": "2024-03-12"}
   ```
2. Ler as despesas com `diaria_id` = DP e seus rateios.
3. `DELETE /api/v1/diarias/{DP}`.
4. Ler `diaria`, `despesa` e `rateio_despesa` do produtor.

**Resultado esperado**
- Passo 1: **200**, `valorTotal` = **256.50**, `dataFato` = `"2024-03-12"`.
- Passo 2: uma única despesa, com o **mesmo id** e o mesmo `criado_em` de DG, `valor_total` = 256,50 e `data_fato` = 2024-03-12. Um único rateio: P1, 1,000000, 256,50.
- Passo 3: **204**.
- Passo 4: DP e DG não existem mais, nem o rateio de DG. D2 e seu rateio (P1, 30,00) continuam intactos.

O passo 2 pega a implementação que cria uma despesa nova a cada alteração (duas despesas para a mesma diária, ou id diferente) e a que altera a diária sem tocar na despesa (DG continuaria com 160,00).

### CT028 — Familiar não gera despesa e a despesa gerada não é editável

| Campo | Conteúdo |
|---|---|
| Regra | RN16 |
| Requisito | RF27 |
| Tipo | Integração |
| Prioridade | **P1** |
| Natureza | Negativo |

**Pré-condição**
P1 e VR24 da massa base. Diária paga DP de 1,00 dia × R$ 90,00, data do fato 2024-03-10, lançada pela API, com a despesa gerada DG de 90,00. Guardar `valor_total`, `atualizado_em` e o rateio de DG.

**Passos**
1. `POST /api/v1/plantios/{P1}/diarias` com:
   ```json
   {"natureza": "FAMILIAR", "quantidadeDias": 1.00, "dataFato": "2024-03-11"}
   ```
2. Contar as despesas do produtor e as com `diaria_id` = id da familiar.
3. `PUT /api/v1/despesas/{DG}` com o mesmo corpo de DG e `valorTotal` = 999.00.
4. `DELETE /api/v1/despesas/{DG}`.
5. `POST /api/v1/despesas` com `categoriaDespesaId` = categoria "Diária", R$ 90,00, data do fato 2024-03-10, `plantioIds` = `[P1]`.
6. Ler DG, seu rateio e DP.

**Resultado esperado**
- Passo 1: **201**, `valorTotal` = 80.00.
- Passo 2: uma despesa no produtor (DG); nenhuma com o `diaria_id` da familiar.
- Passos 3 e 4: **422**, `erro: "REGRA_NEGOCIO"`. Mensagem proposta: "Esta despesa foi gerada por uma diária paga. Altere ou exclua a diária."
- Passo 5: **422**, `erro: "REGRA_NEGOCIO"`. Mensagem proposta: "A categoria Diária é exclusiva das despesas geradas por diária paga."
- Passo 6: DG com `valor_total` 90,00 e o mesmo `atualizado_em`; rateio inalterado; DP inalterada.

O passo 5 fecha a outra metade da dupla contagem (RF28): sem ele, o operador lança à mão a mesma diária como despesa, e o custo dobra pelo outro lado.

---

## RN17 — Vigências não se sobrepõem

Os dois casos já estão automatizados pela L06. A especificação abaixo descreve o que os testes existentes verificam, com os mesmos valores. Datas de 2024, pelo motivo das convenções.

### CT029 — Vigências adjacentes são aceitas

| Campo | Conteúdo |
|---|---|
| Regra | RN17 |
| Requisito | RF25 |
| Tipo | Integração |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
Banco efêmero com as referências da migration (2025 e 2026 em diante); nenhuma referência em 2024. Usuário autenticado.

**Passos**
1. `POST /api/v1/valores-referencia-diaria` com `{"valor": 80.25, "vigenciaInicio": "2024-01-01", "vigenciaFim": "2024-06-30"}`.
2. `POST` com `{"valor": 90.50, "vigenciaInicio": "2024-07-01", "vigenciaFim": "2024-12-31"}`.
3. `GET /api/v1/valores-referencia-diaria/vigente?dataFato=` 2024-01-01, 2024-06-30 e 2024-07-01.
4. `GET /api/v1/valores-referencia-diaria/{primeiro}` e listagem com `tamanho=1&pagina=1`.
5. Em outro teste, com as referências removidas dentro da transação revertida: criar 90,00 de 2024-07-01 sem fim; consultar a vigente em 2099-01-01; tentar 95,00 de 2024-08-01 a 2024-08-31; fechar a aberta em 2024-12-31 por `PUT`; criar 0,01 de 2024-06-30 a 2024-06-30.

**Resultado esperado**
- Passos 1 e 2: **201**, `Location` com `/api/v1/valores-referencia-diaria/`.
- Passo 3: 2024-01-01 e 2024-06-30 devolvem a primeira, com `valor` 80.25; 2024-07-01 devolve a segunda.
- Passo 4: `vigenciaFim` = `"2024-06-30"`; a página 1 com tamanho 1 traz um item. `criado_em`, `atualizado_em` e `criado_por` preenchidos.
- Passo 5: a aberta é vigente em 2099-01-01; agosto contido nela → **422**; o `PUT` para fechar → **200**; o período de um dia, 2024-06-30, adjacente à fechada, → **201** e é a vigente em 2024-06-30.

**Automação:** `ValorReferenciaDiariaIT.deveAceitarPeriodosAdjacentesQuandoNaoSobrepostosCt029` (passos 1 a 4) e `ValorReferenciaDiariaIT.deveAceitarDiaUnicoEVigenciaAbertaQuandoLivre` (passo 5); `ValorReferenciaDiariaServiceTest.deveCriarQuandoPeriodoLivre` e `deveIgnorarProprioIdQuandoAlterarSemConflito`.

### CT030 — Vigências sobrepostas são rejeitadas

| Campo | Conteúdo |
|---|---|
| Regra | RN17 |
| Requisito | RF25 |
| Tipo | Integração |
| Prioridade | P2 |
| Natureza | Negativo |

**Pré-condição**
Referência R de 80,00 com vigência de 2024-03-01 a 2024-06-30, criada pela API.

**Passos**
1. `POST /api/v1/valores-referencia-diaria` com valor 90,00 e cada uma das vigências:

   | # | Início | Fim | Situação |
   |---|---|---|---|
   | a | 2024-02-01 | 2024-03-01 | Toca a borda inicial de R |
   | b | 2024-06-30 | 2024-07-31 | Toca a borda final de R |
   | c | 2024-04-01 | 2024-05-31 | Contida em R |
   | d | 2024-01-01 | 2024-12-31 | Contém R |
   | e | 2024-03-01 | 2024-06-30 | Igual a R |
   | f | 2024-05-01 | sem fim | Aberta, começando dentro de R |

2. Criar S de 95,00 de 2024-07-01 a 2024-12-31 (adjacente, aceita) e alterar S por `PUT` para começar em 2024-06-30.
3. `GET /api/v1/valores-referencia-diaria/{S}`.
4. Inserir por SQL direto, fora do serviço, uma cópia do período de uma referência existente.

**Resultado esperado**
- Passo 1, nas seis linhas: **422**, `erro: "REGRA_NEGOCIO"`, `mensagem` contendo "sobreposição" ("Já existe valor de referência da diária com vigência neste período. Informe um período sem sobreposição.").
- Passo 2: **422** no `PUT`.
- Passo 3: S continua com `vigenciaInicio` = `"2024-07-01"`.
- Passo 4: `SQLException` citando a restrição `ex_vrd_vigencia` — a proteção contra duas requisições concorrentes que passem juntas pela validação do serviço.

**Automação:** `ValorReferenciaDiariaIT.deveRejeitarSobreposicoesQuandoCriarOuAlterarCt030` (passos 1 a 3) e `ValorReferenciaDiariaIT.deveProtegerContraSobreposicaoQuandoSqlIgnorarService` (passo 4); `ValorReferenciaDiariaServiceTest.deveRejeitarCriacaoEAlteracaoQuandoPeriodoSobreposto` e `deveTraduzirConflitoQuandoConstraintBloquearConcorrencia` (a violação de `ex_vrd_vigencia` vira a mesma mensagem de sobreposição).

---

## RN18 — Atualização de referência não retroage

### CT031 — Alterar a referência não muda diária já lançada

| Campo | Conteúdo |
|---|---|
| Regra | RN18 |
| Requisito | RF26 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
P1 da massa base. Referência VR de 80,00, vigente de 2026-01-01 sem fim, devolvida pelo mock como objeto em memória. O repositório de diária devolve, em `findById`, a entidade capturada no `save`.

**Passos**
1. Registrar diária familiar F1 de 1,50 dia em 2026-03-10.
2. Alterar o valor de VR para 95,00, no mesmo objeto que o mock devolve (simula a alteração feita pela L06).
3. Consultar F1 pelo serviço de diária.
4. Registrar diária familiar F2 de 1,50 dia em 2026-03-20.

**Resultado esperado**
- Passo 1: F1 gravada com `valor_unitario` 80,00 e `valor_referencia_diaria_id` = VR; `valorTotal` = 120,00.
- Passo 3: F1 com `valorUnitario` **80,00** e `valorTotal` **120,00**. Falha: 95,00 e 142,50 — a diária leu o valor pela referência em vez da coluna copiada.
- Passo 4: F2 com `valorUnitario` 95,00 e `valorTotal` 142,50.
- `diariaRepository.save` é chamado só nos passos 1 e 4: alterar a referência não regrava diária.

O passo 4 garante que o teste não passa por acidente com um serviço que nunca lê a referência.

**Automação complementar:** `ValorReferenciaDiariaIT.devePreservarDiariaQuandoAlterarValorEVigenciaRn18` já verifica no banco que alterar valor e vigência da referência deixa `valor_unitario`, `valor_referencia_diaria_id` e `atualizado_em` da diária iguais.

---

## Pontos para revisar

1. **Não existe `diaria.valor_total`.** A tabela grava só `quantidade_dias` e `valor_unitario`; o total aparece em `DiariaResposta.valorTotal` e na despesa gerada. Para a paga, o total gravado na despesa é a fonte de verdade da apuração. Para a familiar, a apuração recalcula `dias × valor` a cada consulta — o resultado só não muda porque as duas parcelas estão gravadas e o arredondamento é fixo (decisão 7). Se o arredondamento mudar um dia, as familiares antigas mudam junto. Avaliar uma coluna `valor_total` em migration nova.
2. **Troca de natureza no `PUT`.** Resolvido em 29/10/2026 pelo solicitante na L07: rejeitar com 422 e orientar excluir/registrar novamente, em ambas as direções. Registrado na RN12 e automatizado em `DiariaIT`.
3. **Diária paga em período já emitido.** A L07 respeita a RN11: se a despesa gerada estiver em período emitido para o produtor, excluir ou deslocar a data da diária retorna 422, preservando diária, despesa, rateio e snapshot. Fora do período emitido a exclusão é permitida. Alteração de quantidade/valor continua permitida, sem tocar no snapshot (CT053); estorno permanece na A03b. `DiariaIT.deveRecusarExclusaoQuandoDespesaDaDiariaEstiverEmPeriodoEmitido` cobre também a sequência PUT da data → DELETE.
4. **CT028 e L04b.** A L07 antecipa o bloqueio RN16 no PUT/DELETE de despesa gerada, para o CT028 passar completo. O POST manual em categoria de sistema já era bloqueado pela L04a. Listagens e exclusão de despesa manual continuam na L04b; DELETE manual permanece 501.
5. **Descrição da despesa gerada.** `despesa.descricao` é obrigatória e nenhuma regra diz qual texto a despesa gerada recebe. Os casos não verificam o texto; sugestão: "Diária paga de dd/mm/aaaa".
6. **Zero e negativo em dias e valor.** A L07 valida no serviço, com 422 e mensagem clara, como na área e na referência. `DiariaIT` cobre dias e valor pago não positivos, total pago que arredonda a zero e total que excede NUMERIC(12,2). O valor informado na familiar é ignorado; o aplicado é o da referência.
7. **CT029 e CT030 frente à RN17.** Os testes existentes cobrem a regra com os mesmos valores deste documento. Duas observações, sem mudar os testes: o CT030 não conta as linhas depois das seis rejeições (o 422 vem do serviço antes do `save`, mas a contagem tornaria isso explícito); e a referência aberta já existente contra um período novo anterior a ela só é coberta de passagem, no teste de dia único.
8. **Referências da migration em qualquer banco.** A migration de dados iniciais grava referências de 2025 e de 2026 em diante em qualquer ambiente, inclusive no de teste. Por isso os casos de integração usam 2024. Vale tirar esses exemplos das migrations (por migration nova, já que a aplicada não se edita) e deixá-los na massa de desenvolvimento (`db/dev`), como a F12 fez com os produtores, para os testes poderem usar datas atuais.
9. **Mensagens de erro.** A L07 confirma as mensagens de referência ausente, valor pago ausente e proteção da despesa. Os testes verificam status, campos e orientação ao operador.

## Automação da L07

- `DiariaIT`: CT021/022/026/027/028, formatos e precisão, auditoria/coleta, paginação por plantio, alteração/exclusão e rollback de criação/alteração/exclusão. Exercita as constraints de natureza, referência obrigatória e unicidade de despesa por diária por SQL direto. Inclui concorrência com encerramento (RN03/RN04).
- `DiariaServiceTest`: CT023/024/025/031, valores de referência nas bordas e HALF_UP com os valores desta especificação; a consulta lê o valor copiado, nunca recalcula pela referência atual.
- Na familiar, alterar somente os dias preserva valor/referência; corrigir a data reaplica a referência da nova data. O valor informado no corpo continua ignorado. Registrado na RN13.
