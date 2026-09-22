import { Link, Route, Routes } from 'react-router-dom'
import PaginaPautas from './paginas/PaginaPautas'
import PaginaDetalhePauta from './paginas/PaginaDetalhePauta'

export default function App() {
  return (
    <>
      <header className="cabecalho">
        <div className="cabecalho__interno">
          <Link to="/" className="cabecalho__logo">
            <span>Voto</span>
            Assembleia Cooperativa
          </Link>
        </div>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<PaginaPautas />} />
          <Route path="/pautas/:id" element={<PaginaDetalhePauta />} />
          <Route path="*" element={<PaginaNaoEncontrada />} />
        </Routes>
      </main>
    </>
  )
}

function PaginaNaoEncontrada() {
  return (
    <section className="cartao">
      <h2 className="cartao__titulo">Página não encontrada</h2>
      <p>
        Voltar para <Link to="/">a lista de pautas</Link>.
      </p>
    </section>
  )
}
