# AgroGestão — Casos de Teste: Relatório e Datas (CT050–CT059)

Versão 1.0

Especificação dos casos de relatório, datas dos lançamentos e consentimento no padrão da QA 3.3. Destrava A03a, A03b, L00, Q02, Q03, Q06 e Q07.

**Documentos relacionados**

- `agrogestao-fase-1-regras-negocio.md` — RN11, RN19, RN24 e RN31 a RN37 (RN24 e RN31 completadas na versão 1.6)
- `agrogestao-fase-1-requisitos.md` — RF17, RF40 a RF47 e RF50
- `agrogestao-fase-1-glossario.md` — seção 4 (resultado e "Lucro") e seção 5 (relatório, data do fato, data de coleta, valor declarado)
- `agrogestao-fase-3-qa.md` — 3.2 (matriz, casos críticos CT051, CT052 e CT053, e a solução da Q07 para os P1 de E2E) e 3.3 (padrão)
- `agrogestao-fase-2-der.md` — tabelas `produtor` (`consentimento_em`) e `emissao_relatorio` (`conteudo JSONB`, `ck_emissao_periodo`)
- `agrogestao-fase-2-padroes-tecnicos.md` — 2.4, contrato de relatórios, códigos de status e formato de erro
- `api/src/main/java/com/agrogestao/api/dto/` — `EmissaoRelatorioRequisicao`, `EmissaoRelatorioResposta`, `ConteudoRelatorioResposta`, `ProdutorResposta` e `ProdutorRequisicao`, contrato da F13
- `casos-de-teste-plantio.md` (CT004, fuso), `casos-de-teste-despesa.md` (CT020, estorno) e `casos-de-teste-apuracao.md` (convenções de apuração)

---

## Visão geral

| Caso | Título | Regra | Requisito | Tipo | Prior. | Natureza | Issue |
|---|---|---|---|---|---|---|---|
| CT050 | Relatório emitido traz o conteúdo mínimo | RN31 | RF40 | E2E | P2 | Positivo | Q06 |
| CT051 | Relatório traz a ressalva de valor declarado | RN32 | RF42 | E2E | **P1** | Positivo | Q06, Q07 |
| CT052 | Relatório traz a ressalva de finalidade | RN33 | RF43 | E2E | **P1** | Positivo | Q06, Q07 |
| CT053 | Reemissão cria nova emissão e preserva a anterior | RN34 | RF45 | Integração | **P1** | Positivo | A03a |
| CT054 | Emissão gravada não é alterada, apagada nem montada pelo cliente | RN34 | RF45 | Integração | **P1** | Negativo | A03a |
| CT055 | Lançamento com data do fato hoje é aceito | RN35 | RF17 | API | P3 | Positivo | Q02 |
| CT056 | Lançamento com data do fato futura é rejeitado | RN35 | RF17 | API | P3 | Negativo | Q02 |
| CT057 | Data de coleta é atribuída pelo sistema | RN36 | RF17 | Integração | P2 | Positivo | L00 |
| CT058 | Sem consentimento lança; com consentimento emite | RN37 | RF50 | API | P2 | Positivo | Q03 |
| CT059 | Emissão sem consentimento é bloqueada | RN37 | RF50 | API | P2 | Negativo | Q03 |

CT051 e CT052 são E2E na matriz e rodam na Q06, antes da entrega. Pela solução registrada na QA 3.2 (Q07), os dois têm também verificação em nível de integração, que roda a cada PR; por isso a coluna Issue cita as duas.

**Sobre a regra do par.** RN34 (CT053 positivo, CT054 negativo), RN35 (CT055 positivo, CT056 negativo) e RN37 (CT058 positivo, CT059 negativo) têm par. RN31, RN32 e RN33 são obrigações do conteúdo: não há entrada do operador que produza um relatório sem elas, então não existe violação a rejeitar. O negativo de cada uma é o relatório incompleto, e os resultados esperados o excluem campo a campo e com o texto exato das ressalvas, inclusive na emissão de período sem lançamentos, que é onde um relatório "vazio" tenderia a perder partes. RN36 descreve uma atribuição do sistema: o valor enviado pelo cliente não é rejeitado, é ignorado (critério de pronto da L00), e o CT057 exclui o valor errado com data concreta (2026-10-11, não 2020-01-01 nem 2026-10-12).

---

## Convenções

| Item | Decisão | Por quê |
|---|---|---|
| Período de cada plantio no relatório | O período informado na emissão (`periodoInicio` a `periodoFim`) vale para cada plantio do produtor, qualquer que seja o ciclo. A RN24 vale para a consulta de resultado de um plantio, não para o relatório | Decisão de 2026-10-26 na A03a (#66), registrada na RN24 e na RN31 (regras v1.6) |
| Período sem nenhum lançamento | Emite: **201**, com receita, custo e resultado 0,00, listas de despesas e diárias vazias e as duas ressalvas | Decisão de 2026-10-26 na A03a (#66), registrada na RN31 (regras v1.6). Coerente com a RN25: o resultado é sempre apresentado |
| Apuração dentro do relatório | As convenções da Q00d: `valor_rateado` gravado, despesa estornada fora, diária fora do desembolso, bordas inclusivas | `casos-de-teste-apuracao.md`; RN19 a RN23 |
| Conteúdo | Gravado em `emissao_relatorio.conteudo` no formato de `ConteudoRelatorioResposta`. Data de emissão, período e operador ficam nas colunas `data_emissao`, `periodo_inicio`, `periodo_fim` e `usuario_id` e saem em `EmissaoRelatorioResposta` | DER; contrato da F13 |
| Texto das ressalvas | `ressalvaValorDeclarado` = "Valores declarados pelo produtor, não comprovados por documento fiscal." e `ressalvaFinalidade` = "Este documento não constitui garantia de aceitação para qualquer finalidade externa." Comparação exata | Textos do contrato da F13 (`ConteudoRelatorioResposta`). Ver ponto 2 |
| "Lucro" | No documento impresso, o resultado tem o rótulo "Lucro"; na tela, "Resultado" | RF47; glossário, seção 4 |
| Reemissão | Nova linha em `emissao_relatorio`; a anterior não muda em nada | RN34 |
| Alterar ou apagar emissão pela API | Não existe `PUT` nem `DELETE` em `/api/v1/relatorios/{id}`: **405**, com o corpo no formato único (`status` 405) | Contrato da F13; `ApiExceptionHandler.handleExceptionInternal`. Ver ponto 4 |
| Conteúdo enviado pelo cliente | Ignorado: o corpo da emissão só tem `produtorId`, `periodoInicio` e `periodoFim`; o resto é apurado pela API | Padrões 2.4 ("a emissão não aceita conteúdo apurado do cliente") |
| Data do fato futura | **422** `REGRA_NEGOCIO` em produção, despesa, diária e venda | RN35; mesma linha da RN02 na Q00a |
| "Hoje" | `LocalDate.now(clock)` com `Clock` injetável no fuso `America/Fortaleza`. Nos testes de API, o próprio teste calcula "hoje" e "amanhã" em `America/Fortaleza`, nunca pelo relógio da máquina que roda o teste | Q00a. O CI roda em UTC |
| Data de coleta enviada no corpo | Ignorada; a API grava a data do servidor em Fortaleza. Os DTOs de entrada não têm `dataColeta`, e o Jackson do Spring Boot ignora campo desconhecido | RN36; critério de pronto da L00 |
| Data de coleta na alteração | Não muda: continua a da gravação original | RN36 ("data em que o lançamento foi gravado") |
| Emissão sem consentimento | **422** `REGRA_NEGOCIO`, nada gravado em `emissao_relatorio` | RN37; critério de pronto da A03a |
| Sinalização do consentimento pendente | `ProdutorResposta.consentimentoPendente = true` (`GET /api/v1/produtores/{id}`). Os DTOs de resposta de produção, despesa e diária não têm campo para isso | Contrato da F13. Ver ponto 6 |
| Consentimento futuro | **400** `VALIDACAO`, com `consentimentoEm` em `campos` | `ProdutorRequisicao.consentimentoEm` é `@PastOrPresent` |
| Datas nos casos | Datas do fato de 2026 já passadas; períodos de emissão fixos | Não dependem do dia em que rodam |
| Mensagens | As citadas são propostas, salvo os textos das ressalvas. O teste verifica `status`, `erro` e `campos` | Mesma linha da Q00a e da Q00b |

---

## Massa de dados base

Sintética, conforme `RNF10`. Criada pela API (ou por fixture, nos casos de integração) em cada teste e removida no final.

| Item | Valor |
|---|---|
| Usuário | operador de teste, perfil `LANCAMENTO` |
| Produtor com consentimento | "Produtor Teste Relatório", CPF `99900001311`, endereço "Endereço Fictício Teste, zona rural", profissão "Agricultor", contato "Contato Fictício Teste", `consentimento_em` 2026-01-15 |
| Produtor sem consentimento | "Produtor Teste Sem Consentimento", CPF `99900001400`, `consentimento_em` nulo |
| Produtor do CT058 | "Produtor Teste Emissão", CPF `99900001583`, `consentimento_em` 2026-01-15 |
| Propriedade | "Sítio Teste", área total 10,000 ha, de cada produtor |
| Culturas | Milho (`LONGO`, `kg`) e Hortaliça (`CURTO`, `kg`), do catálogo da migration |
| Plantios | Criados por caso, `data_inicio` 2026-02-01, abertos salvo indicação |
| Categorias | "Sementes e mudas", "Adubo e fertilizantes" e "Diária" (sistema), da migration |
| Cliente | "Cliente Teste Relatório", canal `FEIRA` |
| Valor de referência em 2026 | 80,00, vigente de 2026-01-01 em diante — já gravado pela migration de dados iniciais (ver ponto 8 da Q00c) |
| Período de emissão padrão | 2026-01-01 a 2026-06-30 |

---

## RN31 — Conteúdo mínimo do relatório

### CT050 — Relatório emitido traz o conteúdo mínimo

| Campo | Conteúdo |
|---|---|
| Regra | RN31 |
| Requisito | RF40 |
| Tipo | E2E |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
Criados pela API na fixture do spec, para o "Produtor Teste Relatório":
- PM: Milho `LONGO`, 1,000 ha. PH: Hortaliça `CURTO`, 0,500 ha. Os dois abertos, início 2026-02-01.
- Despesa "Sementes e mudas" de R$ 450,00 em 2026-02-08, só em PM.
- Despesa "Adubo e fertilizantes" de R$ 100,00 em 2026-03-10, em PM e PH: rateio gravado PM 0,666667 / 66,67 e PH 0,333333 / 33,33.
- Diária paga de 2,00 × R$ 90,00 = R$ 180,00 em PM, 2026-03-12 (gera a despesa da categoria "Diária").
- Diária familiar de 1,00 dia em PM, 2026-03-15 (referência de 80,00).
- Produções: 500,000 kg em PM em 2026-06-20; 30,000 kg em PH em 2026-04-10.
- Vendas ao "Cliente Teste Relatório": 200,000 kg de PM a R$ 4,50 em 2026-06-25; 30,000 kg de PH a R$ 3,00 em 2026-04-12.
- Despesa "Sementes e mudas" de R$ 40,00 em 2026-07-05, só em PM (fora do período).

**Passos**
1. Entrar como operador, abrir o detalhe do "Produtor Teste Relatório" e emitir o relatório de 01/01/2026 a 30/06/2026.
2. Abrir a emissão no histórico e ir para a versão de impressão.

**Resultado esperado**
No documento de impressão, cada item abaixo aparece, com o valor indicado:

| Item da RN31 / RF40–RF44 | Campo de origem | Valor esperado |
|---|---|---|
| Nome do produtor | `conteudo.produtor.nome` | Produtor Teste Relatório |
| Documento | `conteudo.produtor.documento` | 999.000.013-11 (formatado pela interface) |
| Endereço | `conteudo.produtor.endereco` | Endereço Fictício Teste, zona rural |
| Profissão | `conteudo.produtor.profissao` | Agricultor |
| Propriedade | `conteudo.propriedades[].nome` | Sítio Teste |
| Período coberto | `periodoInicio`, `periodoFim` da emissão | 01/01/2026 a 30/06/2026 |
| Data de emissão | `dataEmissao` da emissão | a data do dia da emissão, em dd/mm/aaaa no fuso de Fortaleza |
| Culturas produzidas | `conteudo.plantios[].culturaNome` | Milho e Hortaliça |
| Quantidade produzida, por plantio | `conteudo.plantios[].quantidadeProduzida` | Milho 500 kg; Hortaliça 30 kg |
| Quantidade vendida, por plantio | `conteudo.plantios[].quantidadeVendida` | Milho 200 kg; Hortaliça 30 kg |
| Despesas | `conteudo.despesas[]` | três: R$ 450,00, R$ 100,00 e R$ 180,00 (a da diária paga) |
| Diárias | `conteudo.diarias[]` | duas: paga R$ 180,00 e familiar R$ 80,00 |
| Receita | `conteudo.receita` | R$ 990,00 |
| Custo de desembolso | `conteudo.custoDesembolso` | R$ 730,00 |
| Mão de obra familiar, como informação complementar | `conteudo.valorMaoObraFamiliar` | R$ 80,00 |
| Resultado, com o rótulo **"Lucro"** | `conteudo.resultado` | R$ 260,00 |
| Resultado por plantio, rótulo "Lucro" | `conteudo.plantios[].resultado` | Milho R$ 203,33; Hortaliça R$ 56,67 |
| Ressalva de valor declarado | `conteudo.ressalvaValorDeclarado` | texto exato da convenção |
| Ressalva de finalidade | `conteudo.ressalvaFinalidade` | texto exato da convenção |
| Espaço para timbre e assinaturas | leiaute da A04 | timbre no topo; assinaturas do presidente e do agricultor no rodapé |

Valores que indicam defeito:
- custo de desembolso R$ 770,00 — entrou a despesa de 05/07, fora do período
- custo de desembolso R$ 910,00 — a diária paga foi somada além da despesa que ela gerou (RN20)
- quantidade produzida e vendida num campo só, ou a vendida no lugar da produzida (P15)
- o rótulo "Resultado" no documento impresso, ou "Lucro" na tela

PH é de ciclo curto e mesmo assim aparece com o período inteiro da emissão (decisão de 2026-10-26): resultado R$ 56,67 só sai se PH for apurado de janeiro a junho.

---

## RN32 — Ressalva de valor declarado

### CT051 — Relatório traz a ressalva de valor declarado

| Campo | Conteúdo |
|---|---|
| Regra | RN32 |
| Requisito | RF42 |
| Tipo | E2E |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
- Parte E2E (Q06): a mesma massa do CT050.
- Parte de integração (Q07, roda a cada PR): banco efêmero com o "Produtor Teste Relatório", um plantio de Milho com uma despesa de R$ 300,00 em 2026-03-10, e nada mais.

**Passos**
1. E2E: emitir o relatório do produtor de 01/01/2026 a 30/06/2026 e abrir a versão de impressão.
2. Integração: `POST /api/v1/relatorios` com `{"produtorId": <produtor>, "periodoInicio": "2026-01-01", "periodoFim": "2026-06-30"}`.
3. Integração: `POST /api/v1/relatorios` para o mesmo produtor, de 2026-09-01 a 2026-09-30 (período sem lançamentos).
4. Integração: `GET /api/v1/relatorios/{id}` das duas emissões e ler por SQL `conteudo->>'ressalvaValorDeclarado'` das duas linhas.

**Resultado esperado**
- Passo 1: o documento mostra, visível na página impressa, o texto "Valores declarados pelo produtor, não comprovados por documento fiscal."
- Passos 2 e 3: **201**; `conteudo.ressalvaValorDeclarado` igual ao texto acima, caractere a caractere.
- Passo 4: o mesmo texto nas duas respostas e nas duas linhas de `emissao_relatorio`.

A parte de integração é a que falha o build se a ressalva sumir do conteúdo gravado; a parte E2E é a que pega a página de impressão que deixa de mostrar o texto. Uma não substitui a outra: a página que escrevesse o texto fixo passaria no E2E mesmo com o conteúdo gravado sem a ressalva.

---

## RN33 — Ressalva de finalidade

### CT052 — Relatório traz a ressalva de finalidade

| Campo | Conteúdo |
|---|---|
| Regra | RN33 |
| Requisito | RF43 |
| Tipo | E2E |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
As mesmas do CT051: massa do CT050 na parte E2E; produtor com uma despesa de R$ 300,00 em 2026-03-10 na parte de integração.

**Passos**
1. E2E: emitir o relatório do produtor de 01/01/2026 a 30/06/2026 e abrir a versão de impressão.
2. Integração: `POST /api/v1/relatorios` de 2026-01-01 a 2026-06-30.
3. Integração: `POST /api/v1/relatorios` de 2026-09-01 a 2026-09-30 (período sem lançamentos).
4. Integração: `GET /api/v1/relatorios/{id}` das duas emissões e ler por SQL `conteudo->>'ressalvaFinalidade'` das duas linhas.

**Resultado esperado**
- Passo 1: o documento mostra o texto "Este documento não constitui garantia de aceitação para qualquer finalidade externa."
- Passos 2 e 3: **201**; `conteudo.ressalvaFinalidade` igual ao texto acima, caractere a caractere.
- Passo 4: o mesmo texto nas duas respostas e nas duas linhas.

CT051 e CT052 podem ser o mesmo teste de integração com duas asserções, desde que cada uma diga qual CT verifica (no nome do teste ou na mensagem da asserção), para a falha apontar a ressalva certa.

---

## RN34 — Imutabilidade da emissão

### CT053 — Reemissão cria nova emissão e preserva a anterior

| Campo | Conteúdo |
|---|---|
| Regra | RN34 |
| Requisito | RF45 |
| Tipo | Integração |
| Prioridade | **P1** |
| Natureza | Positivo |

**Pré-condição**
Banco efêmero. "Produtor Teste Relatório" com o plantio P1 (Milho, aberto), uma despesa D1 de R$ 300,00 em 2026-03-10 só em P1, e uma venda de 100,000 kg × R$ 5,00 = R$ 500,00 em 2026-03-20.

**Passos**
1. `POST /api/v1/relatorios` de 2026-01-01 a 2026-06-30 → emissão E1. Guardar a linha de E1: `conteudo::text`, `data_emissao`, `periodo_inicio`, `periodo_fim`, `usuario_id` e `atualizado_em`.
2. `PUT /api/v1/despesas/{D1}` alterando o valor para R$ 350,00.
3. `POST /api/v1/relatorios` com o mesmo produtor e o mesmo período → emissão E2.
4. Ler as linhas de E1 e E2 e `GET /api/v1/produtores/{produtor}/relatorios`.
5. `POST /api/v1/despesas/{D1}/estorno` e emitir de novo o mesmo período → E3.
6. Ler as linhas de E1, E2 e E3.

**Resultado esperado**
- Passo 1: **201**; E1 com `conteudo.custoDesembolso` 300.00, `receita` 500.00, `resultado` 200.00.
- Passo 3: **201**; E2 com id diferente de E1, `custoDesembolso` 350.00, `resultado` 150.00.
- Passo 4: E1 com `conteudo::text`, `data_emissao`, `periodo_inicio`, `periodo_fim`, `usuario_id` e `atualizado_em` idênticos aos guardados no passo 1. A listagem traz as duas emissões.
- Passo 5: **201**; E3 com `custoDesembolso` 0.00 e `resultado` 500.00 (a estornada sai das apurações novas, RN19).
- Passo 6: E1 continua com 300,00 e E2 com 350,00, os dois byte a byte iguais ao que foram gravados.

Comparar `conteudo::text` byte a byte, e não só os totais, pega a implementação que regrava a emissão antiga com os mesmos números (o `atualizado_em` mudaria) ou que guarda uma referência aos lançamentos em vez do apurado.

### CT054 — Emissão gravada não é alterada, apagada nem montada pelo cliente

| Campo | Conteúdo |
|---|---|
| Regra | RN34 |
| Requisito | RF45 |
| Tipo | Integração |
| Prioridade | **P1** |
| Natureza | Negativo |

**Pré-condição**
A massa e a emissão E1 do passo 1 do CT053, com a linha guardada.

**Passos**
1. `PUT /api/v1/relatorios/{E1}` com um corpo JSON qualquer.
2. `DELETE /api/v1/relatorios/{E1}`.
3. `POST /api/v1/relatorios` com campos que o contrato não tem:
   ```json
   {"produtorId": <produtor>, "periodoInicio": "2026-01-01", "periodoFim": "2026-06-30",
    "dataEmissao": "2020-01-01T00:00:00Z",
    "conteudo": {"resultado": 99999.99, "ressalvaValorDeclarado": "", "ressalvaFinalidade": ""}}
   ```
4. `DELETE /api/v1/produtores/{produtor}`.
5. Ler a linha de E1 e contar as emissões do produtor.

**Resultado esperado**
- Passos 1 e 2: **405**, corpo no formato único com `status` 405.
- Passo 3: **201** com o conteúdo apurado pela API: `resultado` 200.00, as duas ressalvas com o texto exato e `dataEmissao` no dia da execução, não em 2020.
- Passo 4: **422**, `erro: "REGRA_NEGOCIO"` — o produtor com emissão não é excluído (regra já implementada no `ProdutorService`).
- Passo 5: E1 idêntica à guardada; duas emissões no produtor (E1 e a do passo 3).

O DER não prevê proteção no banco contra `UPDATE` ou `DELETE` direto em `emissao_relatorio` (não há gatilho nem restrição para isso), então este caso não tem passo de SQL direto. Ver ponto 3.

---

## RN35 — Data do fato não futura

Os dois casos rodam contra a API real. O teste calcula `HOJE` e `AMANHA` no fuso `America/Fortaleza` no início de cada caso.

### CT055 — Lançamento com data do fato hoje é aceito

| Campo | Conteúdo |
|---|---|
| Regra | RN35 |
| Requisito | RF17 |
| Tipo | API |
| Prioridade | P3 |
| Natureza | Positivo |

**Pré-condição**
"Produtor Teste Relatório", plantio P1 (Milho, aberto, início 2026-02-01) e cliente da massa base.

**Passos**
1. `POST /api/v1/plantios/{P1}/producoes` com `{"quantidade": 10.000, "unidadeMedida": "kg", "dataFato": HOJE}`.
2. `POST /api/v1/despesas` com `{"produtorId": <produtor>, "categoriaDespesaId": <Adubo e fertilizantes>, "descricao": "Adubo de teste", "valorTotal": 50.00, "dataFato": HOJE, "plantioIds": [<P1>]}`.
3. `POST /api/v1/plantios/{P1}/diarias` com `{"natureza": "PAGA", "quantidadeDias": 1.00, "valorUnitario": 90.00, "dataFato": HOJE}`.
4. `POST /api/v1/vendas` com `{"produtorId": <produtor>, "clienteId": <cliente>, "dataFato": HOJE, "itens": [{"plantioId": <P1>, "quantidade": 5.000, "unidadeMedida": "kg", "valorUnitario": 4.50}]}`.
5. Repetir os passos 1 a 4 com `dataFato` = 2026-03-10.

**Resultado esperado**
- Passos 1 a 4: **201**, com `dataFato` = HOJE e `dataColeta` = HOJE. Valores: diária com `valorTotal` 90.00; venda com `valorTotal` 22.50.
- Passo 5: **201** nos quatro, com `dataFato` = `"2026-03-10"` e `dataColeta` = HOJE.

**Borda a cobrir no mesmo teste (nível de serviço, L00):** com `Clock.fixed(2026-10-12T01:30:00Z, America/Fortaleza)` — 22h30 de 11/10 em Fortaleza, já 12/10 em UTC —, o validador de data aceita 2026-10-11. É a mesma armadilha do CT004: o teste de API não controla o relógio do servidor, então o horário de borda só é determinístico no unitário do validador.

### CT056 — Lançamento com data do fato futura é rejeitado

| Campo | Conteúdo |
|---|---|
| Regra | RN35 |
| Requisito | RF17 |
| Tipo | API |
| Prioridade | P3 |
| Natureza | Negativo |

**Pré-condição**
A mesma do CT055. Contar as linhas de `producao`, `despesa`, `diaria`, `venda` e `item_venda` do produtor antes dos passos.

**Passos**
1. Repetir os quatro `POST` do CT055 com `dataFato` = AMANHA.
2. Repetir os quatro com `dataFato` = 2099-01-01.
3. Contar de novo as linhas.

**Resultado esperado**
- Passos 1 e 2, nos quatro tipos: **422**, `erro: "REGRA_NEGOCIO"`. Mensagem proposta: "A data do fato não pode ser posterior a hoje."
- Passo 3: nenhuma linha nova, em nenhuma das cinco tabelas. A diária paga recusada não deixou despesa gerada.

**Borda a cobrir no mesmo teste (nível de serviço, L00):** com `Clock.fixed(2026-10-12T01:30:00Z, America/Fortaleza)`, o validador rejeita 2026-10-12. Com o "hoje" calculado em UTC, essa data passaria.

---

## RN36 — Data de coleta atribuída pelo sistema

### CT057 — Data de coleta é atribuída pelo sistema

| Campo | Conteúdo |
|---|---|
| Regra | RN36 |
| Requisito | RF17 |
| Tipo | Integração |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
Banco efêmero com o "Produtor Teste Relatório" e o plantio P1 (Milho, aberto, início 2026-02-01). O bean `Clock` da aplicação é substituído no teste por `Clock.fixed(2026-10-12T01:30:00Z, America/Fortaleza)` — 22h30 de 11/10 em Fortaleza.

**Passos**
1. `POST /api/v1/plantios/{P1}/producoes` com `{"quantidade": 10.000, "unidadeMedida": "kg", "dataFato": "2026-03-10", "dataColeta": "2020-01-01"}`.
2. Ler `data_coleta` da produção por SQL.
3. Trocar o relógio para `Clock.fixed(2026-10-20T12:00:00-03:00, America/Fortaleza)` e enviar `PUT /api/v1/producoes/{id}` com `{"quantidade": 12.000, "unidadeMedida": "kg", "dataFato": "2026-03-12", "dataColeta": "2026-10-01"}`.
4. Ler `data_fato` e `data_coleta` da produção.

**Resultado esperado**
- Passo 1: **201**, `dataColeta` = `"2026-10-11"`.
- Passo 2: `data_coleta` = 2026-10-11. Falha: 2020-01-01 (aceitou o valor do cliente) ou 2026-10-12 (data em UTC).
- Passo 3: **200**, `dataColeta` = `"2026-10-11"`.
- Passo 4: `data_fato` = 2026-03-12; `data_coleta` continua 2026-10-11. Falha: 2026-10-20 (a alteração regravou a coleta) ou 2026-10-01.

Cada API de lançamento que usar a base da L00 repete os passos com o seu recurso: despesa na L04a, diária na L07 (a despesa gerada fica com a mesma `data_coleta` da diária) e venda na L09b.

---

## RN37 — Consentimento como condição para emissão

### CT058 — Sem consentimento lança; com consentimento emite

| Campo | Conteúdo |
|---|---|
| Regra | RN37 |
| Requisito | RF50 |
| Tipo | API |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
- "Produtor Teste Sem Consentimento" (B), com o plantio PB (Milho, aberto, início 2026-02-01).
- "Produtor Teste Emissão" (A), com consentimento em 2026-01-15, o plantio PA (Milho, aberto, início 2026-02-01), uma despesa de R$ 120,00 em 2026-03-10 só em PA e uma venda de 10,000 kg × R$ 5,00 = R$ 50,00 em 2026-03-20.
- Cliente da massa base.

**Passos**
1. `GET /api/v1/produtores/{B}`.
2. Lançar para B, com data do fato 2026-03-10: produção de 10,000 kg em PB; despesa de R$ 50,00 em PB; diária paga de 1,00 × R$ 90,00 em PB; venda de 5,000 kg × R$ 4,50 de PB.
3. `POST /api/v1/relatorios` com `{"produtorId": <A>, "periodoInicio": "2026-01-01", "periodoFim": "2026-06-30"}`.
4. `POST /api/v1/relatorios` para A, de 2026-09-01 a 2026-09-30 (sem lançamentos).

**Resultado esperado**
- Passo 1: **200**, `consentimentoEm` = `null`, `consentimentoPendente` = `true`.
- Passo 2: **201** nos quatro lançamentos; a diária com `valorTotal` 90.00 e a venda com `valorTotal` 22.50.
- Passo 3: **201**, `Location` terminando em `/api/v1/relatorios/{id}`. Corpo: `produtorId` = A, `usuarioId` = operador de teste, `periodoInicio` `"2026-01-01"`, `periodoFim` `"2026-06-30"`, `dataEmissao` preenchida. Em `conteudo`: `produtor.consentimentoEm` = `"2026-01-15"`, `produtor.consentimentoPendente` = `false`, `receita` 50.00, `custoDesembolso` 120.00, `resultado` **−70.00**, as duas ressalvas com o texto exato.
- Passo 4: **201**. Em `conteudo`: `receita` 0.00, `custoDesembolso` 0.00, `resultado` 0.00, `valorMaoObraFamiliar` 0.00, `despesas` = `[]`, `diarias` = `[]`, `plantios` com PA e valores 0,00, e as duas ressalvas.

O passo 2 garante que o bloqueio ficou na emissão, e não no lançamento — a alternativa descartada na RN37.

### CT059 — Emissão sem consentimento é bloqueada

| Campo | Conteúdo |
|---|---|
| Regra | RN37 |
| Requisito | RF50 |
| Tipo | API |
| Prioridade | P2 |
| Natureza | Negativo |

**Pré-condição**
O produtor B do CT058, sem consentimento, com os lançamentos do passo 2 daquele caso. Nenhuma emissão de B.

**Passos**
1. `POST /api/v1/relatorios` com `{"produtorId": <B>, "periodoInicio": "2026-01-01", "periodoFim": "2026-06-30"}`.
2. Contar as emissões de B.
3. `PUT /api/v1/produtores/{B}` com o cadastro de B e `consentimentoEm` = AMANHA (calculado em `America/Fortaleza`).
4. `PUT /api/v1/produtores/{B}` com `consentimentoEm` = `"2026-10-01"` e repetir o passo 1.
5. `PUT /api/v1/produtores/{B}` com `consentimentoEm` = `null` e repetir o passo 1.
6. Contar as emissões de B.

**Resultado esperado**
- Passo 1: **422**, `erro: "REGRA_NEGOCIO"`. Mensagem proposta: "O produtor ainda não tem consentimento registrado. Registre o consentimento antes de emitir o relatório."
- Passo 2: zero emissões.
- Passo 3: **400**, `erro: "VALIDACAO"`, com `consentimentoEm` em `campos` (consentimento não pode ser futuro).
- Passo 4: o `PUT` responde **200** com `consentimentoPendente` = `false`; a emissão responde **201**.
- Passo 5: o `PUT` responde **200** com `consentimentoPendente` = `true`; a emissão volta a responder **422**.
- Passo 6: uma emissão — só a do passo 4.

O passo 5 pega a implementação que olha "já teve consentimento" (por exemplo, uma emissão anterior) em vez do `consentimento_em` atual.

---

## Pontos para revisar

1. **Período do relatório × ciclo.** A decisão da A03a apura cada plantio no período informado, inclusive os de ciclo curto num semestre inteiro. Não está decidido se entram no relatório plantios que começaram depois do fim do período ou terminaram antes do início. Os casos daqui usam só plantios ativos no período.
2. **Textos das ressalvas só no Swagger.** Os dois textos existem hoje apenas como exemplo em `@Schema` de `ConteudoRelatorioResposta`. A A03a precisa transformá-los em constantes (e a G07 validar com o sindicato); se o texto mudar, CT051 e CT052 mudam junto.
3. **Proteção no banco.** O DER não prevê gatilho ou restrição que impeça `UPDATE` ou `DELETE` direto em `emissao_relatorio`. Para um documento com uso probatório, vale avaliar um gatilho que recuse as duas operações, ou revogar esses privilégios do usuário da aplicação. Hoje a imutabilidade depende só da API não oferecer a operação.
4. **405 com `erro: "VALIDACAO"`.** Verbo HTTP não suportado responde 405 com `erro` = `"VALIDACAO"`, porque o `ApiExceptionHandler` usa esse código para qualquer erro do Spring que não seja 404 nem 5xx. O status está certo; o código de erro confunde. O CT054 verifica só o status.
5. **Data de emissão e período fora do `conteudo`.** A RN31 lista data de emissão e período coberto como conteúdo mínimo; no contrato, eles ficam em colunas próprias da emissão, não no JSONB. Funciona, porque nenhuma das colunas é alterada, mas a imutabilidade dessas colunas depende do ponto 3.
6. **Sinalização do consentimento pendente no lançamento.** A RN37 manda sinalizar a pendência no lançamento. O contrato só tem `consentimentoPendente` em `ProdutorResposta`; as respostas de produção, despesa e diária não trazem nada, e a venda tem `avisos` sem definição para esse caso. O CT058 verifica a sinalização no produtor. Se a sinalização tiver de vir na resposta do lançamento, é mudança de contrato (aviso ao front, conforme a issue #17).
7. **Horário de borda nos testes de API.** O CT055 e o CT056 não controlam o relógio do servidor; o caso das 22h30 fica no unitário do validador da L00. Há ainda uma janela mínima de instabilidade se o teste calcular HOJE às 23h59min59s e a requisição chegar depois da meia-noite em Fortaleza.
8. **`Clock` ainda não existe.** A convenção de relógio injetável (Q00a) ainda não tem bean na API. O CT057 depende de a L00 criá-lo, para o teste poder substituí-lo.
9. **Alterar despesa em período emitido.** O CT053 altera uma despesa de período já emitido, como a QA 3.2 descreve. A RN11 só proíbe a exclusão; se a A03b decidir bloquear também a alteração, o passo 2 do CT053 passa a usar o estorno.
10. **Mensagens de erro.** As mensagens de CT056 e CT059 são propostas. O teste verifica `status`, `erro` e `campos`; a mensagem exata é confirmada na L00 e na A03a.
