-- ATENÇÃO: este arquivo NÃO é uma migration Flyway.
-- Ele é um dado sintético de teste, carregado manualmente para o cenário
-- de performance de apuração. Não mover para backend/src/main/resources/db/migration.
--
-- O script cria uma pauta com título iniciado em 'perf-apuracao', uma sessão
-- JÁ ENCERRADA (janela de 2h..1h atrás) e 500 mil votos com CPF único,
-- distribuindo SIM e NAO em partes iguais. Ao final roda ANALYZE em voto
-- para atualizar estatísticas e imprime o id da pauta criada.
--
-- Uso típico (via docker compose):
--   docker compose exec -T postgres psql -U votacao -d votacao \
--     < performance/seed-apuracao.sql

\set ON_ERROR_STOP on

DO $$
DECLARE
    v_pauta_id BIGINT;
    v_agora    TIMESTAMP := (now() AT TIME ZONE 'UTC');
BEGIN
    INSERT INTO pauta (titulo, descricao, data_criacao)
    VALUES (
        'perf-apuracao-' || to_char(v_agora, 'YYYYMMDDHH24MISS'),
        'Pauta sintética para medir apuração com 500 mil votos',
        v_agora
    )
    RETURNING id INTO v_pauta_id;

    INSERT INTO sessao_votacao (pauta_id, data_abertura, data_fechamento)
    VALUES (v_pauta_id, v_agora - INTERVAL '2 hours', v_agora - INTERVAL '1 hour');

    INSERT INTO voto (pauta_id, cpf_associado, opcao, data_voto)
    SELECT
        v_pauta_id,
        lpad(g::text, 11, '0'),
        CASE WHEN g % 2 = 0 THEN 'SIM' ELSE 'NAO' END,
        v_agora - INTERVAL '2 hours' + (g * INTERVAL '4 milliseconds')
    FROM generate_series(1, 500000) AS g;

    RAISE NOTICE 'pauta_id=%', v_pauta_id;
END $$;

ANALYZE voto;

SELECT id AS pauta_id, titulo
FROM pauta
WHERE titulo LIKE 'perf-apuracao-%'
ORDER BY id DESC
LIMIT 1;
