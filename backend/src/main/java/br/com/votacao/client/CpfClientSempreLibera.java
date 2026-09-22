package br.com.votacao.client;

public class CpfClientSempreLibera implements CpfClient {

    @Override
    public StatusCpfResponse consultar(String cpf) {
        return new StatusCpfResponse(StatusCpf.ABLE_TO_VOTE);
    }
}
