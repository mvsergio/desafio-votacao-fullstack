package br.com.votacao.repository;

import br.com.votacao.model.SessaoVotacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, Long> {

    Optional<SessaoVotacao> findByPautaId(Long pautaId);

    List<SessaoVotacao> findByPautaIdIn(List<Long> pautaIds);
}
