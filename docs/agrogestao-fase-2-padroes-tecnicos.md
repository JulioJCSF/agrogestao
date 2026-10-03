# AgroGestão — Fase 2: Arquitetura e Padrões Técnicos

Versão 1.6

Define as decisões técnicas e convenções do projeto. Documento de referência: consultado sempre que alguém abre um PR, cria uma migration ou nomeia um endpoint.

**Contexto que justifica o rigor:** equipe de mais de cinco desenvolvedores, monorepo, 15 semanas letivas e disponibilidade parcial. Com essa configuração, convenção fraca custa mais tempo em conflito de merge do que o tempo que a convenção economiza.

**Documentos relacionados**

- `agrogestao-fase-1-glossario.md` — nomenclatura de domínio, que governa nomes de classes, tabelas e endpoints
- `agrogestao-fase-0.md` — restrições de tecnologia e ambiente
- `projeto-extensionista-agrogestao.md` — seção 10, stack definida

---

## 2.1 Guia de Arquitetura

### Camadas

```
Navegador (notebook do sindicato)
        │
        ▼
React + Vite + HeroUI          ← apresentação
        │  HTTP/JSON (Axios)
        ▼
Spring Boot — Controller        ← borda: validação de entrada, tradução HTTP
        │
        ▼
Service                         ← regra de negócio
        │
        ▼
Repository (Spring Data JPA)    ← persistência
        │
        ▼
PostgreSQL (schema via Flyway)
```

### Regras de camada

1. **Controller não contém regra de negócio.** Recebe DTO, valida formato, delega ao Service, traduz o retorno. Se um `if` de negócio aparecer no Controller, ele está no lugar errado.
2. **Repository não é chamado pelo Controller.** Sempre via Service.
3. **Entidade JPA não sai da camada de Service.** O Controller expõe DTO. Expor entidade acopla o contrato da API ao esquema do banco e vaza campos por acidente.
4. **Regra que envolve mais de uma entidade mora no Service**, não na entidade.

### Organização do monorepo

```
agrogestao/
├── api/                          # Spring Boot
│   └── src/main/java/com/agrogestao/api/
│       ├── controller/
│       │   ├── plantio/
│       │   ├── despesa/
│       │   └── relatorio/
│       ├── service/
│       │   ├── plantio/
│       │   ├── despesa/
│       │   └── relatorio/
│       ├── repository/
│       ├── model/                # entidades JPA
│       ├── dto/
│       └── config/
├── web/                          # React + Vite
│   └── src/
│       ├── features/             # pasta por domínio
│       ├── components/           # componentes compartilhados
│       ├── services/             # chamadas Axios
│       └── contexts/
├── e2e/                          # Playwright — ver agrogestao-fase-3-qa.md, seção 3.4
├── docker-compose.yml
├── .env.example
├── .gitignore
├── .gitattributes
└── docs/                         # documentação do projeto
    └── adr/                      # ADRs — ver seção 2.2
```

**Pacote base:** `com.agrogestao.api`, definido na criação do projeto.

**Organização por camada, com subpacote por domínio.** A divisão principal é por camada — `controller`, `service`, `repository` — que é o layout convencional do Spring e não exige decisão sobre onde colocar arquivo novo.

Dentro de `controller` e `service`, subpacotes por domínio. Com seis pessoas trabalhando em paralelo, isso evita que todos disputem os mesmos diretórios: quem cuida de plantio trabalha em `controller/plantio` e `service/plantio`.

`repository`, `model`, `dto` e `config` ficam planos — têm um arquivo por entidade e não ganham nada com subdivisão.

Serviços que cruzam domínios, como a apuração de resultado, ficam no subpacote do domínio que os ancora. `ResultadoService` mora em `service/plantio`, porque plantio é a unidade de apuração.

### Ambiente de desenvolvimento

Pré-requisitos de desenvolvimento em cada máquina: Docker Desktop, JDK 21 e Node 22. Só o PostgreSQL roda em container; a API e o front-end rodam direto no Windows. Ver ADR-0009.

---

## 2.2 ADRs — Architecture Decision Records

Um arquivo curto por decisão técnica relevante, em `docs/adr/`. Nome: `NNNN-titulo-curto.md`.

### Template

```
# ADR-NNNN — [título]

**Status:** Proposta | Aceita | Substituída por ADR-NNNN
**Data:** AAAA-MM-DD

## Contexto
[O problema e as restrições. Por que a decisão precisou ser tomada.]

## Decisão
[O que foi decidido, em uma ou duas frases.]

## Alternativas consideradas
- [Alternativa] — por que não
- [Alternativa] — por que não

## Consequências
[O que passa a ser verdade. Incluir o que piora, não só o que melhora.]
```

### ADRs a redigir

Decisões já tomadas que precisam de registro:

| # | Decisão | Origem |
|---|---|---|
| 0001 | Monorepo para front-end e back-end | Equipe |
| 0002 | React com Vite e HeroUI no front-end | Stack dominada pela equipe |
| 0003 | Spring Boot com Spring Data JPA no back-end | Stack dominada pela equipe |
| 0004 | PostgreSQL com Flyway para versionamento de schema | Fase 0 |
| 0005 | Docker Compose desde o início, com hot-reload — **substituída pela 0009** | Consistência entre as máquinas da equipe |
| 0006 | Autenticação por conta institucional com perfis, sem conta de produtor | VAL01 — produtor não opera o sistema |
| 0007 | Plantio como unidade de custo, separado de Cultura | Glossário, seção 2 |
| 0008 | Organização por camada, com subpacote por domínio em controller e service | Equipe |
| 0009 | Ambiente de desenvolvimento híbrido — substitui a 0005 | Equipe: time inteiro em Windows |

A 0007 é a mais importante: é a que alguém vai questionar daqui a três meses.

A 0008 precisa registrar o que foi pesado. A favor da camada: layout convencional do Spring, conhecido por todos, sem decisão sobre onde colocar arquivo novo. Contra: com seis pessoas, todos trabalham nos mesmos diretórios, e uma funcionalidade toca quatro pastas num único PR. O subpacote por domínio foi a mitigação adotada.

---

## 2.3 Padrão de Código

### Nomenclatura

O glossário de domínio governa os nomes. `Plantio` é `Plantio` no Java, `plantio` na tabela, `plantio` na URL. Não traduzir para inglês — o domínio é em português e a tradução cria uma camada de ambiguidade que ninguém pediu.

Exceção: termos técnicos consagrados permanecem em inglês (`Repository`, `Service`, `Controller`, `DTO`, `findById`).

| Elemento | Convenção | Exemplo |
|---|---|---|
| Classe Java | PascalCase | `PlantioService` |
| Método e variável Java | camelCase | `apurarResultado()` |
| Constante | UPPER_SNAKE | `LIMITE_PAGINA` |
| Componente React | PascalCase | `FormularioDespesa.jsx` |
| Hook | camelCase com `use` | `usePlantio.js` |
| Arquivo de serviço React | camelCase | `plantioService.js` |

### Linters

- **Back-end:** Spotless com Google Java Format. Roda no build; PR não passa com formatação fora do padrão.
- **Front-end:** ESLint e Prettier, configuração compartilhada no repositório.

Formatação automática evita a discussão mais inútil que existe em revisão de código.

### Padrão de commits

Conventional Commits, com escopo obrigatório por causa do monorepo:

```
feat(api): adiciona endpoint de registro de diária
fix(web): corrige cálculo exibido no resumo do plantio
refactor(api): extrai regra de apuração para ResultadoService
docs: atualiza glossário com definição de plantio
chore(infra): ajusta volume do Postgres no compose
test(api): cobre apuração de plantio de ciclo curto
test(e2e): cobre emissão do relatório com as ressalvas
```

Escopos: `api`, `web`, `e2e`, `infra`, `docs`. O `e2e` cobre a suíte Playwright em `e2e/` (QA 3.4), que não pertence a `api` nem a `web`.

---

## 2.4 Padrão de API

### URLs

REST com substantivo no plural, verbo pelo método HTTP. Versionamento por caminho: `/api/v1/`.

```
GET    /api/v1/produtores
POST   /api/v1/produtores
GET    /api/v1/produtores/{id}
PUT    /api/v1/produtores/{id}
DELETE /api/v1/produtores/{id}

GET    /api/v1/plantios?produtorId={id}
POST   /api/v1/plantios/{id}/despesas
GET    /api/v1/plantios/{id}/resultado
```

Recurso aninhado só quando o filho não existe sem o pai. Produtor existe sozinho, então não aninha.

> **Pendente (P17):** o exemplo `POST /api/v1/plantios/{id}/despesas` não comporta despesa vinculada a vários plantios (RF19, RN06) nem a `despesa.produtor_id` do DER — a despesa pertence ao produtor e alcança um ou mais plantios pelo rateio. O contrato de despesa é definido na F13 (esqueleto da API) e registrado aqui; até lá, não implementar a partir deste exemplo.

### Códigos de status

| Situação | Código |
|---|---|
| Consulta com sucesso | 200 |
| Criação com sucesso | 201, com `Location` |
| Operação sem retorno | 204 |
| Erro de validação | 400 |
| Não autenticado | 401 |
| Sem permissão para o recurso | 403 |
| Recurso inexistente | 404 |
| Violação de regra de negócio | 422 |
| Erro não tratado | 500 |

A distinção entre 400 e 422 importa: 400 é formato inválido (data em texto livre), 422 é regra violada (despesa lançada em plantio já encerrado).

### Formato de erro

Resposta única para todo erro, para o front tratar em um lugar só:

```json
{
  "timestamp": "2026-09-13T14:32:10Z",
  "status": 422,
  "erro": "REGRA_NEGOCIO",
  "mensagem": "Não é possível lançar despesa em plantio encerrado.",
  "campos": [
    { "campo": "dataDespesa", "mensagem": "Data posterior ao encerramento do plantio." }
  ]
}
```

`mensagem` é exibível ao operador — escrita em português claro, sem jargão técnico. O operador do sindicato tem familiaridade intermediária; mensagem de erro incompreensível vira chamado para a equipe.

Tratamento centralizado com `@RestControllerAdvice`. Nenhum `try/catch` devolvendo erro direto no Controller.

### Documentação

Swagger/OpenAPI via springdoc, gerado do código. Documentação escrita à mão desatualiza na primeira sprint.

---

## 2.5 Padrão de Banco de Dados

### Nomenclatura

- Tabelas em **snake_case, singular**: `produtor`, `plantio`, `despesa`, `diaria`
- Colunas em snake_case: `data_fato`, `data_coleta`, `valor_total`
- Chave primária: `id`
- Chave estrangeira: `<tabela>_id` — `plantio_id`, `produtor_id`
- Índice: `idx_<tabela>_<coluna>`
- Restrição única: `uk_<tabela>_<coluna>`

### Colunas obrigatórias em toda tabela

| Coluna | Motivo |
|---|---|
| `id` | Identificador |
| `criado_em` | Auditoria |
| `atualizado_em` | Auditoria |
| `criado_por` | Rastreia qual operador lançou — mitigação de R4 |

O `criado_por` não é enfeite. Com titular e suplente lançando dados, saber quem registrou o quê é o que permite investigar inconsistência.

### Migrations Flyway

**Numeração por timestamp, não sequencial.**

```
V20260913_1430__cria_tabela_plantio.sql
V20260913_1615__adiciona_coluna_data_coleta.sql
```

Com mais de cinco pessoas, `V3__` e `V4__` são criados simultaneamente em branches diferentes e colidem no merge. Timestamp elimina o problema.

Regras:

1. **Migration aplicada nunca é editada.** Corrige-se com uma nova.
2. Uma migration por mudança lógica.
3. Nada de alteração de schema fora do Flyway — inclusive em desenvolvimento.
4. `ddl-auto` do Hibernate fixo em `validate`. Nunca `update`.

### Integridade

Chave estrangeira sempre declarada no banco, não só no JPA. `NOT NULL` em tudo que o domínio exige. Campo monetário em `NUMERIC(12,2)` — nunca `float` ou `double`.

Data do fato e data de coleta são colunas distintas em todo registro que descreve um evento, conforme o glossário.

---

## 2.6 Git Flow e Pull Requests

### Modelo

Duas branches permanentes e branch curta por tarefa:

- **`master`** — entregas. Só recebe PR vindo da `develop`, a cada entrega.
- **`develop`** — integração. Toda branch de tarefa sai da `develop` e volta para ela por PR.

Git-flow completo (release, hotfix) é desproporcional para 15 semanas. Trunk-based com commit direto é arriscado com seis pessoas e sem CI madura. A `develop` dá à equipe um ponto de integração contínua sem expor a `master` a trabalho incompleto.

### Branches

Formato `<tipo>/<descrição-curta>`, com o tipo do Conventional Commits:

```
feat/registro-diaria
fix/calculo-resultado-ciclo-curto
refactor/extrai-servico-apuracao
test/emissao-relatorio-ressalvas
docs/glossario-dominio
chore/infra-ambiente-local
```

**Branch vive no máximo três dias.** Com seis pessoas no mesmo repositório, branch de duas semanas vira conflito irrecuperável. Tarefa grande se quebra em pedaços integráveis.

### Regras de Pull Request

1. `master` e `develop` protegidas: sem push direto em nenhuma das duas
2. PR de tarefa aponta para a `develop`, com `Closes #<número>` na descrição
3. **Uma aprovação** para integrar. Duas travariam o fluxo com a disponibilidade parcial da equipe
4. Quem abre o PR não aprova o próprio
5. Build e linter verdes antes do merge
6. PR grande demais para revisar em 15 minutos deve ser dividido

O modelo de descrição com o checklist abaixo está em `.github/pull_request_template.md`.

### Checklist do PR

```
- [ ] Build passa
- [ ] Linter sem erro
- [ ] Testes do que foi alterado passam
- [ ] Nomes seguem o glossário de domínio
- [ ] Migration com timestamp, se houver mudança de schema
- [ ] Sem credencial, token ou dado real de produtor no código
- [ ] Endpoint novo aparece no Swagger
```

O penúltimo item é o mais importante: o sistema guarda dados financeiros de terceiros. Dado real de produtor não entra no repositório em nenhuma hipótese — nem em teste, nem em seed, nem em captura de tela.

### Estratégia de merge

**Squash merge** nos PRs de tarefa para a `develop`: um commit por tarefa, legível e revertível. O título do commit squash segue o Conventional Commits da seção 2.3.

O PR de entrega, da `develop` para a `master`, usa merge commit, para que a `master` registre cada entrega como um ponto identificável.

---

## Pendências desta fase

| # | Pendência | Bloqueia |
|---|---|---|
| P06 | Redigir as ADRs 0001 a 0007 | Não |
| P07 | Configurar Spotless, ESLint e Prettier no repositório | Não, mas quanto antes menos retrabalho de formatação — Spotless configurado na F09; ESLint e Prettier pendentes |
| P08 | Definir pipeline de CI (build e linter no PR) | Não — regra 5 do PR depende dela para ser automática |
| P09 | Docker Compose do banco, Flyway e hot-reload local | **Fechada** — banco e Flyway no PR #11; DevTools e README neste PR (ADR-0009) |
| P16 | Inicializar o projeto React com Vite em `web/` | Sim — hoje há apenas um `index.html` |
| P17 | Contrato de despesa em vários plantios: substituir o exemplo `POST /plantios/{id}/despesas` da 2.4 (RF19, `despesa.produtor_id`) | Sim, a API de despesa — resolvida na F13 |

O projeto Spring já foi criado e commitado em `api/`, com pacote base `com.agrogestao.api`. Banco e migrations já sobem pelo Docker Compose; o hot-reload é nativo, pelo DevTools no back-end e pelo HMR do Vite no front-end (ADR-0009).

O modelo físico de dados não entra nesta fase. `DA01` e `DA07` foram decididas em VAL03 e o DER está desbloqueado: despesa alcança vários plantios com percentual gravado, e o valor de referência da diária é entidade própria com vigência por período.
