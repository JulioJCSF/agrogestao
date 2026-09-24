# Guia de Documentação, Regras de Negócio e Padrões — Do Início ao Fim

Organizado em ordem prática: cada fase mostra o que produzir, quando, e por quê. A ideia é que você siga isso quase como um checklist sequencial, não como uma lista solta de documentos.

---

## Fase 0 — Pré-projeto (antes de qualquer linha de código)

Objetivo: alinhar expectativa antes de gastar esforço técnico.

### 0.1 Termo de Abertura / Project Charter
- Objetivo do projeto em 2-3 frases
- Problema que resolve
- Escopo macro (o que está dentro e o que está fora)
- Prazo estimado e marcos principais
- Patrocinador/responsável pela decisão final

### 0.2 Documento de Visão
- Público-alvo do sistema
- Diferenciais em relação a soluções existentes (se houver)
- Restrições conhecidas (orçamento, tecnologia obrigatória, integração com sistema legado)

### 0.3 Matriz de Stakeholders
- Quem **decide** (aprova regra de negócio, prioridade)
- Quem **valida** (product owner, área de negócio)
- Quem só é **informado** (gestores, outras equipes)

> Sem isso, toda regra de negócio ambígua vira reunião. Com isso, você sabe exatamente quem perguntar.

---

## Fase 1 — Levantamento e definição (a base de tudo)

Objetivo: transformar entendimento de negócio em algo rastreável tecnicamente.

### 1.1 Requisitos Funcionais e Não-Funcionais
- Separados sempre — funcional é "o que o sistema faz", não-funcional é "como ele se comporta" (performance, segurança, disponibilidade)
- Numerados (RF01, RNF01) para rastreabilidade

### 1.2 Glossário de Domínio
- Todo termo de negócio sem ambiguidade
- Exemplo prático pro seu contexto: no SIGER, definir com clareza o que é "reunião pendente" vs "reunião cancelada" vs "reunião arquivada" — isso evita retrabalho quando alguém interpretar diferente

### 1.3 Documento de Regras de Negócio (RN)
Esse é o documento mais crítico pra QA. Estrutura recomendada por regra:

```
RN01 — Cancelamento de reunião
Descrição: Uma reunião só pode ser cancelada até 24h antes do horário marcado.
Origem: RF03
Exceção: Administradores podem cancelar a qualquer momento.
Casos de teste vinculados: CT01, CT02, CT03
```

- Cada regra numerada, rastreável até o requisito de origem e até o(s) caso(s) de teste
- Regras "óbvias" também entram — o que é óbvio pra quem levantou não é óbvio pra quem testa ou pra quem dá manutenção 6 meses depois

### 1.4 Fluxos de Processo (BPMN ou fluxograma simplificado)
- Use para qualquer processo com decisão condicional (aprovações, permissões, múltiplos caminhos)
- Ferramentas simples resolvem: draw.io, Miro, ou até um fluxograma no próprio Confluence/Notion

### 1.5 Máquina de Estados
- Obrigatório quando a entidade principal tem ciclo de vida (reunião: agendada → em andamento → concluída → arquivada)
- Documentar transições válidas E inválidas explicitamente — isso vira teste de caso negativo depois

**Checkpoint antes de avançar:** as regras de negócio precisam estar "estáveis o suficiente" (não 100% fechadas, mas sem contradições internas). Se ainda mudam de direção toda semana, não adianta ir pra Fase 2 ainda.

---

## Fase 2 — Arquitetura e padrões técnicos (antes do código começar a crescer)

Objetivo: garantir que quem entrar no projeto depois (ou você mesmo em 3 meses) não precise adivinhar decisões.

### 2.1 Guia de Arquitetura
- Camadas do sistema (ex: apresentação Angular → API Spring Boot → persistência PostgreSQL)
- Padrão arquitetural adotado (MVC, hexagonal, camadas simples)
- Diagrama de componentes de alto nível

### 2.2 ADRs (Architecture Decision Records)
- Um arquivo curto por decisão técnica relevante: contexto, decisão, alternativas consideradas, consequências
- Exemplo: "por que usamos Flyway em vez de Liquibase" — isso responde a pergunta antes que alguém precise te chamar pra explicar

### 2.3 Padrão de Código
- Convenção de nomenclatura (classes, métodos, variáveis)
- Linters configurados (ESLint no Angular, Checkstyle/Spotless no Spring Boot)
- Padrão de commits (Conventional Commits: `feat:`, `fix:`, `refactor:` etc.)

### 2.4 Padrão de API
- Nomenclatura de endpoints (REST: substantivo no plural, verbos via método HTTP)
- Padrão de resposta de sucesso e de erro (formato de payload consistente)
- Estratégia de versionamento (`/v1/`, header, etc.)
- Documentação viva via Swagger/OpenAPI

### 2.5 Padrão de Banco de Dados
- Nomenclatura de tabelas e colunas (snake_case, prefixos por módulo, etc.)
- Convenção de migrations (Flyway/Liquibase, numeração sequencial)
- Regras de integridade referencial documentadas

### 2.6 Git Flow / Estratégia de Branches
- Modelo adotado (trunk-based, git-flow, GitHub flow)
- Regras de Pull Request: quem aprova, checklist mínimo antes do merge
- Política de branch protegida na main/develop

---

## Fase 3 — Planejamento de QA (em paralelo, assim que as RNs estabilizarem)

Objetivo: transformar regra de negócio em cobertura de teste antes que o código chegue.

### 3.1 Plano de Testes
- Estratégia geral: tipos de teste cobertos (unitário, integração, E2E, exploratório)
- Ambientes de teste e critério de entrada/saída de cada fase
- Ferramentas usadas (Playwright, Maestro para mobile, etc.)

### 3.2 Matriz de Rastreabilidade
- Requisito → Regra de negócio → Caso de teste → Automação
- Isso é o que te salva quando alguém pergunta "essa regra está coberta?"

### 3.3 Padrão de Casos de Teste
- Template fixo: pré-condição, passos, resultado esperado, prioridade
- Casos positivos e negativos sempre em par (regra válida + violação da regra)

### 3.4 Padrão de Automação
- Convenção de nomes de specs (Playwright)
- Estrutura de Page Objects / componentes reutilizáveis
- Padrão de massa de dados de teste (fixtures, dados sintéticos vs. anonimizados)

---

## Fase 4 — Desenvolvimento (documentação contínua, não só no início)

Objetivo: manter tudo acima vivo enquanto o código evolui — documentação que não é atualizada morre rápido.

- Casos de teste escritos/atualizados por sprint, junto com a feature
- Documentação de API atualizada automaticamente (Swagger gerado do código, não escrito à mão)
- Changelog incremental por versão/sprint
- Toda mudança de regra de negócio atualiza o documento de RN — nunca só o código

---

## Fase 5 — Entrega

Objetivo: quem não participou do desenvolvimento consegue operar o sistema.

### 5.1 Manual Técnico
- Como subir o ambiente localmente e em produção
- Variáveis de ambiente e dependências externas
- Diagrama de infraestrutura, se aplicável

### 5.2 Manual do Usuário (se aplicável)
- Voltado pra quem opera o sistema no dia a dia, sem jargão técnico

### 5.3 Release Notes
- O que mudou, por versão, em linguagem acessível

### 5.4 Runbook de Deploy/CI-CD
- Passo a passo do pipeline
- Procedimento de rollback documentado

---

## Fase 6 — Pós-entrega / Manutenção

Objetivo: o conhecimento não pode morrer com quem saiu do time.

- Postmortems de incidentes (o que aconteceu, causa raiz, ação corretiva)
- Glossário e RNs continuam sendo atualizados conforme o negócio muda
- Revisão periódica da matriz de rastreabilidade (regras órfãs sem teste são um sinal de alerta)

---

## Resumo da ordem prática

1. Termo de abertura + visão + stakeholders
2. Requisitos + glossário + regras de negócio (**é o que desbloqueia tudo depois**)
3. Arquitetura + padrões técnicos (em paralelo com o fim da fase 2)
4. Plano de QA + matriz de rastreabilidade (assim que as RNs estabilizarem, não espera 100%)
5. Documentação contínua durante o desenvolvimento
6. Manuais e release notes na entrega
7. Postmortems e atualização contínua depois

O erro mais comum é pular a Fase 1 achando que "todo mundo já entende a regra" — isso sempre aparece depois como bug reportado por QA que na verdade é regra de negócio nunca documentada.
