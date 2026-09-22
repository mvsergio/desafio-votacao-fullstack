import { useEffect, useState } from 'react'
import type { PautaResponse, SessaoResponse } from '../api/tipos'
import { formatarHora, paraData } from '../utils/formatoData'

type Props = {
  pauta: PautaResponse
}

export default function StatusSessao({ pauta }: Props) {
  return <SeloStatus sessao={pauta.sessao} />
}

function SeloStatus({ sessao }: { sessao: SessaoResponse | null }) {
  const [agora, setAgora] = useState(() => Date.now())

  useEffect(() => {
    if (!sessao) return
    const timer = setInterval(() => setAgora(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [sessao])

  if (!sessao) {
    return <span className="selo selo--sem-sessao">Sem sessão</span>
  }

  const fechamento = paraData(sessao.dataFechamento).getTime()
  const aberta = fechamento > agora

  if (aberta) {
    return (
      <span className="selo selo--aberta">
        Aberta até {formatarHora(sessao.dataFechamento)}
      </span>
    )
  }

  return <span className="selo selo--encerrada">Encerrada</span>
}
