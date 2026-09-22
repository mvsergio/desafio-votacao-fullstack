-- Prepara os dados usados pelos cenários pgbench que comparam o custo de gravar um voto
-- no modelo atual (só INSERT) contra as variantes com contador materializado.
--
-- Não é migration Flyway: são objetos de teste, criados e removidos manualmente.
-- Rode com psql e anote o id impresso no final; ele é passado ao pgbench com -D pauta=<id>.

\set ON_ERROR_STOP on

INSERT INTO pauta (titulo, descricao, data_criacao)
VALUES (
    'bench-contador',
    'Pauta usada apenas para medir o custo de gravar voto',
    (now() AT TIME ZONE 'UTC')
)
RETURNING id AS pauta_id \gset

CREATE SEQUENCE IF NOT EXISTS bench_cpf_seq;

DROP TABLE IF EXISTS bench_contador;
DROP TABLE IF EXISTS bench_contador_shard;

-- Variante 1: um contador por pauta. Todos os votos da pauta atualizam esta linha.
CREATE TABLE bench_contador (
    pauta_id BIGINT PRIMARY KEY,
    total    BIGINT NOT NULL DEFAULT 0
);
INSERT INTO bench_contador (pauta_id, total) VALUES (:pauta_id, 0);

-- Variante 2: 16 contadores por pauta. A escrita se espalha, a leitura soma.
CREATE TABLE bench_contador_shard (
    pauta_id BIGINT,
    shard    INT,
    total    BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (pauta_id, shard)
);
INSERT INTO bench_contador_shard (pauta_id, shard, total)
SELECT :pauta_id, s, 0 FROM generate_series(0, 15) AS s;

\echo ''
\echo 'Use este id nos comandos do pgbench (-D pauta=<id>):'
SELECT :pauta_id AS pauta_id;
