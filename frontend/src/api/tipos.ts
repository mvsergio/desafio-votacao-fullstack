export type OpcaoVoto = 'SIM' | 'NAO'

export type SituacaoResultado =
  | 'EM_ANDAMENTO'
  | 'APROVADA'
  | 'REPROVADA'
  | 'EMPATE'
  | 'SEM_SESSAO'

export type SessaoResponse = {
  id: number
  dataAbertura: string
  dataFechamento: string
  aberta: boolean
}

export type PautaResponse = {
  id: number
  titulo: string
  descricao: string | null
  dataCriacao: string
  sessao: SessaoResponse | null
}

export type PaginaPautas = {
  content: PautaResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

export type ResultadoApuracaoResponse = {
  pautaId: number
  totalSim: number
  totalNao: number
  totalVotos: number
  situacao: SituacaoResultado
}

export type VotoRegistradoResponse = {
  id: number
  opcao: OpcaoVoto
  dataVoto: string
}

export type PautaRequest = {
  titulo: string
  descricao?: string
}

export type AbrirSessaoRequest = {
  duracaoEmMinutos?: number
}

export type VotoRequest = {
  cpfAssociado: string
  opcao: OpcaoVoto
}

export type CampoInvalido = {
  campo: string
  mensagem: string
}

export type ProblemDetail = {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  camposInvalidos?: CampoInvalido[]
}
