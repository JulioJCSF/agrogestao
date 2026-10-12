# AgroGestão — Entrega da sprint 2

Data: 12/10/2026. Branch: `feat/sprint-2-fundacao`. Base: `develop`.

## Escopo

| Issue | Entrega | Situação |
|---|---|---|
| #28 — F02 | PostgreSQL 17 efêmero, @ServiceConnection, Surefire/Failsafe e testes de migrations | Implementada |
| #30 — F06 | Login HeroUI, sessionStorage, Bearer, 401, restauração e ocultação de gravação para CONSULTA | Implementada e testada com API real |
| #33 — F11 | Todas as entidades e repositórios do DER; base de auditoria e admin provisório | Implementada |
| #38 — C02 | Criar, listar por produtor, detalhar e alterar propriedades | Implementada |
| #39 — C05 | Criar, listar e alterar culturas; duplicidade 422 | Implementada |
| #52 — L11 | Catálogo de categorias; proteção de categoria de sistema | Implementada |
| #84 — F07b | Campos de moeda/data/CPF e tratamento dos erros por campo | Implementada |
| #85 — F07c | Tabela com busca/paginação, avisos, vazio, carregamento e confirmação | Implementada |
| #87 — F13 | DTOs, controllers do MVP, exemplos Swagger e paginação | Implementada; revisão de front necessária antes do merge |
| #86 — F12 | Massa adicional idempotente, exclusiva do perfil dev, com 3 produtores, propriedades e plantios | Parcial; manter issue aberta |
| #32, #46, #57 | Issues com label qa | Excluídas a pedido |
| #36 — G04 | Validação com o sindicato | Excluída a pedido |
| #29 — F04a | Login JWT da API | Já concluída antes desta entrega |

## Contrato e próximos módulos

O Swagger é gerado dos DTOs e controllers. Login, propriedades, culturas e categorias têm implementação. Os demais endpoints respondem 501 no formato ErroResposta; cada issue de API substituirá o esqueleto mantendo as assinaturas. Não são implementações antecipadas de produtor, plantio, diária, venda ou relatório.

Listas seguem `{itens, pagina, tamanho, total}`, com página inicial zero e tamanho de 1 a 100. O front adapta nomes, enums, IDs e URLs aninhadas ao contrato. Módulo 501 é informado como indisponível; erros de outros status continuam chegando ao operador.

O auditor provisório consulta o admin configurado, como exigido pela F11. A F04b continua responsável pelas permissões de escrita no backend e pela troca do auditor para o usuário do token. A ocultação de ações no front não substitui essa autorização no servidor.

## Migrations e F12

As migrations existentes foram preservadas, sem novo arquivo de migration, conforme orientação do solicitante. A carga antiga contém dados de exemplo e vigência aberta de diária desde 2026; entidades e fixtures se adaptam a ela.

A massa adicional em `db/dev/dados-sinteticos.sql` é um inicializador de desenvolvimento, não uma migration Flyway. Não duplica registros ao reiniciar e usa nomes explicitamente fictícios e CPFs gerados. O perfil padrão e o perfil test não executam esse inicializador; MassaDevIT testa especificamente test+dev.

Não é possível afirmar que a carga antiga está isolada em dev enquanto ela permanece na migration comum. Por isso #86 é relacionada ao PR, sem Closes. Não foi criada uma aprovação do sindicato nem de uma pessoa do front.

## Verificação

- `api/mvnw verify`: 67 testes unitários e 25 de integração, sem falhas; Spotless e build aprovados.
- `npm run lint` e `npm run build`: aprovados com Node 22.
- `npm run smoke`: rotas do protótipo e prévia somente leitura.
- `npm run test:contrato`: 3 testes dos adaptadores, decimais, enums, erros por campo e URLs aninhadas.
- `npm run test:api-real`: 10 testes de navegador aprovados, incluindo login/Bearer contra API real, restauração, logout, 401, CONSULTA, campos e confirmação.
- Os bancos de verificação são temporários, com dados sintéticos. O banco do Compose não é usado.

Comandos e pré-requisitos no [README](../README.md). Detalhes de contrato na [seção 2.4](agrogestao-fase-2-padroes-tecnicos.md#24-padrão-de-api).

## Revisão

O PR deve incluir Closes apenas para as nove issues implementadas. A F13 exige revisão de uma pessoa do front antes do merge. A integração permanece sujeita à aprovação prevista na #17; esta entrega não executa merge.
