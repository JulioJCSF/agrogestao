-- ### emissao_relatorio
CREATE TABLE emissao_relatorio (
    id               BIGSERIAL PRIMARY KEY,
    produtor_id      BIGINT      NOT NULL REFERENCES produtor(id),
    usuario_id       BIGINT      NOT NULL REFERENCES usuario(id),
    data_emissao     TIMESTAMPTZ NOT NULL DEFAULT now(),
    periodo_inicio   DATE        NOT NULL,
    periodo_fim      DATE        NOT NULL,
    conteudo         JSONB       NOT NULL,
    criado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    criado_por       BIGINT      NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_emissao_periodo CHECK (periodo_fim >= periodo_inicio)
);
CREATE INDEX idx_emissao_relatorio_produtor_id
    ON emissao_relatorio (produtor_id, data_emissao DESC);