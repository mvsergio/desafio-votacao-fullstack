import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apurarResultado, detalharPauta } from '../api/pautas'
import { mensagemDoErro } from '../api/erro'
import type {
  PautaResponse,
  ResultadoApuracaoResponse,
  SessaoResponse,
} from '../api/tipos'
import ContagemRegressiva from '../componentes/ContagemRegressiva'
import FormularioSessao from '../componentes/FormularioSessao'
import FormularioVoto from '../componentes/FormularioVoto'
import ResultadoApuracao from '../componentes/ResultadoApuracao'
import { formatarDataHora } from '../utils/formatoData'

// Cadência realista para assembleia: 3s multiplicava requisições sem ganho perceptível,
// 5s mantém o encerramento reativo e reduz a carga por aba em ~40%.
const INTERVALO_POLL_MS = 5000

export default function PaginaDetalhePauta() {
  const { id } = useParams<{ id: string }>()
  const pautaId = Number(id)

  const [pauta, setPauta] = useState<PautaResponse | null>(null)
  const [resultado, setResultado] = useState<ResultadoApuracaoResponse | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [erroResultado, setErroResultado] = useState<string | null>(null)
  const pollControllerRef = useRef<AbortController | null>(null)
  // Sequência incrementada a cada chamada: respostas de requisições antigas são descartadas
  // para evitar que um polling atrasado sobrescreva o resultado de um evento mais novo.
  const seqPautaRef = useRef(0)
  const seqResultadoRef = useRef(0)

  const idInvalido = !Number.isFinite(pautaId) || pautaId <= 0

  const carregarPauta = useCallback(
    async (sinal?: AbortSignal) => {
      if (idInvalido) return
      const seq = ++seqPautaRef.current
      try {
        const dados = await detalharPauta(pautaId, sinal)
        if (sinal?.aborted || seq !== seqPautaRef.current) return
        setPauta(dados)
        setErro(null)
      } catch (erroChamada) {
        if (sinal?.aborted || seq !== seqPautaRef.current) return
        setErro(mensagemDoErro(erroChamada))
      }
    },
    [pautaId, idInvalido],
  )

  const carregarResultado = useCallback(
    async (sinal?: AbortSignal) => {
      if (idInvalido) return
      const seq = ++seqResultadoRef.current
      try {
        const dados = await apurarResultado(pautaId, sinal)
        if (sinal?.aborted || seq !== seqResultadoRef.current) return
        setResultado(dados)
        setErroResultado(null)
      } catch (erroChamada) {
        if (sinal?.aborted || seq !== seqResultadoRef.current) return
        setErroResultado(mensagemDoErro(erroChamada))
      }
    },
    [pautaId, idInvalido],
  )

  useEffect(() => {
    const controlador = new AbortController()
    setCarregando(true)
    setErro(null)
    Promise.all([carregarPauta(controlador.signal), carregarResultado(controlador.signal)]).finally(
      () => {
        if (!controlador.signal.aborted) setCarregando(false)
      },
    )
    return () => controlador.abort()
  }, [carregarPauta, carregarResultado])

  const sessao = pauta?.sessao ?? null
  const sessaoAberta = sessao?.aberta ?? false

  useEffect(() => {
    if (!sessaoAberta) return

    let timer: number | null = null

    // A pauta entra no polling para a tela refletir o encerramento que o servidor decide,
    // mesmo se o relógio do navegador estiver diferente
    const dispararPoll = () => {
      pollControllerRef.current?.abort()
      const controlador = new AbortController()
      pollControllerRef.current = controlador
      carregarPauta(controlador.signal)
      carregarResultado(controlador.signal)
    }

    const iniciarTimer = () => {
      if (timer !== null) return
      timer = window.setInterval(dispararPoll, INTERVALO_POLL_MS)
    }

    const pararTimer = () => {
      if (timer !== null) {
        clearInterval(timer)
        timer = null
      }
      pollControllerRef.current?.abort()
      pollControllerRef.current = null
    }

    // Aba em segundo plano não precisa consumir rede: pausa o polling
    // e faz uma recarga imediata quando o usuário volta à aba.
    const aoMudarVisibilidade = () => {
      if (document.hidden) {
        pararTimer()
      } else {
        dispararPoll()
        iniciarTimer()
      }
    }

    document.addEventListener('visibilitychange', aoMudarVisibilidade)
    if (!document.hidden) iniciarTimer()

    return () => {
      document.removeEventListener('visibilitychange', aoMudarVisibilidade)
      pararTimer()
    }
  }, [sessaoAberta, carregarPauta, carregarResultado])

  function aoAbrirSessao(sessaoAberta: SessaoResponse) {
    // Invalida qualquer detalhamento em curso para não sobrescrever a atualização otimista
    seqPautaRef.current++
    setPauta((atual) => (atual ? { ...atual, sessao: sessaoAberta } : atual))
    carregarResultado()
  }

  function aoRegistrarVoto() {
    carregarResultado()
  }

  if (idInvalido) {
    return (
      <section className="cartao">
        <Link to="/" className="voltar">← Voltar</Link>
        <div className="mensagem mensagem--erro">Identificador de pauta inválido.</div>
      </section>
    )
  }

  return (
    <>
      <Link to="/" className="voltar">← Voltar para lista</Link>

      {carregando && !pauta && (
        <div className="carregando"><span className="spinner" /> Carregando pauta…</div>
      )}

      {erro && (
        <div className="mensagem mensagem--erro" role="alert">{erro}</div>
      )}

      {pauta && (
        <div className="grade-detalhe">
          <section className="cartao cartao--largo">
            <h2 className="cartao__titulo">{pauta.titulo}</h2>
            {pauta.descricao && (
              <p className="cartao__descricao">{pauta.descricao}</p>
            )}
            <div className="linha-info">
              <span className="linha-info__rotulo">Criada em</span>
              <span className="linha-info__valor">{formatarDataHora(pauta.dataCriacao)}</span>
            </div>
            {sessao && (
              <>
                <div className="linha-info">
                  <span className="linha-info__rotulo">Sessão aberta em</span>
                  <span className="linha-info__valor">{formatarDataHora(sessao.dataAbertura)}</span>
                </div>
                <div className="linha-info">
                  <span className="linha-info__rotulo">Fechamento previsto</span>
                  <span className="linha-info__valor">{formatarDataHora(sessao.dataFechamento)}</span>
                </div>
                <div className="linha-info">
                  <span className="linha-info__rotulo">Tempo restante</span>
                  <ContagemRegressiva
                    dataFechamentoIso={sessao.dataFechamento}
                    onEncerrou={() => {
                      carregarPauta()
                      carregarResultado()
                    }}
                  />
                </div>
              </>
            )}
          </section>

          <section className="cartao">
            <h3 className="cartao__titulo">Sessão de votação</h3>
            {!sessao && (
              <>
                <div className="mensagem mensagem--info">Ainda não há sessão aberta para esta pauta.</div>
                <FormularioSessao pautaId={pauta.id} onAberta={aoAbrirSessao} />
              </>
            )}
            {sessao && sessaoAberta && (
              <>
                <div className="mensagem mensagem--info">Sessão aberta — registre seu voto.</div>
                <FormularioVoto pautaId={pauta.id} onVotoRegistrado={aoRegistrarVoto} />
              </>
            )}
            {sessao && !sessaoAberta && (
              <div className="mensagem mensagem--aviso">
                Sessão encerrada em {formatarDataHora(sessao.dataFechamento)}.
              </div>
            )}
          </section>

          <section className="cartao">
            <h3 className="cartao__titulo">Resultado</h3>
            {erroResultado && (
              <div className="mensagem mensagem--erro" role="alert">{erroResultado}</div>
            )}
            {!resultado && !erroResultado && (
              <div className="carregando"><span className="spinner" /> Apurando…</div>
            )}
            {resultado && <ResultadoApuracao resultado={resultado} />}
          </section>
        </div>
      )}
    </>
  )
}
