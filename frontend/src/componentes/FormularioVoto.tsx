import { useState, type FormEvent } from 'react'
import { registrarVoto } from '../api/pautas'
import { mensagemDoErro } from '../api/erro'
import type { OpcaoVoto, VotoRegistradoResponse } from '../api/tipos'
import { apenasDigitosCpf, cpfCompleto, formatarCpf } from '../utils/cpf'

type Props = {
  pautaId: number
  desabilitado?: boolean
  onVotoRegistrado?: (voto: VotoRegistradoResponse) => void
}

export default function FormularioVoto({ pautaId, desabilitado = false, onVotoRegistrado }: Props) {
  const [cpf, setCpf] = useState('')
  const [enviando, setEnviando] = useState<OpcaoVoto | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)

  async function enviarVoto(opcao: OpcaoVoto, evento?: FormEvent) {
    evento?.preventDefault()
    setErro(null)
    setSucesso(null)

    const digitos = apenasDigitosCpf(cpf)
    if (!cpfCompleto(digitos)) {
      setErro('Informe um CPF com 11 dígitos.')
      return
    }

    setEnviando(opcao)
    try {
      const voto = await registrarVoto(pautaId, { cpfAssociado: digitos, opcao })
      setSucesso(`Voto "${opcao === 'SIM' ? 'Sim' : 'Não'}" registrado.`)
      setCpf('')
      onVotoRegistrado?.(voto)
    } catch (erroChamada) {
      setErro(mensagemDoErro(erroChamada))
    } finally {
      setEnviando(null)
    }
  }

  const impedido = desabilitado || enviando !== null

  return (
    <form className="formulario" aria-label="Registrar voto" onSubmit={(e) => e.preventDefault()}>
      <div className="campo">
        <label htmlFor="cpf">CPF do associado</label>
        <input
          id="cpf"
          name="cpf"
          type="text"
          inputMode="numeric"
          autoComplete="off"
          placeholder="000.000.000-00"
          value={formatarCpf(cpf)}
          onChange={(evento) => setCpf(apenasDigitosCpf(evento.target.value))}
          disabled={desabilitado}
          aria-invalid={erro ? 'true' : 'false'}
        />
      </div>

      {erro && <div className="mensagem mensagem--erro" role="alert">{erro}</div>}
      {sucesso && <div className="mensagem mensagem--sucesso">{sucesso}</div>}

      <div className="acoes">
        <button
          type="button"
          className="botao--sim"
          onClick={() => enviarVoto('SIM')}
          disabled={impedido}
          aria-label="Votar Sim"
        >
          {enviando === 'SIM' ? 'Registrando…' : 'Sim'}
        </button>
        <button
          type="button"
          className="botao--nao"
          onClick={() => enviarVoto('NAO')}
          disabled={impedido}
          aria-label="Votar Não"
        >
          {enviando === 'NAO' ? 'Registrando…' : 'Não'}
        </button>
      </div>
    </form>
  )
}
