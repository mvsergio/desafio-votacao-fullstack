package br.com.votacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.votacao.client.CpfClient;
import br.com.votacao.dto.ResultadoApuracaoResponse;
import br.com.votacao.model.OpcaoVoto;
import br.com.votacao.model.Pauta;
import br.com.votacao.model.SessaoVotacao;
import br.com.votacao.model.SituacaoResultado;
import br.com.votacao.repository.PautaRepository;
import br.com.votacao.repository.SessaoVotacaoRepository;
import br.com.votacao.repository.VotoRepository;
import br.com.votacao.repository.VotoRepository.ContagemPorOpcao;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ApuracaoCacheTest.ConfiguracaoDoTeste.class)
class ApuracaoCacheTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 20, 12, 0, 0);

    @Autowired
    private VotoService votoService;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private SessaoVotacaoRepository sessaoVotacaoRepository;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void limparEstadoEntreCenarios() {
        reset(pautaRepository, sessaoVotacaoRepository, votoRepository);
        cacheManager.getCacheNames().forEach(nome -> cacheManager.getCache(nome).clear());
    }

    @Test
    void deveConsultarRepositorioUmaUnicaVezQuandoPautaTemSessaoEncerrada() {
        Long pautaId = 100L;
        SessaoVotacao encerrada = sessao(AGORA.minusHours(2), AGORA.minusHours(1));
        when(pautaRepository.existsById(pautaId)).thenReturn(true);
        when(sessaoVotacaoRepository.findByPautaId(pautaId)).thenReturn(Optional.of(encerrada));
        when(votoRepository.contarPorOpcao(pautaId))
                .thenReturn(List.of(contagem(OpcaoVoto.SIM, 3), contagem(OpcaoVoto.NAO, 1)));

        ResultadoApuracaoResponse primeira = votoService.apurar(pautaId);
        ResultadoApuracaoResponse segunda = votoService.apurar(pautaId);

        assertThat(primeira.situacao()).isEqualTo(SituacaoResultado.APROVADA);
        assertThat(segunda).isEqualTo(primeira);
        verify(votoRepository, times(1)).contarPorOpcao(pautaId);
        verify(sessaoVotacaoRepository, times(1)).findByPautaId(pautaId);
    }

    @Test
    void naoDeveCachearQuandoSessaoAindaEstaAberta() {
        Long pautaId = 200L;
        SessaoVotacao aberta = sessao(AGORA.minusMinutes(1), AGORA.plusMinutes(10));
        when(pautaRepository.existsById(pautaId)).thenReturn(true);
        when(sessaoVotacaoRepository.findByPautaId(pautaId)).thenReturn(Optional.of(aberta));
        when(votoRepository.contarPorOpcao(pautaId))
                .thenReturn(List.of(contagem(OpcaoVoto.SIM, 2)));

        ResultadoApuracaoResponse primeira = votoService.apurar(pautaId);
        ResultadoApuracaoResponse segunda = votoService.apurar(pautaId);

        assertThat(primeira.situacao()).isEqualTo(SituacaoResultado.EM_ANDAMENTO);
        assertThat(segunda.situacao()).isEqualTo(SituacaoResultado.EM_ANDAMENTO);
        verify(votoRepository, times(2)).contarPorOpcao(pautaId);
        verify(sessaoVotacaoRepository, times(2)).findByPautaId(pautaId);
    }

    @Test
    void naoDeveCachearQuandoPautaNaoPossuiSessao() {
        Long pautaId = 300L;
        when(pautaRepository.existsById(pautaId)).thenReturn(true);
        when(sessaoVotacaoRepository.findByPautaId(pautaId)).thenReturn(Optional.empty());
        when(votoRepository.contarPorOpcao(pautaId)).thenReturn(List.of());

        ResultadoApuracaoResponse primeira = votoService.apurar(pautaId);
        ResultadoApuracaoResponse segunda = votoService.apurar(pautaId);

        assertThat(primeira.situacao()).isEqualTo(SituacaoResultado.SEM_SESSAO);
        assertThat(segunda.situacao()).isEqualTo(SituacaoResultado.SEM_SESSAO);
        verify(votoRepository, times(2)).contarPorOpcao(pautaId);
        verify(sessaoVotacaoRepository, times(2)).findByPautaId(pautaId);
    }

    private SessaoVotacao sessao(LocalDateTime abertura, LocalDateTime fechamento) {
        Pauta pauta = new Pauta();
        pauta.setId(1L);
        pauta.setTitulo("Pauta");
        pauta.setDataCriacao(AGORA);
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

    @Configuration
    @EnableCaching
    static class ConfiguracaoDoTeste {

        @Bean
        CacheManager cacheManager() {
            CaffeineCacheManager gerenciador = new CaffeineCacheManager("apuracao");
            gerenciador.setCaffeine(Caffeine.newBuilder()
                    .maximumSize(100)
                    .expireAfterWrite(Duration.ofMinutes(10)));
            return gerenciador;
        }

        @Bean
        PautaRepository pautaRepository() {
            return mock(PautaRepository.class);
        }

        @Bean
        SessaoVotacaoRepository sessaoVotacaoRepository() {
            return mock(SessaoVotacaoRepository.class);
        }

        @Bean
        VotoRepository votoRepository() {
            return mock(VotoRepository.class);
        }

        @Bean
        CpfClient cpfClient() {
            return mock(CpfClient.class);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }

        @Bean
        VotoService votoService(PautaRepository pautaRepository,
                                SessaoVotacaoRepository sessaoVotacaoRepository,
                                VotoRepository votoRepository,
                                CpfClient cpfClient,
                                Clock clock) {
            return new VotoService(pautaRepository, sessaoVotacaoRepository, votoRepository, cpfClient, clock);
        }
    }
}
