// Teste de carga do fluxo de votação com k6.
//
// Setup: cria uma pauta, abre uma sessão de 10 minutos.
// Ramp-up até ~200 VUs registrando votos com CPFs únicos.
// Thresholds: p95 < 300ms, erro < 1%.

import http from 'k6/http'
import { check, fail } from 'k6'
import { Rate } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080/api/v1'
const DURACAO_SESSAO_MIN = 10

export const options = {
  scenarios: {
    votacao: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 200 },
        { duration: '2m', target: 200 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
      gracefulStop: '10s',
      exec: 'votar',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
    'http_req_duration{operacao:voto}': ['p(95)<300'],
    voto_ok: ['rate>0.99'],
  },
}

const votoOk = new Rate('voto_ok')

export function setup() {
  const cabecalhos = { headers: { 'Content-Type': 'application/json' } }

  const criaPauta = http.post(
    `${BASE_URL}/pautas`,
    JSON.stringify({
      titulo: `Carga ${Date.now()}`,
      descricao: 'Pauta gerada pelo teste de carga k6',
    }),
    cabecalhos,
  )
  if (criaPauta.status !== 201) {
    fail(`Falha ao cadastrar pauta: ${criaPauta.status} ${criaPauta.body}`)
  }
  const pautaId = criaPauta.json('id')

  const abreSessao = http.post(
    `${BASE_URL}/pautas/${pautaId}/sessao`,
    JSON.stringify({ duracaoEmMinutos: DURACAO_SESSAO_MIN }),
    cabecalhos,
  )
  if (abreSessao.status !== 201) {
    fail(`Falha ao abrir sessão: ${abreSessao.status} ${abreSessao.body}`)
  }

  return { pautaId }
}

export function votar(dados) {
  const cpf = gerarCpfUnico(__VU, __ITER)
  const opcao = (__VU + __ITER) % 2 === 0 ? 'SIM' : 'NAO'

  const resposta = http.post(
    `${BASE_URL}/pautas/${dados.pautaId}/votos`,
    JSON.stringify({ cpfAssociado: cpf, opcao }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { operacao: 'voto' },
    },
  )

  const ok = check(resposta, {
    'status 201': (r) => r.status === 201,
  })
  votoOk.add(ok)
}

export function teardown(dados) {
  const apuracao = http.get(`${BASE_URL}/pautas/${dados.pautaId}/resultado`)
  console.log(`Resultado final da pauta ${dados.pautaId}: ${apuracao.body}`)
}

// CPFs únicos por (VU, iteração). 11 dígitos, sem cálculo real.
function gerarCpfUnico(vu, iter) {
  const semente = vu * 1_000_000 + iter
  const base = String(semente).padStart(11, '0')
  return base.slice(-11)
}
