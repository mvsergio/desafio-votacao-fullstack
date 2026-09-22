package br.com.votacao.service;

import br.com.votacao.dto.PautaRequest;
import br.com.votacao.dto.PautaResponse;
import br.com.votacao.dto.SessaoResponse;
import br.com.votacao.exception.RecursoNaoEncontradoException;
import br.com.votacao.model.Pauta;
import br.com.votacao.model.SessaoVotacao;
import br.com.votacao.repository.PautaRepository;
import br.com.votacao.repository.SessaoVotacaoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PautaService {

    private final PautaRepository pautaRepository;
    private final SessaoVotacaoRepository sessaoVotacaoRepository;
    private final Clock clock;

    @Transactional
    public Pauta cadastrar(PautaRequest request) {
        Pauta pauta = new Pauta();
        pauta.setTitulo(request.titulo());
        pauta.setDescricao(request.descricao());
        pauta.setDataCriacao(LocalDateTime.now(clock));
        Pauta salva = pautaRepository.save(pauta);
        log.info("Pauta cadastrada: id={} titulo={}", salva.getId(), salva.getTitulo());
        return salva;
    }

    @Transactional(readOnly = true)
    public Page<PautaResponse> listar(Pageable pageable) {
        Pageable ordenado = pageable.getSort().isSorted()
                ? pageable
                : org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "dataCriacao"));

        Page<Pauta> pagina = pautaRepository.findAll(ordenado);
        List<Long> ids = pagina.getContent().stream().map(Pauta::getId).toList();
        Map<Long, SessaoVotacao> sessoesPorPauta = ids.isEmpty()
                ? Map.of()
                : sessaoVotacaoRepository.findByPautaIdIn(ids).stream()
                        .collect(Collectors.toMap(s -> s.getPauta().getId(), Function.identity()));

        LocalDateTime agora = LocalDateTime.now(clock);
        return pagina.map(pauta -> montarResponse(pauta, sessoesPorPauta.get(pauta.getId()), agora));
    }

    @Transactional(readOnly = true)
    public PautaResponse detalhar(Long id) {
        Pauta pauta = pautaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta " + id + " não encontrada"));
        SessaoVotacao sessao = sessaoVotacaoRepository.findByPautaId(id).orElse(null);
        return montarResponse(pauta, sessao, LocalDateTime.now(clock));
    }

    private PautaResponse montarResponse(Pauta pauta, SessaoVotacao sessao, LocalDateTime agora) {
        SessaoResponse sessaoResponse = sessao == null ? null : new SessaoResponse(
                sessao.getId(),
                sessao.getDataAbertura(),
                sessao.getDataFechamento(),
                sessao.isAberta(agora));
        return new PautaResponse(
                pauta.getId(),
                pauta.getTitulo(),
                pauta.getDescricao(),
                pauta.getDataCriacao(),
                sessaoResponse);
    }
}
