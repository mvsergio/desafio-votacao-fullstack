import type { CampoInvalido, ProblemDetail } from './tipos'

export class ErroApi extends Error {
  readonly status: number
  readonly titulo?: string
  readonly camposInvalidos?: CampoInvalido[]

  constructor(mensagem: string, status: number, titulo?: string, camposInvalidos?: CampoInvalido[]) {
    super(mensagem)
    this.name = 'ErroApi'
    this.status = status
    this.titulo = titulo
    this.camposInvalidos = camposInvalidos
  }
}

export function mensagemDoErro(erro: unknown): string {
  if (erro instanceof ErroApi) {
    if (erro.camposInvalidos && erro.camposInvalidos.length > 0) {
      return erro.camposInvalidos.map((c) => c.mensagem).join(' • ')
    }
    return erro.message
  }
  if (erro instanceof Error) return erro.message
  return 'Erro inesperado'
}

export function extrairProblema(corpo: unknown, status: number): ErroApi {
  if (corpo && typeof corpo === 'object') {
    const problema = corpo as ProblemDetail
    const detalhe =
      problema.detail ??
      problema.title ??
      `Falha na requisição (HTTP ${status})`
    return new ErroApi(detalhe, status, problema.title, problema.camposInvalidos)
  }
  return new ErroApi(`Falha na requisição (HTTP ${status})`, status)
}
