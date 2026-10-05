-- ### cliente
CREATE TABLE cliente (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(150) NOT NULL,
    tipo_canal     VARCHAR(30)  NOT NULL,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    criado_por     BIGINT       NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_cliente_canal CHECK (tipo_canal IN
        ('COMERCIO','FEIRA','CEASA','MERENDA','COOPERATIVA','OUTRO'))
);

-- ### venda
CREATE TABLE venda (
    id             BIGSERIAL PRIMARY KEY,
    produtor_id    BIGINT        NOT NULL REFERENCES produtor(id),
    cliente_id     BIGINT        NOT NULL REFERENCES cliente(id),
    valor_total    NUMERIC(12,2) NOT NULL,
    data_fato      DATE          NOT NULL,
    data_coleta    DATE          NOT NULL,
    criado_em      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    criado_por     BIGINT        NOT NULL REFERENCES usuario(id)
);
CREATE INDEX idx_venda_produtor_id ON venda (produtor_id, data_fato);

-- ### item_venda
CREATE TABLE item_venda (
    id              BIGSERIAL PRIMARY KEY,
    venda_id        BIGINT        NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
    plantio_id      BIGINT        NOT NULL REFERENCES plantio(id),
    quantidade      NUMERIC(12,3) NOT NULL,
    unidade_medida  VARCHAR(10)   NOT NULL,
    valor_unitario  NUMERIC(12,2) NOT NULL,
    valor_total     NUMERIC(12,2) NOT NULL,
    criado_em       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    criado_por      BIGINT        NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_item_venda_qtd   CHECK (quantidade > 0),
    CONSTRAINT ck_item_venda_valor CHECK (valor_total > 0)
);
CREATE INDEX idx_item_venda_plantio_id ON item_venda (plantio_id);