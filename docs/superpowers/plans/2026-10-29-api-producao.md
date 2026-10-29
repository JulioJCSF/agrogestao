# API de produção (#50) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Registrar, consultar, alterar e excluir colheitas por plantio.

**Architecture:** Manter as rotas F13 e a separação Controller → Service → Repository. Reutilizar LancamentoService e DataColetaListener; unidade omitida recebe a da cultura, sem modificar registros anteriores. O solicitante decidiu exigir a unidade da cultura em 29/10; RN38 e CT060/CT061 registram a decisão. Bloqueios transacionais coordenam produção, alteração e encerramento do plantio e unidade da cultura.

**Tech Stack:** Java 21, Spring Boot, JPA, PostgreSQL, JUnit, Mockito e Testcontainers.

**Spec:** Issue #50; docs/agrogestao-fase-1-requisitos.md (RF15–17), docs/agrogestao-fase-1-regras-negocio.md (RN04, RN35, RN36), padrões técnicos 2.1–2.6 e issue #17.

## Global Constraints

- Branch da develop, PR para develop, Conventional Commits com escopo api, Closes #50.
- Quantidade NUMERIC(12,3), dinheiro BigDecimal, nomes do glossário em português.
- Formato inválido 400, regra violada 422, recurso inexistente 404; erro centralizado.
- Paginação por id, tamanho 1–100; data de coleta da API em America/Fortaleza.
- Sem migrations, novas dependências ou alterações nas telas da L02.

## Review Focus

- Mais de uma colheita e isolamento entre plantios na paginação.
- Unidade ausente usa a cultura; unidade explícita equivalente é normalizada, e divergente é recusada.
- Alteração preserva data de coleta e autoria.
- Hoje e encerramento são inclusivos; datas futuras e posteriores ao encerramento não gravam.
- Quantidade zero/negativa e precisão inválida não chegam ao banco.

### Task 1: CRUD de produção

**Files:** ProducaoController.java, ProducaoRequisicao.java, ProducaoRepository.java, novo service/producao/ProducaoService.java; novos ProducaoServiceTest.java e ProducaoIT.java; atualizar LancamentoIT.java.

**Interfaces:** Consome LancamentoService.validarDataFato(LocalDate, Plantio), PaginacaoConsulta.pageable() e DataColetaListener. Produz listar(Long, PaginacaoConsulta), detalhar(Long), criar(Long, ProducaoRequisicao), alterar(Long, ProducaoRequisicao) e excluir(Long), com os DTOs já publicados.

- [x] Escrever ProducaoIT e atualizar CT057 para esperar 201/dataColeta do sistema; observar falhas 400/501 no contrato antigo.
- [x] Implementar service transacional, consulta por plantio e controller; unidade omitida recebe cultura, branca é recusada no DTO e divergente no service.
- [x] Escrever testes unitários de quantidade, datas, unidade e não encontrado (250.125 kg, mínimo 0.001).
- [x] Revisão independente: reproduzir a corrida entre produção e encerramento com teste falhando; bloquear a linha do plantio antes das validações. Nova decisão de unidade também reproduzida antes da correção, incluindo cultura/plantio.
- [x] Executar `mvn -B -ntp spotless:apply verify` em Java 21 com Docker; build e Spotless aprovados, 166 unitários e 124 integrações sem falhas.
- [ ] Revisar o diff, documentar compatibilidade da unidade opcional no PR e commitar `feat(api): implementa registro de produção` com a identidade Git do solicitante.
