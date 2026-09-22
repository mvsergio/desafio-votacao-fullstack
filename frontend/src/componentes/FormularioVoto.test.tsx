import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FormularioVoto from './FormularioVoto'
import * as apiPautas from '../api/pautas'
import { ErroApi } from '../api/erro'

describe('FormularioVoto', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('exige CPF completo antes de registrar voto', async () => {
    const registrarMock = vi.spyOn(apiPautas, 'registrarVoto')
    const usuario = userEvent.setup()

    render(<FormularioVoto pautaId={1} />)

    await usuario.type(screen.getByLabelText(/CPF do associado/i), '123')
    await usuario.click(screen.getByRole('button', { name: /votar sim/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/11 dígitos/i)
    expect(registrarMock).not.toHaveBeenCalled()
  })

  it('formata o CPF ao digitar e envia apenas dígitos', async () => {
    const registrarMock = vi
      .spyOn(apiPautas, 'registrarVoto')
      .mockResolvedValue({ id: 10, opcao: 'SIM', dataVoto: '2026-01-01T00:00:00' })
    const usuario = userEvent.setup()
    const onVotoRegistrado = vi.fn()

    render(<FormularioVoto pautaId={42} onVotoRegistrado={onVotoRegistrado} />)

    const campoCpf = screen.getByLabelText(/CPF do associado/i) as HTMLInputElement
    await usuario.type(campoCpf, '12345678901')
    expect(campoCpf.value).toBe('123.456.789-01')

    await usuario.click(screen.getByRole('button', { name: /votar sim/i }))

    expect(registrarMock).toHaveBeenCalledWith(42, {
      cpfAssociado: '12345678901',
      opcao: 'SIM',
    })
    expect(await screen.findByText(/voto "sim" registrado/i)).toBeInTheDocument()
    expect(onVotoRegistrado).toHaveBeenCalled()
  })

  it('exibe a mensagem do ProblemDetail retornado pela API', async () => {
    vi.spyOn(apiPautas, 'registrarVoto').mockRejectedValue(
      new ErroApi('Associado já votou nesta pauta', 409, 'Voto duplicado'),
    )
    const usuario = userEvent.setup()

    render(<FormularioVoto pautaId={1} />)

    await usuario.type(screen.getByLabelText(/CPF do associado/i), '12345678901')
    await usuario.click(screen.getByRole('button', { name: /votar não/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/já votou/i)
  })

  it('desabilita os botões quando a sessão está fechada', () => {
    render(<FormularioVoto pautaId={1} desabilitado />)
    expect(screen.getByRole('button', { name: /votar sim/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /votar não/i })).toBeDisabled()
    expect(screen.getByLabelText(/CPF do associado/i)).toBeDisabled()
  })
})
