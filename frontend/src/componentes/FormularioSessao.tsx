import { useState, type FormEvent } from 'react'
import { abrirSessao } from '../api/pautas'
import { mensagemDoErro } from '../api/erro'
import type { SessaoResponse } from '../api/tipos'

type Props = {
  pautaId: number
  onAberta: (sessao: SessaoResponse) => void
}

export default function FormularioSessao({ pautaId, onAberta }: Props) {
  const [duracao, setDuracao] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  async function aoEnviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setErro(null)

    let duracaoEmMinutos: number | undefined
    if (duracao.trim() !== '') {
      const numero = Number(duracao)
      if (!Number.isFinite(numero) || numero < 1) {
        setErro('Duração deve ser um número inteiro maior ou igual a 1.')
        return
      }
      duracaoEmMinutos = Math.floor(numero)
    }

    setEnviando(true)
    try {
      const sessao = await abrirSessao(pautaId, { duracaoEmMinutos })
      onAberta(sessao)
    } catch (erroChamada) {
      setErro(mensagemDoErro(erroChamada))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="formulario" onSubmit={aoEnviar} aria-label="Abrir sessão">
      <div className="campo">
        <label htmlFor="duracao">Duração em minutos</label>
        <input
          id="duracao"
          name="duracao"
          type="number"
          min={1}
          step={1}
          inputMode="numeric"
          placeholder="Vazio = 1 minuto"
          value={duracao}
          onChange={(evento) => setDuracao(evento.target.value)}
        />
      </div>
      {erro && <div className="mensagem mensagem--erro" role="alert">{erro}</div>}
      <div className="acoes">
        <button type="submit" className="botao" disabled={enviando}>
          {enviando ? 'Abrindo…' : 'Abrir sessão'}
        </button>
      </div>
    </form>
  )
}
