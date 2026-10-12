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
