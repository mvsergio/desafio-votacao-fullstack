package br.com.votacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.votacao.dto.PautaRequest;
import br.com.votacao.dto.PautaResponse;
import br.com.votacao.exception.RecursoNaoEncontradoException;
import br.com.votacao.model.Pauta;
import br.com.votacao.model.SessaoVotacao;
import br.com.votacao.repository.PautaRepository;
import br.com.votacao.repository.SessaoVotacaoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PautaServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 1, 15, 10, 0, 0);

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoVotacaoRepository sessaoVotacaoRepository;

    private final Clock clock = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private PautaService service;

    @BeforeEach
    void configurar() {
        service = new PautaService(pautaRepository, sessaoVotacaoRepository, clock);
    }

    @Test
    void deveCadastrarPautaComDataCriacaoObtidaDoClock() {
        PautaRequest request = new PautaRequest("Reforma do estatuto", "Alterar cláusula 7");
        Pauta salva = pautaComId(1L);
        when(pautaRepository.save(any(Pauta.class))).thenReturn(salva);

        Pauta resultado = service.cadastrar(request);

        ArgumentCaptor<Pauta> captor = ArgumentCaptor.forClass(Pauta.class);
        verify(pautaRepository).save(captor.capture());
        Pauta enviada = captor.getValue();
        assertThat(enviada.getTitulo()).isEqualTo("Reforma do estatuto");
        assertThat(enviada.getDescricao()).isEqualTo("Alterar cláusula 7");
        assertThat(enviada.getDataCriacao()).isEqualTo(AGORA);
        assertThat(resultado).isSameAs(salva);
    }

    @Test
    void deveDetalharPautaComSessaoAberta() {
        Pauta pauta = pautaComId(10L);
        SessaoVotacao sessao = sessaoDe(pauta, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        when(pautaRepository.findById(10L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.findByPautaId(10L)).thenReturn(Optional.of(sessao));

        PautaResponse response = service.detalhar(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.sessao()).isNotNull();
        assertThat(response.sessao().aberta()).isTrue();
    }

    @Test
    void deveDetalharPautaSemSessao() {
        Pauta pauta = pautaComId(11L);
        when(pautaRepository.findById(11L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.findByPautaId(11L)).thenReturn(Optional.empty());

        PautaResponse response = service.detalhar(11L);

        assertThat(response.sessao()).isNull();
    }

    @Test
    void deveLancarRecursoNaoEncontradoQuandoPautaInexistenteAoDetalhar() {
        when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalhar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deveListarPautasAplicandoOrdenacaoPadraoQuandoClienteNaoInformarSort() {
        Pauta pauta = pautaComId(1L);
        SessaoVotacao sessao = sessaoDe(pauta, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        Pageable semSort = PageRequest.of(0, 10);
        when(pautaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(pauta)));
        when(sessaoVotacaoRepository.findByPautaIdIn(List.of(1L))).thenReturn(List.of(sessao));

        Page<PautaResponse> pagina = service.listar(semSort);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(pautaRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "dataCriacao"));
        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().getFirst().sessao().aberta()).isTrue();
    }

    @Test
    void deveListarPautasRespeitandoOrdenacaoInformadaPeloCliente() {
        Pauta pauta = pautaComId(1L);
        Pageable comSort = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "titulo"));
        when(pautaRepository.findAll(comSort)).thenReturn(new PageImpl<>(List.of(pauta)));
        when(sessaoVotacaoRepository.findByPautaIdIn(List.of(1L))).thenReturn(List.of());

        Page<PautaResponse> pagina = service.listar(comSort);

        verify(pautaRepository).findAll(comSort);
        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().getFirst().sessao()).isNull();
    }

    @Test
    void deveRetornarPaginaVaziaSemConsultarSessoesQuandoNaoHaPautas() {
        Pageable pageable = PageRequest.of(0, 10);
        when(pautaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        Page<PautaResponse> pagina = service.listar(pageable);

        assertThat(pagina.getContent()).isEmpty();
        verify(sessaoVotacaoRepository, never()).findByPautaIdIn(anyList());
    }

    private Pauta pautaComId(Long id) {
        Pauta pauta = new Pauta();
        pauta.setId(id);
        pauta.setTitulo("Titulo " + id);
        pauta.setDescricao("Descricao " + id);
        pauta.setDataCriacao(AGORA);
        return pauta;
    }

    private SessaoVotacao sessaoDe(Pauta pauta, LocalDateTime abertura, LocalDateTime fechamento) {
        SessaoVotacao sessao = new SessaoVotacao();
        sessao.setId(1L);
        sessao.setPauta(pauta);
        sessao.setDataAbertura(abertura);
        sessao.setDataFechamento(fechamento);
        return sessao;
    }
}
