CREATE TABLE IF NOT EXISTS sprint3_controle_vagas (
    id INTEGER PRIMARY KEY
);

INSERT INTO sprint3_controle_vagas (id) VALUES (1) ON CONFLICT DO NOTHING;

CREATE SEQUENCE IF NOT EXISTS sprint3_quarto_seq;
CREATE SEQUENCE IF NOT EXISTS sprint3_internacao_seq;

CREATE TABLE IF NOT EXISTS sprint3_quarto (
    id BIGINT PRIMARY KEY,
    numero INTEGER NOT NULL UNIQUE,
    andar INTEGER NOT NULL,
    capacidade INTEGER NOT NULL CHECK (capacidade > 0)
);

CREATE TABLE IF NOT EXISTS sprint3_ocupacao (
    internacao_id BIGINT PRIMARY KEY,
    quarto_id BIGINT NOT NULL REFERENCES sprint3_quarto (id)
);

CREATE INDEX IF NOT EXISTS sprint3_ocupacao_quarto_idx ON sprint3_ocupacao (quarto_id);

CREATE TABLE IF NOT EXISTS sprint3_internacao (
    id BIGINT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    profissional_id BIGINT NOT NULL,
    quarto_id BIGINT NOT NULL,
    data_entrada DATE NOT NULL,
    data_prevista_alta DATE NOT NULL,
    data_efetiva_alta DATE,
    observacoes TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS sprint3_estadia (
    internacao_id BIGINT NOT NULL REFERENCES sprint3_internacao (id),
    ordem INTEGER NOT NULL,
    quarto_id BIGINT NOT NULL,
    numero_quarto INTEGER NOT NULL,
    data_entrada DATE NOT NULL,
    data_saida DATE,
    PRIMARY KEY (internacao_id, ordem),
    CHECK (data_saida IS NULL OR data_saida >= data_entrada)
);
