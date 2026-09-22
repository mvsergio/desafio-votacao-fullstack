import { Link } from 'react-router-dom'
import type { PautaResponse } from '../api/tipos'
import StatusSessao from './StatusSessao'
import { formatarDataHora } from '../utils/formatoData'

type Props = {
  pautas: PautaResponse[]
}

export default function ListaPautas({ pautas }: Props) {
  if (pautas.length === 0) {
    return <div className="vazio">Nenhuma pauta cadastrada ainda.</div>
  }

  return (
    <ul className="lista-pautas">
      {pautas.map((pauta) => (
        <li key={pauta.id}>
          <Link to={`/pautas/${pauta.id}`} className="item-pauta">
            <div className="item-pauta__cabecalho">
              <h3 className="item-pauta__titulo">{pauta.titulo}</h3>
              <StatusSessao pauta={pauta} />
            </div>
            {pauta.descricao && (
              <p className="item-pauta__descricao">{pauta.descricao}</p>
            )}
            <small className="item-pauta__meta">
              Criada em {formatarDataHora(pauta.dataCriacao)}
            </small>
          </Link>
        </li>
      ))}
    </ul>
  )
}
