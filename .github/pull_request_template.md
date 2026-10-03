## O que muda

<!-- Uma ou duas frases. O título do PR segue Conventional Commits com escopo: feat(api): …, fix(web): …, test(web): … -->

Closes #

## Como testar

<!-- Passos para o revisor conferir. Se houver CT relacionado, cite o número (CT0nn). -->

1.

## Checklist (padrões técnicos, seção 2.6)

- [ ] Build passa
- [ ] Linter sem erro
- [ ] Testes do que foi alterado passam
- [ ] Nomes seguem o glossário de domínio
- [ ] Migration com timestamp, se houver mudança de schema
- [ ] Sem credencial, token ou dado real de produtor no código
- [ ] Endpoint novo aparece no Swagger

<!--
Antes de pedir revisão:
- PR para a develop, nunca direto para a master.
- Se não dá para revisar em 15 minutos, divida.
- Mudou regra de negócio? Atualize docs/agrogestao-fase-1-regras-negocio.md neste mesmo PR.
- Critério de pronto geral: issue fixada "Leia antes" (#17).
-->
