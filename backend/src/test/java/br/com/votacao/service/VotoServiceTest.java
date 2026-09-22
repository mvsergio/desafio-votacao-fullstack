package br.com.votacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import br.com.votacao.repository.VotoRepository.ContagemPorOpcao;
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
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 20, 12, 0, 0);
    private static final String CPF = "12345678901";

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoVotacaoRepository sessaoVotacaoRepository;

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private CpfClient cpfClient;

    private final Clock clock = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private VotoService service;

    @BeforeEach
    void configurar() {
        service = new VotoService(pautaRepository, sessaoVotacaoRepository, votoRepository, cpfClient, clock);
    }

    @Test
    void deveRegistrarVotoQuandoSessaoAbertaEAssociadoApto() {
        Pauta pauta = pautaComId(1L);
        SessaoVotacao sessao = sessaoDe(pauta, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(cpfClient.consultar(CPF)).thenReturn(new StatusCpfResponse(StatusCpf.ABLE_TO_VOTE));
        when(votoRepository.saveAndFlush(any(Voto.class))).thenAnswer(inv -> inv.getArgument(0));

        Voto voto = service.registrar(1L, new VotoRequest(CPF, OpcaoVoto.SIM));

        ArgumentCaptor<Voto> captor = ArgumentCaptor.forClass(Voto.class);
        verify(votoRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCpfAssociado()).isEqualTo(CPF);
        assertThat(captor.getValue().getOpcao()).isEqualTo(OpcaoVoto.SIM);
        assertThat(captor.getValue().getDataVoto()).isEqualTo(AGORA);
        assertThat(voto.getPauta()).isSameAs(pauta);
    }

    @Test
    void deveLancar404QuandoPautaNaoExisteAoRegistrarVoto() {
        when(pautaRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(77L, new VotoRequest(CPF, OpcaoVoto.SIM)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(cpfClient, votoRepository);
    }

    @Test
    void deveLancarRegraNegocioQuandoPautaNaoPossuiSessao() {
        Pauta pauta = pautaComId(2L);
        when(pautaRepository.findById(2L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.findByPautaId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(2L, new VotoRequest(CPF, OpcaoVoto.SIM)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não possui sessão");
    }

    @Test
    void deveLancarRegraNegocioQuandoSessaoJaEncerrada() {
        Pauta pauta = pautaComId(3L);
        SessaoVotacao expirada = sessaoDe(pauta, AGORA.minusMinutes(10), AGORA.minusMinutes(5));
        when(pautaRepository.findById(3L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.findByPautaId(3L)).thenReturn(Optional.of(expirada));

        assertThatThrownBy(() -> service.registrar(3L, new VotoRequest(CPF, OpcaoVoto.SIM)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("encerrada");
    }

    @Test
    void devePropagarCpfInvalidoQuandoConsultaFalha() {
        Pauta pauta = pautaComId(4L);
        SessaoVotacao sessao = sessaoDe(pauta, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        when(pautaRepository.findById(4L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.findByPautaId(4L)).thenReturn(Optional.of(sessao));
        when(cpfClient.consultar(CPF)).thenThrow(new CpfInvalidoException("CPF inválido"));

        assertThatThrownBy(() -> service.registrar(4L, new VotoRequest(CPF, OpcaoVoto.SIM)))
                .isInstanceOf(CpfInvalidoException.class);
    }

    @Test
    void deveLancarAssociadoNaoAptoQuandoStatusForUnableToVote() {
        Pauta pauta = pautaComId(5L);
        SessaoVotacao sessao = sessaoDe(pauta, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        when(pautaRepository.findById(5L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.findByPautaId(5L)).thenReturn(Optional.of(sessao));
        when(cpfClient.consultar(CPF)).thenReturn(new StatusCpfResponse(StatusCpf.UNABLE_TO_VOTE));

        assertThatThrownBy(() -> service.registrar(5L, new VotoRequest(CPF, OpcaoVoto.SIM)))
                .isInstanceOf(AssociadoNaoAptoException.class);
    }

    @Test
    void deveRetornar409QuandoAssociadoVotarDuasVezes() {
        Pauta pauta = pautaComId(6L);
        SessaoVotacao sessao = sessaoDe(pauta, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        when(pautaRepository.findById(6L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.findByPautaId(6L)).thenReturn(Optional.of(sessao));
        when(cpfClient.consultar(CPF)).thenReturn(new StatusCpfResponse(StatusCpf.ABLE_TO_VOTE));
        when(votoRepository.saveAndFlush(any(Voto.class)))
                .thenThrow(new DataIntegrityViolationException("uk_voto_pauta_cpf"));

        assertThatThrownBy(() -> service.registrar(6L, new VotoRequest(CPF, OpcaoVoto.SIM)))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    void devePropagarViolacaoDeOutraConstraintSemTraduzirComoVotoDuplicado() {
        Pauta pauta = pautaComId(7L);
        SessaoVotacao sessao = sessaoDe(pauta, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        when(pautaRepository.findById(7L)).thenReturn(Optional.of(pauta));
        when(sessaoVotacaoRepository.findByPautaId(7L)).thenReturn(Optional.of(sessao));
        when(cpfClient.consultar(CPF)).thenReturn(new StatusCpfResponse(StatusCpf.ABLE_TO_VOTE));
        DataIntegrityViolationException outraViolacao =
                new DataIntegrityViolationException("fk_outra_constraint qualquer");
        when(votoRepository.saveAndFlush(any(Voto.class))).thenThrow(outraViolacao);

        assertThatThrownBy(() -> service.registrar(7L, new VotoRequest(CPF, OpcaoVoto.SIM)))
                .isSameAs(outraViolacao);
    }

    @Test
    void deveApurarResultadoSemSessaoQuandoNuncaAbriuSessao() {
        when(pautaRepository.existsById(1L)).thenReturn(true);
        when(votoRepository.contarPorOpcao(1L)).thenReturn(List.of());
        when(sessaoVotacaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());

        ResultadoApuracaoResponse resposta = service.apurar(1L);

        assertThat(resposta.situacao()).isEqualTo(SituacaoResultado.SEM_SESSAO);
        assertThat(resposta.totalVotos()).isZero();
    }

    @Test
    void deveApurarResultadoEmAndamentoQuandoSessaoAindaEstaAberta() {
        Pauta pauta = pautaComId(2L);
        SessaoVotacao aberta = sessaoDe(pauta, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        when(pautaRepository.existsById(2L)).thenReturn(true);
        when(votoRepository.contarPorOpcao(2L)).thenReturn(List.of(contagem(OpcaoVoto.SIM, 3), contagem(OpcaoVoto.NAO, 1)));
        when(sessaoVotacaoRepository.findByPautaId(2L)).thenReturn(Optional.of(aberta));

        ResultadoApuracaoResponse resposta = service.apurar(2L);

        assertThat(resposta.situacao()).isEqualTo(SituacaoResultado.EM_ANDAMENTO);
        assertThat(resposta.totalSim()).isEqualTo(3L);
        assertThat(resposta.totalNao()).isEqualTo(1L);
        assertThat(resposta.totalVotos()).isEqualTo(4L);
    }

    @Test
    void deveApurarResultadoAprovadaQuandoSimSuperaNaoAposEncerramento() {
        Pauta pauta = pautaComId(3L);
        SessaoVotacao encerrada = sessaoDe(pauta, AGORA.minusMinutes(10), AGORA.minusMinutes(1));
        when(pautaRepository.existsById(3L)).thenReturn(true);
        when(votoRepository.contarPorOpcao(3L)).thenReturn(List.of(contagem(OpcaoVoto.SIM, 5), contagem(OpcaoVoto.NAO, 2)));
        when(sessaoVotacaoRepository.findByPautaId(3L)).thenReturn(Optional.of(encerrada));

        ResultadoApuracaoResponse resposta = service.apurar(3L);

        assertThat(resposta.situacao()).isEqualTo(SituacaoResultado.APROVADA);
    }

    @Test
    void deveApurarResultadoReprovadaQuandoNaoSuperaSimAposEncerramento() {
        Pauta pauta = pautaComId(4L);
        SessaoVotacao encerrada = sessaoDe(pauta, AGORA.minusMinutes(10), AGORA.minusMinutes(1));
        when(pautaRepository.existsById(4L)).thenReturn(true);
        when(votoRepository.contarPorOpcao(4L)).thenReturn(List.of(contagem(OpcaoVoto.SIM, 1), contagem(OpcaoVoto.NAO, 4)));
        when(sessaoVotacaoRepository.findByPautaId(4L)).thenReturn(Optional.of(encerrada));

        ResultadoApuracaoResponse resposta = service.apurar(4L);

        assertThat(resposta.situacao()).isEqualTo(SituacaoResultado.REPROVADA);
    }

    @Test
    void deveApurarResultadoEmpateQuandoSimIgualNaoAposEncerramento() {
        Pauta pauta = pautaComId(5L);
        SessaoVotacao encerrada = sessaoDe(pauta, AGORA.minusMinutes(10), AGORA.minusMinutes(1));
        when(pautaRepository.existsById(5L)).thenReturn(true);
        when(votoRepository.contarPorOpcao(5L)).thenReturn(List.of(contagem(OpcaoVoto.SIM, 2), contagem(OpcaoVoto.NAO, 2)));
        when(sessaoVotacaoRepository.findByPautaId(5L)).thenReturn(Optional.of(encerrada));

        ResultadoApuracaoResponse resposta = service.apurar(5L);

        assertThat(resposta.situacao()).isEqualTo(SituacaoResultado.EMPATE);
    }

    @Test
    void deveLancar404QuandoPautaInexisteAoApurar() {
        when(pautaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.apurar(999L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Pauta pautaComId(Long id) {
        Pauta pauta = new Pauta();
        pauta.setId(id);
        pauta.setTitulo("Pauta " + id);
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

    private ContagemPorOpcao contagem(OpcaoVoto opcao, long total) {
        return new ContagemPorOpcao() {
            @Override
            public OpcaoVoto getOpcao() {
                return opcao;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
