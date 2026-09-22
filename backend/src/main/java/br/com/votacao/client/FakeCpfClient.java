package br.com.votacao.client;

import br.com.votacao.exception.CpfInvalidoException;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FakeCpfClient implements CpfClient {

    private final RandomGenerator randomGenerator;

    @Override
    public StatusCpfResponse consultar(String cpf) {
        int sorteio = randomGenerator.nextInt(3);
        return switch (sorteio) {
            case 0 -> throw new CpfInvalidoException("CPF inválido");
            case 1 -> new StatusCpfResponse(StatusCpf.ABLE_TO_VOTE);
            default -> new StatusCpfResponse(StatusCpf.UNABLE_TO_VOTE);
        };
    }
}
