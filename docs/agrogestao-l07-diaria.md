# L07 — API de diária (#58)

Rotas F13 implementadas: GET/POST `/api/v1/plantios/{plantioId}/diarias`, GET/PUT/DELETE `/api/v1/diarias/{id}`. POST responde 201 com Location, DELETE 204; listas paginadas por id, tamanho 1–100. Formatos e precisão inválidos retornam 400; recursos inexistentes, 404; regras violadas, 422.

Familiar usa a referência vigente na data do fato, com bordas inclusivas, copiando valor unitário e vínculo. O valor informado no corpo é ignorado. Sem referência, a API orienta cadastrá-la e não grava. Alterar a referência não regrava diárias; corrigir só os dias preserva o valor copiado, e corrigir a data reaplica a vigência da nova data.

Paga exige valor unitário positivo e gera despesa da categoria de sistema Diária, para o produtor do plantio, com um rateio de 100%. Dias × valor unitário usam BigDecimal/HALF_UP em duas casas. O total pago deve caber em NUMERIC(12,2) e ser positivo depois do arredondamento. Despesa herda fato/coleta da diária. Alterar a paga atualiza a mesma despesa e rateio, preservando id, criação e coleta. Excluir remove os três registros na mesma transação. Uma falha em qualquer passo desfaz a operação inteira.

PUT/DELETE de despesa gerada retornam 422 orientando alterar/excluir a diária (RN16); POST manual na categoria de sistema já era recusado. Essa proteção é antecipada da L04b para completar CT028. Exclusão manual e listagens de despesas continuam na L04b; DELETE manual permanece 501. Estorno e histórico de emissões continuam na A03b.

O solicitante decidiu em 29/10/2026 rejeitar a troca de natureza no PUT com 422, orientando excluir e cadastrar de novo (RN12). O preenchimento da data de coleta usa a L00, ignora valor enviado e preserva a coleta na alteração. Locks do plantio e da diária coordenam encerramento e alterações concorrentes.

Excluir diária paga também respeita a RN11: despesa com data do fato abrangida por emissão do produtor bloqueia a exclusão com 422 e orientação para estorno. Nenhum dos registros ou o conteúdo emitido muda. Fora do período emitido a exclusão é permitida. O endpoint de estorno permanece na A03b.

A data da paga abrangida por emissão também não pode ser trocada (422), para impedir deslocá-la para fora do período e depois excluir. Alterações de quantidade/valor permanecem permitidas, mantendo o conteúdo já emitido intacto.

Verificação em `api/`, com Java 21 e Docker: `./mvnw -B -ntp verify`. `DiariaServiceTest` cobre CT023/024/025/031; `DiariaIT`, CT021/022/026/027/028 e integridade, concorrência e rollback. Fixtures exclusivamente sintéticas; nenhum teste usa o banco do Compose. Sem migration ou dependência nova. As telas permanecem na L08.
