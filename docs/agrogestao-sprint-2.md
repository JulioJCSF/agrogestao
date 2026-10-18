# AgroGestão — Entrega da sprint 2

Data: 12/10/2026. Entrega dividida em cinco PRs; o PR agregado #111 foi substituído.

## Organização dos PRs

| Branch | Issues | Base inicial |
|---|---|---|
| `feat/sprint2-api-fundacao` | #28, #33 | `develop` |
| `feat/sprint2-contrato-api` | #87 | `feat/sprint2-api-fundacao` |
| `feat/sprint2-cadastros-api` | #38, #39, #52 | `feat/sprint2-contrato-api` |
| `feat/sprint2-sessao-componentes-web` | #30, #84, #85 | `feat/sprint2-cadastros-api` |
| `feat/sprint2-massa-dev` | #86, parcial | `feat/sprint2-api-fundacao` |

Os PRs dependentes usam a branch anterior como base para mostrar somente seu grupo de mudanças. Antes de integrar um PR dependente, atualize sua base para `develop` após o merge da dependência e confira o diff. Como o projeto usa squash, atualize também a branch dependente para retirar os commits já integrados. A massa dev pode seguir após a fundação; o front segue após os cadastros. Cada commit e PR mantém a autoria do solicitante.

## Revisão de 18/10/2026

- #113: nomes de cultura/propriedade no snapshot, POST com 201 e Location, DELETE com 204, exemplos corrigidos e seção 2.4 limitada ao contrato. A base do PR passa para `develop`, que já recebeu #112 e #115.
- #114: unicidade sem diferenciar maiúsculas nos services, rejeição explícita de categoria em uso e testes com fixtures próprias e casos negativos. Nenhuma migration foi alterada ou criada; o índice case-insensitive no banco continua sujeito à decisão do grupo.
- #116: moeda sem zeros à esquerda, percentual em pt-BR, erros de conexão/timeout em português, testids derivados na confirmação e testes com digitação sequencial e cultura criada pelo próprio teste. O banco temporário do teste real é descartado pelo runner ao terminar.
- Área total da propriedade e confirmação da prioridade da RF22 não foram decididas nesta revisão. A revisão de uma pessoa do front para F13 permanece necessária.

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

- `api/mvnw verify`: 67 testes unitários e 34 de integração na API completa revisada, sem falhas; Spotless e build aprovados. Na etapa do contrato (#113): 52 unitários e 22 de integração.
- `npm run lint` e `npm run build`: aprovados com Node 22.
- `npm run smoke`: rotas do protótipo e prévia somente leitura.
- `npm run test:contrato`: 5 testes dos adaptadores, decimais, enums, erros por campo, URLs aninhadas, moeda e mensagens de rede/timeout.
- `npm run test:api-real`: 13 testes de navegador aprovados, incluindo login/Bearer contra API real, restauração, logout, 401, CONSULTA, campos, confirmação, digitação sequencial, percentual e erro de conexão.
- Os bancos de verificação são temporários, com dados sintéticos. O banco do Compose não é usado.

Comandos e pré-requisitos no [README](../README.md). Detalhes de contrato na [seção 2.4](agrogestao-fase-2-padroes-tecnicos.md#24-padrão-de-api).

## Revisão

Os PRs incluem Closes apenas para as nove issues implementadas; o PR parcial da #86 usa somente referência relacionada. A F13 exige revisão de uma pessoa do front antes do merge. A integração permanece sujeita à aprovação prevista na #17; esta entrega não executa merge.
