import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ContagemRegressiva from './ContagemRegressiva'

describe('ContagemRegressiva', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('chama onEncerrou uma única vez ao chegar em zero, mesmo com re-render', () => {
    const onEncerrou = vi.fn()
    const iso = '2026-01-01T00:00:02'

    const { rerender } = render(
      <ContagemRegressiva dataFechamentoIso={iso} onEncerrou={onEncerrou} />,
    )

    expect(onEncerrou).not.toHaveBeenCalled()

    act(() => {
      vi.advanceTimersByTime(3000)
    })
    expect(onEncerrou).toHaveBeenCalledTimes(1)

    // Simula re-render do pai (nova referência de onEncerrou) — não deve disparar de novo.
    rerender(<ContagemRegressiva dataFechamentoIso={iso} onEncerrou={() => onEncerrou()} />)
    act(() => {
      vi.advanceTimersByTime(5000)
    })
    expect(onEncerrou).toHaveBeenCalledTimes(1)
  })

  it('não chama onEncerrou antes do horário de fechamento', () => {
    vi.setSystemTime(new Date('2026-01-01T00:00:00.500Z'))
    const onEncerrou = vi.fn()

    render(<ContagemRegressiva dataFechamentoIso="2026-01-01T00:00:02" onEncerrou={onEncerrou} />)

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(onEncerrou).not.toHaveBeenCalled()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(onEncerrou).toHaveBeenCalledTimes(1)
  })

  it('atualiza o aria-label conforme o tempo restante e ao encerrar', () => {
    render(<ContagemRegressiva dataFechamentoIso="2026-01-01T00:00:02" />)

    expect(screen.getByLabelText('Tempo restante 00:02')).toBeInTheDocument()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByLabelText('Tempo restante 00:01')).toBeInTheDocument()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByLabelText('Sessão encerrada')).toBeInTheDocument()
  })
})
