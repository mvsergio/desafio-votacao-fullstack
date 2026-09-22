-- Modelo atual: o voto é só um INSERT, e a unicidade é garantida pela constraint uk_voto_pauta_cpf.
-- pgbench -D pauta=<id> -f voto-insert.sql
INSERT INTO voto (pauta_id, cpf_associado, opcao, data_voto)
VALUES (:pauta, lpad(nextval('bench_cpf_seq')::text, 11, '0'), 'SIM', now());
