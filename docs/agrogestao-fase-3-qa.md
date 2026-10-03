# AgroGestão — Fase 3: Planejamento de QA

Versão 1.1

Transforma regra de negócio em cobertura de teste antes do código chegar.

**Documentos relacionados**

- `agrogestao-fase-1-regras-negocio.md` — as 37 regras que este plano precisa cobrir
- `agrogestao-fase-1-requisitos.md` — requisitos de origem
- `agrogestao-fase-2-der.md` — restrições que o banco garante e as que ficam no serviço
- `agrogestao-fase-2-padroes-tecnicos.md` — seção 2.6, o que trava um PR

---

## 3.1 Plano de Testes

### Estratégia por camada

| Tipo | O que cobre | Ferramenta |
|---|---|---|
| Unitário | Regras de negócio na camada de serviço, isoladas de banco | JUnit 5 + Mockito |
| Integração | Repositórios, restrições do banco, transação e rollback | JUnit 5 + Testcontainers (PostgreSQL) |
| API | Contrato dos endpoints: status, formato de erro, validação | Playwright (request) |
| E2E | Fluxos completos na interface, do login ao relatório | Playwright + TypeScript |
| Exploratório | Sessões com carta de teste, focadas em entrada de dado ruim | Manual |

**Não há teste mobile.** O sistema é desktop-only por `RNF01`. Maestro não entra neste projeto — registrado para ninguém propor depois.

### Onde concentrar esforço

O DER lista sete regras que não cabem em restrição declarativa e ficam no serviço. São elas que exigem teste explícito, porque o banco não protege:

`RN04` lançamento em plantio encerrado · `RN07` e `RN09` cálculo e fechamento de centavos · `RN16` geração da despesa da diária · `RN20` vedação de dupla contagem · `RN24` período de apuração pelo ciclo · `RN29` saldo não bloqueia venda · `RN37` consentimento como condição de emissão

Três delas falham **em silêncio** — não lançam exceção, não quebram nada, só produzem número errado no papel entregue ao produtor. São a prioridade máxima da suíte:

- **`RN09`** — soma dos rateios diferente do valor da despesa
- **`RN20`** — diária contada duas vezes, dobrando o custo
- **`RN34`** — relatório reemitido divergindo do que foi entregue

### Ambientes

| Ambiente | Uso | Dados |
|---|---|---|
| Local | Desenvolvimento e execução da suíte | Sintéticos, via fixture |
| CI | Unitário, integração e API a cada PR | Sintéticos, banco efêmero |
| Homologação | E2E e exploratório antes de cada entrega | Sintéticos |

**Nunca há dado real de produtor em nenhum ambiente de teste** (`RNF10`). Não é só sobre o repositório: banco de homologação com dado real também está vedado.

### Critérios de entrada e saída

**Entrada para teste de uma funcionalidade:** RF e RN correspondentes escritos; endpoint documentado no Swagger; build passando.

**Saída — o que permite entregar:**

- Toda RN de prioridade máxima com caso positivo e negativo automatizados, passando
- Nenhum defeito aberto de severidade alta
- Fluxo de cadastro até emissão do relatório executado ponta a ponta em homologação
- Sessão exploratória realizada sobre entrada de dado inconsistente

---

## 3.2 Matriz de Rastreabilidade

Cadeia completa: **Dor → Requisito → Regra → Caso de teste → Automação**.

Prioridade: **P1** falha em silêncio ou afeta o documento entregue ao produtor · **P2** regra de negócio com efeito visível · **P3** validação de formato.

| Regra | Requisito | Casos | Tipo | Prior. |
|---|---|---|---|---|
| RN01 área positiva | RF11 | CT001, CT002 | Integração | P3 |
| RN02 início não futuro | RF11 | CT003, CT004 | Unitário | P3 |
| RN03 encerramento ≥ início | RF13 | CT005, CT006 | Integração | P3 |
| RN04 lançamento em plantio encerrado | RF13 | CT007, CT008 | Unitário | P2 |
| RN05 alteração de área não retroage | RF21 | CT009 | Integração | **P1** |
| RN06 vínculo mínimo | RF19 | CT010, CT011 | Unitário | P2 |
| RN07 cálculo do rateio | RF20 | CT012, CT013 | Unitário | **P1** |
| RN08 despesa em plantio único | RF20 | CT014 | Unitário | P2 |
| RN09 fechamento de centavos | RF20 | CT015, CT016, CT017 | Unitário | **P1** |
| RN10 gravação no lançamento | RF21 | CT018 | Integração | **P1** |
| RN11 exclusão de despesa | RF45 | CT019, CT020 | Integração | P2 |
| RN12 natureza obrigatória | RF24 | CT021, CT022 | Integração | P3 |
| RN13 valoração da familiar | RF26 | CT023 | Unitário | **P1** |
| RN14 sem referência vigente | RF25 | CT024 | Unitário | P2 |
| RN15 valor da paga | RF24 | CT025 | Unitário | P2 |
| RN16 geração de despesa | RF27 | CT026, CT027, CT028 | Integração | **P1** |
| RN17 vigências sem sobreposição | RF25 | CT029, CT030 | Integração | P2 |
| RN18 referência não retroage | RF26 | CT031 | Unitário | **P1** |
| RN19 custo de desembolso | RF34 | CT032 | Unitário | **P1** |
| RN20 vedação de dupla contagem | RF39 | CT033, CT034 | Unitário | **P1** |
| RN21 custo com mão de obra | RF37 | CT035 | Unitário | P2 |
| RN22 receita do plantio | RF35 | CT036 | Unitário | **P1** |
| RN23 resultado | RF36 | CT037 | Unitário | **P1** |
| RN24 período pelo ciclo | RF38 | CT038, CT039 | Unitário | **P1** |
| RN25 produção e venda distintas | RF36 | CT040, CT041 | E2E | P2 |
| RN26 venda com ao menos um item | RF30 | CT042, CT043 | API | P3 |
| RN27 item aponta para plantio | RF31 | CT044 | API | P2 |
| RN28 sugestão de plantio | RF32 | CT045, CT046 | E2E | P3 |
| RN29 saldo não bloqueia venda | RF33 | CT047 | API | P2 |
| RN30 coerência de unidade | RF31 | CT048, CT049 | Unitário | P3 |
| RN31 conteúdo mínimo | RF40 | CT050 | E2E | P2 |
| RN32 ressalva de valor declarado | RF42 | CT051 | E2E | **P1** |
| RN33 ressalva de finalidade | RF43 | CT052 | E2E | **P1** |
| RN34 imutabilidade da emissão | RF45 | CT053, CT054 | Integração | **P1** |
| RN35 data do fato não futura | RF17 | CT055, CT056 | API | P3 |
| RN36 data de coleta pelo sistema | RF17 | CT057 | Integração | P2 |
| RN37 consentimento para emissão | RF50 | CT058, CT059 | API | P2 |

Quinze regras em P1. É a suíte que roda a cada PR.

**P1 de tipo E2E (CT051, CT052).** A 3.4 reserva o E2E de interface para antes da entrega, mas as ressalvas do relatório precisam falhar o build se sumirem. Solução adotada (Q07): as duas ressalvas são verificadas também em nível de API/integração — o conteúdo gravado da emissão contém os dois textos — e esses testes rodam a cada PR. O E2E de interface de CT051 e CT052 continua rodando antes da entrega.

### Casos críticos detalhados

**CT015 — fechamento de centavos, três plantios de área igual**
Pré: despesa de R$ 100,00 vinculada a três plantios de 1 ha cada.
Esperado: rateios de 33,33 / 33,33 / 33,34, somando exatamente 100,00. O residual vai para o plantio de maior área; no empate, o de menor id.

**CT016 — fechamento de centavos, áreas desiguais**
Pré: despesa de R$ 100,00 entre plantios de 1 ha, 1 ha e 2 ha.
Esperado: 25,00 / 25,00 / 50,00, somando 100,00.

**CT017 — soma dos rateios igual ao total (propriedade)**
Executar com valores e áreas variados e verificar sempre a igualdade. Bom candidato a teste parametrizado.

**CT033 — diária paga não é somada duas vezes**
Pré: plantio com uma diária paga de R$ 80,00 e nenhuma outra despesa.
Esperado: custo de desembolso = R$ 80,00, não R$ 160,00.

**CT034 — diária familiar fora do custo de desembolso**
Pré: plantio com uma diária familiar de R$ 80,00 e nenhuma despesa.
Esperado: custo de desembolso = R$ 0,00; custo com mão de obra familiar = R$ 80,00.

**CT053 — reemissão não altera emissão anterior**
Pré: relatório emitido para um período; em seguida uma despesa daquele período é alterada.
Esperado: nova emissão reflete a alteração; a emissão anterior permanece com o conteúdo original.

**CT009 — alteração de área não recalcula rateio**
Pré: despesa rateada entre dois plantios; depois a área de um deles é corrigida.
Esperado: valores rateados gravados permanecem inalterados.

**CT051 e CT052 — ressalvas no relatório**
Esperado: todo relatório emitido contém o texto de valor declarado e o de ausência de garantia. São os dois casos que protegem o produtor, e devem falhar o build se sumirem.

---

## 3.3 Padrão de Casos de Teste

```
### CTnnn — [título]

| Campo | Conteúdo |
|---|---|
| Regra | RNnn |
| Requisito | RFnn |
| Tipo | Unitário / Integração / API / E2E |
| Prioridade | P1 / P2 / P3 |
| Natureza | Positivo / Negativo |

**Pré-condição**
[Estado necessário antes de executar.]

**Passos**
1. [Ação]
2. [Ação]

**Resultado esperado**
[O que deve acontecer, com valores concretos.]
```

**Regra do par.** Toda regra tem caso positivo e negativo: um verificando o comportamento válido, outro verificando a rejeição da violação. Regra com só um dos dois está meio coberta — e costuma ser o negativo que falta.

Exceção: regras que descrevem ausência de restrição, como `RN29` (saldo não bloqueia venda). O caso é positivo por natureza; não existe violação a rejeitar.

**Valores concretos, não genéricos.** "Resultado correto" não é resultado esperado. "Custo de desembolso = R$ 80,00" é.

---

## 3.4 Padrão de Automação

### Estrutura

```
api/src/test/java/com/agrogestao/api/
├── service/
│   ├── plantio/RateioDespesaServiceTest.java
│   └── plantio/ApuracaoResultadoServiceTest.java
└── repository/
    └── DespesaRepositoryIT.java

e2e/
├── tests/
│   ├── plantio.spec.ts
│   ├── despesa-rateio.spec.ts
│   └── relatorio-emissao.spec.ts
├── pages/
│   ├── LoginPage.ts
│   └── PlantioPage.ts
├── fixtures/
│   ├── produtor.ts
│   └── plantio.ts
└── playwright.config.ts
```

### Nomes

| Item | Convenção | Exemplo |
|---|---|---|
| Teste unitário | `<Classe>Test` | `RateioDespesaServiceTest` |
| Teste de integração | `<Classe>IT` | `DespesaRepositoryIT` |
| Método de teste | `deve<Comportamento>Quando<Condição>` | `deveAtribuirResidualAoMaiorPlantioQuandoHouverDizima` |
| Spec Playwright | `<dominio>.spec.ts` | `despesa-rateio.spec.ts` |
| Page Object | `<Tela>Page.ts` | `PlantioPage.ts` |

Sufixo `IT` separa integração de unitário no build: o unitário roda sempre, o de integração sobe container e roda no CI.

### Page Objects

Um por tela, expondo ações de negócio e não cliques. `plantioPage.registrarPlantio(dados)`, não `plantioPage.clicarBotaoSalvar()`. Seletor de elemento fica dentro do Page Object; spec nenhuma conhece seletor.

Preferir `data-testid` a seletor de texto ou classe — texto muda, classe de framework muda, testid não.

### Massa de dados

**Sintética, sempre.** Nomes e CPFs gerados, nunca copiados de produtor real (`RNF10`). CPF de teste deve ser válido em dígito verificador mas não pertencer a ninguém.

Cada spec cria o que precisa pela API e limpa no final. Teste que depende de estado deixado por outro teste quebra quando a ordem muda.

Fixtures em `e2e/fixtures/`, com builders que aceitam sobreposição:

```ts
const plantio = plantioFixture({ area: 2, tipoCiclo: 'LONGO' });
```

Assim cada teste declara só o que importa para ele.

### O que roda em cada momento

| Momento | Suíte |
|---|---|
| Local, antes do commit | Unitários |
| PR | Unitários, integração e API |
| Antes da entrega | Tudo, mais sessão exploratória |

---

## Sessões exploratórias

Automação verifica o que foi previsto. O maior risco do projeto — `R5`, dado vindo da memória do produtor — produz situações que ninguém previu.

Cartas de teste sugeridas, com tempo fixo por sessão:

- Registrar produção com quantidade absurda (10.000 sacas num canteiro) e observar se algo impede ou alerta
- Lançar despesa com data muito anterior ao início do plantio
- Vender quantidade muito acima do produzido e confirmar que passa, com sinalização (`RN29`)
- Cadastrar plantios sobrepostos na mesma área da mesma propriedade
- Emitir relatório de período sem nenhum lançamento
- Emitir relatório para produtor sem consentimento e confirmar o bloqueio

A última linha de cada sessão é o que o operador do sindicato faria de errado sem perceber — e é onde costuma aparecer o defeito que importa.
