# AgroGestão — Casos de Teste: Apuração (CT032–CT041)

Versão 1.0

Especificação dos casos de apuração de resultado do plantio no padrão da QA 3.3. Destrava A01 e Q04.

**Documentos relacionados**

- `agrogestao-fase-1-regras-negocio.md` — RN10, RN11 e RN19 a RN25 (RN19 e RN24 completadas na versão 1.3)
- `agrogestao-fase-1-requisitos.md` — RF12, RF16 e RF34 a RF39
- `agrogestao-fase-1-glossario.md` — seção 2 (safra, ciclo) e seção 4 (custo de desembolso, período de apuração, resultado)
- `agrogestao-fase-3-qa.md` — 3.2 (matriz e casos críticos CT033 e CT034), 3.3 (padrão) e 3.4 (nomes)
- `agrogestao-fase-2-der.md` — tabelas `plantio`, `producao`, `despesa`, `rateio_despesa`, `diaria`, `venda` e `item_venda`; seção 5 (RN20 e RN24 ficam no serviço)
- `agrogestao-fase-2-padroes-tecnicos.md` — 2.4, contrato `GET /api/v1/plantios/{id}/resultado?mes=AAAA-MM` e formato de erro
- `casos-de-teste-despesa.md` — convenção de rateio (residual ao menor id) e ponto 5, fechado aqui

---

## Visão geral

| Caso | Título | Regra | Requisito | Tipo | Prior. | Natureza | Issue |
|---|---|---|---|---|---|---|---|
| CT032 | Desembolso soma o valor rateado das despesas do período | RN19 | RF34 | Unitário | **P1** | Positivo | A01 |
| CT033 | Diária paga não é somada duas vezes | RN20 | RF39 | Unitário | **P1** | Positivo | A01 |
| CT034 | Diária familiar fora do custo de desembolso | RN20 | RF39 | Unitário | **P1** | Negativo | A01 |
| CT035 | Custo com mão de obra familiar ao lado do desembolso | RN21 | RF37 | Unitário | P2 | Positivo | A01 |
| CT036 | Receita só dos itens que referenciam o plantio | RN22 | RF35 | Unitário | **P1** | Positivo | A01 |
| CT037 | Resultado é receita menos desembolso, inclusive negativo | RN23 | RF36 | Unitário | **P1** | Positivo | A01 |
| CT038 | Período derivado do ciclo efetivo do plantio | RN24 | RF38 | Unitário | **P1** | Positivo | A01 |
| CT039 | Lançamento fora do período derivado fica de fora | RN24 | RF38 | Unitário | **P1** | Negativo | A01 |
| CT040 | Subsistência: produção sem venda, resultado negativo exibido | RN25 | RF36 | E2E | P2 | Negativo | Q04 |
| CT041 | Venda parcial: produzido e vendido como números distintos | RN25 | RF36 | E2E | P2 | Positivo | Q04 |

**Sobre a regra do par.** RN20 tem par: CT033 é o positivo (a diária paga entra no desembolso uma vez, pela despesa que gerou) e CT034 o negativo (a familiar é recusada no desembolso). RN24 tem par: CT038 positivo (cada ciclo deriva o seu período) e CT039 negativo (o que cai fora do período derivado é recusado). RN25 tem par: CT041 positivo (venda parcial) e CT040 negativo — não há entrada a rejeitar, a violação que o caso recusa é a omissão ou o mascaramento do resultado negativo, que foi a alternativa descartada na regra. RN19, RN21, RN22 e RN23 descrevem cálculo, sem violação que o operador possa cometer: o "negativo" de cada uma é o número errado, e os resultados esperados abaixo já o excluem com valor concreto (por exemplo, CT032 recusa 200,00, 133,33 e 633,34; CT035 recusa resultado −90,00; CT036 recusa 645,00; CT037 recusa 233,17).

---

## Convenções

| Item | Decisão | Por quê |
|---|---|---|
| Período do ciclo longo ("safra") | Da `data_inicio` à `data_encerramento` do plantio; se aberto, até hoje. O parâmetro `mes` é ignorado | Decisão de 2026-10-26 na A01 (#64), registrada na RN24 (regras v1.3) |
| Período do ciclo curto | Mês-calendário do parâmetro `mes` (dia 1 ao último dia). Sem `mes`, o mês corrente | Decisão de 2026-10-26 na A01 (#64), RN24 (regras v1.3). O contrato da F13 já traz `mes` como `YearMonth` opcional |
| Ciclo que decide o período | `plantio.tipo_ciclo` (ciclo efetivo, que pode sobrepor o da cultura), nunca `cultura.tipo_ciclo_padrao` | RN24 fala em "ciclo efetivo do plantio"; RF12 |
| Bordas do período | Inclusivas nas duas pontas | Mesma linha da Q00a e do intervalo fechado `'[]'` do `daterange` da L06 |
| "Hoje" | `LocalDate.now(clock)` com `Clock` injetável no fuso `America/Fortaleza` | Mesma convenção da Q00a; servidor em UTC não pode adiantar o dia nem o mês |
| Base do desembolso | `rateio_despesa.valor_rateado` gravado para o plantio, nunca `despesa.valor_total` nem `percentual × valor_total` | RN19 fala em "valores rateados"; RN10 e DER (`valor_rateado` é a fonte de verdade) |
| Data que coloca a despesa no período | `despesa.data_fato` | RN19 |
| Despesa estornada | `estornada = TRUE` não entra no desembolso de nenhuma apuração feita depois do estorno. Emissão já gravada não muda | Decisão de 2026-10-26 na A01 (#64), registrada na RN19 (regras v1.3). Fecha o ponto 5 da Q00b |
| Diária | Nunca entra no desembolso. A paga participa pela despesa gerada (categoria "Diária", `diaria_id` preenchido); a familiar só entra em `valorMaoObraFamiliar` | RN20, RN21, RF39 |
| Valor de cada diária familiar | `quantidade_dias × valor_unitario` gravado, arredondado em 2 casas com `HALF_UP` por diária; `valorMaoObraFamiliar` é a soma desses valores | Decisão 7 da Q00c (2026-10-26). Os casos daqui usam produtos exatos, para não depender do arredondamento |
| Receita | Soma de `item_venda.valor_total` gravado, só dos itens com `plantio_id` deste plantio cuja venda tem `data_fato` no período. Nunca `venda.valor_total` | RN22; o item não tem data própria, a data do fato é a da venda |
| Quantidades | `quantidadeProduzida` = soma de `producao.quantidade` com data do fato no período; `quantidadeVendida` = soma de `item_venda.quantidade` deste plantio com venda no período. Três casas | `ResultadoPlantioResposta` tem um só período para a resposta inteira; RN25 |
| Resultado | `receita − custoDesembolso`, com sinal. Negativo é devolvido e exibido como tal | RN23, RN25 |
| Comparação de valores | `BigDecimal.compareTo`, duas casas para dinheiro | RNF08; evita falso negativo de escala (`80.0` × `80.00`) |
| Status de erro | A apuração não tem regra de negócio a rejeitar: não existe 422 neste módulo. Plantio inexistente → **404** `NAO_ENCONTRADO`; `mes` fora do formato (`2026-13`) → **400** `VALIDACAO` com `mes` em `campos` | Padrões 2.4; `ApiExceptionHandler.handleTypeMismatch` |
| Termo na tela | "Resultado". "Lucro" só no relatório impresso | Glossário, seção 4; RF47 |

Os casos unitários chamam `ApuracaoResultadoService.apurar(plantioId, mes)` — nome da QA 3.4, com a assinatura do controller da F13 (`mes` é `YearMonth`, opcional) — e verificam os campos de `ResultadoPlantioResposta`. Os repositórios mockados devolvem a lista **completa** de registros do plantio, inclusive os de fora do período e os estornados, para que o recorte por período e por estorno seja exercitado no serviço. Ver ponto 2 de "Pontos para revisar".

---

## Massa de dados base

Sintética, conforme `RNF10`. Testes unitários usam objetos em memória; os E2E criam tudo pela API numa fixture e removem no final.

| Item | Valor |
|---|---|
| Usuário | operador de teste, perfil `LANCAMENTO` |
| Produtor (unitários) | "Produtor Teste Apuração", CPF `99900001079` |
| Produtores (E2E) | "Produtor Teste Subsistência", CPF `99900001826` (CT040); "Produtor Teste Venda Parcial", CPF `99900001907` (CT041) |
| Propriedade | "Sítio Teste", área total 10,000 ha, do produtor do caso |
| Cultura | Milho, do catálogo da migration: ciclo padrão `LONGO`, unidade `kg` |
| Plantios de ciclo curto | P1, P2 e P3: Milho com `tipo_ciclo = CURTO` sobreposto (RF12), 1,000 ha cada, `data_inicio` 2026-02-01, abertos. P1 tem o menor id |
| Plantio longo encerrado | PL: Milho `LONGO`, 2,000 ha, `data_inicio` 2026-02-01, `data_encerramento` 2026-07-31 |
| Plantio longo aberto | PA: Milho `LONGO`, 2,000 ha, `data_inicio` 2026-02-01, sem encerramento |
| Categorias | "Adubo e fertilizantes" e "Sementes e mudas" (`sistema = FALSE`); "Diária" (`sistema = TRUE`) para a despesa gerada por diária paga |
| Cliente | "Cliente Teste Apuração", canal `FEIRA` |
| Diária familiar | Já com `valor_unitario` 80,00 copiado da referência (RN13); a referência não participa da apuração |
| "Hoje" nos unitários | 2026-10-11, por `Clock.fixed(2026-10-11T12:00:00-03:00, America/Fortaleza)`, salvo indicação |

Os plantios de ciclo curto são de Milho de propósito: a cultura diz `LONGO` e o plantio diz `CURTO`. Implementação que lê o ciclo da cultura apura P1 como safra e falha nos casos abaixo.

Os E2E não fixam relógio: usam datas de 2026 já passadas e plantios longos encerrados, cujo período não depende do dia em que rodam.

---

## RN19 — Custo de desembolso

### CT032 — Desembolso soma o valor rateado das despesas do período

| Campo | Conteúdo |
|---|---|
| Regra | RN19 |
| Requisito | RF34 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Plantios P1, P2 e P3 (ciclo curto, 1,000 ha cada). Repositórios mockados devolvem estas despesas com seus rateios:

| Despesa | Data do fato | Valor total | Plantios | Rateio gravado de P1 | Estornada |
|---|---|---|---|---|---|
| D1 | 2026-03-10 | 100,00 | P1, P2, P3 | 0,333333 / **33,34** | não |
| D2 | 2026-03-01 | 57,89 | P1 | 1,000000 / 57,89 | não |
| D3 | 2026-03-31 | 42,11 | P1 | 1,000000 / 42,11 | não |
| D4 | 2026-02-28 | 1.000,00 | P1 | 1,000000 / 1.000,00 | não |
| D5 | 2026-04-01 | 2.000,00 | P1 | 1,000000 / 2.000,00 | não |
| D6 | 2026-03-15 | 500,00 | P1 | 1,000000 / 500,00 | **sim** |

D1 está rateada como a RN09 manda: P1 0,333333 / 33,34 (residual ao menor id), P2 0,333333 / 33,33, P3 0,333333 / 33,33.

**Passos**
1. Apurar P1 com `mes` = 2026-03.
2. Apurar P2 com `mes` = 2026-03.

**Resultado esperado**
- Passo 1: `periodoInicio` 2026-03-01, `periodoFim` 2026-03-31, `custoDesembolso` = **R$ 133,34** (33,34 + 57,89 + 42,11).
- Passo 2: `custoDesembolso` = **R$ 33,33** — só a parcela de D1 rateada a P2.

Valores que indicam defeito no passo 1:
- 200,00 — somou `valor_total` de D1 em vez do valor rateado
- 133,33 — recalculou D1 por `percentual × valor_total` em vez de ler os 33,34 gravados (RN10)
- 633,34 — somou D6, que está estornada
- 33,34 — tratou as bordas como exclusivas e perdeu D2 e D3
- 3.133,34 — não recortou o período e somou D4 e D5

O residual de D1 em P1 é o que distingue "leu o gravado" de "recalculou": percentual × total dá 33,33, o gravado é 33,34.

---

## RN20 — Vedação de dupla contagem

### CT033 — Diária paga não é somada duas vezes

| Campo | Conteúdo |
|---|---|
| Regra | RN20 |
| Requisito | RF39 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Plantio P1. Uma diária paga DP de 1,00 dia × R$ 80,00 = R$ 80,00, data do fato 2026-03-10, e a despesa que ela gerou: DG, categoria "Diária", `diaria_id` = DP, valor total 80,00, data do fato 2026-03-10, rateio 1,000000 / 80,00 a P1. Nenhuma outra despesa, diária ou venda.

O repositório de diária mockado **devolve DP** quando consultado pelo plantio, e o de despesa devolve DG.

**Passos**
1. Apurar P1 com `mes` = 2026-03.

**Resultado esperado**
- `custoDesembolso` = **R$ 80,00**, não R$ 160,00.
- `valorMaoObraFamiliar` = R$ 0,00; `custoComMaoObraFamiliar` = R$ 80,00.
- `receita` = R$ 0,00; `resultado` = −R$ 80,00.

Se o mock de diária devolvesse lista vazia, o teste passaria mesmo com o serviço somando diárias. Devolver DP é o que faz o caso pegar a dupla contagem.

### CT034 — Diária familiar fora do custo de desembolso

| Campo | Conteúdo |
|---|---|
| Regra | RN20 |
| Requisito | RF39 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Negativo |

**Pré-condição**
Plantio P1. Uma diária familiar DF de 1,00 dia × R$ 80,00 (valor copiado da referência) = R$ 80,00, data do fato 2026-03-10. Nenhuma despesa e nenhuma venda.

**Passos**
1. Apurar P1 com `mes` = 2026-03.

**Resultado esperado**
- `custoDesembolso` = **R$ 0,00**.
- `valorMaoObraFamiliar` = R$ 80,00; `custoComMaoObraFamiliar` = **R$ 80,00**.
- `resultado` = **R$ 0,00** (receita 0,00 − desembolso 0,00). Resultado −80,00 indica que a familiar entrou no custo usado pelo resultado.

---

## RN21 — Custo com mão de obra familiar

### CT035 — Custo com mão de obra familiar ao lado do desembolso

| Campo | Conteúdo |
|---|---|
| Regra | RN21 |
| Requisito | RF37 |
| Tipo | Unitário |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
Plantio P1, com:
- Despesa D1 "Adubo e fertilizantes", R$ 250,00, data do fato 2026-03-05, rateio 1,000000 / 250,00 a P1.
- Diária paga DP de 2,00 dias × R$ 90,00 = R$ 180,00 em 2026-03-12, com a despesa gerada DG (categoria "Diária", `diaria_id` = DP, 180,00, rateio 100% a P1).
- Diárias familiares F1 de 1,50 × 80,00 = R$ 120,00 em 2026-03-12 e F2 de 0,50 × 80,00 = R$ 40,00 em 2026-03-20.
- Diária familiar F4 de 1,00 × 80,00 em 2026-04-02 (fora do período).
- Venda em 2026-03-25 com um item de P1: 100,000 kg × R$ 5,00 = R$ 500,00.

**Passos**
1. Apurar P1 com `mes` = 2026-03.
2. Acrescentar ao mock a diária familiar F3 de 1,25 × 80,00 = R$ 100,00 em 2026-03-26.
3. Apurar P1 de novo com `mes` = 2026-03.

**Resultado esperado**

| Campo | Passo 1 | Passo 3 |
|---|---|---|
| `custoDesembolso` | **430,00** | **430,00** |
| `valorMaoObraFamiliar` | 160,00 | 260,00 |
| `custoComMaoObraFamiliar` | 590,00 | 690,00 |
| `receita` | 500,00 | 500,00 |
| `resultado` | **70,00** | **70,00** |

Os dois números de custo vêm juntos na mesma resposta. Valores que indicam defeito:
- `custoDesembolso` 590,00 — o custo com mão de obra substituiu o desembolso
- `custoDesembolso` 610,00 — DP somada por fora, além de DG
- `resultado` −90,00 — o resultado usou o custo com mão de obra (RN23 usa só o desembolso)
- `valorMaoObraFamiliar` 240,00 no passo 1 — F4, de abril, entrou no período de março

O passo 3 prova que a diária familiar nova mexe só no número complementar: o desembolso e o resultado não se movem.

---

## RN22 — Receita do plantio

### CT036 — Receita só dos itens que referenciam o plantio

| Campo | Conteúdo |
|---|---|
| Regra | RN22 |
| Requisito | RF35 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Plantios P1 e P2. Vendas ao "Cliente Teste Apuração":

| Venda | Data do fato | Itens | Total da venda |
|---|---|---|---|
| V1 | 2026-03-20 | A: P1, 100,000 kg × 4,50 = 450,00 · B: P2, 50,000 kg × 3,00 = 150,00 | 600,00 |
| V2 | 2026-04-01 | P1, 10,000 kg × 4,50 = 45,00 | 45,00 |
| V3 | 2026-03-01 | P1, 20,000 kg × 2,25 = 45,00 | 45,00 |

Nenhuma despesa.

**Passos**
1. Apurar P1 com `mes` = 2026-03.
2. Apurar P2 com `mes` = 2026-03.

**Resultado esperado**
- Passo 1: `receita` = **R$ 495,00** (450,00 do item A + 45,00 de V3); `quantidadeVendida` = 120,000; `resultado` = R$ 495,00.
- Passo 2: `receita` = **R$ 150,00** (item B); `quantidadeVendida` = 50,000.

Valores que indicam defeito no passo 1:
- 645,00 — usou `venda.valor_total` de V1, que inclui o item de P2
- 540,00 — incluiu V2, de abril
- 450,00 — perdeu V3, na borda inicial do mês

---

## RN23 — Resultado

### CT037 — Resultado é receita menos desembolso, inclusive negativo

| Campo | Conteúdo |
|---|---|
| Regra | RN23 |
| Requisito | RF36 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Três plantios de ciclo curto, cada um com lançamentos em março de 2026:

| Plantio | Itens de venda | Despesas (rateadas 100% ao plantio) |
|---|---|---|
| P1 | 25,000 kg × 4,50 = 112,50 | 300,00 + 45,67 = 345,67 |
| P2 | 100,000 kg × 5,00 = 500,00 | 120,00 |
| P3 | 40,000 kg × 3,75 = 150,00 | 150,00 |

**Passos**
1. Apurar P1, P2 e P3 com `mes` = 2026-03.

**Resultado esperado**

| Plantio | `receita` | `custoDesembolso` | `resultado` |
|---|---|---|---|
| P1 | 112,50 | 345,67 | **−233,17** |
| P2 | 500,00 | 120,00 | 380,00 |
| P3 | 150,00 | 150,00 | 0,00 |

O resultado de P1 é devolvido com sinal negativo. Falha: 233,17 (sinal invertido ou valor absoluto), `null`, 0,00 (resultado limitado a zero) ou campo ausente.

---

## RN24 — Período de apuração

### CT038 — Período derivado do ciclo efetivo do plantio

| Campo | Conteúdo |
|---|---|
| Regra | RN24 |
| Requisito | RF38 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Plantios P1 (Milho com ciclo `CURTO` sobreposto), PL (Milho `LONGO`, encerrado em 2026-07-31) e PA (Milho `LONGO`, aberto). Despesas rateadas 100% ao plantio:

| Plantio | Despesas |
|---|---|
| P1 | 60,00 em 2026-03-01 · 40,00 em 2026-03-31 |
| PL | 100,00 em 2026-02-01 · 80,00 em 2026-05-10 · 50,00 em 2026-07-31 |
| PA | 25,00 em 2026-10-11 |

**Passos**
1. Com o relógio padrão (hoje = 2026-10-11), apurar P1 com `mes` = 2026-03.
2. Apurar P1 com `mes` = 2026-02.
3. Apurar P1 sem `mes`.
4. Com `Clock.fixed(2026-11-01T01:30:00Z, America/Fortaleza)` — 22h30 de 31/10 em Fortaleza, já 01/11 em UTC —, apurar P1 sem `mes`.
5. Apurar PL com `mes` = 2026-03.
6. Apurar PL sem `mes`.
7. Apurar PA sem `mes`.

**Resultado esperado**

| Passo | `tipoCiclo` | `periodoInicio` | `periodoFim` | `custoDesembolso` |
|---|---|---|---|---|
| 1 | `CURTO` | 2026-03-01 | 2026-03-31 | 100,00 |
| 2 | `CURTO` | 2026-02-01 | 2026-02-28 | 0,00 |
| 3 | `CURTO` | 2026-10-01 | 2026-10-31 | 0,00 |
| 4 | `CURTO` | 2026-10-01 | 2026-10-31 | 0,00 |
| 5 | `LONGO` | 2026-02-01 | 2026-07-31 | **230,00** |
| 6 | `LONGO` | 2026-02-01 | 2026-07-31 | 230,00 |
| 7 | `LONGO` | 2026-02-01 | 2026-10-11 | 25,00 |

- Passo 1: `periodoInicio` 2026-02-01 indica que o ciclo foi lido da cultura (Milho é `LONGO` no catálogo), não do plantio.
- Passo 4: período de novembro indica "hoje" calculado em UTC.
- Passo 5: `custoDesembolso` 0,00 indica que o `mes` recortou um plantio de ciclo longo — nenhuma despesa de PL cai em março.

### CT039 — Lançamento fora do período derivado fica de fora

| Campo | Conteúdo |
|---|---|
| Regra | RN24 |
| Requisito | RF38 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Negativo |

**Pré-condição**
Hoje = 2026-10-11.

P1 (ciclo curto), com:
- Despesas rateadas 100% a P1: 30,00 em 2026-02-28 · 10,00 em 2026-03-15 · 70,00 em 2026-04-01 · 15,00 em 2026-09-30 · 35,00 em 2026-10-01.
- Venda em 2026-04-01 com item de P1: 10,000 kg × 4,50 = 45,00.
- Diária familiar de 1,00 × 80,00 em 2026-02-28.
- Produção de 12,000 kg em 2026-04-01.

PL (ciclo longo, de 2026-02-01 a 2026-07-31), com despesas rateadas 100% a PL: 200,00 em 2026-01-31 (um dia antes do início; nenhuma regra proíbe) · 100,00 em 2026-02-01.

**Passos**
1. Apurar P1 com `mes` = 2026-03.
2. Apurar P1 sem `mes`.
3. Apurar PL sem `mes`.

**Resultado esperado**
- Passo 1: `custoDesembolso` = **R$ 10,00**; `receita` = R$ 0,00; `valorMaoObraFamiliar` = R$ 0,00; `quantidadeProduzida` = 0,000; `resultado` = −R$ 10,00. Os lançamentos de 28/02 e 01/04, um dia de cada lado do mês, ficam de fora — seja despesa, venda, diária ou produção.
- Passo 2: período de outubro, `custoDesembolso` = **R$ 35,00**; os 15,00 de 30/09 ficam de fora.
- Passo 3: `custoDesembolso` = **R$ 100,00**; os 200,00 de 31/01 ficam de fora.

Valores que indicam defeito:
- Passo 1 com 160,00 — P1 apurado como safra (2026-02-01 até hoje)
- Passo 1 com `receita` 45,00 ou `valorMaoObraFamiliar` 80,00 — só as despesas foram recortadas
- Passo 3 com 300,00 — o período longo não começa no início do plantio

---

## RN25 — Produção e venda são grandezas distintas

Os dois casos são E2E na tela de resultado do plantio (A02). O relatório impresso tem a mesma exigência e é coberto pelo CT050 (Q00f).

### CT040 — Subsistência: produção sem venda, resultado negativo exibido

| Campo | Conteúdo |
|---|---|
| Regra | RN25 |
| Requisito | RF36 |
| Tipo | E2E |
| Prioridade | P2 |
| Natureza | Negativo |

**Pré-condição**
Criados pela API na fixture do spec:
- "Produtor Teste Subsistência" com propriedade "Sítio Teste".
- Plantio PS: Milho `LONGO`, 1,000 ha, início 2026-02-01.
- Despesa "Sementes e mudas" de R$ 450,00 em 2026-02-08 e despesa "Adubo e fertilizantes" de R$ 150,00 em 2026-03-10, ambas só em PS.
- Produção de 500,000 kg em PS em 2026-07-20.
- PS encerrado em 2026-07-31.
- Nenhuma venda.

**Passos**
1. Entrar como operador e abrir o detalhe do plantio PS.
2. Consultar o resultado do plantio.

**Resultado esperado**
- Período exibido: 01/02/2026 a 31/07/2026, sem pedir mês (ciclo longo).
- Quantidade produzida 500 kg e quantidade vendida 0 kg, em elementos distintos.
- Receita R$ 0,00; custo de desembolso R$ 600,00.
- Resultado **-R$ 600,00**, exibido com o rótulo "Resultado" e o mesmo destaque de um resultado positivo. Falha: campo oculto, vazio, "—", "não se aplica", ou valor 0,00.
- Custo com mão de obra familiar R$ 600,00, identificado como informação complementar.

O teste lê o número e a unidade exibidos, sem depender de quantas casas decimais a tela mostra.

### CT041 — Venda parcial: produzido e vendido como números distintos

| Campo | Conteúdo |
|---|---|
| Regra | RN25 |
| Requisito | RF36 |
| Tipo | E2E |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
Criados pela API na fixture do spec:
- "Produtor Teste Venda Parcial" com propriedade "Sítio Teste" e cliente "Cliente Teste Apuração" (`FEIRA`).
- Plantio PV: Milho `LONGO`, 1,000 ha, início 2026-02-01.
- Despesas de R$ 450,00 em 2026-02-08 e R$ 150,00 em 2026-03-10, só em PV.
- Duas colheitas em PV: 300,000 kg em 2026-07-10 e 200,000 kg em 2026-07-20.
- Venda em 2026-07-25 com um item de PV: 200,000 kg × R$ 4,50.
- PV encerrado em 2026-07-31.

**Passos**
1. Entrar como operador e abrir o detalhe do plantio PV.
2. Consultar o resultado do plantio.

**Resultado esperado**
- Quantidade produzida **500 kg**; quantidade vendida **200 kg**.
- Receita R$ 900,00; custo de desembolso R$ 600,00; resultado R$ 300,00.

Valores que indicam defeito: quantidade produzida 200 ou 300 (só uma colheita, contra a RF16); quantidade vendida 300 (o saldo, produzido − vendido, exibido no lugar do vendido); um único número de quantidade na tela.

---

## Pontos para revisar

1. **Glossário × decisão da safra.** O glossário define safra como "período anual de produção de uma cultura de ciclo longo". A decisão da A01 apura o ciclo longo do início ao encerramento do plantio, que pode passar de um ano ou durar menos. A RN24 foi completada na versão 1.3; falta alinhar o verbete "Safra" e o "Período de apuração" do glossário. Fora do escopo deste PR.
2. **Tipo Unitário e recorte por consulta.** A matriz põe CT032, CT035, CT036 e CT039 como unitários. Se a A01 fizer o recorte de período e de estorno numa consulta JPQL/SQL, o mock devolve o que o teste mandar e o recorte deixa de ser testado. Neste caso, os passos de borda e de estorno de CT032 e CT039 precisam rodar também contra o banco (Testcontainers), na mesma issue. Ajuste do tipo na QA 3.2, se for o caso, fica para a próxima revisão.
3. **Quantidades por período.** Este documento recorta `quantidadeProduzida` e `quantidadeVendida` pelo mesmo período da apuração, porque `ResultadoPlantioResposta` tem um só período. A RN25 não diz. Confirmar na A01.
4. **Produção em mais de uma unidade.** `quantidadeProduzida` soma quantidades, e `unidadeMedida` é uma só. Se um plantio tiver produção em `kg` e em outra unidade, a soma não tem sentido. A RN30 (Q00e) trata da venda; da produção ninguém trata. Os casos daqui usam só `kg`.
5. **Milho com ciclo curto na Q00b.** A massa da Q00b diz "Milho (ciclo `CURTO`)", mas o catálogo da migration traz Milho `LONGO`. Lá não muda nenhum valor, porque o rateio não usa o ciclo; aqui o plantio sobrepõe o ciclo de propósito. Vale corrigir a massa da Q00b na próxima revisão.
6. **Estorno e emissões anteriores.** A RN19 v1.3 tira a despesa estornada das apurações novas. Uma emissão anterior ao estorno continua com a despesa no `conteudo` (RN34). Isso é intencional e é testado no CT020 e no CT053; registrado para ninguém ler a divergência como defeito.
7. **Encerramento futuro.** A Q00a (ponto 3) deixou aberto encerrar plantio com data futura. Se isso for aceito, o período do ciclo longo vai até uma data que ainda não chegou. Nenhum valor daqui depende disso.
8. **Mês sem `mes` e ciclo longo com `mes`.** O contrato da F13 aceita `mes` para qualquer plantio. Para o ciclo longo ele é ignorado sem aviso; a resposta mostra o período usado em `periodoInicio` e `periodoFim`. Se o front precisar avisar o operador, é ajuste da A02.
