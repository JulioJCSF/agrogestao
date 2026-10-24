# C06a — Cadastro e consulta de plantios (#40)

Os endpoints GET/POST `/api/v1/plantios` e GET/PUT `/api/v1/plantios/{id}` usam o contrato F13, com DTOs e paginação por id. O POST retorna 201 e Location; vínculos inexistentes retornam 404.

- Área ausente ou fora de `NUMERIC(12,3)`: 400 `VALIDACAO`. Área zero ou negativa: 422 `REGRA_NEGOCIO` (RN01).
- Início não futuro, comparado com `Clock` injetável em `America/Fortaleza` (RN02, convenção da Q00a).
- Tipo de ciclo omitido usa o padrão atual da cultura; informado, sobrepõe esse padrão (RF12), inclusive no PUT.
- Filtros opcionais por produtor, cultura, situação e período combinam-se. O período seleciona a data de início, com limites inclusivos; período invertido retorna 422. Consulta sem correspondência retorna página vazia.
- A alteração preserva a data de encerramento e rejeita início posterior a ela (RN03). Não recalcula nem atualiza rateios históricos (RN05).

## Verificação

Em `api/`: `.\mvnw.cmd -B spotless:apply verify`, com Docker Desktop ativo. `PlantioServiceTest` cobre RN01/RN02, fuso na virada do dia, herança e sobreposição de ciclo, referências inexistentes e RN03 na alteração. `PlantioIT` cobre precisão, auditoria existente da F11, paginação/filtros, persistência dos rateios e constraints por SQL direto.

CT001–CT004 pertencem à C06a. A #40 também cita CT005/CT006, mas a Q00a atribui esses cenários de encerramento à C06b, expressamente fora do escopo da #40. Esta entrega cobre a RN03 ao alterar plantio já encerrado e testa a constraint `ck_plantio_encerra`; não implementa o POST de encerramento nem declara aqueles cenários completos.

Permissões por perfil e substituição do auditor provisório da F11 pelo usuário do token continuam na F04b. Nenhuma migration foi alterada ou criada.
