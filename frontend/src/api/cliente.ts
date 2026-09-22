import { ErroApi, extrairProblema } from './erro'

const urlBase = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

type Opcoes = {
  metodo?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  corpo?: unknown
  params?: Record<string, string | number | undefined>
  sinal?: AbortSignal
}

function montarUrl(caminho: string, params?: Opcoes['params']): string {
  // urlBase pode ser absoluta (dev) ou relativa como '/api/v1' (build de produção via nginx),
  // por isso usa window.location.origin como base quando disponível.
  const bruta = `${urlBase.replace(/\/$/, '')}${caminho}`
  const base = typeof window !== 'undefined' ? window.location.origin : undefined
  const url = new URL(bruta, base)
  if (params) {
    for (const [chave, valor] of Object.entries(params)) {
      if (valor !== undefined && valor !== null) {
        url.searchParams.set(chave, String(valor))
      }
    }
  }
  return url.toString()
}

export async function requisitar<T = unknown>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const { metodo = 'GET', corpo, params, sinal } = opcoes
  const resposta = await fetch(montarUrl(caminho, params), {
    method: metodo,
    headers: corpo !== undefined ? { 'Content-Type': 'application/json', Accept: 'application/json' } : { Accept: 'application/json' },
    body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    signal: sinal,
  })

  if (resposta.status === 204) {
    return undefined as T
  }

  const texto = await resposta.text()
  const dados = texto ? tentarParsear(texto) : null

  if (!resposta.ok) {
    throw extrairProblema(dados, resposta.status)
  }

  return dados as T
}

function tentarParsear(texto: string): unknown {
  try {
    return JSON.parse(texto)
  } catch {
    return texto
  }
}

export { ErroApi, urlBase }
