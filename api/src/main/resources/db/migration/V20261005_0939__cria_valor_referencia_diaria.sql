-- ### valor_referencia_diaria
CREATE TABLE valor_referencia_diaria (
    id              BIGSERIAL PRIMARY KEY,
    valor           NUMERIC(12,2) NOT NULL,
    vigencia_inicio DATE          NOT NULL,
    vigencia_fim    DATE,
    criado_em       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    criado_por      BIGINT        NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_vrd_valor CHECK (valor > 0),
    CONSTRAINT ex_vrd_vigencia EXCLUDE USING gist (
        daterange(vigencia_inicio, COALESCE(vigencia_fim, 'infinity'::date), '[]')
        WITH &&
    )
);