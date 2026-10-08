-- ### producao
CREATE TABLE producao (
    id              BIGSERIAL PRIMARY KEY,
    plantio_id      BIGINT        NOT NULL REFERENCES plantio(id),
    quantidade      NUMERIC(12,3) NOT NULL,
    unidade_medida  VARCHAR(10)   NOT NULL,
    data_fato       DATE          NOT NULL,
    data_coleta     DATE          NOT NULL,
    criado_em       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    criado_por      BIGINT        NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_producao_qtd CHECK (quantidade > 0)
);
CREATE INDEX idx_producao_plantio_id ON producao (plantio_id, data_fato);