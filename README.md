# Desafio Votação

Solução do desafio de sessões de votação em cooperativas: cadastro de pautas, abertura de sessões por tempo determinado, registro de votos (Sim/Não, um por associado) e apuração do resultado.

## Visão geral

Monorepo com backend REST em Java/Spring e frontend em React. Persistência em PostgreSQL, migrações via Flyway. Toda a interface pública fica em `/api/v1`. Em produção o frontend é servido pelo nginx com proxy interno para `/api/v1`; em desenvolvimento o Vite serve o app em `http://localhost:5173` e o browser chama o backend em `http://localhost:8080/api/v1` diretamente — o CORS já libera essa origem.

## Stack

Backend
- Java 25
- Spring Boot 4.1.1 (webmvc, data-jpa, validation, actuator)
- Flyway 11
- springdoc-openapi 3.1
- Lombok
- Maven Wrapper (`mvnw`)
- Testes: JUnit 5, MockMvc, Testcontainers (Postgres), JaCoCo

Frontend
- Node 22
- React 19, React Router 7
- TypeScript 6
- Vite 8
- Vitest + Testing Library

Infra local
- PostgreSQL 18 (Alpine)
- Docker Compose
- k6 (teste de carga)

## Como executar

Pré-requisitos comuns: Git, Docker Desktop (ou engine equivalente).

Se você só tem JDK 17 ou 21 instalado (o backend exige JDK 25), prefira o caminho (a) via Docker Compose: ele constrói e executa tudo dentro dos containers e não precisa de Java na máquina host.

### (a) Tudo via Docker Compose

Sobe Postgres, backend e frontend em containers. Nada precisa estar instalado além do Docker.

```bash
docker compose up -d --build
```

- Frontend: http://localhost:3000
- Backend:  http://localhost:8080/api/v1
- Swagger:  http://localhost:8080/swagger-ui.html
- Postgres: `localhost:5432` (usuário/senha/db = `votacao`)

Para desligar tudo:

```bash
docker compose down
```

Para rodar o teste de carga sem chamar o serviço externo de CPF (ver `performance/README.md`):

```bash
# bash
VOTACAO_VALIDACAO_CPF_HABILITADA=false docker compose up -d --build
```

```powershell
# PowerShell
$env:VOTACAO_VALIDACAO_CPF_HABILITADA = "false"
docker compose up -d --build
```

### (b) Local para desenvolvimento

Postgres continua no Docker; backend e frontend rodam nativos.

Pré-requisitos adicionais:
- JDK 25 (Temurin recomendado)
- Node.js 22 e npm 10+

Passos:

```bash
# 1) Postgres via compose (só o banco)
docker compose up -d postgres

# 2) Backend (na pasta backend/)
cd backend
./mvnw spring-boot:run
# Windows PowerShell: .\mvnw.cmd spring-boot:run

# 3) Frontend (em outro terminal, na pasta frontend/)
cd frontend
npm install
npm run dev
```

- Frontend dev: http://localhost:5173
- Backend:      http://localhost:8080/api/v1
- Swagger:      http://localhost:8080/swagger-ui.html

O CORS do backend já libera `http://localhost:5173` por padrão (ajustável via `VOTACAO_CORS_ORIGENS`).

## Endpoints

Todos sob o prefixo `/api/v1`.

| Método | Caminho                              | Descrição                                                     |
|--------|--------------------------------------|---------------------------------------------------------------|
| POST   | `/pautas`                            | Cadastra uma pauta                                            |
| GET    | `/pautas`                            | Lista pautas paginadas com status da sessão                   |
| GET    | `/pautas/{id}`                       | Detalha uma pauta e sua sessão                                |
| POST   | `/pautas/{pautaId}/sessao`           | Abre sessão de votação (duração no corpo ou 1 min por padrão) |
| POST   | `/pautas/{pautaId}/votos`            | Registra o voto de um associado                               |
| GET    | `/pautas/{pautaId}/resultado`        | Apura o resultado da pauta                                    |

OpenAPI: `GET /v3/api-docs` · Swagger UI: `GET /swagger-ui.html`.

## Decisões técnicas

- **Monorepo `backend/` + `frontend/`** — mantém as duas pontas versionadas juntas e simplifica o `docker-compose.yml`; não há tamanho ou time separado que justifique dois repositórios.
- **Camadas simples (controller → service → repository)** — sem interfaces por camada nem mapeadores automáticos; o domínio é pequeno o bastante para não ganhar nada com abstrações extras.
- **PostgreSQL + Flyway** — banco relacional é natural para o domínio (pauta, sessão, voto com unicidade) e o Flyway garante que o schema evolua igual em dev, teste e prod.
- **Fechamento da sessão calculado na leitura** — `SessaoVotacao.isAberta(agora)` compara com `Clock`; não há job/scheduler para "fechar" nada, o estado é sempre derivado. Menos código, menos race condition.
- **Voto único garantido por constraint `uk_voto_pauta_cpf`** — em vez de `SELECT` antes de inserir (que abre janela de corrida), o service faz `saveAndFlush` e traduz `DataIntegrityViolationException` em `VotoDuplicadoException`. Serve para 1 voto ou 200k.
- **Apuração agregada no banco** — `SELECT opcao, COUNT(*) ... GROUP BY opcao` apoiada pelo índice `ix_voto_pauta_opcao`. Nunca carrega a lista de votos na aplicação.
- **`ProblemDetail` (RFC 7807) no `@RestControllerAdvice`** — respostas de erro padronizadas, sem espalhar `ResponseEntity` de erro pelos services.
- **`Clock` injetado como bean** — permite testar comportamento dependente de tempo (sessão aberta/fechada) sem `Thread.sleep`.
- **Client fake de CPF com propriedade para desligar** — `FakeCpfClient` (default) sorteia resultados; `CpfClientSempreLibera` é ativado por `votacao.validacao-cpf.habilitada=false`. Troca via `@ConditionalOnProperty`, sem `if` no service. Útil para teste de carga e demonstração.
- **404 também para `UNABLE_TO_VOTE`** — o enunciado pede explicitamente "retornar 404 no client tb". Semanticamente 422/403 seriam mais fiéis (o recurso existe, o associado é que não pode votar), mas mantive 404 para respeitar o contrato.

## Testes e qualidade

Backend:

```bash
cd backend
./mvnw verify
```

- Unitários (services, controllers via MockMvc), integração de fluxo completo e teste de voto concorrente com Testcontainers Postgres.
- JaCoCo com mínimo de **80%** de cobertura de linhas configurado em `check` — o build falha se cair abaixo.
- Relatório em `backend/target/site/jacoco/index.html`.

Frontend:

```bash
cd frontend
npm test
```

- Vitest + Testing Library nos componentes e páginas principais.

Teste de carga (k6): fluxo de voto até ~200 VUs, thresholds `p95 < 300ms` e falha `< 1%`. Instruções, execução em Docker e resultado de referência em [`performance/README.md`](performance/README.md).

## Bônus 2 — performance

Cenário medido: `GET /api/v1/pautas/{id}/resultado` contra uma pauta com **500 mil votos** persistidos, com carga de 1, 5, 10 e 30 VUs constantes por 30 s (k6). Setup gerado por `performance/seed-apuracao.sql`, que também roda `ANALYZE voto` no final. Ambiente: Docker Desktop no Windows 11, Postgres 18 no mesmo host, backend `eclipse-temurin:25-jre`, pool Hikari com 30 conexões, threads virtuais habilitadas. Os números abaixo são de ambiente local — servem para comparar cenários entre si, não como SLA.

Antes x depois na mesma pauta de 500 mil votos:

| Estágio               | Plano do Postgres                                | Buffers do plano | Tempo de query | p95 @ 1 VU | p95 @ 5 VUs | p95 @ 10 VUs | p95 @ 30 VUs |
|-----------------------|--------------------------------------------------|------------------|----------------|------------|-------------|--------------|--------------|
| Antes                 | Parallel Bitmap Heap Scan + HashAggregate        | 4.623 páginas    | 82 ms          | 118,82 ms  | 477,93 ms   | 1,16 s       | 2,67 s       |
| Depois — só contagem  | Parallel Index Only Scan em `ix_voto_pauta_opcao` | 399 páginas      | 49 ms          | 61,32 ms   | 275,48 ms   | 517,77 ms    | 1,75 s       |
| Depois — cache        | Caffeine local (não vai ao banco)                | —                | —              | 1,51 ms    | 1,46 ms     | 1,68 ms      | 4,51 ms      |

O que mudou:

- **Contagem que permite Index Only Scan.** A consulta agregada trocou `COUNT(v.id)` por `COUNT(*)`; sem referenciar coluna do heap, o Postgres serve tudo pelo índice `ix_voto_pauta_opcao (pauta_id, opcao)`. O plano deixou de tocar o heap: 4.623 → 399 buffers, tempo do EXPLAIN caiu de 82 ms para 49 ms e o p95 em 1 VU caiu de 118,82 ms para 61,32 ms. Ainda assim, a partir de ~10 VUs o Postgres satura a CPU somando os 500 mil valores e o p95 estoura os 300 ms.
- **Cache de sessões encerradas.** Sessão fechada tem resultado imutável, então `VotoService.apurar` é `@Cacheable(cacheNames = "apuracao", key = "#pautaId", unless = "… EM_ANDAMENTO or … SEM_SESSAO")` em Caffeine local (`maximumSize=10000, expireAfterWrite=1h`, ajustável por `VOTACAO_CACHE_APURACAO_SPEC`). A partir do segundo request o banco não é tocado e o p95 fica abaixo de 5 ms mesmo em 30 VUs — 1,75 s → 4,51 ms em relação ao estágio anterior (~390×), ou 2,67 s → 4,51 ms em relação ao ponto de partida (~592×).

O que foi descartado:

- **Contador materializado somando a cada voto.** Testado com `pgbench` (50 clientes, 15 s), com os scripts em [`performance/pgbench/`](performance/pgbench/). Em duas execuções, com volumes diferentes na tabela: o insert puro do modelo atual sustentou 3.419 e 4.929 inserções/s; com um contador único por pauta caiu para 176 e 211 inserções/s, porque todos os votos da mesma pauta disputam a mesma linha — **20 a 25× pior**. Uma variante com 16 contadores por pauta (sharding) ficou em 1.554 e 3.265 inserções/s, ainda abaixo do modelo atual, e exige tabela de shards, soma na leitura e backfill. Trocaria um problema de leitura, que o cache resolve em 20 linhas, por um gargalo no caminho de escrita — justamente o que o enunciado estressa.
- **Particionamento da tabela de votos.** O índice `(pauta_id, opcao)` já isola a pauta: independente do tamanho da tabela, o `Index Only Scan` para 500 mil votos toca menos de 5 MB do índice (399 páginas × 8 KB). Particionar por hash de `pauta_id` só reduziria buffers que já são irrelevantes e complicaria a `uk_voto_pauta_cpf`.
- **Modelo reativo com WebFlux e R2DBC.** O ganho de reativo é liberar a thread durante o I/O, e isso já existe hoje com `spring.threads.virtual.enabled: true`. JDBC é bloqueante por especificação e "async" não reduz custo de query. Sob 30 VUs no cenário sem cache, o pool de 30 conexões ficou 100% ocupado e o Hikari mostrou 141 requisições na fila enquanto o Postgres consumia CPU — ou seja, o gargalo era o banco, não o modelo de threads. Reescrever para R2DBC não tira trabalho da CPU do banco; o cache tira.

Dependência de estatísticas frescas: o Postgres só escolhe o plano bom (`Parallel Index Only Scan`, 399 buffers) quando as estatísticas de `voto` refletem o volume real. Sem `ANALYZE` depois de uma carga grande, o planejador subestima o número de linhas e volta para o `Bitmap Heap Scan` de 4.600 buffers. Autovacuum não é otimização opcional aqui — é parte da performance da apuração. Por isso o `seed-apuracao.sql` roda `ANALYZE voto` no final; em produção, o padrão do autovacuum é suficiente.

Detalhes de reprodução, plano de execução completo e resultado bruto do k6 ficam em [`performance/README.md`](performance/README.md).

## Bônus 3 — versionamento da API

**Estratégia escolhida: versionamento por URI (`/api/v1`).**

Por quê:
- É explícito e visível no path; qualquer curl/log/Swagger deixa claro qual versão está em uso.
- Simples de rotear em proxy/ingress (basta um `location /api/v1/`).
- Fácil de documentar: uma OpenAPI por versão, sem depender de headers customizados.
- Cache HTTP e ferramentas de teste (Postman, k6) tratam sem configuração especial.

Como evoluiria para uma v2:
- Publicar `/api/v2` **convivendo** com `/api/v1` no mesmo binário (controllers separados sob `@RequestMapping("/api/v2/...")`). Nada de breaking em versão publicada.
- Anunciar depreciação da v1 respondendo com os headers `Deprecation: true` e `Sunset: <data RFC 8594>`, mais um link para o guia de migração.
- Comunicar o prazo de sunset (típico 6 meses) por changelog e nas respostas; remover a v1 só depois do sunset.
- Se um endpoint específico da v1 puder ser mantido inalterado na v2, reencaminhar internamente para o handler comum em vez de duplicar código.

Alternativas consideradas e por que não escolhi:
- **Header customizado (`X-API-Version`)** — invisível ao olhar, exige configuração em cliente/CDN, atrapalha cache e dificulta debugar por logs.
- **Media type / content negotiation (`Accept: application/vnd.votacao.v2+json`)** — teoricamente mais "RESTful", mas verboso, difícil de testar via browser/Swagger e menos legível em pipelines. Ganho pequeno para o custo operacional.

## Melhorias futuras

- **Autenticação** — hoje qualquer chamada é autorizada (conforme o enunciado). O próximo passo natural é OAuth2/JWT com escopo por operação (abrir sessão, votar, apurar).
- **Mensageria para notificar o resultado** — publicar um evento (Kafka/RabbitMQ) quando a sessão fecha, permitindo que sistemas interessados (frontend via websocket, e-mail, BI) reajam sem polling.
- **Invalidar o cache de apuração em cluster** — hoje o Caffeine é local por instância. Rodando 2+ réplicas, o cache pode divergir por até `expireAfterWrite` (1h) até refletir a sessão que acabou de fechar. Para operar em cluster, trocar por Redis com TTL, ou publicar invalidação via mensageria no momento em que a sessão encerra.

## Autor

Vinícius Ribeiro · [github.com/mvsergio](https://github.com/mvsergio)

---

## Enunciado original

### Objetivo

No cooperativismo, cada associado possui um voto e as decisões são tomadas em assembleias, por votação. Imagine que você deve criar uma solução web para gerenciar e participar dessas sessões de votação.
Essa solução deve ser executada na nuvem e promover as seguintes funcionalidades através de uma API REST / Front:

- Cadastrar uma nova pauta
- Abrir uma sessão de votação em uma pauta (a sessão de votação deve ficar aberta por um tempo determinado na chamada de abertura ou 1 minuto por default)
- Receber votos dos associados em pautas (os votos são apenas 'Sim'/'Não'. Cada associado é identificado por um id único e pode votar apenas uma vez por pauta)
- Contabilizar os votos e dar o resultado da votação na pauta

Para fins de exercício, a segurança das interfaces pode ser abstraída e qualquer chamada para as interfaces pode ser considerada como autorizada. A solução deve ser construída em java com Spring-boot e Angular/React conforme orientação, mas os frameworks e bibliotecas são de livre escolha (desde que não infrinja direitos de uso).

É importante que as pautas e os votos sejam persistidos e que não sejam perdidos com o restart da aplicação.

### Como proceder

Por favor, realize o FORK desse repositório e implemente sua solução no FORK em seu repositório GitHub, ao final, notifique da conclusão para que possamos analisar o código implementado.

Lembre de deixar todas as orientações necessárias para executar o seu código.

### Tarefas bônus

#### Tarefa Bônus 1 — Integração com sistemas externos

- Criar uma Facade/Client Fake que retorna aleatoriamente se um CPF recebido é válido ou não.
- Caso o CPF seja inválido, a API retornará o HTTP Status 404 (Not Found). Você pode usar geradores de CPF para gerar CPFs válidos.
- Caso o CPF seja válido, a API retornará se o usuário pode (`ABLE_TO_VOTE`) ou não pode (`UNABLE_TO_VOTE`) executar a operação. Essa operação retorna resultados aleatórios, portanto um mesmo CPF pode funcionar em um teste e não funcionar no outro.

```
// CPF Ok para votar
{
    "status": "ABLE_TO_VOTE"
}
// CPF Nao Ok para votar - retornar 404 no client tb
{
    "status": "UNABLE_TO_VOTE"
}
```

#### Tarefa Bônus 2 — Performance

- Imagine que sua aplicação possa ser usada em cenários que existam centenas de milhares de votos. Ela deve se comportar de maneira performática nesses cenários.
- Testes de performance são uma boa maneira de garantir e observar como sua aplicação se comporta.

#### Tarefa Bônus 3 — Versionamento da API

- Como você versionaria a API da sua aplicação? Que estratégia usar?

### O que será analisado

- Simplicidade no design da solução (evitar over engineering)
- Organização do código
- Arquitetura do projeto
- Boas práticas de programação (manutenibilidade, legibilidade etc)
- Possíveis bugs
- Tratamento de erros e exceções
- Explicação breve do porquê das escolhas tomadas durante o desenvolvimento da solução
- Uso de testes automatizados e ferramentas de qualidade
- Limpeza do código
- Documentação do código e da API
- Logs da aplicação
- Mensagens e organização dos commits
- Testes
- Layout responsivo

### Dicas

- Teste bem sua solução, evite bugs.

Observações importantes:
- Não inicie o teste sem sanar todas as dúvidas.
- Iremos executar a aplicação para testá-la, cuide com qualquer dependência externa e deixe claro caso haja instruções especiais para execução do mesmo.

Classificação da informação: Uso Interno.
