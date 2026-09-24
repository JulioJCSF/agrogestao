# ADR-0004 — PostgreSQL com Flyway para versionamento de schema

**Status:** Aceita
**Data:** 2026-09-24

## Contexto
O sistema guarda dados financeiros de produtores rurais, com retenção de longo prazo (`RNF05`) e valores monetários que exigem precisão decimal fixa (`RNF08`). A stack foi definida na Fase 0 como restrição de tecnologia, já dominada pela equipe. Com mais de cinco desenvolvedores trabalhando em paralelo em branches curtas, o schema precisa evoluir de forma versionada e reproduzível em todas as máquinas.

## Decisão
PostgreSQL como banco de dados, com schema versionado exclusivamente por migrations Flyway, numeradas por timestamp.

## Alternativas consideradas
- Hibernate com `ddl-auto=update` gerenciando o schema — alteração de schema fora do controle de versão; `ddl-auto` fica fixo em `validate` (padrões técnicos, seção 2.5, regra 4)
- Numeração sequencial de migrations (`V3__`, `V4__`) — com mais de cinco pessoas, migrations são criadas simultaneamente em branches diferentes e colidem no merge

## Consequências
- Toda alteração de schema passa pelo Flyway, inclusive em desenvolvimento.
- Migration aplicada nunca é editada; correção se faz com uma nova.
- Chave estrangeira declarada no banco, não só no JPA; campo monetário em `NUMERIC(12,2)`.
- Piora: nenhuma mudança de schema é rápida — até um ajuste trivial exige migration nova, e erro em migration já aplicada não se desfaz editando o arquivo.
