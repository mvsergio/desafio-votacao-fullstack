import { act, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import PaginaDetalhePauta from './PaginaDetalhePauta'
import * as apiPautas from '../api/pautas'
import { ErroApi } from '../api/erro'
import type { PautaResponse, ResultadoApuracaoResponse } from '../api/tipos'

function renderizarRota(id: number) {
  return render(
    <MemoryRouter initialEntries={[`/pautas/${id}`]}>
      <Routes>
        <Route path="/pautas/:id" element={<PaginaDetalhePauta />} />
      </Routes>
    </MemoryRouter>,
  )
}

function pautaComSessaoAberta(): PautaResponse {
  return {
    id: 1,
    titulo: 'Pauta Poll',
    descricao: null,
    dataCriacao: '2026-01-01T00:00:00',
    sessao: {
      id: 10,
      dataAbertura: '2026-01-01T00:00:00',
      dataFechamento: '2099-12-31T23:59:59',
      aberta: true,
    },
  }
}

function resultadoBase(): ResultadoApuracaoResponse {
  return {
    pautaId: 1,
    totalSim: 0,
    totalNao: 0,
    totalVotos: 0,
    situacao: 'EM_ANDAMENTO',
  }
}

function definirVisibilidade(escondida: boolean) {
  Object.defineProperty(document, 'hidden', {
    configurable: true,
    get: () => escondida,
  })
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: () => (escondida ? 'hidden' : 'visible'),
  })
  document.dispatchEvent(new Event('visibilitychange'))
}

describe('PaginaDetalhePauta', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('mostra "Sessão encerrada" quando sessao.aberta=false mesmo com fechamento no futuro', async () => {
    vi.spyOn(apiPautas, 'detalharPauta').mockResolvedValue({
      id: 1,
      titulo: 'Pauta X',
      descricao: null,
      dataCriacao: '2026-01-01T00:00:00',
      sessao: {
        id: 10,
        dataAbertura: '2099-12-31T23:00:00',
        dataFechamento: '2099-12-31T23:59:59',
        aberta: false,
      },
    })
    vi.spyOn(apiPautas, 'apurarResultado').mockResolvedValue({
      pautaId: 1,
      totalSim: 0,
      totalNao: 0,
      totalVotos: 0,
      situacao: 'REPROVADA',
    })

    renderizarRota(1)

    expect(await screen.findByText(/Sessão encerrada em/)).toBeInTheDocument()
    expect(screen.queryByLabelText(/CPF do associado/i)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /votar sim/i })).not.toBeInTheDocument()
  })

  it('exibe o spinner "Carregando pauta…" enquanto a chamada não resolve', async () => {
    let liberarDetalhar: (valor: Awaited<ReturnType<typeof apiPautas.detalharPauta>>) => void = () => {}
    vi.spyOn(apiPautas, 'detalharPauta').mockImplementation(
      () => new Promise((resolve) => {
        liberarDetalhar = resolve
      }),
    )
    vi.spyOn(apiPautas, 'apurarResultado').mockResolvedValue({
      pautaId: 1,
      totalSim: 0,
      totalNao: 0,
      totalVotos: 0,
      situacao: 'EM_ANDAMENTO',
    })

    renderizarRota(1)

    expect(await screen.findByText(/Carregando pauta/)).toBeInTheDocument()

    liberarDetalhar({
      id: 1,
      titulo: 'Pauta Y',
      descricao: null,
      dataCriacao: '2026-01-01T00:00:00',
      sessao: null,
    })

    expect(await screen.findByText('Pauta Y')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.queryByText(/Carregando pauta/)).not.toBeInTheDocument()
    })
  })

  it('exibe alerta com a mensagem do ProblemDetail quando detalharPauta falha', async () => {
    vi.spyOn(apiPautas, 'detalharPauta').mockRejectedValue(
      new ErroApi('Pauta não encontrada', 404, 'Recurso ausente'),
    )
    vi.spyOn(apiPautas, 'apurarResultado').mockRejectedValue(
      new ErroApi('Pauta não encontrada', 404, 'Recurso ausente'),
    )

    renderizarRota(999)

    expect(await screen.findByRole('alert')).toHaveTextContent(/Pauta não encontrada/)
  })

  describe('polling com sessão aberta', () => {
    afterEach(() => {
      definirVisibilidade(false)
      vi.useRealTimers()
    })

    it('pausa o polling enquanto a aba está escondida e recarrega ao voltar ao foco', async () => {
      const spyPauta = vi.spyOn(apiPautas, 'detalharPauta').mockResolvedValue(pautaComSessaoAberta())
      const spyResultado = vi.spyOn(apiPautas, 'apurarResultado').mockResolvedValue(resultadoBase())

      vi.useFakeTimers()
      definirVisibilidade(false)
      renderizarRota(1)

      // Deixa a carga inicial e os efeitos rodarem — o polling só é agendado após setPauta.
      await act(async () => {
        await vi.advanceTimersByTimeAsync(0)
      })
      expect(spyPauta).toHaveBeenCalledTimes(1)
      expect(spyResultado).toHaveBeenCalledTimes(1)

      // Aba visível: um tick de intervalo dispara mais uma requisição de cada.
      await act(async () => {
        await vi.advanceTimersByTimeAsync(5000)
      })
      expect(spyPauta).toHaveBeenCalledTimes(2)
      expect(spyResultado).toHaveBeenCalledTimes(2)

      // Aba escondida: intervalo pausa, novos ticks não devem gerar requisições.
      await act(async () => {
        definirVisibilidade(true)
        await vi.advanceTimersByTimeAsync(20000)
      })
      expect(spyPauta).toHaveBeenCalledTimes(2)
      expect(spyResultado).toHaveBeenCalledTimes(2)

      // Voltando ao foco: recarga imediata, sem esperar pelo próximo tick.
      await act(async () => {
        definirVisibilidade(false)
        await vi.advanceTimersByTimeAsync(0)
      })
      expect(spyPauta).toHaveBeenCalledTimes(3)
      expect(spyResultado).toHaveBeenCalledTimes(3)

      // E o polling volta a rodar no intervalo.
      await act(async () => {
        await vi.advanceTimersByTimeAsync(5000)
      })
      expect(spyPauta).toHaveBeenCalledTimes(4)
      expect(spyResultado).toHaveBeenCalledTimes(4)
    })

    it('limpa o alerta de erro do detalhamento quando o polling seguinte volta a ter sucesso', async () => {
      const spyPauta = vi.spyOn(apiPautas, 'detalharPauta')
      spyPauta.mockResolvedValueOnce(pautaComSessaoAberta())
      spyPauta.mockRejectedValueOnce(new ErroApi('Falha de rede', 500))
      spyPauta.mockResolvedValue(pautaComSessaoAberta())
      vi.spyOn(apiPautas, 'apurarResultado').mockResolvedValue(resultadoBase())

      vi.useFakeTimers()
      definirVisibilidade(false)
      renderizarRota(1)

      await act(async () => {
        await vi.advanceTimersByTimeAsync(0)
      })
      expect(screen.queryByRole('alert')).not.toBeInTheDocument()

      // Tick 1: pauta falha, alerta aparece.
      await act(async () => {
        await vi.advanceTimersByTimeAsync(5000)
      })
      expect(screen.getByRole('alert')).toHaveTextContent(/Falha de rede/)

      // Tick 2: pauta volta a resolver e o alerta some.
      await act(async () => {
        await vi.advanceTimersByTimeAsync(5000)
      })
      expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    })
  })
})
