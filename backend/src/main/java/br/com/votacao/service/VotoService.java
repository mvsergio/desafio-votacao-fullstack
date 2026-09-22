package br.com.votacao.service;

import br.com.votacao.client.CpfClient;
import br.com.votacao.client.StatusCpf;
import br.com.votacao.client.StatusCpfResponse;
import br.com.votacao.dto.ResultadoApuracaoResponse;
import br.com.votacao.dto.VotoRequest;
import br.com.votacao.exception.AssociadoNaoAptoException;
import br.com.votacao.exception.CpfInvalidoException;
import br.com.votacao.exception.RecursoNaoEncontradoException;
import br.com.votacao.exception.RegraNegocioException;
import br.com.votacao.exception.VotoDuplicadoException;
import br.com.votacao.model.OpcaoVoto;
import br.com.votacao.model.Pauta;
import br.com.votacao.model.SessaoVotacao;
import br.com.votacao.model.SituacaoResultado;
import br.com.votacao.model.Voto;
import br.com.votacao.repository.PautaRepository;
import br.com.votacao.repository.SessaoVotacaoRepository;
import br.com.votacao.repository.VotoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VotoService {

    private final PautaRepository pautaRepository;
    private final SessaoVotacaoRepository sessaoVotacaoRepository;
    private final VotoRepository votoRepository;
    private final CpfClient cpfClient;
    private final Clock clock;

    @Transactional
    public Voto registrar(Long pautaId, VotoRequest request) {
        Pauta pauta = pautaRepository.findById(pautaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta " + pautaId + " não encontrada"));

        SessaoVotacao sessao = sessaoVotacaoRepository.findByPautaId(pautaId)
                .orElseThrow(() -> {
                    log.warn("Voto rejeitado: pauta {} não possui sessão de votação", pautaId);
                    return new RegraNegocioException("Pauta " + pautaId + " não possui sessão de votação");
                });

        LocalDateTime agora = LocalDateTime.now(clock);
        if (!sessao.isAberta(agora)) {
            log.warn("Voto rejeitado: sessão da pauta {} está encerrada", pautaId);
            throw new RegraNegocioException("Sessão de votação da pauta " + pautaId + " está encerrada");
        }

        validarAptidaoDoAssociado(pautaId, request.cpfAssociado());

        Voto voto = new Voto();
        voto.setPauta(pauta);
        voto.setCpfAssociado(request.cpfAssociado());
        voto.setOpcao(request.opcao());
        voto.setDataVoto(agora);

        try {
            Voto salvo = votoRepository.saveAndFlush(voto);
            log.info("Voto registrado na pauta {} pelo associado {} opção {}",
                    pautaId, mascararCpf(request.cpfAssociado()), request.opcao());
            return salvo;
        } catch (DataIntegrityViolationException ex) {
            if (violouConstraint(ex, "uk_voto_pauta_cpf")) {
                log.warn("Associado {} já votou na pauta {}", mascararCpf(request.cpfAssociado()), pautaId);
                throw new VotoDuplicadoException("Associado já votou nesta pauta");
            }
            throw ex;
        }
    }

    private boolean violouConstraint(DataIntegrityViolationException ex, String nomeConstraint) {
        Throwable causa = ex.getMostSpecificCause();
        String mensagem = causa == null ? null : causa.getMessage();
        return mensagem != null && mensagem.toLowerCase().contains(nomeConstraint);
    }

    // Sessão encerrada tem resultado imutável, então cacheia. EM_ANDAMENTO
    // e SEM_SESSAO precisam refletir o estado atual do banco e por isso
    // são explicitamente excluídos via unless.
    @Cacheable(
            cacheNames = "apuracao",
            key = "#pautaId",
            unless = "#result.situacao() == T(br.com.votacao.model.SituacaoResultado).EM_ANDAMENTO "
                    + "or #result.situacao() == T(br.com.votacao.model.SituacaoResultado).SEM_SESSAO")
    @Transactional(readOnly = true)
    public ResultadoApuracaoResponse apurar(Long pautaId) {
        if (!pautaRepository.existsById(pautaId)) {
            throw new RecursoNaoEncontradoException("Pauta " + pautaId + " não encontrada");
        }

        long totalSim = 0L;
        long totalNao = 0L;
        for (var contagem : votoRepository.contarPorOpcao(pautaId)) {
            if (contagem.getOpcao() == OpcaoVoto.SIM) {
                totalSim = contagem.getTotal();
            } else if (contagem.getOpcao() == OpcaoVoto.NAO) {
                totalNao = contagem.getTotal();
            }
        }

        SituacaoResultado situacao = determinarSituacao(pautaId, totalSim, totalNao);
        return new ResultadoApuracaoResponse(pautaId, totalSim, totalNao, totalSim + totalNao, situacao);
    }

    private SituacaoResultado determinarSituacao(Long pautaId, long totalSim, long totalNao) {
        SessaoVotacao sessao = sessaoVotacaoRepository.findByPautaId(pautaId).orElse(null);
        if (sessao == null) {
            return SituacaoResultado.SEM_SESSAO;
        }
        if (sessao.isAberta(LocalDateTime.now(clock))) {
            return SituacaoResultado.EM_ANDAMENTO;
        }
        if (totalSim > totalNao) {
            return SituacaoResultado.APROVADA;
        }
        if (totalNao > totalSim) {
            return SituacaoResultado.REPROVADA;
        }
        return SituacaoResultado.EMPATE;
    }

    private void validarAptidaoDoAssociado(Long pautaId, String cpf) {
        StatusCpfResponse resposta;
        try {
            resposta = cpfClient.consultar(cpf);
        } catch (CpfInvalidoException ex) {
            log.warn("Voto rejeitado na pauta {}: CPF {} inválido", pautaId, mascararCpf(cpf));
            throw ex;
        }
        if (resposta.status() == StatusCpf.UNABLE_TO_VOTE) {
            log.warn("Voto rejeitado na pauta {}: associado {} não apto a votar", pautaId, mascararCpf(cpf));
            throw new AssociadoNaoAptoException("Associado não está apto a votar");
        }
    }

    private String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return "***.***.***-**";
        }
        return "***.***.***-" + cpf.substring(9);
    }
}
