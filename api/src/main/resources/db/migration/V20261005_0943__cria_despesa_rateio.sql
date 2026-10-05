-- ### categoria_despesa
CREATE TABLE categoria_despesa (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(80) NOT NULL UNIQUE,
    sistema        BOOLEAN     NOT NULL DEFAULT FALSE,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    criado_por     BIGINT      NOT NULL REFERENCES usuario(id)
);

-- ### despesa
CREATE TABLE despesa (
    id                    BIGSERIAL PRIMARY KEY,
    produtor_id           BIGINT        NOT NULL REFERENCES produtor(id),
    categoria_despesa_id  BIGINT        NOT NULL REFERENCES categoria_despesa(id),
    diaria_id             BIGINT        UNIQUE REFERENCES diaria(id),
    descricao             VARCHAR(255)  NOT NULL,
    valor_total           NUMERIC(12,2) NOT NULL,
    data_fato             DATE          NOT NULL,
    data_coleta           DATE          NOT NULL,
    estornada             BOOLEAN       NOT NULL DEFAULT FALSE,
    criado_em             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    criado_por            BIGINT        NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_despesa_valor CHECK (valor_total > 0)
);
CREATE INDEX idx_despesa_produtor_id ON despesa (produtor_id, data_fato);

-- ### rateio_despesa
CREATE TABLE rateio_despesa (
    id             BIGSERIAL PRIMARY KEY,
    despesa_id     BIGINT        NOT NULL REFERENCES despesa(id) ON DELETE CASCADE,
    plantio_id     BIGINT        NOT NULL REFERENCES plantio(id),
    percentual     NUMERIC(9,6)  NOT NULL,
    valor_rateado  NUMERIC(12,2) NOT NULL,
    criado_em      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    criado_por     BIGINT        NOT NULL REFERENCES usuario(id),
    CONSTRAINT uk_rateio_despesa_plantio UNIQUE (despesa_id, plantio_id),
    CONSTRAINT ck_rateio_percentual CHECK (percentual > 0 AND percentual <= 1)
);
CREATE INDEX idx_rateio_despesa_plantio_id ON rateio_despesa (plantio_id);