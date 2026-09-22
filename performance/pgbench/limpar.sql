-- Remove tudo que os cenários pgbench criaram, preservando o resto do banco.
\set ON_ERROR_STOP on

DELETE FROM voto WHERE pauta_id IN (SELECT id FROM pauta WHERE titulo = 'bench-contador');
DELETE FROM sessao_votacao WHERE pauta_id IN (SELECT id FROM pauta WHERE titulo = 'bench-contador');
DELETE FROM pauta WHERE titulo = 'bench-contador';

DROP TABLE IF EXISTS bench_contador;
DROP TABLE IF EXISTS bench_contador_shard;
DROP SEQUENCE IF EXISTS bench_cpf_seq;
