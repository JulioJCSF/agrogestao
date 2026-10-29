# API de diária (#58) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Registrar, consultar, alterar e excluir diárias pagas/familiares mantendo despesa e rateio consistentes.

**Architecture:** Service transacional, rotas e DTOs F13; L00 para datas/coleta, referência vigente pela data do fato, cópia do valor na familiar. A paga cria uma despesa e um rateio integral na mesma transação. Bloqueio do plantio coordena lançamentos e encerramento.

**Tech Stack:** Java 21, Spring Boot, JPA, PostgreSQL, JUnit/Mockito e Testcontainers.

**Spec:** Issue #58, issue #17, docs/qa/casos-de-teste-diaria.md (CT021–028, CT031), RN04/RN12–18/RN35–36. O solicitante decidiu em 29/10 rejeitar troca de natureza no PUT com 422.

## Global Constraints

- Branch independente da develop; PR para develop com Closes #58; autoria do solicitante.
- BigDecimal, dias NUMERIC(6,2), valor unitário NUMERIC(12,2), HALF_UP em duas casas.
- Familiar não gera despesa; ausência de referência é 422; valor informado na familiar é ignorado.
- Valor e referência já copiados não mudam com alteração da referência; corrigir a data do fato reaplica a vigente na nova data.
- Dias e valor pago positivos; totais pagos devem caber na despesa e ser positivos após arredondar.
- Sem migrations/dependências novas; listagens de despesas e estorno continuam nas próprias issues.

## Review Focus

- Falha no rateio desfaz diária e despesa; alteração/exclusão também são atômicas.
- Despesa gerada mantém id/criação/coleta na alteração e não é editável/excluível pela API de despesa (CT028).
- Familiar preserva valor histórico ao editar somente os dias; nova data consulta a vigência correspondente.
- Datas limite, natureza/precisão inválidas, ausência de referência, zero/negativo e total pago fora da precisão.
- Concorrência com encerramento e entre alterações da mesma diária.
- RN11 impede deslocar data de diária paga em período emitido para depois excluí-la; quantidade/valor seguem corrigíveis.

### Task 1: CRUD, valoração e sincronização

**Files:** DiariaController.java, novo service/diaria/DiariaService.java, DiariaRepository.java, DespesaRepository.java, CategoriaDespesaRepository.java, PlantioRepository.java, PlantioService.java; proteção RN16 em DespesaService/Controller; novos DiariaServiceTest e DiariaIT; documentação da RN12/CTs.

**Interfaces:** Consome LancamentoService.validarDataFato/herdarDataColeta, ValorReferenciaDiariaRepository.buscarVigente, repositórios JPA e PaginacaoConsulta.pageable. Produz listar(Long,PaginacaoConsulta), detalhar(Long), criar(Long,DiariaRequisicao), alterar(Long,DiariaRequisicao), excluir(Long) com DTOs F13.

- [x] Escrever DiariaIT com os valores CT021/022/026/027/028; 8 falhas esperadas em endpoints 501, sem erro de execução.
- [x] Implementar service, consultas, locks e controller. Atualizar/excluir a despesa junto; recusar edição direta e troca de natureza.
- [x] Cobrir CT023/024/025/031 nos unitários, com VR1=80.25, VR2=90.50, 1.25×90.50=113.13 e 0.75×90.50=67.88.
- [x] Verificar rollback no último save, constraints, coleta/autoria, histórico da familiar, precisão e datas. Proteção RN11: teste de exclusão em período emitido falhou com 204 e passou com 422 após a guarda; snapshot e registros preservados.
- [x] Revisão independente encontrou a sequência PUT de data → DELETE contornando RN11. O teste reproduziu PUT 200 indevido; a guarda agora também protege a data original de diárias pagas abrangidas por emissão.
- [x] Executar `mvn -B -ntp spotless:apply verify` em Java 21; 168 unitários e 128 integrações sem falhas, build e Spotless aprovados.
- [ ] Revisão independente e commit `feat(api): implementa registro de diárias`; enviar e abrir PR separado.
