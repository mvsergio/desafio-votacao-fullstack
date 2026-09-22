package br.com.votacao.repository;

import br.com.votacao.model.OpcaoVoto;
import br.com.votacao.model.Voto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    // COUNT(*) evita referenciar v.id; Postgres serve tudo pelo índice
    // ix_voto_pauta_opcao (pauta_id, opcao) via Index Only Scan.
    @Query("""
            SELECT v.opcao AS opcao, COUNT(*) AS total
            FROM Voto v
            WHERE v.pauta.id = :pautaId
            GROUP BY v.opcao
            """)
    List<ContagemPorOpcao> contarPorOpcao(@Param("pautaId") Long pautaId);

    interface ContagemPorOpcao {
        OpcaoVoto getOpcao();
        long getTotal();
    }
}
