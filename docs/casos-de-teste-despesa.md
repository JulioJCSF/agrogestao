# AgroGestão — Casos de Teste: Despesa e Rateio (CT009–CT020)

Versão 1.0

Especificação dos casos de despesa e rateio no padrão da QA 3.3. Destrava L03, L04a, L04b e a parte de estorno da A03b.

**Documentos relacionados**

- `agrogestao-fase-1-regras-negocio.md` — RN05 a RN11
- `agrogestao-fase-3-qa.md` — 3.2 (matriz e casos críticos) e 3.3 (padrão)
- `agrogestao-fase-2-der.md` — tabelas `despesa`, `rateio_despesa` e `emissao_relatorio`
- `agrogestao-fase-2-padroes-tecnicos.md` — 2.4, formato de erro

---

## Visão geral

| Caso | Título | Regra | Requisito | Tipo | Prior. | Natureza | Issue |
|---|---|---|---|---|---|---|---|
| CT009 | Alteração de área não recalcula rateio gravado | RN05 | RF21 | Integração | **P1** | Positivo | L04a |
| CT010 | Despesa vinculada a um plantio é aceita | RN06 | RF19 | Unitário | P2 | Positivo | L04a |
| CT011 | Despesa sem plantio vinculado é rejeitada | RN06 | RF19 | Unitário | P2 | Negativo | L04a |
| CT012 | Rateio proporcional à área de cada plantio | RN07 | RF20 | Unitário | **P1** | Positivo | L03 |
| CT013 | Plantio não vinculado fica fora da base do rateio | RN07 | RF20 | Unitário | **P1** | Negativo | L03 |
| CT014 | Despesa em plantio único recebe 100% | RN08 | RF20 | Unitário | P2 | Positivo | L03 |
| CT015 | Fechamento de centavos, três plantios de área igual | RN09 | RF20 | Unitário | **P1** | Positivo | L03 |
| CT016 | Áreas desiguais sem dízima não geram residual | RN09 | RF20 | Unitário | **P1** | Positivo | L03 |
| CT017 | Soma dos rateios igual ao total (parametrizado) | RN09 | RF20 | Unitário | **P1** | Positivo | L03 |
| CT018 | Rateio gravado no lançamento e lido nas consultas | RN10 | RF21 | Integração | **P1** | Positivo | L04a |
| CT019 | Excluir despesa remove seus rateios | RN11 | RF45 | Integração | P2 | Positivo | L04b |
| CT020 | Despesa em período emitido não é excluída, é estornada | RN11 | RF45 | Integração | P2 | Negativo | A03b |

**Sobre a regra do par.** RN06 e RN11 têm positivo e negativo. RN07 tem par com CT013 como negativo: verifica que o cálculo rejeita a base errada. RN05, RN08, RN09 e RN10 descrevem cálculo ou gravação, sem violação a rejeitar pelo operador — o "negativo" de cada uma é o valor errado, e os resultados esperados abaixo já o excluem com valor concreto.

---

## Convenções de cálculo

Decisões que os casos assumem. RN09 diz *para onde* vai o residual; estas dizem *como* cada parcela é arredondada antes disso.

| Item | Decisão | Por quê |
|---|---|---|
| Valor rateado de cada parcela | `BigDecimal`, truncado em 2 casas (`RoundingMode.DOWN`) | Residual sempre ≥ 0, coerente com "somada" da RN09 |
| Residual | `valor total − Σ parcelas truncadas`, somado ao plantio de maior área; no empate, ao de menor id | RN09 |
| Percentual gravado | `área / Σ áreas`, arredondado em 6 casas (`RoundingMode.HALF_UP`) | Cabe em `NUMERIC(9,6)`; não é fonte de verdade |
| Fonte de verdade | `valor_rateado` | DER, `rateio_despesa` |
| Base do rateio | Soma das áreas dos plantios **vinculados àquela despesa** | RN07 |

Com truncamento, o residual fica entre R$ 0,00 e (n − 1) centavos, sendo n o número de plantios.

Nas tabelas, os plantios aparecem em ordem crescente de id: P1 tem o menor id.

---

## Massa de dados base

Sintética, conforme `RNF10`. Criada por fixture em cada teste e removida no final.

| Item | Valor |
|---|---|
| Usuário | operador de teste, perfil operador |
| Produtor | "Produtor Teste Despesa", CPF sintético válido em dígito verificador |
| Propriedade | "Sítio Teste", área total 10,000 ha, do produtor acima |
| Cultura | Milho (ciclo `CURTO`) |
| Categoria | "Adubo e fertilizantes" (`sistema = FALSE`, da migration de dados iniciais) |
| Plantios | Criados por caso, todos na propriedade acima, `data_inicio` 2026-02-01, sem encerramento |
| Data do fato da despesa | 2026-03-10, salvo indicação |

Testes unitários não usam banco: os plantios entram como objetos com id e área.

---

## RN05 — Alteração de área não retroage

### CT009 — Alteração de área não recalcula rateio gravado

| Campo | Conteúdo |
|---|---|
| Regra | RN05 |
| Requisito | RF21 |
| Tipo | Integração |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Plantios P1 (1,000 ha) e P2 (2,000 ha). Despesa D1 de R$ 90,00 lançada vinculada a P1 e P2, com rateio gravado:

| Plantio | Percentual | Valor rateado |
|---|---|---|
| P1 | 0,333333 | 30,00 |
| P2 | 0,666667 | 60,00 |

**Passos**
1. Alterar a área de P2 de 2,000 ha para 0,500 ha pelo serviço de plantio.
2. Consultar os rateios de D1.
3. Lançar uma nova despesa D2 de R$ 90,00 vinculada a P1 e P2.
4. Consultar os rateios de D2.

**Resultado esperado**
- Passo 2: rateios de D1 inalterados — P1 0,333333 / 30,00 e P2 0,666667 / 60,00. Nenhuma linha de `rateio_despesa` de D1 teve `atualizado_em` alterado.
- Passo 4: D2 usa a área nova — P1 0,666667 / 60,00 e P2 0,333333 / 30,00.

O passo 3 garante que o teste não passa por acidente com um rateio que simplesmente ignora a área.

---

## RN06 — Vínculo mínimo

### CT010 — Despesa vinculada a um plantio é aceita

| Campo | Conteúdo |
|---|---|
| Regra | RN06 |
| Requisito | RF19 |
| Tipo | Unitário |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
Plantio P1 (1,000 ha) do produtor. Repositórios mockados.

**Passos**
1. Chamar o serviço de lançamento de despesa com valor R$ 150,00, categoria "Adubo e fertilizantes", data do fato 2026-03-10 e lista de plantios `[P1]`.

**Resultado esperado**
Nenhuma exceção. O repositório de despesa recebe uma despesa de R$ 150,00 com exatamente um rateio: P1, percentual 1,000000, valor 150,00.

### CT011 — Despesa sem plantio vinculado é rejeitada

| Campo | Conteúdo |
|---|---|
| Regra | RN06 |
| Requisito | RF19 |
| Tipo | Unitário |
| Prioridade | P2 |
| Natureza | Negativo |

**Pré-condição**
Repositórios mockados.

**Passos**
1. Chamar o serviço de lançamento com valor R$ 150,00, categoria "Adubo e fertilizantes", data do fato 2026-03-10 e lista de plantios vazia `[]`.
2. Repetir com a lista de plantios nula.

**Resultado esperado**
- Nos dois passos o serviço lança `RegraNegocioException` com a mensagem "A despesa precisa estar vinculada a pelo menos um plantio."
- O repositório de despesa e o de rateio não são chamados (`verify(..., never()).save(...)`).
- Na API, a mesma situação responde **422** com `erro: "REGRA_NEGOCIO"`. O DTO de entrada **não** usa `@NotEmpty` na lista de plantios, para a regra não virar 400.

---

## RN07 — Cálculo do rateio

### CT012 — Rateio proporcional à área de cada plantio

| Campo | Conteúdo |
|---|---|
| Regra | RN07 |
| Requisito | RF20 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Plantios P1 (1,500 ha) e P2 (0,500 ha).

**Passos**
1. Chamar `RateioDespesaService` com valor total R$ 300,00 e os plantios P1 e P2.

**Resultado esperado**

| Plantio | Percentual | Valor rateado |
|---|---|---|
| P1 | 0,750000 | 225,00 |
| P2 | 0,250000 | 75,00 |

Soma 300,00. Divisão igual (150,00 / 150,00) é falha.

### CT013 — Plantio não vinculado fica fora da base do rateio

| Campo | Conteúdo |
|---|---|
| Regra | RN07 |
| Requisito | RF20 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Negativo |

**Pré-condição**
Propriedade com área total 10,000 ha e três plantios: P1 (1,000 ha), P2 (3,000 ha) e P3 (6,000 ha). A despesa é vinculada só a P1 e P2.

**Passos**
1. Chamar `RateioDespesaService` com valor total R$ 400,00 e os plantios P1 e P2.

**Resultado esperado**

| Plantio | Percentual | Valor rateado |
|---|---|---|
| P1 | 0,250000 | 100,00 |
| P2 | 0,750000 | 300,00 |

Nenhum rateio para P3. Resultados que indicam base errada, todos falha:
- 40,00 / 120,00 — base na área total da propriedade (10 ha)
- 40,00 / 120,00 / 240,00 — base em todos os plantios da propriedade

---

## RN08 — Despesa em plantio único

### CT014 — Despesa em plantio único recebe 100%

| Campo | Conteúdo |
|---|---|
| Regra | RN08 |
| Requisito | RF20 |
| Tipo | Unitário |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
Plantio P1 com área fracionária de 0,750 ha.

**Passos**
1. Chamar `RateioDespesaService` com valor total R$ 57,89 e o plantio P1.

**Resultado esperado**
Um rateio: P1, percentual 1,000000, valor 57,89. O caminho é o mesmo cálculo da RN07 — não deve existir `if` para plantio único.

---

## RN09 — Fechamento de centavos

### CT015 — Fechamento de centavos, três plantios de área igual

| Campo | Conteúdo |
|---|---|
| Regra | RN09 |
| Requisito | RF20 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Plantios P1, P2 e P3, todos com 1,000 ha (empate na maior área).

**Passos**
1. Chamar `RateioDespesaService` com valor total R$ 100,00 e os três plantios.

**Resultado esperado**

| Plantio | Percentual | Valor rateado |
|---|---|---|
| P1 | 0,333333 | **33,34** |
| P2 | 0,333333 | 33,33 |
| P3 | 0,333333 | 33,33 |

Soma exatamente 100,00. O residual de 0,01 vai para P1: todos empatam na maior área, P1 tem o menor id. Método sugerido: `deveAtribuirResidualAoMenorIdQuandoAreasEmpatarem`.

**Variação obrigatória:** passar os plantios na ordem P3, P2, P1. O resultado deve ser o mesmo — o desempate é pelo id, não pela posição na lista.

### CT016 — Áreas desiguais sem dízima não geram residual

| Campo | Conteúdo |
|---|---|
| Regra | RN09 |
| Requisito | RF20 |
| Tipo | Unitário |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Plantios P1 (1,000 ha), P2 (1,000 ha) e P3 (2,000 ha).

**Passos**
1. Chamar `RateioDespesaService` com valor total R$ 100,00 e os três plantios.

**Resultado esperado**

| Plantio | Percentual | Valor rateado |
|---|---|---|
| P1 | 0,250000 | 25,00 |
| P2 | 0,250000 | 25,00 |
| P3 | 0,500000 | 50,00 |

Soma 100,00, residual 0,00. Garante que a lógica de residual não acrescenta centavo quando não há dízima.

### CT017 — Soma dos rateios igual ao total (parametrizado)

| Campo | Conteúdo |
|---|---|
| Regra | RN09 |
| Requisito | RF20 |
| Tipo | Unitário (parametrizado, `@ParameterizedTest` + `@MethodSource`) |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Nenhuma além dos dados de cada linha.

**Passos**
1. Para cada linha da tabela, chamar `RateioDespesaService` com o valor total e as áreas indicadas (P1, P2, … em ordem de id).
2. Verificar os valores rateados de cada plantio.
3. Verificar que a soma dos valores rateados é **exatamente** igual ao valor total (`compareTo == 0`).

**Resultado esperado**

| # | Valor total | Áreas (ha) | Valores rateados esperados | Residual → plantio | O que exercita |
|---|---|---|---|---|---|
| 1 | 100,00 | 1 · 2 · 4 | 14,28 · 28,57 · **57,15** | 0,01 → P3 | Residual no maior, que é o último |
| 2 | 10,00 | 0,5 · 0,5 · 2 | 1,66 · 1,66 · **6,68** | 0,02 → P3 | Residual de 2 centavos |
| 3 | 50,00 | 3 · 3 · 3 | **16,68** · 16,66 · 16,66 | 0,02 → P1 | Empate total, residual de 2 centavos |
| 4 | 100,00 | 3 · 3 · 1 | **42,87** · 42,85 · 14,28 | 0,02 → P1 | Empate só entre os maiores |
| 5 | 123,45 | 1 · 2 · 3 | 20,57 · 41,15 · **61,73** | 0,01 → P3 | Valor com centavos |
| 6 | 1.500,00 | 0,3 · 0,7 · 1,2 | 204,54 · 477,27 · **818,19** | 0,01 → P3 | Áreas fracionárias |
| 7 | 250,00 | 1,25 · 2,5 · 0,75 | 69,44 · **138,90** · 41,66 | 0,02 → P2 | Maior área no meio da lista |
| 8 | 75,50 | 2,5 · 1 | **53,93** · 21,57 | 0,01 → P1 | Dois plantios |
| 9 | 20,00 | 1 · 1 · 1 · 1 · 1 · 1 | **3,35** · 3,33 · 3,33 · 3,33 · 3,33 · 3,33 | 0,02 → P1 | Seis plantios |
| 10 | 99,99 | 1 · 1 · 1 | 33,33 · 33,33 · 33,33 | 0,00 | Divisão exata em área igual |
| 11 | 1.000.000,00 | 1 · 1 · 1 | **333.333,34** · 333.333,33 · 333.333,33 | 0,01 → P1 | Valor alto, sem perda de precisão |

Em negrito, o plantio que recebeu o residual. Toda linha soma exatamente o valor total.

Percentuais esperados das linhas com dízima (HALF_UP, 6 casas), verificados no mesmo teste:

| # | Percentuais |
|---|---|
| 1 | 0,142857 · 0,285714 · 0,571429 |
| 2 | 0,166667 · 0,166667 · 0,666667 |
| 5 | 0,166667 · 0,333333 · 0,500000 |
| 6 | 0,136364 · 0,318182 · 0,545455 |
| 7 | 0,277778 · 0,555556 · 0,166667 |
| 8 | 0,714286 · 0,285714 |
| 9 | 0,166667 em todos |

Para incluir uma linha nova: calcular cada parcela truncada em 2 casas, somar o residual ao maior plantio (menor id no empate) e conferir a soma. Linha que precisar de residual negativo indica arredondamento errado na implementação.

---

## RN10 — Gravação no lançamento

### CT018 — Rateio gravado no lançamento e lido nas consultas

| Campo | Conteúdo |
|---|---|
| Regra | RN10 |
| Requisito | RF21 |
| Tipo | Integração |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Plantios P1, P2 e P3, todos com 1,000 ha. Banco efêmero (Testcontainers).

**Passos**
1. Lançar despesa D1 de R$ 100,00 vinculada a P1, P2 e P3 pelo serviço de lançamento.
2. Ler as linhas de `rateio_despesa` de D1 direto pelo repositório.
3. Alterar por SQL, fora do serviço, o `valor_rateado` do rateio de P2 para 40,00 (valor sentinela, impossível pelo cálculo).
4. Consultar as despesas de P2 pelo serviço de consulta (RF23).

**Resultado esperado**
- Passo 2: três linhas gravadas no mesmo lançamento — P1 0,333333 / 33,34; P2 0,333333 / 33,33; P3 0,333333 / 33,33.
- Passo 4: a consulta devolve **40,00** para D1 em P2. Se devolver 33,33, a consulta está recalculando a partir do percentual ou da área — falha.

O sentinela do passo 3 é o que distingue "leu o gravado" de "recalculou e coincidiu".

---

## RN11 — Exclusão de despesa

### CT019 — Excluir despesa remove seus rateios

| Campo | Conteúdo |
|---|---|
| Regra | RN11 |
| Requisito | RF45 |
| Tipo | Integração |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
Plantios P1 (1,000 ha) e P2 (1,000 ha). Despesa D1 de R$ 80,00, data do fato 2026-03-10, vinculada a P1 e P2 (40,00 / 40,00). Despesa D2 de R$ 30,00 vinculada só a P1. Nenhuma emissão de relatório para o produtor.

**Passos**
1. Excluir D1 pelo serviço.
2. Consultar `despesa` e `rateio_despesa`.
3. Consultar as despesas de P1 (RF23).

**Resultado esperado**
- D1 não existe mais; nenhuma linha de `rateio_despesa` com `despesa_id` de D1.
- D2 e seu rateio (P1, 30,00) permanecem intactos.
- A consulta de P1 lista só D2, com 30,00.
- Na API: `DELETE` responde 204.

### CT020 — Despesa em período emitido não é excluída, é estornada

| Campo | Conteúdo |
|---|---|
| Regra | RN11 |
| Requisito | RF45 |
| Tipo | Integração |
| Prioridade | P2 |
| Natureza | Negativo |

**Pré-condição**
Plantios P1 (1,000 ha) e P2 (1,000 ha). Despesa D1 de R$ 80,00, data do fato 2026-03-10, vinculada a P1 e P2 (40,00 / 40,00). Relatório emitido para o produtor com período de 2026-01-01 a 2026-06-30 — D1 está dentro do período. Guardar o `conteudo` dessa emissão (E1) para comparação.

**Passos**
1. Tentar excluir D1 pelo serviço.
2. Consultar `despesa` e `rateio_despesa`.
3. Estornar D1 pelo serviço.
4. Consultar D1, seus rateios e a emissão E1.

**Resultado esperado**
- Passo 1: `RegraNegocioException` com a mensagem "Esta despesa já consta em relatório emitido e não pode ser excluída. Use o estorno." Na API: **422**, `erro: "REGRA_NEGOCIO"`.
- Passo 2: D1 continua existindo com `estornada = FALSE`; os dois rateios (40,00 / 40,00) continuam gravados.
- Passo 4: D1 com `estornada = TRUE`, nenhuma despesa nova criada (sem lançamento de ajuste), rateios de D1 preservados. O `conteudo` de E1 é idêntico ao guardado na pré-condição (`RN34`).

**Borda a cobrir no mesmo teste:** despesa D3 com data do fato 2026-07-01, fora do período emitido, é excluída normalmente.

---

## Pontos para revisar

1. **Convenção de arredondamento.** Truncar o valor e usar HALF_UP no percentual foi decidido na Q00b. Vale registrar na RN09 e no critério de pronto da L03, para não ficar só neste documento.
2. **CT015 na QA 3.2.** O texto lista "33,33 / 33,33 / 33,34" sem dizer qual plantio leva o centavo. Este documento fixa: o de menor id (P1). Ajustar a 3.2 na próxima revisão.
3. **Despesa que não cobre um centavo por plantio.** R$ 0,02 entre três plantios iguais dá 0,02 / 0,00 / 0,00: dois rateios gravados com valor zero (o percentual continua > 0, então a constraint passa). Caso raro e fora do CT017; decidir se a API recusa despesa com valor menor que o número de plantios em centavos.
4. **Mensagens de erro.** As mensagens de CT011 e CT020 são propostas. O teste verifica `erro` e status; a mensagem exata é confirmada quando a L04a e a A03b forem implementadas.
5. **Estorno e apuração.** Este documento não define se despesa estornada sai do custo de desembolso em novas emissões. Isso pertence à Q00d (apuração).
