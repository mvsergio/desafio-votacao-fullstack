CREATE TABLE pauta (
    id            BIGSERIAL     PRIMARY KEY,
    titulo        VARCHAR(150)  NOT NULL,
    descricao     VARCHAR(1000),
    data_criacao  TIMESTAMP     NOT NULL
);

CREATE TABLE sessao_votacao (
    id               BIGSERIAL PRIMARY KEY,
    pauta_id         BIGINT    NOT NULL,
    data_abertura    TIMESTAMP NOT NULL,
    data_fechamento  TIMESTAMP NOT NULL,
    CONSTRAINT uk_sessao_votacao_pauta UNIQUE (pauta_id),
    CONSTRAINT fk_sessao_votacao_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id)
);

CREATE TABLE voto (
    id             BIGSERIAL   PRIMARY KEY,
    pauta_id       BIGINT      NOT NULL,
    cpf_associado  VARCHAR(11) NOT NULL,
    opcao          VARCHAR(3)  NOT NULL,
    data_voto      TIMESTAMP   NOT NULL,
    CONSTRAINT fk_voto_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id),
    CONSTRAINT uk_voto_pauta_cpf UNIQUE (pauta_id, cpf_associado)
);

CREATE INDEX ix_voto_pauta_opcao ON voto (pauta_id, opcao);
