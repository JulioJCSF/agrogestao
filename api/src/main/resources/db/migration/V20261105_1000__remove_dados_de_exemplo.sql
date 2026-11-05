-- A parte 2 da V20261005_1020 gravou dados de exemplo em todo banco, inclusive no de produção:
-- usuários com a senha conhecida 12345678, produtores com nome de pessoa, lançamentos, clientes,
-- referências de diária e uma emissão em formato antigo, que quebra a leitura dos relatórios.
-- A migration aplicada não pode mudar (checksum do Flyway); esta remove o que ela gravou.
--
-- Só sai o que tem as chaves dos exemplos; nada mais é tocado:
--   produtores   documento '11111111111' ou '22222222222' (CPFs inválidos, que a API recusa:
--                só a migration pode tê-los gravado), com tudo que pende deles;
--   referências  (70,00, 2025-01-01, 2025-12-31) e (80,00, 2026-01-01, sem fim), criadas pelo admin;
--   clientes     os 6 nomes da carga, criados pelo usuário titular;
--   usuários     logins titular, suplente e consulta, com os nomes "... de Exemplo".
--
-- Sem CASCADE: se um registro de outro produtor apontar para um plantio de exemplo, a chave
-- estrangeira derruba a migration. É melhor parar o deploy do que apagar dado alheio.
--
-- Referências, clientes e usuários podem estar em uso num banco de dev. Referência e cliente em uso
-- ficam; usuário em uso fica inativo e com senha aleatória, para o login antigo não entrar mais.
--
-- Os gatilhos da emissão (V20261104_1000, RN34) são desligados só aqui, para apagar as emissões dos
-- produtores de exemplo, e religados no fim deste script.

ALTER TABLE emissao_relatorio DISABLE TRIGGER tg_emissao_relatorio_imutavel;
ALTER TABLE emissao_relatorio DISABLE TRIGGER tg_emissao_relatorio_sem_truncate;

DO $$
    DECLARE
        v_linhas BIGINT;
    BEGIN
        CREATE TEMPORARY TABLE exemplo_produtor ON COMMIT DROP AS
            SELECT id FROM produtor WHERE documento IN ('11111111111', '22222222222');
        CREATE TEMPORARY TABLE exemplo_plantio ON COMMIT DROP AS
            SELECT pl.id
            FROM plantio pl
            JOIN propriedade pr ON pr.id = pl.propriedade_id
            WHERE pr.produtor_id IN (SELECT id FROM exemplo_produtor);

        DELETE FROM emissao_relatorio WHERE produtor_id IN (SELECT id FROM exemplo_produtor);
        GET DIAGNOSTICS v_linhas = ROW_COUNT;
        RAISE NOTICE 'Dados de exemplo: % emissões apagadas.', v_linhas;

        DELETE FROM item_venda
        WHERE venda_id IN (SELECT id FROM venda WHERE produtor_id IN (SELECT id FROM exemplo_produtor));
        DELETE FROM venda WHERE produtor_id IN (SELECT id FROM exemplo_produtor);
        GET DIAGNOSTICS v_linhas = ROW_COUNT;
        RAISE NOTICE 'Dados de exemplo: % vendas apagadas, com os itens.', v_linhas;

        -- A despesa da diária paga aponta para a diária: sai antes dela.
        DELETE FROM rateio_despesa
        WHERE despesa_id IN (SELECT id FROM despesa WHERE produtor_id IN (SELECT id FROM exemplo_produtor));
        DELETE FROM despesa WHERE produtor_id IN (SELECT id FROM exemplo_produtor);
        GET DIAGNOSTICS v_linhas = ROW_COUNT;
        RAISE NOTICE 'Dados de exemplo: % despesas apagadas, com os rateios.', v_linhas;

        DELETE FROM diaria WHERE plantio_id IN (SELECT id FROM exemplo_plantio);
        GET DIAGNOSTICS v_linhas = ROW_COUNT;
        RAISE NOTICE 'Dados de exemplo: % diárias apagadas.', v_linhas;
        DELETE FROM producao WHERE plantio_id IN (SELECT id FROM exemplo_plantio);
        GET DIAGNOSTICS v_linhas = ROW_COUNT;
        RAISE NOTICE 'Dados de exemplo: % produções apagadas.', v_linhas;

        DELETE FROM plantio WHERE id IN (SELECT id FROM exemplo_plantio);
        GET DIAGNOSTICS v_linhas = ROW_COUNT;
        RAISE NOTICE 'Dados de exemplo: % plantios apagados.', v_linhas;
        DELETE FROM propriedade WHERE produtor_id IN (SELECT id FROM exemplo_produtor);
        GET DIAGNOSTICS v_linhas = ROW_COUNT;
        RAISE NOTICE 'Dados de exemplo: % propriedades apagadas.', v_linhas;
        DELETE FROM produtor WHERE id IN (SELECT id FROM exemplo_produtor);
        GET DIAGNOSTICS v_linhas = ROW_COUNT;
        RAISE NOTICE 'Dados de exemplo: % produtores apagados.', v_linhas;
    END
$$;

ALTER TABLE emissao_relatorio ENABLE TRIGGER tg_emissao_relatorio_imutavel;
ALTER TABLE emissao_relatorio ENABLE TRIGGER tg_emissao_relatorio_sem_truncate;

DO $$
    DECLARE
        v_registro RECORD;
    BEGIN
        FOR v_registro IN
            SELECT r.id, r.valor, r.vigencia_inicio
            FROM valor_referencia_diaria r
            WHERE r.criado_por = (SELECT id FROM usuario WHERE login = '${ADMIN_LOGIN}')
              AND ((r.valor = 70.00 AND r.vigencia_inicio = DATE '2025-01-01' AND r.vigencia_fim = DATE '2025-12-31')
                OR (r.valor = 80.00 AND r.vigencia_inicio = DATE '2026-01-01' AND r.vigencia_fim IS NULL))
        LOOP
            BEGIN
                DELETE FROM valor_referencia_diaria WHERE id = v_registro.id;
                RAISE NOTICE 'Dados de exemplo: referência de diária % desde % apagada.',
                    v_registro.valor, v_registro.vigencia_inicio;
            EXCEPTION WHEN foreign_key_violation THEN
                RAISE NOTICE 'Dados de exemplo: referência de diária % desde % mantida, em uso.',
                    v_registro.valor, v_registro.vigencia_inicio;
            END;
        END LOOP;

        FOR v_registro IN
            SELECT c.id, c.nome
            FROM cliente c
            WHERE c.criado_por = (SELECT id FROM usuario WHERE login = 'titular')
              AND c.nome IN ('Mercadinho Central', 'Feira Livre do Município', 'CEASA Regional',
                             'Merenda Escolar Municipal', 'Cooperativa Agrícola Local', 'Comprador Avulso')
        LOOP
            BEGIN
                DELETE FROM cliente WHERE id = v_registro.id;
                RAISE NOTICE 'Dados de exemplo: cliente "%" apagado.', v_registro.nome;
            EXCEPTION WHEN foreign_key_violation THEN
                RAISE NOTICE 'Dados de exemplo: cliente "%" mantido, em uso.', v_registro.nome;
            END;
        END LOOP;

        FOR v_registro IN
            SELECT u.id, u.login
            FROM usuario u
            WHERE (u.login, u.nome) IN (('titular', 'Titular de Exemplo'),
                                        ('suplente', 'Suplente de Exemplo'),
                                        ('consulta', 'Consulta de Exemplo'))
        LOOP
            BEGIN
                DELETE FROM usuario WHERE id = v_registro.id;
                RAISE NOTICE 'Dados de exemplo: usuário % apagado.', v_registro.login;
            EXCEPTION WHEN foreign_key_violation THEN
                UPDATE usuario
                SET ativo         = FALSE,
                    senha_hash    = public.crypt(gen_random_uuid()::text, public.gen_salt('bf', 10)),
                    atualizado_em = now()
                WHERE id = v_registro.id;
                RAISE NOTICE 'Dados de exemplo: usuário % desativado, em uso; a senha foi trocada por um valor aleatório.',
                    v_registro.login;
            END;
        END LOOP;
    END
$$;
