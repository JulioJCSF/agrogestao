-- Gerados por algoritmo, não derivados de dados pessoais. Somente @Profile("dev").
-- Initializer idempotente: não é migration; migrations existentes são preservadas.
INSERT INTO produtor(nome, documento, endereco, profissao, criado_por)
SELECT 'Produtor Fictício Dev 1', '99900000188', 'Endereço Fictício Dev 1', 'Agricultor de demonstração', id
FROM usuario WHERE perfil = 'ADMIN' ORDER BY id LIMIT 1
ON CONFLICT(documento) DO NOTHING;
INSERT INTO produtor(nome, documento, endereco, profissao, criado_por)
SELECT 'Produtor Fictício Dev 2', '99900000269', 'Endereço Fictício Dev 2', 'Agricultor de demonstração', id
FROM usuario WHERE perfil = 'ADMIN' ORDER BY id LIMIT 1
ON CONFLICT(documento) DO NOTHING;
INSERT INTO produtor(nome, documento, endereco, profissao, criado_por)
SELECT 'Produtor Fictício Dev 3', '99900000340', 'Endereço Fictício Dev 3', 'Agricultor de demonstração', id
FROM usuario WHERE perfil = 'ADMIN' ORDER BY id LIMIT 1
ON CONFLICT(documento) DO NOTHING;
INSERT INTO propriedade(produtor_id,nome,area_total,localizacao,criado_por)
SELECT p.id, 'Propriedade Fictícia Dev ' || right(p.nome,1), 5.000, 'Localização fictícia',p.criado_por
FROM produtor p WHERE p.documento IN ('99900000188','99900000269','99900000340')
AND NOT EXISTS(SELECT 1 FROM propriedade pr WHERE pr.produtor_id=p.id AND pr.nome='Propriedade Fictícia Dev ' || right(p.nome,1));
INSERT INTO plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por)
SELECT pr.id,c.id,1.000,c.tipo_ciclo_padrao,DATE '2026-02-01',pr.criado_por
FROM propriedade pr JOIN produtor p ON p.id=pr.produtor_id
JOIN cultura c ON c.nome=CASE right(p.nome,1) WHEN '1' THEN 'Milho' WHEN '2' THEN 'Banana' ELSE 'Feijão' END
WHERE p.documento IN ('99900000188','99900000269','99900000340')
AND pr.nome='Propriedade Fictícia Dev ' || right(p.nome,1)
AND NOT EXISTS(SELECT 1 FROM plantio pl WHERE pl.propriedade_id=pr.id AND pl.cultura_id=c.id AND pl.data_inicio=DATE '2026-02-01');

-- Lançamentos do Produtor Fictício Dev 1, para a apuração, o painel e a emissão terem o que mostrar.
-- Cada INSERT confere antes se já existe: o arquivo roda a cada subida em dev. Sem bloco DO, porque
-- o inicializador divide o script por ponto e vírgula. A emissão se faz pela tela ou pela API.
-- Conta do plantio de milho: custo 450,00 + 180,00 = 630,00, receita 450,00, resultado -180,00 e
-- mão de obra familiar 3 x 80,00 = 240,00. O Dev 2 continua sem consentimento (RN37).
UPDATE produtor SET consentimento_em = DATE '2026-01-10', atualizado_em = now()
WHERE documento = '99900000188' AND consentimento_em IS NULL;

-- A restrição de vigência recusa sobreposição: só cria se nada cobrir 2026 em diante.
INSERT INTO valor_referencia_diaria(valor, vigencia_inicio, vigencia_fim, criado_por)
SELECT 80.00, DATE '2026-01-01', NULL, u.id
FROM usuario u WHERE u.perfil = 'ADMIN'
AND NOT EXISTS(SELECT 1 FROM valor_referencia_diaria r WHERE COALESCE(r.vigencia_fim, DATE 'infinity') >= DATE '2026-01-01')
ORDER BY u.id LIMIT 1;

INSERT INTO cliente(nome, tipo_canal, criado_por)
SELECT c.nome, c.canal, u.id
FROM (VALUES ('Cliente Fictício Dev Feira', 'FEIRA'), ('Cliente Fictício Dev Merenda', 'MERENDA')) AS c(nome, canal)
CROSS JOIN (SELECT id FROM usuario WHERE perfil = 'ADMIN' ORDER BY id LIMIT 1) u
WHERE NOT EXISTS(SELECT 1 FROM cliente cl WHERE cl.nome = c.nome);

INSERT INTO despesa(produtor_id, categoria_despesa_id, descricao, valor_total, data_fato, data_coleta, criado_por)
SELECT p.id, cat.id, 'Sementes fictícias de milho', 450.00, DATE '2026-03-05', DATE '2026-03-06', p.criado_por
FROM produtor p JOIN categoria_despesa cat ON cat.nome = 'Sementes e mudas'
WHERE p.documento = '99900000188'
AND NOT EXISTS(SELECT 1 FROM despesa d WHERE d.produtor_id = p.id AND d.descricao = 'Sementes fictícias de milho');
INSERT INTO rateio_despesa(despesa_id, plantio_id, percentual, valor_rateado, criado_por)
SELECT d.id, pl.id, 1.000000, d.valor_total, d.criado_por
FROM despesa d
JOIN produtor p ON p.id = d.produtor_id
JOIN propriedade pr ON pr.produtor_id = p.id AND pr.nome = 'Propriedade Fictícia Dev 1'
JOIN plantio pl ON pl.propriedade_id = pr.id AND pl.data_inicio = DATE '2026-02-01'
WHERE p.documento = '99900000188' AND d.descricao = 'Sementes fictícias de milho'
AND NOT EXISTS(SELECT 1 FROM rateio_despesa r WHERE r.despesa_id = d.id);

-- Diária familiar: sem despesa, com o valor da referência vigente (RN13).
INSERT INTO diaria(plantio_id, valor_referencia_diaria_id, natureza, quantidade_dias, valor_unitario, data_fato, data_coleta, criado_por)
SELECT pl.id, r.id, 'FAMILIAR', 3.00, r.valor, DATE '2026-03-12', DATE '2026-03-13', pl.criado_por
FROM plantio pl
JOIN propriedade pr ON pr.id = pl.propriedade_id AND pr.nome = 'Propriedade Fictícia Dev 1'
JOIN produtor p ON p.id = pr.produtor_id AND p.documento = '99900000188'
JOIN valor_referencia_diaria r ON DATE '2026-03-12' BETWEEN r.vigencia_inicio AND COALESCE(r.vigencia_fim, DATE 'infinity')
WHERE pl.data_inicio = DATE '2026-02-01'
AND NOT EXISTS(SELECT 1 FROM diaria d WHERE d.plantio_id = pl.id AND d.natureza = 'FAMILIAR' AND d.data_fato = DATE '2026-03-12');

-- Diária paga: valor informado (RN15), com a despesa gerada na categoria Diária e o rateio de 100 % (RN16).
INSERT INTO diaria(plantio_id, natureza, quantidade_dias, valor_unitario, data_fato, data_coleta, criado_por)
SELECT pl.id, 'PAGA', 2.00, 90.00, DATE '2026-03-20', DATE '2026-03-21', pl.criado_por
FROM plantio pl
JOIN propriedade pr ON pr.id = pl.propriedade_id AND pr.nome = 'Propriedade Fictícia Dev 1'
JOIN produtor p ON p.id = pr.produtor_id AND p.documento = '99900000188'
WHERE pl.data_inicio = DATE '2026-02-01'
AND NOT EXISTS(SELECT 1 FROM diaria d WHERE d.plantio_id = pl.id AND d.natureza = 'PAGA' AND d.data_fato = DATE '2026-03-20');
INSERT INTO despesa(produtor_id, categoria_despesa_id, diaria_id, descricao, valor_total, data_fato, data_coleta, criado_por)
SELECT p.id, cat.id, d.id, 'Diária paga de 20/03/2026', 180.00, d.data_fato, d.data_coleta, d.criado_por
FROM diaria d
JOIN plantio pl ON pl.id = d.plantio_id
JOIN propriedade pr ON pr.id = pl.propriedade_id AND pr.nome = 'Propriedade Fictícia Dev 1'
JOIN produtor p ON p.id = pr.produtor_id AND p.documento = '99900000188'
JOIN categoria_despesa cat ON cat.nome = 'Diária' AND cat.sistema
WHERE d.natureza = 'PAGA' AND d.data_fato = DATE '2026-03-20'
AND NOT EXISTS(SELECT 1 FROM despesa de WHERE de.diaria_id = d.id);
INSERT INTO rateio_despesa(despesa_id, plantio_id, percentual, valor_rateado, criado_por)
SELECT de.id, d.plantio_id, 1.000000, de.valor_total, de.criado_por
FROM despesa de
JOIN diaria d ON d.id = de.diaria_id
JOIN produtor p ON p.id = de.produtor_id AND p.documento = '99900000188'
WHERE d.natureza = 'PAGA' AND d.data_fato = DATE '2026-03-20'
AND NOT EXISTS(SELECT 1 FROM rateio_despesa r WHERE r.despesa_id = de.id);

INSERT INTO producao(plantio_id, quantidade, unidade_medida, data_fato, data_coleta, criado_por)
SELECT pl.id, 1200.000, 'kg', DATE '2026-07-20', DATE '2026-07-21', pl.criado_por
FROM plantio pl
JOIN propriedade pr ON pr.id = pl.propriedade_id AND pr.nome = 'Propriedade Fictícia Dev 1'
JOIN produtor p ON p.id = pr.produtor_id AND p.documento = '99900000188'
WHERE pl.data_inicio = DATE '2026-02-01'
AND NOT EXISTS(SELECT 1 FROM producao pd WHERE pd.plantio_id = pl.id AND pd.data_fato = DATE '2026-07-20');

-- Venda: 100,000 kg x 4,50 = 450,00 para o cliente da feira.
INSERT INTO venda(produtor_id, cliente_id, valor_total, data_fato, data_coleta, criado_por)
SELECT p.id, c.id, 450.00, DATE '2026-07-25', DATE '2026-07-26', p.criado_por
FROM produtor p JOIN cliente c ON c.nome = 'Cliente Fictício Dev Feira'
WHERE p.documento = '99900000188'
AND NOT EXISTS(SELECT 1 FROM venda v WHERE v.produtor_id = p.id AND v.cliente_id = c.id AND v.data_fato = DATE '2026-07-25');
INSERT INTO item_venda(venda_id, plantio_id, quantidade, unidade_medida, valor_unitario, valor_total, criado_por)
SELECT v.id, pl.id, 100.000, 'kg', 4.50, 450.00, v.criado_por
FROM venda v
JOIN produtor p ON p.id = v.produtor_id AND p.documento = '99900000188'
JOIN cliente c ON c.id = v.cliente_id AND c.nome = 'Cliente Fictício Dev Feira'
JOIN propriedade pr ON pr.produtor_id = p.id AND pr.nome = 'Propriedade Fictícia Dev 1'
JOIN plantio pl ON pl.propriedade_id = pr.id AND pl.data_inicio = DATE '2026-02-01'
WHERE v.data_fato = DATE '2026-07-25'
AND NOT EXISTS(SELECT 1 FROM item_venda i WHERE i.venda_id = v.id);
