import { useCallback, useEffect, useState } from 'react'
import { listarPautas } from '../api/pautas'
import { mensagemDoErro } from '../api/erro'
import type { PaginaPautas as PaginaPautasResposta } from '../api/tipos'
import FormularioPauta from '../componentes/FormularioPauta'
import ListaPautas from '../componentes/ListaPautas'

const TAMANHO_PAGINA = 10

export default function PaginaPautas() {
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<PaginaPautasResposta | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)

  const carregar = useCallback(async (paginaAlvo: number, sinal?: AbortSignal) => {
    setCarregando(true)
    setErro(null)
    try {
      const resposta = await listarPautas(paginaAlvo, TAMANHO_PAGINA, sinal)
      setDados(resposta)
    } catch (erroChamada) {
      if (sinal?.aborted) return
      setErro(mensagemDoErro(erroChamada))
    } finally {
      if (!sinal?.aborted) setCarregando(false)
    }
  }, [])

  useEffect(() => {
    const controlador = new AbortController()
    carregar(pagina, controlador.signal)
    return () => controlador.abort()
  }, [pagina, carregar])

  return (
    <>
      <section className="cartao">
        <h2 className="cartao__titulo">Nova pauta</h2>
        <FormularioPauta
          onCadastrada={() => {
            if (pagina === 0) {
              carregar(0)
            } else {
              setPagina(0)
            }
          }}
        />
      </section>

      <section className="cartao" aria-live="polite">
        <h2 className="cartao__titulo">Pautas cadastradas</h2>
        {erro && <div className="mensagem mensagem--erro" role="alert">{erro}</div>}

        {carregando && !dados && (
          <div className="carregando">
            <span className="spinner" /> Carregando pautas…
          </div>
        )}

        {dados && <ListaPautas pautas={dados.content} />}

        {dados && dados.totalPages > 1 && (
          <nav className="paginacao" aria-label="Paginação">
            <button
              type="button"
              className="botao--secundario"
              onClick={() => setPagina((p) => Math.max(0, p - 1))}
              disabled={dados.first || carregando}
            >
              ← Anterior
            </button>
            <span className="paginacao__info">
              Página {dados.number + 1} de {dados.totalPages} • {dados.totalElements} pautas
            </span>
            <button
              type="button"
              className="botao--secundario"
              onClick={() => setPagina((p) => p + 1)}
              disabled={dados.last || carregando}
            >
              Próxima →
            </button>
          </nav>
        )}
      </section>
    </>
  )
}
