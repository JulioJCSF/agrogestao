# Encerramento por inatividade — #83

> Execução: superpowers:executing-plans, com testes antes da implementação.

**Objetivo:** encerrar a sessão após 30 minutos sem interação, prazo definido pelo responsável em 04/11/2026, e informar o motivo no login (RF04).
**Arquitetura:** manter o AuthProvider como dono da sessão e usar sessionStorage já existente para preservar a última atividade em recargas. Não renovar o token nem alterar o contrato da API.
**Stack:** React, JavaScript, sessionStorage e Playwright existentes.
**Especificação:** issues #83 e #17; requisitos RF04; padrões técnicos e QA em docs/.

## Restrições

- Branch independente de develop, commit feat(web), PR para develop com Closes #83.
- Mensagem em português no login; mesmos componentes e estilos existentes.
- 30 minutos sem interação: teclado, ponteiro, rolagem e toque; requisições de fundo não contam.
- A expiração absoluta do token continua válida mesmo com atividade.

## Riscos e cobertura

- Recarga não reinicia a contagem; sessão antiga sem atividade registrada recebe prazo na primeira restauração.
- Ao retornar de suspensão, verificar o horário antes de aceitar atividade.
- Sair e entrar de novo elimina a mensagem e inicia novo prazo.
- Limpar timer e listeners ao terminar sessão, sem requisições autenticadas após encerramento.
- Abas preservam o isolamento existente de sessionStorage; sem sincronização nova entre abas.

## Tarefa única

Arquivos: AuthContext.jsx, App.jsx, tests/browser/inatividade.spec.js, tests/pages/LoginPage.js e documentação RF04.

- [x] Escrever testes para expiração em 30 minutos, interação que renova prazo, recarga, suspensão, novo login e token absoluto.
- [x] Executar e confirmar falha por comportamento ausente.
- [x] Implementar timer e última atividade no AuthProvider, mensagem acessível no login e registrar prazo nos requisitos.
- [x] Executar suíte de sessão, lint, build, smoke, contrato, mocks e suíte browser completa.
- [ ] Criar commit como João Gustavo Nobre Gois <jggnobregu7@gmail.com> e PR separado após revisão final.

## Verificação

- RED observado no encerramento após 30 minutos e na corrida com `/auth/me` atrasado; GREEN: 11 testes de inatividade.
- Build e lint aprovados; smoke: nove rotas e administração; contrato: 5; cobertura de mock: 2; browser mock: 13.
- Suíte completa com API real: 36 testes aprovados. O banco efêmero foi preparado removendo somente emissões sintéticas antigas cujo JSON é incompatível com o DTO atual; a execução com a carga original falha ao consultar esses relatórios. Nenhuma alteração de API, migration ou banco de desenvolvimento.
- Revisão identificou e corrigiu perda de atividade recente ao receber `/auth/me`; teste de regressão confirmou o cenário.
