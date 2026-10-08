-- ============ PARTE 1: CARGA INICIAL ============
CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA public;

-- Verifica se a extensão está em 'public', caso contrário move para públic a extensão.
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM pg_extension e
            JOIN pg_namespace n ON n.oid = e.extnamespace
            WHERE e.extname = 'pgcrypto'
              AND n.nspname <> 'public'
        ) THEN
            ALTER EXTENSION pgcrypto SET SCHEMA public;
        END IF;
    END
$$;

-- 1. Usuário administrador inicial (raiz da cadeia de auditoria; sem criado_por)
INSERT INTO usuario (nome, login, senha_hash, perfil)
VALUES ('Administrador', '${ADMIN_LOGIN}', public.crypt('${ADMIN_SENHA}', public.gen_salt('bf', 10)), 'ADMIN')
ON CONFLICT (login) DO NOTHING;

-- 2. Categorias de despesa
-- "Diária" é categoria de sistema: a RN16 depende dela existir.
INSERT INTO categoria_despesa (nome, sistema, criado_por)
SELECT c.nome, c.sistema, u.id
FROM (VALUES
    ('Diária',                     TRUE),
    ('Sementes e mudas',           FALSE),
    ('Adubo e fertilizantes',      FALSE),
    ('Defensivos',                 FALSE),
    ('Combustível',                FALSE),
    ('Manutenção de equipamentos', FALSE),
    ('Transporte',                 FALSE),
    ('Embalagem',                  FALSE),
    ('Energia e água',             FALSE),
    ('Outros',                     FALSE)
) AS c(nome, sistema)
CROSS JOIN (SELECT id FROM usuario WHERE login = '${ADMIN_LOGIN}') u
ON CONFLICT (nome) DO NOTHING;

-- 3. Catálogo inicial de culturas (informadas pelo sindicato em VAL02)
INSERT INTO cultura (nome, tipo_ciclo_padrao, unidade_medida, criado_por)
SELECT c.nome, c.ciclo, c.unidade, u.id
FROM (VALUES
    ('Milho',     'LONGO', 'kg'),
    ('Feijão',    'LONGO', 'kg'),
    ('Fava',      'LONGO', 'kg'),
    ('Mandioca',  'LONGO', 'kg'),
    ('Urucum',    'LONGO', 'kg'),
    ('Castanha',  'LONGO', 'kg'),
    ('Caju',      'LONGO', 'kg'),
    ('Banana',    'CURTO', 'kg'),
    ('Hortaliça', 'CURTO', 'kg')
) AS c(nome, ciclo, unidade)
CROSS JOIN (SELECT id FROM usuario WHERE login = '${ADMIN_LOGIN}') u
ON CONFLICT (nome) DO NOTHING;

-- ============ PARTE 2: DADOS DE EXEMPLO ============

DO $$
    DECLARE
        v_admin     BIGINT;
        v_titular   BIGINT;
        v_suplente  BIGINT;
        v_cat_diar  BIGINT;
        v_cat_sem   BIGINT;
        v_cat_adubo BIGINT;
        v_ref_2025  BIGINT;
        v_ref_2026  BIGINT;
        v_prod1     BIGINT;
        v_prod2     BIGINT;
        v_prop1     BIGINT;
        v_prop2     BIGINT;
        v_milho     BIGINT;
        v_banana    BIGINT;
        v_feijao    BIGINT;
        v_pl_milho  BIGINT;
        v_pl_banana BIGINT;
        v_pl_feijao BIGINT;
        v_diaria    BIGINT;
        v_despesa   BIGINT;
        v_cli_feira BIGINT;
        v_cli_mer   BIGINT;
        v_venda     BIGINT;
    BEGIN
        SELECT id INTO v_admin FROM usuario WHERE login = '${ADMIN_LOGIN}';
        IF v_admin IS NULL THEN
            RAISE EXCEPTION 'Execute a carga inicial antes dos dados de exemplo.';
        END IF;

        -- Usuários
        INSERT INTO usuario (nome, login, senha_hash, perfil) VALUES
            ('Titular de Exemplo',  'titular',  public.crypt('12345678', public.gen_salt('bf', 10)), 'LANCAMENTO'),
            ('Suplente de Exemplo', 'suplente', public.crypt('12345678', public.gen_salt('bf', 10)), 'LANCAMENTO'),
            ('Consulta de Exemplo', 'consulta', public.crypt('12345678', public.gen_salt('bf', 10)), 'CONSULTA')
        ON CONFLICT (login) DO NOTHING;
        SELECT id INTO v_titular  FROM usuario WHERE login = 'titular';
        SELECT id INTO v_suplente FROM usuario WHERE login = 'suplente';

        SELECT id INTO v_cat_diar  FROM categoria_despesa WHERE nome = 'Diária';
        SELECT id INTO v_cat_sem   FROM categoria_despesa WHERE nome = 'Sementes e mudas';
        SELECT id INTO v_cat_adubo FROM categoria_despesa WHERE nome = 'Adubo e fertilizantes';
        SELECT id INTO v_milho  FROM cultura WHERE nome = 'Milho';
        SELECT id INTO v_banana FROM cultura WHERE nome = 'Banana';
        SELECT id INTO v_feijao FROM cultura WHERE nome = 'Feijão';

        -- Valor de referência da diária (vigências sem sobreposição — RN17)
        INSERT INTO valor_referencia_diaria (valor, vigencia_inicio, vigencia_fim, criado_por)
        VALUES (70.00, '2025-01-01', '2025-12-31', v_admin) RETURNING id INTO v_ref_2025;
        INSERT INTO valor_referencia_diaria (valor, vigencia_inicio, vigencia_fim, criado_por)
        VALUES (80.00, '2026-01-01', NULL, v_admin) RETURNING id INTO v_ref_2026;

        -- Produtores (CPF fictício, só dígitos)
        INSERT INTO produtor (nome, documento, endereco, profissao, contato, consentimento_em, criado_por)
        VALUES ('Maria das Graças Silva', '11111111111', 'Sítio Boa Vista, zona rural', 'Agricultora',
                '(85) 90000-0001', '2026-01-10', v_titular) RETURNING id INTO v_prod1;
        -- Produtor 2 sem consentimento registrado: RN37 deve bloquear emissão de relatório
        INSERT INTO produtor (nome, documento, endereco, profissao, contato, consentimento_em, criado_por)
        VALUES ('José Raimundo Oliveira', '22222222222', 'Sítio Várzea Alegre, zona rural', 'Agricultor',
                '(85) 90000-0002', NULL, v_suplente) RETURNING id INTO v_prod2;

        -- Propriedades
        INSERT INTO propriedade (produtor_id, nome, area_total, localizacao, criado_por)
        VALUES (v_prod1, 'Sítio Boa Vista', 5.000, 'Zona rural', v_titular) RETURNING id INTO v_prop1;
        INSERT INTO propriedade (produtor_id, nome, area_total, localizacao, criado_por)
        VALUES (v_prod2, 'Sítio Várzea Alegre', 3.000, 'Zona rural', v_suplente) RETURNING id INTO v_prop2;

        -- Plantios (produtor 1: milho 2,0 ha e banana 1,0 ha; produtor 2: feijão 1,5 ha)
        INSERT INTO plantio (propriedade_id, cultura_id, area, tipo_ciclo, data_inicio, previsao_colheita, criado_por)
        VALUES (v_prop1, v_milho, 2.000, 'LONGO', '2026-02-10', '2026-07-15', v_titular) RETURNING id INTO v_pl_milho;
        INSERT INTO plantio (propriedade_id, cultura_id, area, tipo_ciclo, data_inicio, criado_por)
        VALUES (v_prop1, v_banana, 1.000, 'CURTO', '2025-10-01', v_titular) RETURNING id INTO v_pl_banana;
        INSERT INTO plantio (propriedade_id, cultura_id, area, tipo_ciclo, data_inicio, previsao_colheita, criado_por)
        VALUES (v_prop2, v_feijao, 1.500, 'LONGO', '2026-03-05', '2026-06-20', v_suplente) RETURNING id INTO v_pl_feijao;

        -- Produção
        INSERT INTO producao (plantio_id, quantidade, unidade_medida, data_fato, data_coleta, criado_por) VALUES
            (v_pl_milho,  1200.000, 'kg', '2026-07-20', '2026-07-21', v_titular),
            (v_pl_banana,  180.500, 'kg', '2026-08-05', '2026-08-06', v_titular),
            (v_pl_banana,  165.250, 'kg', '2026-09-02', '2026-09-03', v_titular),
            (v_pl_feijao,  450.000, 'kg', '2026-06-25', '2026-06-26', v_suplente);

        -- Diária FAMILIAR: sem despesa; valor vem da referência vigente (RN13)
        INSERT INTO diaria (plantio_id, valor_referencia_diaria_id, natureza, quantidade_dias,
                            valor_unitario, data_fato, data_coleta, criado_por)
        VALUES (v_pl_milho, v_ref_2026, 'FAMILIAR', 3.00, 80.00, '2026-03-12', '2026-03-13', v_titular);

        -- Diária PAGA: valor informado (RN15); gera despesa na categoria "Diária" (RN16)
        INSERT INTO diaria (plantio_id, natureza, quantidade_dias, valor_unitario,
                            data_fato, data_coleta, criado_por)
        VALUES (v_pl_milho, 'PAGA', 2.00, 90.00, '2026-03-20', '2026-03-21', v_titular)
        RETURNING id INTO v_diaria;

        INSERT INTO despesa (produtor_id, categoria_despesa_id, diaria_id, descricao, valor_total,
                             data_fato, data_coleta, criado_por)
        VALUES (v_prod1, v_cat_diar, v_diaria, 'Diária paga — capina', 180.00,
                '2026-03-20', '2026-03-21', v_titular) RETURNING id INTO v_despesa;
        INSERT INTO rateio_despesa (despesa_id, plantio_id, percentual, valor_rateado, criado_por)
        VALUES (v_despesa, v_pl_milho, 1.000000, 180.00, v_titular);

        -- Despesa 100% em um plantio
        INSERT INTO despesa (produtor_id, categoria_despesa_id, descricao, valor_total,
                             data_fato, data_coleta, criado_por)
        VALUES (v_prod1, v_cat_sem, 'Sementes de milho', 450.00,
                '2026-02-08', '2026-02-09', v_titular) RETURNING id INTO v_despesa;
        INSERT INTO rateio_despesa (despesa_id, plantio_id, percentual, valor_rateado, criado_por)
        VALUES (v_despesa, v_pl_milho, 1.000000, 450.00, v_titular);

        -- Despesa rateada por área (2,0 ha / 1,0 ha). 66,67 + 33,33 = 100,00 (RN07/RN09)
        INSERT INTO despesa (produtor_id, categoria_despesa_id, descricao, valor_total,
                             data_fato, data_coleta, criado_por)
        VALUES (v_prod1, v_cat_adubo, 'Adubo orgânico', 100.00,
                '2026-02-15', '2026-02-16', v_titular) RETURNING id INTO v_despesa;
        INSERT INTO rateio_despesa (despesa_id, plantio_id, percentual, valor_rateado, criado_por) VALUES
            (v_despesa, v_pl_milho,  0.666667, 66.67, v_titular),
            (v_despesa, v_pl_banana, 0.333333, 33.33, v_titular);

        -- Despesa do produtor 2
        INSERT INTO despesa (produtor_id, categoria_despesa_id, descricao, valor_total,
                             data_fato, data_coleta, criado_por)
        VALUES (v_prod2, v_cat_sem, 'Sementes de feijão', 120.00,
                '2026-03-01', '2026-03-02', v_suplente) RETURNING id INTO v_despesa;
        INSERT INTO rateio_despesa (despesa_id, plantio_id, percentual, valor_rateado, criado_por)
        VALUES (v_despesa, v_pl_feijao, 1.000000, 120.00, v_suplente);

        -- Clientes (um por canal)
        INSERT INTO cliente (nome, tipo_canal, criado_por) VALUES
            ('Mercadinho Central',            'COMERCIO',    v_titular),
            ('Feira Livre do Município',      'FEIRA',       v_titular),
            ('CEASA Regional',                'CEASA',       v_titular),
            ('Merenda Escolar Municipal',     'MERENDA',     v_titular),
            ('Cooperativa Agrícola Local',    'COOPERATIVA', v_titular),
            ('Comprador Avulso',              'OUTRO',       v_titular);
        SELECT id INTO v_cli_feira FROM cliente WHERE nome = 'Feira Livre do Município';
        SELECT id INTO v_cli_mer   FROM cliente WHERE nome = 'Merenda Escolar Municipal';

        -- Venda 1 (produtor 1): 450,00 + 150,00 = 600,00
        INSERT INTO venda (produtor_id, cliente_id, valor_total, data_fato, data_coleta, criado_por)
        VALUES (v_prod1, v_cli_feira, 600.00, '2026-07-25', '2026-07-26', v_titular) RETURNING id INTO v_venda;
        INSERT INTO item_venda (venda_id, plantio_id, quantidade, unidade_medida, valor_unitario, valor_total, criado_por) VALUES
            (v_venda, v_pl_milho,  100.000, 'kg', 4.50, 450.00, v_titular),
            (v_venda, v_pl_banana,  50.000, 'kg', 3.00, 150.00, v_titular);

        -- Venda 2 (produtor 2): 300 kg x 5,00 = 1500,00
        INSERT INTO venda (produtor_id, cliente_id, valor_total, data_fato, data_coleta, criado_por)
        VALUES (v_prod2, v_cli_mer, 1500.00, '2026-07-02', '2026-07-03', v_suplente) RETURNING id INTO v_venda;
        INSERT INTO item_venda (venda_id, plantio_id, quantidade, unidade_medida, valor_unitario, valor_total, criado_por)
        VALUES (v_venda, v_pl_feijao, 300.000, 'kg', 5.00, 1500.00, v_suplente);

        -- Emissão de relatório (só produtor 1, que tem consentimento — RN37).
        -- conteudo é um retrato do apurado na emissão (RN34).
        INSERT INTO emissao_relatorio (produtor_id, usuario_id, periodo_inicio, periodo_fim, conteudo, criado_por)
        VALUES (v_prod1, v_titular, '2026-01-01', '2026-09-30',
                jsonb_build_object(
                    'produtor', 'Maria das Graças Silva',
                    'receita_total', 600.00,
                    'despesa_total', 730.00,
                    'resultado', -130.00,
                    'observacao', 'Exemplo ilustrativo para testes'
                ), v_titular);
    END
$$;