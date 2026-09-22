package br.com.votacao.exception;

public class AssociadoNaoAptoException extends RuntimeException {

    public AssociadoNaoAptoException(String mensagem) {
        super(mensagem);
    }
}
