import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FormularioSessao from './FormularioSessao'
import * as apiPautas from '../api/pautas'

describe('FormularioSessao', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('rejeita valor negativo e não chama abrirSessao', async () => {
    const abrirMock = vi.spyOn(apiPautas, 'abrirSessao')

    const { container } = render(<FormularioSessao pautaId={1} onAberta={vi.fn()} />)

    fireEvent.change(screen.getByLabelText(/Duração em minutos/i), {
      target: { value: '-5' },
    })
    // Submit direto evita a validação HTML5 (min=1) do jsdom, que bloquearia o handler JS.
    fireEvent.submit(container.querySelector('form')!)

    expect(await screen.findByRole('alert')).toHaveTextContent(/número inteiro/i)
    expect(abrirMock).not.toHaveBeenCalled()
  })

  it('permite envio com duração vazia (usa default do backend)', async () => {
    const abrirMock = vi.spyOn(apiPautas, 'abrirSessao').mockResolvedValue({
      id: 99,
      dataAbertura: '2026-01-01T00:00:00',
      dataFechamento: '2026-01-01T00:01:00',
      aberta: true,
    })
    const onAberta = vi.fn()
    const usuario = userEvent.setup()

    render(<FormularioSessao pautaId={7} onAberta={onAberta} />)

    await usuario.click(screen.getByRole('button', { name: /abrir sessão/i }))

    expect(abrirMock).toHaveBeenCalledWith(7, {})
    expect(await screen.findByRole('button', { name: /abrir sessão/i })).toBeEnabled()
    expect(onAberta).toHaveBeenCalledTimes(1)
  })

  it('envia duracaoEmMinutos inteiro quando informado', async () => {
    const sessao = {
      id: 42,
      dataAbertura: '2026-01-01T00:00:00',
      dataFechamento: '2026-01-01T00:10:00',
      aberta: true,
    }
    const abrirMock = vi.spyOn(apiPautas, 'abrirSessao').mockResolvedValue(sessao)
    const onAberta = vi.fn()
    const usuario = userEvent.setup()

    render(<FormularioSessao pautaId={3} onAberta={onAberta} />)

    fireEvent.change(screen.getByLabelText(/Duração em minutos/i), {
      target: { value: '10' },
    })
    await usuario.click(screen.getByRole('button', { name: /abrir sessão/i }))

    expect(abrirMock).toHaveBeenCalledWith(3, { duracaoEmMinutos: 10 })
    await screen.findByRole('button', { name: /abrir sessão/i })
    expect(onAberta).toHaveBeenCalledWith(sessao)
  })
})
