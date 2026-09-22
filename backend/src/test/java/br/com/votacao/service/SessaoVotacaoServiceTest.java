package br.com.votacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.votacao.dto.AbrirSessaoRequest;
import br.com.votacao.exception.RecursoNaoEncontradoException;
import br.com.votacao.exception.RegraNegocioException;
import br.com.votacao.model.Pauta;
import br.com.votacao.model.SessaoVotacao;
import br.com.votacao.repository.PautaRepository;
import br.com.votacao.repository.SessaoVotacaoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SessaoVotacaoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 14, 0, 0);

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoVotacaoRepository sessaoVotacaoRepository;

    private final Clock clock = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private SessaoVotacaoService service;

    @BeforeEach
    void configurar() {
        service = new SessaoVotacaoService(pautaRepository, sessaoVotacaoRepository, clock);
    }

    @Test
    void deveAbrirSessaoComDuracaoPadraoDeUmMinutoQuandoRequestForNulo() {
        Pauta pauta = pautaComId(1L);
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.saveAndFlush(any(SessaoVotacao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SessaoVotacao sessao = service.abrir(1L, null);

        assertThat(sessao.getDataAbertura()).isEqualTo(AGORA);
        assertThat(sessao.getDataFechamento()).isEqualTo(AGORA.plusMinutes(1));
    }

    @Test
    void deveAbrirSessaoComDuracaoPadraoDeUmMinutoQuandoDuracaoForNula() {
        Pauta pauta = pautaComId(2L);
        when(pautaRepository.findById(2L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.saveAndFlush(any(SessaoVotacao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SessaoVotacao sessao = service.abrir(2L, new AbrirSessaoRequest(null));

        assertThat(sessao.getDataFechamento()).isEqualTo(AGORA.plusMinutes(1));
    }

    @Test
    void deveAbrirSessaoComDuracaoCustomizadaEmMinutos() {
        Pauta pauta = pautaComId(3L);
        when(pautaRepository.findById(3L)).thenReturn(Optional.of(pauta));
        ArgumentCaptor<SessaoVotacao> captor = ArgumentCaptor.forClass(SessaoVotacao.class);
        when(sessaoVotacaoRepository.saveAndFlush(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        SessaoVotacao sessao = service.abrir(3L, new AbrirSessaoRequest(15));

        assertThat(sessao.getDataAbertura()).isEqualTo(AGORA);
        assertThat(sessao.getDataFechamento()).isEqualTo(AGORA.plusMinutes(15));
        assertThat(captor.getValue().getPauta()).isSameAs(pauta);
    }

    @Test
    void deveLancarRecursoNaoEncontradoQuandoPautaInexistente() {
        when(pautaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abrir(404L, new AbrirSessaoRequest(5)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("404");
    }

    @Test
    void deveLancarRegraNegocioQuandoPautaJaPossuiSessao() {
        Pauta pauta = pautaComId(9L);
        when(pautaRepository.findById(9L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.saveAndFlush(any(SessaoVotacao.class)))
                .thenThrow(new DataIntegrityViolationException("uk_sessao_votacao_pauta"));

        assertThatThrownBy(() -> service.abrir(9L, new AbrirSessaoRequest(5)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já possui sessão");
    }

    @Test
    void devePropagarViolacaoDeOutraConstraintSemTraduzirComoJaPossuiSessao() {
        Pauta pauta = pautaComId(10L);
        when(pautaRepository.findById(10L)).thenReturn(Optional.of(pauta));
        DataIntegrityViolationException outraViolacao =
                new DataIntegrityViolationException("fk_outra_constraint qualquer");
        when(sessaoVotacaoRepository.saveAndFlush(any(SessaoVotacao.class)))
                .thenThrow(outraViolacao);

        assertThatThrownBy(() -> service.abrir(10L, new AbrirSessaoRequest(5)))
                .isSameAs(outraViolacao);
    }

    private Pauta pautaComId(Long id) {
        Pauta pauta = new Pauta();
        pauta.setId(id);
        pauta.setTitulo("Pauta " + id);
        pauta.setDataCriacao(AGORA);
        return pauta;
    }
}
