package br.com.votacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.votacao.dto.AbrirSessaoRequest;
import br.com.votacao.dto.PautaRequest;
import br.com.votacao.dto.VotoRequest;
import br.com.votacao.exception.VotoDuplicadoException;
import br.com.votacao.model.OpcaoVoto;
import br.com.votacao.model.Pauta;
import br.com.votacao.repository.VotoRepository;
import br.com.votacao.service.PautaService;
import br.com.votacao.service.SessaoVotacaoService;
import br.com.votacao.service.VotoService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "votacao.validacao-cpf.habilitada=false")
@ExtendWith(OutputCaptureExtension.class)
class VotoConcorrenteIntegrationTests {

    private static final String CPF = "77788899900";
    private static final int NUMERO_DE_THREADS = 20;

    @Autowired
    private PautaService pautaService;

    @Autowired
    private SessaoVotacaoService sessaoVotacaoService;

    @Autowired
    private VotoService votoService;

    @Autowired
    private VotoRepository votoRepository;

    @Test
    void deveGravarApenasUmVotoQuandoMesmoCpfDisparaVotosConcorrentes(CapturedOutput saida) throws Exception {
        Pauta pauta = pautaService.cadastrar(new PautaRequest("Concorrência", "Corrida no voto"));
        sessaoVotacaoService.abrir(pauta.getId(), new AbrirSessaoRequest(5));

        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger duplicados = new AtomicInteger();
        AtomicInteger outrasFalhas = new AtomicInteger();
        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch termino = new CountDownLatch(NUMERO_DE_THREADS);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < NUMERO_DE_THREADS; i++) {
                executor.submit(() -> {
                    try {
                        inicio.await();
                        votoService.registrar(pauta.getId(), new VotoRequest(CPF, OpcaoVoto.SIM));
                        sucessos.incrementAndGet();
                    } catch (VotoDuplicadoException ex) {
                        duplicados.incrementAndGet();
                    } catch (Exception ex) {
                        outrasFalhas.incrementAndGet();
                    } finally {
                        termino.countDown();
                    }
                });
            }
            inicio.countDown();
            assertThat(termino.await(30, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(sucessos.get()).isEqualTo(1);
        assertThat(duplicados.get()).isEqualTo(NUMERO_DE_THREADS - 1);
        assertThat(outrasFalhas.get()).isZero();
        assertThat(votoRepository.count()).isEqualTo(1L);
        assertThat(saida.getAll()).doesNotContain(CPF);
    }
}
