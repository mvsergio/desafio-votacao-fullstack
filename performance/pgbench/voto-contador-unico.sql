-- Variante descartada: manter um contador por pauta atualizado a cada voto.
-- Todos os votos da mesma pauta disputam a mesma linha e serializam no lock.
-- pgbench -D pauta=<id> -f voto-contador-unico.sql
INSERT INTO voto (pauta_id, cpf_associado, opcao, data_voto)
VALUES (:pauta, lpad(nextval('bench_cpf_seq')::text, 11, '0'), 'SIM', now());
UPDATE bench_contador SET total = total + 1 WHERE pauta_id = :pauta;
