-- ### produtor
CREATE TABLE produtor (
    id                BIGSERIAL PRIMARY KEY,
    nome              VARCHAR(150) NOT NULL,
    documento         CHAR(11)     NOT NULL UNIQUE,
    endereco          VARCHAR(255),
    profissao         VARCHAR(100),
    contato           VARCHAR(100),
    consentimento_em  DATE,
    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    criado_por        BIGINT       NOT NULL REFERENCES usuario(id)
);
CREATE INDEX idx_produtor_nome ON produtor (nome);

-- ###propriedade
CREATE TABLE propriedade (
    id             BIGSERIAL PRIMARY KEY,
    produtor_id    BIGINT       NOT NULL REFERENCES produtor(id),
    nome           VARCHAR(150) NOT NULL,
    area_total     NUMERIC(12,3),
    localizacao    VARCHAR(255),
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    criado_por     BIGINT       NOT NULL REFERENCES usuario(id)
);
CREATE INDEX idx_propriedade_produtor_id ON propriedade (produtor_id);