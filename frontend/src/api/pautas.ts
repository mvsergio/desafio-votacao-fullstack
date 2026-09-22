import { requisitar } from './cliente'
import type {
  AbrirSessaoRequest,
  PaginaPautas,
  PautaRequest,
  PautaResponse,
  ResultadoApuracaoResponse,
  SessaoResponse,
  VotoRegistradoResponse,
  VotoRequest,
} from './tipos'

export function listarPautas(
  pagina: number,
  tamanho = 10,
  sinal?: AbortSignal,
): Promise<PaginaPautas> {
  return requisitar<PaginaPautas>('/pautas', {
    params: { page: pagina, size: tamanho, sort: 'dataCriacao,desc' },
    sinal,
  })
}

export function detalharPauta(id: number, sinal?: AbortSignal): Promise<PautaResponse> {
  return requisitar<PautaResponse>(`/pautas/${id}`, { sinal })
}

export function cadastrarPauta(pauta: PautaRequest): Promise<PautaResponse> {
  return requisitar<PautaResponse>('/pautas', { metodo: 'POST', corpo: pauta })
}

export function abrirSessao(
  pautaId: number,
  requisicao: AbrirSessaoRequest,
): Promise<SessaoResponse> {
  const corpo =
    requisicao.duracaoEmMinutos !== undefined && requisicao.duracaoEmMinutos !== null
      ? requisicao
      : {}
  return requisitar<SessaoResponse>(`/pautas/${pautaId}/sessao`, {
    metodo: 'POST',
    corpo,
  })
}

export function registrarVoto(
  pautaId: number,
  voto: VotoRequest,
): Promise<VotoRegistradoResponse> {
  return requisitar<VotoRegistradoResponse>(`/pautas/${pautaId}/votos`, {
    metodo: 'POST',
    corpo: voto,
  })
}

export function apurarResultado(
  pautaId: number,
  sinal?: AbortSignal,
): Promise<ResultadoApuracaoResponse> {
  return requisitar<ResultadoApuracaoResponse>(`/pautas/${pautaId}/resultado`, { sinal })
}
