// Teste de carga da apuração: GET /api/v1/pautas/{id}/resultado
//
// Recebe o id da pauta e o número de VUs por variável de ambiente:
//   PAUTA_ID (obrigatório) — id retornado por seed-apuracao.sql.
//   VUS      (opcional, padrão 10) — VUs constantes durante DURATION.
//   DURATION (opcional, padrão '30s').
//
// Thresholds no mesmo molde do votacao.js: p95 de duração e taxa de erro.

import http from 'k6/http'
import { check, fail } from 'k6'
import { Rate } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080/api/v1'
const PAUTA_ID = __ENV.PAUTA_ID
const VUS = Number(__ENV.VUS || 10)
const DURATION = __ENV.DURATION || '30s'

if (!PAUTA_ID) {
  fail('PAUTA_ID não informado. Rode seed-apuracao.sql e exporte PAUTA_ID.')
}

export const options = {
  scenarios: {
    apuracao: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
      gracefulStop: '5s',
      exec: 'apurar',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
    'http_req_duration{operacao:apuracao}': ['p(95)<300'],
    apuracao_ok: ['rate>0.99'],
  },
}

const apuracaoOk = new Rate('apuracao_ok')

export function apurar() {
  const resposta = http.get(`${BASE_URL}/pautas/${PAUTA_ID}/resultado`, {
    tags: { operacao: 'apuracao' },
  })

  const ok = check(resposta, {
    'status 200': (r) => r.status === 200,
  })
  apuracaoOk.add(ok)
}
