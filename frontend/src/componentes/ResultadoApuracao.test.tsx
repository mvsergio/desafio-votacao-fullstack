import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ResultadoApuracao from './ResultadoApuracao'

describe('ResultadoApuracao', () => {
  it('mostra totais, situação e proporções nas barras', () => {
    render(
      <ResultadoApuracao
        resultado={{
          pautaId: 1,
          totalSim: 7,
          totalNao: 3,
          totalVotos: 10,
          situacao: 'APROVADA',
        }}
      />,
    )

    expect(screen.getByText('Aprovada')).toBeInTheDocument()
    expect(screen.getByText('10')).toBeInTheDocument()
    expect(screen.getByText(/7 \(70%\)/)).toBeInTheDocument()
    expect(screen.getByText(/3 \(30%\)/)).toBeInTheDocument()

    const barraSim = screen.getByRole('progressbar', { name: /sim/i })
    expect(barraSim).toHaveAttribute('aria-valuenow', '70')
    const barraNao = screen.getByRole('progressbar', { name: /não/i })
    expect(barraNao).toHaveAttribute('aria-valuenow', '30')
  })

  it('exibe zero por cento quando não há votos', () => {
    render(
      <ResultadoApuracao
        resultado={{
          pautaId: 1,
          totalSim: 0,
          totalNao: 0,
          totalVotos: 0,
          situacao: 'EM_ANDAMENTO',
        }}
      />,
    )

    expect(screen.getByText('Em andamento')).toBeInTheDocument()
    expect(screen.getAllByText(/0 \(0%\)/)).toHaveLength(2)
    for (const barra of screen.getAllByRole('progressbar')) {
      expect(barra).toHaveAttribute('aria-valuenow', '0')
    }
  })

  it('rotula corretamente a situação SEM_SESSAO', () => {
    render(
      <ResultadoApuracao
        resultado={{
          pautaId: 1,
          totalSim: 0,
          totalNao: 0,
          totalVotos: 0,
          situacao: 'SEM_SESSAO',
        }}
      />,
    )

    expect(screen.getByText('Sem sessão')).toBeInTheDocument()
  })
})
