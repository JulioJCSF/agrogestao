-- ### diaria
CREATE TABLE diaria (
    id                          BIGSERIAL PRIMARY KEY,
    plantio_id                  BIGINT        NOT NULL REFERENCES plantio(id),
    valor_referencia_diaria_id  BIGINT        REFERENCES valor_referencia_diaria(id),
    natureza                    VARCHAR(10)   NOT NULL,
    quantidade_dias             NUMERIC(6,2)  NOT NULL,
    valor_unitario              NUMERIC(12,2) NOT NULL,
    data_fato                   DATE          NOT NULL,
    data_coleta                 DATE          NOT NULL,
    criado_em                   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    criado_por                  BIGINT        NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_diaria_natureza CHECK (natureza IN ('PAGA','FAMILIAR')),
    CONSTRAINT ck_diaria_qtd      CHECK (quantidade_dias > 0),
    CONSTRAINT ck_diaria_valor    CHECK (valor_unitario > 0),
    CONSTRAINT ck_diaria_ref      CHECK (
        (natureza = 'FAMILIAR' AND valor_referencia_diaria_id IS NOT NULL)
        OR natureza = 'PAGA'
    )
);
CREATE INDEX idx_diaria_plantio_id ON diaria (plantio_id, data_fato);