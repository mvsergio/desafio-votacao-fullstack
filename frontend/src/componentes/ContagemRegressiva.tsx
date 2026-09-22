import { useEffect, useRef, useState } from 'react'
import { formatarCronometro, paraData } from '../utils/formatoData'

type Props = {
  dataFechamentoIso: string
  onEncerrou?: () => void
}

// Arredonda para cima: com floor o contador zerava até 1s antes do fechamento real,
// e a pauta recarregada nesse instante ainda vinha como aberta
function segundosRestantes(fechamento: number) {
  return Math.max(0, Math.ceil((fechamento - Date.now()) / 1000))
}

export default function ContagemRegressiva({ dataFechamentoIso, onEncerrou }: Props) {
  const fechamento = paraData(dataFechamentoIso).getTime()
  const [segundos, setSegundos] = useState(() => segundosRestantes(fechamento))

  // Mantém a referência mais recente sem re-executar o efeito do cronômetro,
  // para não reiniciar o timer nem disparar onEncerrou repetidamente.
  const onEncerrouRef = useRef(onEncerrou)
  useEffect(() => {
    onEncerrouRef.current = onEncerrou
  }, [onEncerrou])

  useEffect(() => {
    let disparado = false
    setSegundos(segundosRestantes(fechamento))
    const timer = setInterval(() => {
      const restante = segundosRestantes(fechamento)
      setSegundos(restante)
      if (restante === 0) {
        clearInterval(timer)
        if (!disparado) {
          disparado = true
          onEncerrouRef.current?.()
        }
      }
    }, 1000)
    return () => clearInterval(timer)
  }, [fechamento])

  const encerrada = segundos <= 0

  return (
    <span
      className={`contagem${encerrada ? ' contagem--encerrada' : ''}`}
      aria-live="polite"
      aria-label={encerrada ? 'Sessão encerrada' : `Tempo restante ${formatarCronometro(segundos)}`}
    >
      {encerrada ? 'Encerrada' : formatarCronometro(segundos)}
    </span>
  )
}
