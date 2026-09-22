# Teste de performance — k6

Cenário de carga do fluxo de votação:

1. Setup cria uma pauta e abre uma sessão de 10 minutos.
2. Ramp-up até ~200 VUs registrando votos com CPFs únicos.
3. Thresholds: `http_req_duration p95 < 300ms` e `http_req_failed < 1%`.

## Pré-requisitos

- Docker (para rodar `grafana/k6` sem instalar nada localmente).
- Backend acessível em `http://localhost:8080/api/v1`.
- Validação de CPF **desligada** durante o teste. O `FakeCpfClient` sorteia
  aleatoriamente entre `CPF inválido`, `ABLE_TO_VOTE` e `UNABLE_TO_VOTE`, o que
  faria ~2/3 dos votos serem rejeitados e distorceria o teste de carga. Suba com
  `VOTACAO_VALIDACAO_CPF_HABILITADA=false` para bypassar essa checagem.

Exemplo, subindo a stack local com a validação desligada:

```bash
VOTACAO_VALIDACAO_CPF_HABILITADA=false docker compose up -d --build
```

No PowerShell:

```powershell
$env:VOTACAO_VALIDACAO_CPF_HABILITADA = "false"
docker compose up -d --build
```

## Executando o k6

Do diretório raiz do projeto:

```bash
docker run --rm -i \
  -v "$PWD/performance:/scripts" \
  -e BASE_URL=http://host.docker.internal:8080/api/v1 \
  grafana/k6 run /scripts/votacao.js
```

PowerShell (Windows):

```powershell
docker run --rm -i `
  -v "${PWD}/performance:/scripts" `
  -e BASE_URL=http://host.docker.internal:8080/api/v1 `
  grafana/k6 run /scripts/votacao.js
```

Se `host.docker.internal` não resolver no seu Docker, use a rede do compose:

```bash
docker run --rm -i \
  --network desafio-votacao-fullstack_default \
  -v "$PWD/performance:/scripts" \
  -e BASE_URL=http://backend:8080/api/v1 \
  grafana/k6 run /scripts/votacao.js
```

Ao final o k6 imprime o resumo com métricas e o resultado dos thresholds
(`✓` passou, `✗` falhou).

## Cenário de apuração (bônus 2 — centenas de milhares de votos)

Mede `GET /api/v1/pautas/{id}/resultado` sob carga contra uma pauta que já
tem **500 mil votos** persistidos. É o cenário usado para validar o requisito
de performance com esse volume.

Dois arquivos participam:

- `seed-apuracao.sql` — carga sintética. **Não é migration Flyway**, é
  dado de teste; roda manualmente. Cria a pauta (título `perf-apuracao-*`),
  uma sessão já encerrada, insere 500 mil votos com CPF único e distribui
  SIM/NAO em partes iguais. Ao final roda `ANALYZE voto` e imprime o
  `pauta_id` gerado.
- `apuracao.js` — cenário k6 enxuto, `constant-vus` durante `DURATION`,
  medindo o endpoint de resultado. Recebe `PAUTA_ID`, `VUS` e `DURATION`
  por variável de ambiente. Mesmos moldes de threshold do `votacao.js`
  (`p95<300ms`, `http_req_failed<1%`).

### Rodando o seed

Com o compose de pé (`docker compose up -d`):

```bash
docker compose exec -T postgres psql -U votacao -d votacao \
  < performance/seed-apuracao.sql
```

PowerShell:

```powershell
Get-Content performance/seed-apuracao.sql `
  | docker compose exec -T postgres psql -U votacao -d votacao
```

A última linha do output traz `pauta_id` (também repetido pelo
`RAISE NOTICE`). Guarde o valor para passar ao k6.

### Rodando o k6 de apuração

```bash
docker run --rm -i \
  --network desafio-votacao-fullstack_default \
  -v "$PWD/performance:/scripts" \
  -e BASE_URL=http://backend:8080/api/v1 \
  -e PAUTA_ID=123 \
  -e VUS=10 \
  -e DURATION=30s \
  grafana/k6 run /scripts/apuracao.js
```

PowerShell:

```powershell
docker run --rm -i `
  --network desafio-votacao-fullstack_default `
  -v "${PWD}/performance:/scripts" `
  -e BASE_URL=http://backend:8080/api/v1 `
  -e PAUTA_ID=123 `
  -e VUS=10 `
  -e DURATION=30s `
  grafana/k6 run /scripts/apuracao.js
```

Para varrer níveis de carga (`1, 5, 10, 30` VUs) basta repetir a chamada
alternando `VUS`. Um controle útil é rodar a mesma bateria contra uma
pauta vazia (id de uma pauta recém-criada, sem votos) para separar custo
do endpoint do custo real da apuração.

## O que garante a performance no backend

- **Constraint única `uk_voto_pauta_cpf`** em `voto (pauta_id, cpf_associado)`:
  o service não faz `SELECT` antes de inserir — chama `saveAndFlush` e traduz
  `DataIntegrityViolationException` em `VotoDuplicadoException`.
- **Apuração por query agregada** (`SELECT opcao, COUNT(*) ... GROUP BY opcao`)
  apoiada pelo índice `ix_voto_pauta_opcao (pauta_id, opcao)`.
- **Threads virtuais** habilitadas (`spring.threads.virtual.enabled: true`).
- **HikariCP** dimensionado (`maximum-pool-size: 30`, `minimum-idle: 10`).
- **`open-in-view: false`** para não segurar conexão fora da transação.
- **Sem N+1 na listagem** de pautas: uma consulta paginada em `pauta` +
  uma única consulta `findByPautaIdIn` para as sessões.
- **Cache Caffeine na apuração de sessões encerradas** — `VotoService.apurar`
  usa `@Cacheable(cacheNames = "apuracao", key = "#pautaId")` com `unless`
  que exclui `EM_ANDAMENTO` e `SEM_SESSAO`. O spec é configurável em runtime
  via `VOTACAO_CACHE_APURACAO_SPEC` (default
  `maximumSize=10000,expireAfterWrite=1h`). Para medir o cenário sem cache
  basta subir com `VOTACAO_CACHE_APURACAO_SPEC=maximumSize=0`.

## Resultado

Última execução local (Docker Desktop, PostgreSQL 18 no mesmo host, `VOTACAO_VALIDACAO_CPF_HABILITADA=false`):

```text
  █ THRESHOLDS

    http_req_duration
    ✓ 'p(95)<300' p(95)=212.91ms

      {operacao:voto}
      ✓ 'p(95)<300' p(95)=212.91ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%

    voto_ok
    ✓ 'rate>0.99' rate=100.00%


  █ TOTAL RESULTS

    checks_total.......: 380231  1581.943709/s
    checks_succeeded...: 100.00% 380231 out of 380231
    checks_failed......: 0.00%   0 out of 380231

    ✓ status 201

    voto_ok........................: 100.00% 380231 out of 380231

    http_req_duration..............: avg=90.4ms  min=3.7ms  med=85.46ms max=1.06s p(90)=167.92ms p(95)=212.91ms
    http_req_failed................: 0.00%   0 out of 380234
    http_reqs......................: 380234  1581.95619/s

    iterations.....................: 380231  1581.943709/s
    vus............................: 3       min=1                max=200
    vus_max........................: 200     min=200              max=200

Resultado final da pauta: totalSim=190113, totalNao=190118, totalVotos=380231, situacao=EM_ANDAMENTO
```

## Resultado — cenário de apuração (500 mil votos)

Última execução local (Docker Desktop, PostgreSQL 18 no mesmo host, pauta com 500 mil votos, sessão já encerrada, `VOTACAO_VALIDACAO_CPF_HABILITADA=false`). Cada linha é uma execução independente do `apuracao.js` com `constant-vus` por 30 s.

Plano do Postgres para a consulta agregada (`EXPLAIN (ANALYZE, BUFFERS)` executada diretamente no banco depois do seed), demonstrando o `Index Only Scan` que a versão atual usa:

```text
Finalize GroupAggregate  (cost=1000.45..9723.81 rows=2 width=12) (actual time=45.953..48.594 rows=2 loops=1)
  Group Key: opcao
  Buffers: shared hit=399
  ->  Gather Merge
        Workers Planned: 2
        Workers Launched: 2
        Buffers: shared hit=399
        ->  Partial GroupAggregate
              ->  Parallel Index Only Scan using ix_voto_pauta_opcao on voto v1_0
                    Index Cond: (pauta_id = 31)
                    Heap Fetches: 0
                    Buffers: shared hit=399
Execution Time: 48.663 ms
```

Antes da otimização (com `COUNT(v.id)`, que forçava heap scan) a mesma query pegava `Parallel Bitmap Heap Scan` com **4.623 buffers** e execution time **~82 ms** — 12× mais páginas.

k6 no endpoint `GET /pautas/{id}/resultado`:

```text
  █ THRESHOLDS (todas as rodadas passaram, cache Caffeine ativo)

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%

    apuracao_ok
    ✓ 'rate>0.99' rate=100.00%

    http_req_duration
    ✓ 'p(95)<300'  (todas as rodadas)


  █ RESUMO por nível de carga (30 s constant-vus, cache ligado)

    VUs=1   iterations=39124   ~1.304 req/s   p(95)=1.51ms
    VUs=5   iterations=176428  ~5.881 req/s   p(95)=1.46ms
    VUs=10  iterations=313807  ~10.460 req/s  p(95)=1.68ms
    VUs=30  iterations=417571  ~13.919 req/s  p(95)=4.51ms


  █ COMPARATIVO — mesma pauta de 500 mil votos, sem cache

    Baseline (COUNT(v.id), Bitmap Heap Scan):
      VUs=1   p(95)=118.82ms
      VUs=5   p(95)=477.93ms   ✗ threshold estourado
      VUs=10  p(95)=1.16s      ✗
      VUs=30  p(95)=2.67s      ✗

    Só contagem (COUNT(*), Index Only Scan, sem cache):
      VUs=1   p(95)=61.32ms
      VUs=5   p(95)=275.48ms
      VUs=10  p(95)=517.77ms   ✗
      VUs=30  p(95)=1.75s      ✗

    Só contagem + cache Caffeine (versão atual):
      VUs=1   p(95)=1.51ms
      VUs=5   p(95)=1.46ms
      VUs=10  p(95)=1.68ms
      VUs=30  p(95)=4.51ms
```

Para medir a base (sem cache) sem mudar o código, basta subir com `VOTACAO_CACHE_APURACAO_SPEC=maximumSize=0`. Para forçar re-uso de plano ruim depois de uma carga grande, rodar antes das medidas: `docker compose exec -T postgres psql -U votacao -d votacao -c "ANALYZE voto;"`.

## Contador materializado: por que foi descartado

O caminho óbvio para acelerar a apuração seria manter um contador por pauta, somando 1 a cada voto, e ler só esse número. O problema é que isso muda o custo de lugar: todos os votos da mesma pauta passam a atualizar a mesma linha e serializam no lock. Os scripts em `pgbench/` medem isso direto no banco, sem a aplicação no meio.

Só o Postgres precisa estar de pé:

```bash
docker compose up -d postgres
docker compose cp performance/pgbench postgres:/tmp/pgbench
docker compose exec -T postgres psql -U votacao -d votacao -f /tmp/pgbench/preparar.sql
```

O último comando imprime o id da pauta criada. Use esse id nos três cenários (50 clientes, 15 segundos cada):

```bash
docker compose exec -T postgres pgbench -n -c 50 -T 15 -D pauta=<id> -f /tmp/pgbench/voto-insert.sql -U votacao votacao
docker compose exec -T postgres pgbench -n -c 50 -T 15 -D pauta=<id> -f /tmp/pgbench/voto-contador-unico.sql -U votacao votacao
docker compose exec -T postgres pgbench -n -c 50 -T 15 -D pauta=<id> -f /tmp/pgbench/voto-contador-shards.sql -U votacao votacao
```

No final, remover os objetos de teste:

```bash
docker compose exec -T postgres psql -U votacao -d votacao -f /tmp/pgbench/limpar.sql
```

Resultado em duas execuções nesta máquina (Docker Desktop, 8 vCPU), com volumes diferentes na tabela `voto`:

| Cenário | Execução 1 (tabela com ~1,3 mi de votos) | Execução 2 (tabela menor) |
|---|---|---|
| Modelo atual, só INSERT | 3.419 tps · 14,6 ms | 4.929 tps · 10,1 ms |
| Contador único por pauta | 176 tps · 283,5 ms | 211 tps · 236,5 ms |
| 16 contadores por pauta | 1.554 tps · 32,2 ms | 3.265 tps · 15,3 ms |

Os números absolutos variam com a máquina e com o volume; a ordem de grandeza não. O contador único fica 20 a 25 vezes mais lento que só inserir, e nem o sharding em 16 linhas alcança o modelo atual. Como o caminho de escrita é justamente o que o enunciado estressa (centenas de milhares de votos), a leitura foi resolvida com cache, que não custa nada na escrita.
