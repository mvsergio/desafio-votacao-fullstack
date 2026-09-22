import type { SituacaoResultado } from '../api/tipos'

export function rotuloSituacao(situacao: SituacaoResultado): string {
  switch (situacao) {
    case 'APROVADA':
      return 'Aprovada'
    case 'REPROVADA':
      return 'Reprovada'
    case 'EMPATE':
      return 'Empate'
    case 'EM_ANDAMENTO':
      return 'Em andamento'
    case 'SEM_SESSAO':
      return 'Sem sessão'
  }
}

export function classeSituacao(situacao: SituacaoResultado): string {
  switch (situacao) {
    case 'APROVADA':
      return 'situacao situacao--aprovada'
    case 'REPROVADA':
      return 'situacao situacao--reprovada'
    case 'EMPATE':
      return 'situacao situacao--empate'
    case 'EM_ANDAMENTO':
      return 'situacao situacao--andamento'
    case 'SEM_SESSAO':
      return 'situacao situacao--sem-sessao'
  }
}
