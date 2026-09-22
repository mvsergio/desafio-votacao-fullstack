import { useState, type FormEvent } from 'react'
import { cadastrarPauta } from '../api/pautas'
import { mensagemDoErro } from '../api/erro'
import type { PautaResponse } from '../api/tipos'

type Props = {
  onCadastrada: (pauta: PautaResponse) => void
}

export default function FormularioPauta({ onCadastrada }: Props) {
  const [titulo, setTitulo] = useState('')
  const [descricao, setDescricao] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)

  async function aoEnviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setErro(null)
    setSucesso(null)

    const tituloLimpo = titulo.trim()
    if (!tituloLimpo) {
      setErro('Informe um título para a pauta.')
      return
    }

    setEnviando(true)
    try {
      const nova = await cadastrarPauta({
        titulo: tituloLimpo,
        descricao: descricao.trim() || undefined,
      })
      setTitulo('')
      setDescricao('')
      setSucesso(`Pauta "${nova.titulo}" cadastrada.`)
      onCadastrada(nova)
    } catch (erroChamada) {
      setErro(mensagemDoErro(erroChamada))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="formulario" onSubmit={aoEnviar} aria-label="Cadastrar pauta">
      <div className="campo">
        <label htmlFor="titulo">Título</label>
        <input
          id="titulo"
          name="titulo"
          type="text"
          maxLength={150}
          placeholder="Ex.: Aprovação do orçamento 2026"
          value={titulo}
          onChange={(evento) => setTitulo(evento.target.value)}
          required
        />
      </div>
      <div className="campo">
        <label htmlFor="descricao">Descrição (opcional)</label>
        <textarea
          id="descricao"
          name="descricao"
          maxLength={1000}
          placeholder="Detalhe o assunto para os associados"
          value={descricao}
          onChange={(evento) => setDescricao(evento.target.value)}
        />
      </div>
      {erro && <div className="mensagem mensagem--erro" role="alert">{erro}</div>}
      {sucesso && <div className="mensagem mensagem--sucesso">{sucesso}</div>}
      <div className="acoes">
        <button type="submit" className="botao" disabled={enviando}>
          {enviando ? 'Cadastrando…' : 'Cadastrar pauta'}
        </button>
      </div>
    </form>
  )
}
