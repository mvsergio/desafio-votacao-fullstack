-- Variante descartada: 16 contadores por pauta para espalhar a contenção da variante anterior.
-- A leitura passa a somar os shards; a escrita melhora, mas continua pior que só inserir.
-- pgbench -D pauta=<id> -f voto-contador-shards.sql
\set shard random(0, 15)
INSERT INTO voto (pauta_id, cpf_associado, opcao, data_voto)
VALUES (:pauta, lpad(nextval('bench_cpf_seq')::text, 11, '0'), 'SIM', now());
UPDATE bench_contador_shard SET total = total + 1 WHERE pauta_id = :pauta AND shard = :shard;
