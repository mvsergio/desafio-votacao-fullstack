package br.com.votacao.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.votacao.exception.CpfInvalidoException;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FakeCpfClientTest {

    private static final String CPF = "12345678901";

    @Mock
    private RandomGenerator randomGenerator;

    @Test
    void deveLancarCpfInvalidoQuandoSorteioResultarEmZero() {
        when(randomGenerator.nextInt(3)).thenReturn(0);
        FakeCpfClient client = new FakeCpfClient(randomGenerator);

        assertThatThrownBy(() -> client.consultar(CPF))
                .isInstanceOf(CpfInvalidoException.class)
                .hasMessage("CPF inválido");
    }

    @Test
    void deveRetornarAbleToVoteQuandoSorteioResultarEmUm() {
        when(randomGenerator.nextInt(3)).thenReturn(1);
        FakeCpfClient client = new FakeCpfClient(randomGenerator);

        StatusCpfResponse resposta = client.consultar(CPF);

        assertThat(resposta.status()).isEqualTo(StatusCpf.ABLE_TO_VOTE);
    }

    @Test
    void deveRetornarUnableToVoteQuandoSorteioResultarEmDois() {
        when(randomGenerator.nextInt(3)).thenReturn(2);
        FakeCpfClient client = new FakeCpfClient(randomGenerator);

        StatusCpfResponse resposta = client.consultar(CPF);

        assertThat(resposta.status()).isEqualTo(StatusCpf.UNABLE_TO_VOTE);
    }
}
