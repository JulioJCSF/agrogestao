# AgroGestão — Casos de Teste: Plantio (CT001–CT008)

Versão 1.0

Especificação dos casos de plantio no padrão da QA 3.3. Destrava C06a, C06b e L00.

**Documentos relacionados**

- `agrogestao-fase-1-regras-negocio.md` — RN01 a RN04
- `agrogestao-fase-3-qa.md` — 3.2 (matriz) e 3.3 (padrão)
- `agrogestao-fase-2-der.md` — tabela `plantio` (`ck_plantio_area`, `ck_plantio_encerra`)
- `agrogestao-fase-2-padroes-tecnicos.md` — 2.4, formato de erro

---

## Visão geral

| Caso | Título | Regra | Requisito | Tipo | Prior. | Natureza | Issue |
|---|---|---|---|---|---|---|---|
| CT001 | Plantio com área positiva é registrado | RN01 | RF11 | Integração | P3 | Positivo | C06a |
| CT002 | Plantio com área zero, negativa ou ausente é rejeitado | RN01 | RF11 | Integração | P3 | Negativo | C06a |
| CT003 | Data de início hoje ou no passado é aceita | RN02 | RF11 | Unitário | P3 | Positivo | C06a |
| CT004 | Data de início futura é rejeitada | RN02 | RF11 | Unitário | P3 | Negativo | C06a |
| CT005 | Encerramento na data de início ou depois é aceito | RN03 | RF13 | Integração | P3 | Positivo | C06b |
| CT006 | Encerramento anterior ao início é rejeitado | RN03 | RF13 | Integração | P3 | Negativo | C06b |
| CT007 | Lançamento até a data de encerramento é aceito | RN04 | RF13 | Unitário | P2 | Positivo | L00 |
| CT008 | Lançamento depois do encerramento é rejeitado | RN04 | RF13 | Unitário | P2 | Negativo | L00 |

Todas as regras têm par positivo e negativo.

---

## Convenções

| Item | Decisão | Por quê |
|---|---|---|
| Violação de regra (área ≤ 0, data futura, encerramento antes do início, lançamento após encerramento) | Service lança `RegraNegocioException` → **422** `REGRA_NEGOCIO` | Padrões 2.4: 422 é regra violada. Mesma linha da RN06 na Q00b |
| Campo ausente ou fora do formato (área nula, mais de 3 casas decimais) | Bean Validation no DTO → **400** `VALIDACAO`, com o campo em `campos` | Padrões 2.4: 400 é formato inválido |
| "Data corrente" | `LocalDate.now(clock)` com `Clock` injetável no fuso `America/Fortaleza` | Teste unitário controla o "hoje"; o servidor em UTC não pode adiantar o dia |
| Comparação de datas | Inclusiva na borda: mesmo dia é permitido em RN02, RN03 e RN04 | As regras proíbem "posterior" e "anterior", não "igual" |
| Datas nas mensagens | `dd/MM/aaaa` | Mensagem é lida pelo operador |

Nos testes de integração, além do serviço, a restrição do banco é exercitada direto por SQL. É a segunda camada que o DER pede para RN01 e RN03.

---

## Massa de dados base

Sintética, conforme `RNF10`. Criada por fixture em cada teste e removida no final.

| Item | Valor |
|---|---|
| Usuário | operador de teste, perfil operador |
| Produtor | "Produtor Teste Plantio", CPF sintético válido em dígito verificador |
| Propriedade | "Sítio Teste", área total 10,000 ha, do produtor acima |
| Cultura | Feijão (ciclo `CURTO`) |
| "Hoje" nos testes unitários | 2026-10-11, por `Clock.fixed` em `America/Fortaleza` |

Testes de integração não fixam o relógio: usam datas bem no passado (2026-02-01 em diante) para não depender do dia em que rodam.

---

## RN01 — Área obrigatória e positiva

### CT001 — Plantio com área positiva é registrado

| Campo | Conteúdo |
|---|---|
| Regra | RN01 |
| Requisito | RF11 |
| Tipo | Integração |
| Prioridade | P3 |
| Natureza | Positivo |

**Pré-condição**
Propriedade e cultura da massa base. Banco efêmero (Testcontainers).

**Passos**
1. Registrar plantio com área 2,500 ha, data de início 2026-02-01 e previsão de colheita 2026-05-15.
2. Registrar plantio com área 0,001 ha (menor valor que cabe em `NUMERIC(12,3)`) e data de início 2026-02-01.
3. Ler os dois plantios pelo repositório.

**Resultado esperado**
- Os dois são gravados sem exceção.
- Áreas gravadas exatamente como 2,500 e 0,001 — sem arredondar para 2,50 ou 0,00.
- `tipo_ciclo` = `CURTO`, copiado da cultura (RF12).

### CT002 — Plantio com área zero, negativa ou ausente é rejeitado

| Campo | Conteúdo |
|---|---|
| Regra | RN01 |
| Requisito | RF11 |
| Tipo | Integração |
| Prioridade | P3 |
| Natureza | Negativo |

**Pré-condição**
Propriedade e cultura da massa base. Nenhum plantio cadastrado.

**Passos**
1. Registrar plantio com área 0,000 ha, data de início 2026-02-01.
2. Registrar plantio com área −1,000 ha, data de início 2026-02-01.
3. Pela API, enviar `POST /api/v1/plantios` sem o campo `area`.
4. Pela API, enviar `POST /api/v1/plantios` com `area` = 0,0004.
5. Inserir por SQL direto, fora do serviço, um plantio com `area = 0`.
6. Contar os plantios gravados.

**Resultado esperado**
- Passos 1 e 2: `RegraNegocioException` com a mensagem "A área do plantio deve ser maior que zero." Na API: **422**, `erro: "REGRA_NEGOCIO"`.
- Passo 3: **400**, `erro: "VALIDACAO"`, com `campos` contendo `area`.
- Passo 4: **400**, `erro: "VALIDACAO"`, com `campos` contendo `area`. O valor não pode ser aceito e gravado como 0,000 pelo arredondamento do banco.
- Passo 5: violação da restrição `ck_plantio_area`.
- Passo 6: zero plantios gravados.

---

## RN02 — Data de início não futura

### CT003 — Data de início hoje ou no passado é aceita

| Campo | Conteúdo |
|---|---|
| Regra | RN02 |
| Requisito | RF11 |
| Tipo | Unitário |
| Prioridade | P3 |
| Natureza | Positivo |

**Pré-condição**
Service de plantio com `Clock.fixed(2026-10-11T12:00:00-03:00, America/Fortaleza)`. Repositórios mockados.

**Passos**
1. Registrar plantio com área 1,000 ha e data de início 2026-10-11 (o próprio "hoje").
2. Registrar plantio com área 1,000 ha e data de início 2025-11-20.

**Resultado esperado**
Nenhuma exceção nos dois passos; o repositório de plantio recebe `save` duas vezes, com as datas 2026-10-11 e 2025-11-20.

### CT004 — Data de início futura é rejeitada

| Campo | Conteúdo |
|---|---|
| Regra | RN02 |
| Requisito | RF11 |
| Tipo | Unitário |
| Prioridade | P3 |
| Natureza | Negativo |

**Pré-condição**
Repositórios mockados.

**Passos**
1. Com `Clock.fixed(2026-10-11T12:00:00-03:00, America/Fortaleza)`, registrar plantio com área 1,000 ha e data de início 2026-10-12.
2. Com `Clock.fixed(2026-10-12T01:30:00Z, America/Fortaleza)` — 22h30 de 11/10 em Fortaleza, mas já 12/10 em UTC —, registrar plantio com data de início 2026-10-12.

**Resultado esperado**
- Nos dois passos: `RegraNegocioException` com a mensagem "A data de início não pode ser posterior a hoje." Na API: **422**, `erro: "REGRA_NEGOCIO"`.
- O repositório de plantio não é chamado (`verify(..., never()).save(...)`).

O passo 2 pega o defeito de calcular "hoje" em UTC: à noite no Ceará, o servidor aceitaria a data de amanhã.

---

## RN03 — Encerramento posterior ao início

### CT005 — Encerramento na data de início ou depois é aceito

| Campo | Conteúdo |
|---|---|
| Regra | RN03 |
| Requisito | RF13 |
| Tipo | Integração |
| Prioridade | P3 |
| Natureza | Positivo |

**Pré-condição**
Dois plantios abertos, PL1 e PL2, ambos com área 1,000 ha e data de início 2026-02-01. Banco efêmero.

**Passos**
1. Encerrar PL1 em 2026-06-30.
2. Encerrar PL2 em 2026-02-01 (mesmo dia do início).
3. Ler os dois plantios.

**Resultado esperado**
- PL1 com `data_encerramento` = 2026-06-30; PL2 com `data_encerramento` = 2026-02-01.
- Os dois aparecem como encerrados no filtro de situação (RF14).

### CT006 — Encerramento anterior ao início é rejeitado

| Campo | Conteúdo |
|---|---|
| Regra | RN03 |
| Requisito | RF13 |
| Tipo | Integração |
| Prioridade | P3 |
| Natureza | Negativo |

**Pré-condição**
Plantio aberto PL1, área 1,000 ha, data de início 2026-02-01. Banco efêmero.

**Passos**
1. Encerrar PL1 em 2026-01-31.
2. Ler PL1.
3. Executar por SQL direto, fora do serviço, `UPDATE plantio SET data_encerramento = '2026-01-31'` em PL1.

**Resultado esperado**
- Passo 1: `RegraNegocioException` com a mensagem "A data de encerramento não pode ser anterior à data de início (01/02/2026)." Na API: **422**, `erro: "REGRA_NEGOCIO"`.
- Passo 2: PL1 continua aberto, `data_encerramento` nula.
- Passo 3: violação da restrição `ck_plantio_encerra`.

---

## RN04 — Lançamento em plantio encerrado

Os dois casos testam o validador reutilizável da L00, isolado das APIs de lançamento. Cada API que o usa (L01, L04a, L07, L09b) herda a cobertura; não precisa repetir o caso.

### CT007 — Lançamento até a data de encerramento é aceito

| Campo | Conteúdo |
|---|---|
| Regra | RN04 |
| Requisito | RF13 |
| Tipo | Unitário |
| Prioridade | P2 |
| Natureza | Positivo |

**Pré-condição**
- PL1: data de início 2026-02-01, encerrado em 2026-06-30.
- PL2: data de início 2026-02-01, aberto (sem encerramento).

**Passos**
1. Validar lançamento em PL1 com data do fato 2026-06-30 (o próprio dia do encerramento).
2. Validar lançamento em PL1 com data do fato 2026-06-15.
3. Validar lançamento em PL2 com data do fato 2026-09-01.
4. Validar lançamento vinculado a PL1 e PL2 juntos (caso de despesa rateada), data do fato 2026-06-30.

**Resultado esperado**
Nenhuma exceção em nenhum dos quatro passos.

### CT008 — Lançamento depois do encerramento é rejeitado

| Campo | Conteúdo |
|---|---|
| Regra | RN04 |
| Requisito | RF13 |
| Tipo | Unitário |
| Prioridade | P2 |
| Natureza | Negativo |

**Pré-condição**
Os mesmos PL1 (encerrado em 2026-06-30) e PL2 (aberto) do CT007.

**Passos**
1. Validar lançamento em PL1 com data do fato 2026-07-01.
2. Validar lançamento vinculado a PL1 e PL2 juntos, data do fato 2026-07-01.

**Resultado esperado**
- Nos dois passos: `RegraNegocioException` com a mensagem "O plantio foi encerrado em 30/06/2026. A data do fato não pode ser posterior ao encerramento." Na API: **422**, `erro: "REGRA_NEGOCIO"`, com a data de encerramento na mensagem, como pede a RN04.
- Passo 2: o lançamento inteiro é rejeitado — um plantio encerrado basta, mesmo com o outro aberto. Nada é gravado para PL2.

---

## Pontos para revisar

1. **Convenções de status.** Área nula e mais de 3 casas decimais saem como 400; área zero ou negativa sai como 422. Confirmar com quem pegar a C06a, para o DTO não ganhar `@Positive` e transformar a RN01 em 400.
2. **Fuso do "hoje".** `America/Fortaleza` não está registrado nos padrões técnicos. A mesma convenção vale para a RN35 (data do fato não futura, Q00f). Vale registrar na 2.4.
3. **Encerramento com data futura.** Nenhuma regra proíbe encerrar um plantio com data posterior a hoje. Se for para proibir, é regra nova na Fase 1, não um caso deste documento.
4. **Mensagens de erro.** São propostas. O teste verifica `erro` e status; a mensagem exata é confirmada na implementação.
