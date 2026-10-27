# AgroGestão — Casos de Teste: Venda (CT042–CT049)

Versão 1.0

Especificação dos casos de venda no padrão da QA 3.3. Destrava L09b, Q02 e Q05.

**Documentos relacionados**

- `agrogestao-fase-1-regras-negocio.md` — RN26 a RN30 (RN30 completada na versão 1.4)
- `agrogestao-fase-1-requisitos.md` — RF30 a RF33
- `agrogestao-fase-3-qa.md` — 3.2 (matriz) e 3.3 (padrão)
- `agrogestao-fase-2-der.md` — tabelas `cultura`, `producao`, `venda` e `item_venda`; seção 5 (RN29 é ausência deliberada de restrição)
- `agrogestao-fase-2-padroes-tecnicos.md` — 2.4, contrato de vendas e saldo, códigos de status e formato de erro
- `api/src/main/java/com/agrogestao/api/dto/` — `VendaRequisicao`, `ItemVendaRequisicao`, `VendaResposta` e `SaldoPlantioResposta`, contrato da F13
- `api/src/test/java/com/agrogestao/api/controller/ContratoIT.java` — já verifica `itens[0].plantioId` em `campos` para item sem plantio

---

## Visão geral

| Caso | Título | Regra | Requisito | Tipo | Prior. | Natureza | Issue |
|---|---|---|---|---|---|---|---|
| CT042 | Venda com um ou mais itens é registrada | RN26 | RF30 | API | P3 | Positivo | Q02 |
| CT043 | Venda sem itens é rejeitada | RN26 | RF30 | API | P3 | Negativo | Q02 |
| CT044 | Item sem plantio de origem ou com plantio inexistente é rejeitado | RN27 | RF31 | API | P2 | Negativo | Q02 |
| CT045 | Único plantio aberto da cultura vem pré-selecionado | RN28 | RF32 | E2E | P3 | Positivo | Q05 |
| CT046 | Com dois plantios abertos, o operador escolhe | RN28 | RF32 | E2E | P3 | Negativo | Q05 |
| CT047 | Venda acima do saldo é aceita e sinalizada | RN29 | RF33 | API | P2 | Positivo | Q02 |
| CT048 | Item na unidade da produção é aceito, com totais calculados | RN30 | RF31 | Unitário | P3 | Positivo | L09b |
| CT049 | Item em unidade diferente é rejeitado | RN30 | RF31 | Unitário | P3 | Negativo | L09b |

**Sobre a regra do par.** RN26 (CT042 positivo, CT043 negativo), RN28 (CT045 positivo, CT046 negativo — o sistema recusa decidir no lugar do operador) e RN30 (CT048 positivo, CT049 negativo) têm par. RN27 tem só o CT044 na matriz, que é o negativo; o positivo da RN27 está dentro do CT042, em que cada item traz `plantioId` válido e o 201 devolve o mesmo `plantioId` em cada item. RN29 descreve ausência de restrição, exceção prevista na QA 3.3: não há violação a rejeitar. O CT047 já exclui o comportamento errado com valor concreto — 422 em vez de 201, ou `avisos` vazio com saldo −50,000.

---

## Convenções

| Item | Decisão | Por quê |
|---|---|---|
| `itens` ausente, `null` ou `[]` | **400** `VALIDACAO`, com `itens` em `campos` | `VendaRequisicao.itens` é `@NotEmpty` no contrato da F13. É diferente da RN06 na Q00b (422): ver ponto 1 |
| Item sem `plantioId`, `null`, zero ou negativo | **400** `VALIDACAO`, com `itens[n].plantioId` em `campos` | `ItemVendaRequisicao.plantioId` é `@NotNull @Positive`; o `ContratoIT` já verifica o caminho `itens[0].plantioId` |
| `plantioId` que não existe | **404** `NAO_ENCONTRADO`, `campos` vazio | Padrões 2.4: recurso inexistente é 404, via `RecursoNaoEncontradoException`, mesmo caminho que os services já usam ao não achar o que procuram. 422 fica para dado que existe e viola regra. Mesmo tratamento para `produtorId` e `clienteId` inexistentes |
| Unidade do item diferente da referência | **422** `REGRA_NEGOCIO` | Padrões 2.4: regra violada |
| Referência da unidade (RN30) | A unidade da produção registrada no plantio; sem produção registrada, a unidade da cultura do plantio | Decisão de 2026-10-26 na L09b (#93), registrada na RN30 (regras v1.4) |
| Comparação de unidade | Ignora espaços nas pontas e diferença de maiúsculas: `" KG "`, `"Kg"` e `"kg"` são iguais; `"kgs"` e `"k g"` não | Decisão de 2026-10-26 na L09b (#93), RN30 v1.4. Mesma linha dos nomes de cultura e categoria, já comparados sem diferenciar caixa |
| Total do item | `quantidade × valorUnitario`, arredondado em 2 casas com `RoundingMode.HALF_UP` | Decisão de 2026-10-26 na L09b (#93), RN30 v1.4 |
| Total da venda | Soma dos totais dos itens já arredondados | Mesma decisão; o total bate com a soma das linhas que o operador vê |
| Venda acima do saldo | Aceita: **201**. A divergência vai em `VendaResposta.avisos` (lista de texto) quando o vendido acumulado do plantio passa do produzido | RN29; `avisos` existe no contrato da F13; DER, seção 5 (sem `CHECK` de saldo) |
| Saldo | `GET /api/v1/plantios/{id}/saldo`: `saldo = quantidadeProduzida − quantidadeVendida`, sem recorte de período; pode ser negativo | RF33, M05, `SaldoPlantioResposta` |
| Sugestão de plantio (RN28) | Conta só plantio **aberto** (sem `data_encerramento`), da cultura informada no item, **do produtor da venda**. Um → pré-selecionado; dois ou mais → nenhum pré-selecionado | RN28, M05 ("plantios abertos de uma cultura para um produtor"), M06 |
| RN04, RN35 e RN36 na venda | Validadas pela L09b com os validadores comuns da L00. Os casos são CT007/CT008 (Q00a) e CT055/CT056/CT057 (Q00f); não se repetem aqui | Q00a, issue L00 |
| Datas nos casos de API e E2E | Datas do fato de 2026 já passadas, plantios abertos | Não dependem do dia em que rodam nem esbarram na RN35 |
| Mensagens | As citadas são propostas. O teste verifica `status`, `erro` e `campos`; a mensagem exata é confirmada na implementação | Mesma linha da Q00a e da Q00b |

Os casos de API usam o token de um usuário de perfil `LANCAMENTO` e conferem o efeito no banco contando linhas de `venda` e `item_venda` antes e depois.

---

## Massa de dados base

Sintética, conforme `RNF10`. Criada por fixture em cada teste e removida no final.

| Item | Valor |
|---|---|
| Usuário | operador de teste, perfil `LANCAMENTO` |
| Produtor | "Produtor Teste Venda", CPF `99900001150`, com consentimento registrado |
| Outro produtor (CT045) | "Produtor Teste Vizinho", CPF `99900001664` |
| Propriedade | "Sítio Teste", área total 10,000 ha, de cada produtor |
| Culturas | Milho e Feijão, do catálogo da migration: unidade `kg` nas duas |
| Cliente | "Cliente Teste Venda", canal `FEIRA` |
| Plantios | Criados por caso, 1,000 ha, `data_inicio` 2026-02-01, abertos salvo indicação |
| Data do fato da venda | 2026-06-10, salvo indicação |
| "Hoje" nos unitários | 2026-10-11, por `Clock.fixed(2026-10-11T12:00:00-03:00, America/Fortaleza)` |

Nos unitários, plantio, cultura e produções entram como objetos em memória; os repositórios são mockados e o de venda captura a entidade recebida em `save`.

---

## RN26 — Venda com ao menos um item

### CT042 — Venda com um ou mais itens é registrada

| Campo | Conteúdo |
|---|---|
| Regra | RN26 |
| Requisito | RF30 |
| Tipo | API |
| Prioridade | P3 |
| Natureza | Positivo |

**Pré-condição**
Produtor, cliente e dois plantios abertos: P1 (Milho) com produção de 100,000 kg e P2 (Feijão) com produção de 50,000 kg. Nenhuma venda do produtor.

**Passos**
1. `POST /api/v1/vendas` com:
   ```json
   {"produtorId": <produtor>, "clienteId": <cliente>, "dataFato": "2026-06-10",
    "itens": [{"plantioId": <P1>, "quantidade": 10.000, "unidadeMedida": "kg", "valorUnitario": 4.50}]}
   ```
2. `POST /api/v1/vendas` com dois itens: P1, 20,000 kg × 4,50, e P2, 5,000 kg × 7,25, mesma data.
3. `GET /api/v1/vendas/{id}` da venda do passo 2.
4. Contar as linhas de `venda` e `item_venda` do produtor.

**Resultado esperado**
- Passo 1: **201**, `Location` terminando em `/api/v1/vendas/{id}`. Corpo com `produtorId`, `clienteId` e `dataFato` iguais aos enviados, `dataColeta` preenchida, `valorTotal` = 45.00, `avisos` = `[]` e um item: `plantioId` = P1, `quantidade` = 10.000, `unidadeMedida` = `"kg"`, `valorUnitario` = 4.50, `valorTotal` = 45.00.
- Passo 2: **201**; itens com `valorTotal` 90.00 (P1) e 36.25 (P2); `valorTotal` da venda = **126.25**; cada item devolve o `plantioId` enviado.
- Passo 3: **200** com o mesmo corpo do passo 2.
- Passo 4: 2 vendas e 3 itens.

### CT043 — Venda sem itens é rejeitada

| Campo | Conteúdo |
|---|---|
| Regra | RN26 |
| Requisito | RF30 |
| Tipo | API |
| Prioridade | P3 |
| Natureza | Negativo |

**Pré-condição**
Produtor e cliente da massa base. Nenhuma venda do produtor.

**Passos**
1. `POST /api/v1/vendas` sem o campo `itens`:
   ```json
   {"produtorId": <produtor>, "clienteId": <cliente>, "dataFato": "2026-06-10"}
   ```
2. Repetir com `"itens": []`.
3. Repetir com `"itens": null`.
4. Contar as linhas de `venda` e `item_venda` do produtor.

**Resultado esperado**
- Passos 1, 2 e 3: **400**, `erro: "VALIDACAO"`, `campos` com um elemento de `campo` = `"itens"`.
- Passo 4: zero vendas e zero itens.

O status é 400, e não 422, porque o contrato da F13 valida a lista no DTO (`@NotEmpty`). A rejeição acontece antes do serviço; não há teste unitário de serviço para a RN26.

---

## RN27 — Item aponta para plantio

### CT044 — Item sem plantio de origem ou com plantio inexistente é rejeitado

| Campo | Conteúdo |
|---|---|
| Regra | RN27 |
| Requisito | RF31 |
| Tipo | API |
| Prioridade | P2 |
| Natureza | Negativo |

**Pré-condição**
Produtor, cliente e plantio P1 (Milho) aberto, com produção de 100,000 kg. Nenhuma venda do produtor.

**Passos**
1. `POST /api/v1/vendas` com um item sem `plantioId`:
   ```json
   {"produtorId": <produtor>, "clienteId": <cliente>, "dataFato": "2026-06-10",
    "itens": [{"quantidade": 10.000, "unidadeMedida": "kg", "valorUnitario": 4.50}]}
   ```
2. Repetir com `"plantioId": null`.
3. Repetir com `"plantioId": 0`.
4. Repetir com `"plantioId": 9223372036854775807` (id que não existe).
5. Enviar dois itens: o primeiro com `plantioId` = P1, válido; o segundo sem `plantioId`.
6. Contar as linhas de `venda` e `item_venda` do produtor.

**Resultado esperado**
- Passos 1, 2 e 3: **400**, `erro: "VALIDACAO"`, `campos` com `campo` = `"itens[0].plantioId"`.
- Passo 4: **404**, `erro: "NAO_ENCONTRADO"`, `campos` = `[]`. Mensagem proposta: "Plantio não encontrado."
- Passo 5: **400**, `campos` com `campo` = `"itens[1].plantioId"`.
- Passo 6: zero vendas e zero itens. O item válido do passo 5 não pode ter sido gravado sozinho.

O passo 4 pega a implementação que só confia no `@NotNull`: sem consultar o plantio, a gravação cairia na chave estrangeira do banco e sairia como 422 com mensagem genérica, ou como 500.

---

## RN28 — Sugestão de plantio

Os dois casos são E2E na tela de venda (L10, com a sugestão da M06).

### CT045 — Único plantio aberto da cultura vem pré-selecionado

| Campo | Conteúdo |
|---|---|
| Regra | RN28 |
| Requisito | RF32 |
| Tipo | E2E |
| Prioridade | P3 |
| Natureza | Positivo |

**Pré-condição**
Criados pela API na fixture do spec:
- "Produtor Teste Venda", com PM1 (Milho, aberto), PM0 (Milho, início 2026-01-10, encerrado em 2026-05-31) e PF (Feijão, aberto).
- "Produtor Teste Vizinho", com PY (Milho, aberto).
- Cliente "Cliente Teste Venda".

**Passos**
1. Entrar como operador e iniciar uma venda do "Produtor Teste Venda" para o "Cliente Teste Venda", data 10/06/2026.
2. Adicionar um item e informar a cultura Milho.
3. Informar 10 kg a R$ 4,50 e salvar a venda.
4. Iniciar outra venda e adicionar um item com a cultura Feijão.

**Resultado esperado**
- Passo 2: o plantio do item vem preenchido com **PM1**. Não pode ser PM0 (encerrado) nem PY (de outro produtor). O teste compara o id do plantio selecionado, não o texto exibido.
- Passo 3: venda gravada; `GET /api/v1/vendas/{id}` devolve um item com `plantioId` = PM1 e `valorTotal` = 45.00.
- Passo 4: o plantio do item vem preenchido com **PF**.

PM0 e PY estão ali para o caso não passar por acidente: contando plantio encerrado ou de outro produtor, a cultura Milho teria três candidatos e nada seria pré-selecionado.

### CT046 — Com dois plantios abertos, o operador escolhe

| Campo | Conteúdo |
|---|---|
| Regra | RN28 |
| Requisito | RF32 |
| Tipo | E2E |
| Prioridade | P3 |
| Natureza | Negativo |

**Pré-condição**
Criados pela API na fixture do spec: "Produtor Teste Venda", com PM1 e PM2 (Milho, ambos abertos, PM1 com o menor id) e PM0 (Milho, encerrado em 2026-05-31). Cliente "Cliente Teste Venda". Nenhuma venda do produtor.

**Passos**
1. Entrar como operador e iniciar uma venda do "Produtor Teste Venda" para o "Cliente Teste Venda", data 10/06/2026.
2. Adicionar um item e informar a cultura Milho.
3. Informar 8 kg a R$ 4,75 e tentar salvar sem escolher o plantio.
4. Escolher PM2 e salvar.

**Resultado esperado**
- Passo 2: **nenhum** plantio pré-selecionado. PM1 e PM2 aparecem como opções.
- Passo 3: nenhuma venda gravada; a lista de vendas do produtor continua vazia.
- Passo 4: venda gravada; `GET /api/v1/vendas/{id}` devolve um item com `plantioId` = **PM2** e `valorTotal` = 38.00.

Escolher PM2, e não PM1, pega a implementação que pré-seleciona "o primeiro da lista" ou que grava o primeiro plantio em vez do escolhido.

---

## RN29 — Saldo não bloqueia venda

### CT047 — Venda acima do saldo é aceita e sinalizada

| Campo | Conteúdo |
|---|---|
| Regra | RN29 |
| Requisito | RF33 |
| Tipo | API |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
Produtor, cliente e plantio P1 (Milho) aberto, com uma produção de 100,000 kg. Nenhuma venda do produtor.

**Passos**
1. `POST /api/v1/vendas` com um item de P1: 60,000 kg × 5,00.
2. `GET /api/v1/plantios/{P1}/saldo`.
3. `POST /api/v1/vendas` com um item de P1: 90,000 kg × 5,00.
4. `GET /api/v1/plantios/{P1}/saldo`.
5. Contar os itens de venda de P1.

**Resultado esperado**
- Passo 1: **201**, item com `valorTotal` 300.00, `avisos` = `[]`.
- Passo 2: **200**, `quantidadeProduzida` = 100.000, `quantidadeVendida` = 60.000, `saldo` = 40.000, `unidadeMedida` = `"kg"`.
- Passo 3: **201** — a venda é aceita. Item com `valorTotal` 450.00. `avisos` com **um** elemento. Mensagem proposta: "A quantidade vendida do plantio (150,000 kg) supera a produção registrada (100,000 kg). Confira os registros."
- Passo 4: **200**, `quantidadeProduzida` = 100.000, `quantidadeVendida` = 150.000, `saldo` = **−50.000**.
- Passo 5: 2 itens gravados.

O passo 3 sozinho (90,000 kg) não passa do produzido; só o acumulado (150,000 kg) passa. Ele pega a implementação que compara a quantidade da venda nova com a produção em vez de olhar o saldo. O passo 1 garante que `avisos` não vem preenchido sempre.

---

## RN30 — Coerência de unidade

### CT048 — Item na unidade da produção é aceito, com totais calculados

| Campo | Conteúdo |
|---|---|
| Regra | RN30 |
| Requisito | RF31 |
| Tipo | Unitário |
| Prioridade | P3 |
| Natureza | Positivo |

**Pré-condição**
- P1: Milho (cultura em `kg`), aberto, com produção de 100,000 kg registrada em `"kg"`.
- P2: Milho (cultura em `kg`), aberto, sem nenhuma produção registrada.

Repositórios mockados; o de venda captura a entidade passada a `save`.

**Passos**
1. Registrar venda com dois itens de P1 em `"kg"`: A, 3,333 × 4,50; B, 2,335 × 3,00.
2. Registrar venda com um item de P1 em `" KG "`: 4,000 × 2,50.
3. Registrar venda com dois itens de P2: 12,500 × 3,10 em `"kg"` e 1,000 × 3,10 em `"Kg"`.

**Resultado esperado**
- Passo 1: nenhuma exceção. Item A com `valor_total` **15,00** (14,9985 arredondado); item B com **7,01** (7,005 arredondado); venda com `valor_total` **22,01**.
- Passo 2: nenhuma exceção; item com 10,00.
- Passo 3: nenhuma exceção — sem produção, vale a unidade da cultura. Itens com 38,75 e 3,10; venda com 41,85.

Valores que indicam defeito no passo 1:
- venda 22,00 — arredondou só a soma dos valores brutos (22,0035), ou usou `HALF_EVEN` nos itens (item B 7,00)
- venda 21,99 — truncou os itens (`DOWN`: 14,99 e 7,00)

### CT049 — Item em unidade diferente é rejeitado

| Campo | Conteúdo |
|---|---|
| Regra | RN30 |
| Requisito | RF31 |
| Tipo | Unitário |
| Prioridade | P3 |
| Natureza | Negativo |

**Pré-condição**
Os mesmos P1 (produção em `"kg"`) e P2 (sem produção, cultura em `kg`) do CT048. Repositórios mockados.

**Passos**
1. Registrar venda com um item de P1 em `"saca"`, 2,000 × 60,00.
2. Registrar venda com um item de P1 em `"kgs"` e, separadamente, outra em `"k g"`.
3. Registrar venda com um item de P2 em `"saca"`.
4. Registrar venda com dois itens de P1: o primeiro em `"kg"`, o segundo em `"saca"`.

**Resultado esperado**
- Passos 1, 2 e 4: `RegraNegocioException` com a mensagem proposta "A unidade do item (saca) é diferente da unidade da produção registrada no plantio (kg)." — com a unidade informada no lugar de "saca". Na API: **422**, `erro: "REGRA_NEGOCIO"`.
- Passo 3: `RegraNegocioException` com a mensagem proposta "A unidade do item (saca) é diferente da unidade da cultura do plantio (kg)." Na API: **422**, `erro: "REGRA_NEGOCIO"`.
- Nenhum passo chama `save` no repositório de venda (`verify(vendaRepository, never()).save(any())`). No passo 4, o item em `"kg"` não é gravado sozinho.

O passo 2 garante que a normalização é só de espaço nas pontas e de caixa: semelhança de grafia não basta.

---

## Pontos para revisar

1. **RN26 como 400.** Lista de itens vazia sai como 400 `VALIDACAO`, porque o contrato da F13 pôs `@NotEmpty` em `itens`. A RN06 da Q00b, que é a mesma situação (lista vazia de plantios), sai como 422 porque o DTO de despesa não tem `@NotEmpty`. As duas são válidas pela 2.4, mas o front recebe dois formatos para o mesmo tipo de erro. A mensagem padrão do Bean Validation ("não deve estar vazio") tampouco é clara para o operador (RNF03). Decidir se fica assim ou se a venda passa ao mesmo tratamento da despesa.
2. **404 sem indicar o item.** Plantio inexistente sai como 404 com `campos` vazio, então o front não sabe qual linha marcar. Se a L09b quiser apontar o item, a alternativa é 422 com `itens[n].plantioId` em `campos`; isso muda o CT044, passo 4.
3. **Plantio de outro produtor.** Nenhuma regra impede item de venda do produtor A apontando para plantio do produtor B. Isso lança receita no plantio errado. A L04a exige plantios "do mesmo produtor" para despesa; vale a mesma regra na venda (422), mas é regra nova e não tem CT na matriz.
4. **Unidade gravada.** A comparação ignora caixa e espaços, mas não está decidido se `item_venda.unidade_medida` grava o texto informado (`" KG "`) ou a grafia da referência (`"kg"`). O CT048 não verifica isso. Recomendação: gravar a grafia da referência, para o saldo e o relatório não mostrarem três grafias.
5. **Produção em mais de uma unidade.** A RN30 supõe uma única unidade de produção por plantio. Se a L01 aceitar produções do mesmo plantio em unidades diferentes, a referência fica ambígua. Mesma lacuna do ponto 4 da Q00d.
6. **Arredondamento na RN30.** A L09b pediu que o modo de arredondamento fosse registrado na RN30, e assim foi feito, embora não seja coerência de unidade. Numa revisão das regras, pode virar regra própria.
7. **`avisos` na consulta.** O CT047 verifica `avisos` na resposta do POST. Não está decidido se `GET /api/v1/vendas/{id}` recalcula os avisos ou devolve lista vazia.
8. **Mensagens de erro.** As mensagens de CT044, CT047 e CT049 são propostas. O teste verifica `status`, `erro` e `campos`; a mensagem exata é confirmada na L09b.
