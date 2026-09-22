package br.com.votacao.service;

import br.com.votacao.dto.AbrirSessaoRequest;
import br.com.votacao.exception.RecursoNaoEncontradoException;
import br.com.votacao.exception.RegraNegocioException;
import br.com.votacao.model.Pauta;
import br.com.votacao.model.SessaoVotacao;
import br.com.votacao.repository.PautaRepository;
import br.com.votacao.repository.SessaoVotacaoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessaoVotacaoService {

    private static final int DURACAO_PADRAO_MINUTOS = 1;

    private final PautaRepository pautaRepository;
    private final SessaoVotacaoRepository sessaoVotacaoRepository;
    private final Clock clock;

    @Transactional
    public SessaoVotacao abrir(Long pautaId, AbrirSessaoRequest request) {
        Pauta pauta = pautaRepository.findById(pautaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta " + pautaId + " não encontrada"));

        int duracao = (request == null || request.duracaoEmMinutos() == null)
                ? DURACAO_PADRAO_MINUTOS
                : request.duracaoEmMinutos();

        LocalDateTime abertura = LocalDateTime.now(clock);
        LocalDateTime fechamento = abertura.plusMinutes(duracao);

        SessaoVotacao sessao = new SessaoVotacao();
        sessao.setPauta(pauta);
        sessao.setDataAbertura(abertura);
        sessao.setDataFechamento(fechamento);

        try {
            SessaoVotacao salva = sessaoVotacaoRepository.saveAndFlush(sessao);
            log.info("Sessão de votação aberta para pauta {} com fechamento previsto em {}",
                    pautaId, fechamento);
            return salva;
        } catch (DataIntegrityViolationException ex) {
            if (violouConstraint(ex, "uk_sessao_votacao_pauta")) {
                log.warn("Tentativa de abrir sessão para pauta {} que já possui sessão", pautaId);
                throw new RegraNegocioException("Pauta " + pautaId + " já possui sessão de votação");
            }
            throw ex;
        }
    }

    private boolean violouConstraint(DataIntegrityViolationException ex, String nomeConstraint) {
        Throwable causa = ex.getMostSpecificCause();
        String mensagem = causa == null ? null : causa.getMessage();
        return mensagem != null && mensagem.toLowerCase().contains(nomeConstraint);
    }
}
