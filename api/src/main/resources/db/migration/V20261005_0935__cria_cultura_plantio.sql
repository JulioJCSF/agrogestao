-- ### cultura
CREATE TABLE cultura (
    id                 BIGSERIAL PRIMARY KEY,
    nome               VARCHAR(100) NOT NULL UNIQUE,
    tipo_ciclo_padrao  VARCHAR(10)  NOT NULL,
    unidade_medida     VARCHAR(10)  NOT NULL,
    criado_em          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    criado_por         BIGINT       NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_cultura_ciclo CHECK (tipo_ciclo_padrao IN ('CURTO','LONGO'))
);

-- ### plantio
CREATE TABLE plantio (
    id                  BIGSERIAL PRIMARY KEY,
    propriedade_id      BIGINT       NOT NULL REFERENCES propriedade(id),
    cultura_id          BIGINT       NOT NULL REFERENCES cultura(id),
    area                NUMERIC(12,3) NOT NULL,
    tipo_ciclo          VARCHAR(10)  NOT NULL,
    data_inicio         DATE         NOT NULL,
    previsao_colheita   DATE,
    data_encerramento   DATE,
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    criado_por          BIGINT       NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_plantio_area      CHECK (area > 0),
    CONSTRAINT ck_plantio_ciclo     CHECK (tipo_ciclo IN ('CURTO','LONGO')),
    CONSTRAINT ck_plantio_encerra   CHECK (data_encerramento IS NULL
                                           OR data_encerramento >= data_inicio)
);
CREATE INDEX idx_plantio_propriedade_id ON plantio (propriedade_id);
CREATE INDEX idx_plantio_cultura_id     ON plantio (cultura_id);