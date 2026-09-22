package br.com.votacao.exception;

public class CpfInvalidoException extends RuntimeException {

    public CpfInvalidoException(String mensagem) {
        super(mensagem);
    }
}
