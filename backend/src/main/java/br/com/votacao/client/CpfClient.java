package br.com.votacao.client;

public interface CpfClient {

    StatusCpfResponse consultar(String cpf);
}
