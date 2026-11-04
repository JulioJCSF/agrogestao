-- A emissão é o retrato entregue ao produtor e tem uso probatório (INSS): depois de gravada, não
-- muda nem some (RN34). A aplicação só insere em emissao_relatorio; estes gatilhos recusam
-- UPDATE, DELETE e TRUNCATE vindos de qualquer origem (bug, script ou SQL manual).
-- Uma correção de dados futura precisa de migration própria e revisada, que desligue os gatilhos
-- explicitamente só naquele script (ALTER TABLE emissao_relatorio DISABLE TRIGGER ... e ENABLE
-- TRIGGER ... no fim), nunca pelo código da aplicação.
CREATE FUNCTION recusa_alteracao_emissao_relatorio() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'Emissão de relatório não pode ser alterada nem excluída (RN34).'
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER tg_emissao_relatorio_imutavel
    BEFORE UPDATE OR DELETE ON emissao_relatorio
    FOR EACH ROW EXECUTE FUNCTION recusa_alteracao_emissao_relatorio();

-- O gatilho de linha não pega TRUNCATE.
CREATE TRIGGER tg_emissao_relatorio_sem_truncate
    BEFORE TRUNCATE ON emissao_relatorio
    FOR EACH STATEMENT EXECUTE FUNCTION recusa_alteracao_emissao_relatorio();
