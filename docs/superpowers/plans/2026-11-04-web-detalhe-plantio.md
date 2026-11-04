# Detalhe e encerramento do plantio — Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans nesta sessão, sem novos agentes. Passos em TDD; sem commit, push ou PR por instrução do controlador.

**Goal:** Entregar somente C08b (#94): detalhe com lançamentos e resultado da API, encerramento com data e confirmação.

**Architecture:** Preservar a rota `/plantios/:id`, o detalhe, os atalhos, componentes e carregamento paginado existentes. Consultar resultado no serviço, com mês opcional para ciclo curto; jamais apurar valores no navegador. Encerramento usa o modal e Confirmacao existentes e atualiza o contexto pela resposta da API.

**Tech Stack:** JavaScript, React, Axios, HeroUI, MSW, Playwright e node:test existentes.

**Spec:** [#94](https://github.com/JulioJCSF/agrogestao/issues/94), [#17](https://github.com/JulioJCSF/agrogestao/issues/17), RF13, RN03/RN04 e RN24; documentação integral de README, docs e .github conferida.

## Global Constraints

- Worktree `/home/lit/dev/agrogestao-issue-94`, branch `feat/web-detalhe-encerramento-plantio`, base `7c23f9a`.
- C08a (#48) ausente como entrega concluída: reutilizar lista/cadastro/acesso atuais, sem implementá-la.
- JavaScript; componentes e identidade visual atuais; desktop; `data-testid` em todo interativo; gravações escondidas para CONSULTA.
- API e migrations preservadas. API real global e infraestrutura a cargo do controlador.
- Porta de testes deste worktree: 4173; nunca usar 4174 (outro worktree).

## Review Focus

- Carregamento não pode parecer registro inexistente; falha de consulta não pode virar resultado zero.
- Ciclo efetivo curto consulta meses anteriores; resposta atrasada não substitui mês novo.
- Itens de venda e rateios usam valores da API, sem multiplicação/soma monetária.
- Encerramento rejeitado preserva dados/data digitada; cancelar não grava e envio não duplica.
- Mais de 100 registros e módulos 501 não podem produzir histórico incompleto apresentado como vazio.

### Task 1: Detalhe e resultado

**Files:** `web/src/features/producao/Producao.jsx`, novo `ResultadoPlantio.jsx`, `web/src/services/registroService.js`; testes em `web/tests/browser/plantio.spec.js` e `web/tests/pages/PlantioPage.js`.

**Interfaces:** manter `useData()` e `openModal`; produzir consulta `consultarResultadoPlantio(id, mes)` com DTO da API intacto.

- [x] Escrever testes de valores sentinela da API (resultado positivo/negativo, item total e rateio), carregamento, erro, vazio, inexistente, ciclo longo/curto e páginas adicionais.
- [x] Rodar `npm test -- plantio.spec.js` e observar falhas por funcionalidades ausentes.
- [x] Implementar estados, identificação/datas, seções e resultado com mês e recuperação de erro, mantendo os atalhos de lançamento.
- [x] Rodar os testes e confirmar aprovação.

### Task 2: Encerramento

**Files:** `web/src/components/ModalRegistro.jsx`, `web/src/contexts/DataContext.jsx`, `web/src/services/registroService.js`; mesmos testes browser.

**Interfaces:** `encerrarPlantioRegistro(id, endedAt)` devolve plantio adaptado; contexto substitui o registro pela resposta; `Confirmacao` executa a gravação.

- [x] Escrever testes de cancelamento, data obrigatória, confirmação/data, envio único, 400 por campo, 422/erro de rede e CONSULTA.
- [x] Rodar teste focal e observar RED.
- [x] Implementar confirmação acessível, bloqueio durante envio e mensagem da API; explicar RN04 sem impedir lançamentos históricos.
- [x] Rodar teste focal e confirmar GREEN.

### Task 3: Mock e verificação

**Files:** testes do mock/contrato e browser mock de plantio; `web/src/mocks/handlers/resultado.js` somente se necessário para período mensal do contrato; testes de API real browser para o controlador.

- [x] Rodar testes falhando antes de qualquer ajuste de mock.
- [x] Validar mock pela porta 4173 com configuração temporária, sem alterar porta padrão de outras suítes.
- [x] Rodar lint, build, contrato, cobertura de handlers, node:test, `npm test` e browser mock; inspecionar desktop uma vez e corrigir falhas materiais.
- [x] Registrar resultados e limitações, revisão própria (sem novos agentes), sem commit/push/PR.

## Execução e decisões

- Sessão autorizada para implementação direta; aprovações de planejamento/commits das skills substituídas pelas instruções explícitas do usuário.
- Paginação global já busca todas as páginas (`tamanho=100`); reutilizar e testar, sem criar outra camada de paginação.
- Valores fixos do mock são exemplos documentados; não implementar apuração no mock/front.

## Evidências da execução

- RED de detalhe/resultado/carregamento e confirmação observado em `/tmp/agrogestao-94-red.log` e `green1.log`; GREEN inicial 9/9 e ampliado 13/13.
- Achado P2 da revisão: seletor de venda descartava plantios encerrados. RED `Expected "1", received ""` em `/tmp/agrogestao-94-review-red.log`; retirado apenas o filtro de situação, mantendo o produtor. GREEN 14/14.
- Percentual: RED mostrou `33.3333%`; helper `number(..., 2)` exibe `33,33%`, sem refazer rateio. Verificação final 14/14 em `/tmp/agrogestao-94-plantio-final.log`.
- Mock mensal: RED de fevereiro bissexto (período da safra em vez de mês); GREEN 26/26 no conjunto de handlers, incluindo 3 novos testes.
- Lint, Prettier, build, contrato (5/5), cobertura do mock (66 endpoints, 2/2) aprovados. Build mantém aviso de bundle > 500 kB.
- API real inicial 40/41: o teste navegava antes de concluir login CONSULTA; a espera pelo menu do perfil corrigiu o teste. Comando padrão aprovou 41/41 antes da revisão e 42/42 após os ajustes, incluindo venda posterior recusada e venda histórica aceita.
- Harness real agora limpa somente snapshots legados com `produtor` string no container efêmero. Não altera banco local, API ou migrations; SQL e triggers são restaurados na mesma transação.
- Inspeção desktop aprovada pelo controlador; sem novas alterações visuais além do percentual solicitado.
- Smoke inicial falhou em `/produtores/1`; não se reproduziu no diagnóstico nem no comando padrão final (9 rotas + ADMIN, exit 0). O log final inclui ECONNREFUSED da proxy no encerramento; nenhum código de produto/produtor foi alterado para esse smoke.
- Revisão própria realizada; revisão independente Godel coordenada pelo controlador, achado P2 corrigido. Sem novos agentes, commit, push ou PR nesta sessão.

- Verificação final: 14/14 plantios com stubs, 2/2 browser MSW, 26/26 handlers (incluindo resultado mensal), 42/42 suíte API real, lint/Prettier/build/smoke aprovados. Logs `/tmp/agrogestao-94-{plantio-final,mock-browser,api-real-pos-review,build-final,smoke-final}.log`. Nenhum arquivo de diagnóstico local incluído.
