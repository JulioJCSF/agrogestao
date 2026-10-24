# L06 — Valor de referência da diária (#44)

Implementa o contrato F13: GET/POST `/api/v1/valores-referencia-diaria`, GET/PUT `/api/v1/valores-referencia-diaria/{id}` e GET `/api/v1/valores-referencia-diaria/vigente?dataFato=AAAA-MM-DD`.

- Listagem paginada e ordenada por id, nos limites da F13. Criação retorna 201 e Location.
- Valor positivo, em `BigDecimal` / `NUMERIC(12,2)`. Valor ausente ou fora da precisão retorna 400; zero ou negativo retorna 422.
- Início obrigatório; fim opcional, sem limite superior quando ausente. Ambos os limites são inclusivos, conforme o `daterange(..., '[]')` existente no DER. Um período pode durar um dia. Fim anterior ao início retorna 422.
- Vigências sobrepostas retornam 422 com orientação para informar um período livre. O service valida antes de gravar; no PUT, exclui o próprio id dessa consulta. A constraint `ex_vrd_vigencia` permanece como proteção concorrente e sua violação recebe a mesma mensagem clara.
- Consulta vigente retorna 404 com orientação para cadastrar a referência quando não encontra período. A L07 pode usar a consulta do service e tratar a ausência segundo RN14 ao lançar uma diária familiar.
- Alterar referência não atualiza valores, vínculos nem auditoria de diárias existentes (RN18). A L07 continua responsável por copiar o valor no lançamento.

## Verificação

Em `api/`, com Docker Desktop ativo: `.\mvnw.cmd -B spotless:apply verify`.

`ValorReferenciaDiariaServiceTest` cobre os pares de validação, ausência de referência, exclusão do próprio id e tradução da constraint. `ValorReferenciaDiariaIT` cobre CT029 (períodos adjacentes aceitos) e CT030 (interseção, contenção, igualdade, bordas e períodos sem fim rejeitados), valor vigente nas bordas, precisão, auditoria, preservação da diária e proteção por SQL direto.

A Q00c (#46) ainda não tem especificação em `docs/qa/casos-de-teste-diaria.md` nesta base. Os testes usam valores concretos alinhados à matriz de QA, RN17/RN18 e ao DER, sem declarar a Q00c concluída. Usam fixtures sintéticas de 2024; o cenário de vigência aberta limpa as referências legadas apenas dentro de sua transação revertida no banco efêmero.

Nenhuma migration foi criada ou alterada. A auditoria provisória da F11 e as permissões por perfil continuam sob a F04b.
