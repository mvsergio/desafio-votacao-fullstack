import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import StatusSessao from './StatusSessao'
import type { PautaResponse } from '../api/tipos'

function pautaCom(sessao: PautaResponse['sessao']): PautaResponse {
  return {
    id: 1,
    titulo: 'Pauta X',
    descricao: null,
    dataCriacao: '2026-01-01T00:00:00',
    sessao,
  }
}

describe('StatusSessao', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('mostra "Sem sessão" quando a pauta não tem sessão', () => {
    render(<StatusSessao pauta={pautaCom(null)} />)
    expect(screen.getByText('Sem sessão')).toBeInTheDocument()
  })

  it('mostra "Aberta até ..." quando dataFechamento está no futuro', () => {
    render(
      <StatusSessao
        pauta={pautaCom({
          id: 10,
          dataAbertura: '2026-01-01T00:00:00',
          dataFechamento: '2026-01-01T00:05:00',
          aberta: true,
        })}
      />,
    )
    expect(screen.getByText(/Aberta até/)).toBeInTheDocument()
  })

  it('mostra "Encerrada" quando dataFechamento já passou', () => {
    render(
      <StatusSessao
        pauta={pautaCom({
          id: 10,
          dataAbertura: '2025-12-31T23:00:00',
          dataFechamento: '2025-12-31T23:59:00',
          aberta: false,
        })}
      />,
    )
    expect(screen.getByText('Encerrada')).toBeInTheDocument()
  })
})
