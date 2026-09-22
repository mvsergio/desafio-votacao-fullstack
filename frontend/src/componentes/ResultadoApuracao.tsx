import type { ResultadoApuracaoResponse } from '../api/tipos'
import { classeSituacao, rotuloSituacao } from '../utils/situacao'

type Props = {
  resultado: ResultadoApuracaoResponse
}

export default function ResultadoApuracao({ resultado }: Props) {
  const { totalSim, totalNao, totalVotos, situacao } = resultado
  const percentual = (parte: number) => (totalVotos === 0 ? 0 : (parte / totalVotos) * 100)
  const percSim = percentual(totalSim)
  const percNao = percentual(totalNao)

  return (
    <div>
      <div className="linha-info">
        <span className="linha-info__rotulo">Situação</span>
        <span className={classeSituacao(situacao)}>{rotuloSituacao(situacao)}</span>
      </div>
      <div className="linha-info">
        <span className="linha-info__rotulo">Total de votos</span>
        <span className="linha-info__valor">{totalVotos}</span>
      </div>

      <div className="barras" aria-label="Barras de apuração">
        <div className="barra barra--sim">
          <span className="barra__rotulo">Sim</span>
          <div className="barra__trilha">
            <div
              className="barra__preenchimento"
              style={{ width: `${percSim}%` }}
              role="progressbar"
              aria-label="Percentual de votos Sim"
              aria-valuenow={Math.round(percSim)}
              aria-valuemin={0}
              aria-valuemax={100}
            />
          </div>
          <span className="barra__valor">
            {totalSim} ({percSim.toFixed(0)}%)
          </span>
        </div>
        <div className="barra barra--nao">
          <span className="barra__rotulo">Não</span>
          <div className="barra__trilha">
            <div
              className="barra__preenchimento"
              style={{ width: `${percNao}%` }}
              role="progressbar"
              aria-label="Percentual de votos Não"
              aria-valuenow={Math.round(percNao)}
              aria-valuemin={0}
              aria-valuemax={100}
            />
          </div>
          <span className="barra__valor">
            {totalNao} ({percNao.toFixed(0)}%)
          </span>
        </div>
      </div>
    </div>
  )
}
