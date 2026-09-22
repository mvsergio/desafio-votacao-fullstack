package br.com.votacao.exception;

public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
