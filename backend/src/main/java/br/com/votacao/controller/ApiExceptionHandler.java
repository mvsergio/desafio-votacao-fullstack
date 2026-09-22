package br.com.votacao.controller;

import br.com.votacao.exception.AssociadoNaoAptoException;
import br.com.votacao.exception.CpfInvalidoException;
import br.com.votacao.exception.RecursoNaoEncontradoException;
import br.com.votacao.exception.RegraNegocioException;
import br.com.votacao.exception.VotoDuplicadoException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler(CpfInvalidoException.class)
    public ProblemDetail tratarCpfInvalido(CpfInvalidoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("CPF inválido");
        return problema;
    }

    @ExceptionHandler(AssociadoNaoAptoException.class)
    public ProblemDetail tratarAssociadoNaoApto(AssociadoNaoAptoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Associado não apto");
        return problema;
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail tratarRegraNegocio(RegraNegocioException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problema.setTitle("Regra de negócio violada");
        return problema;
    }

    @ExceptionHandler(VotoDuplicadoException.class)
    public ProblemDetail tratarVotoDuplicado(VotoDuplicadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("Voto duplicado");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacao(MethodArgumentNotValidException ex) {
        List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Requisição contém campos inválidos");
        problema.setTitle("Dados inválidos");
        problema.setProperty("camposInvalidos", campos);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        log.warn("Corpo da requisição inválido: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente ou mal formatado");
        problema.setTitle("Requisição inválida");
        return problema;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tratarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String tipoEsperado = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconhecido";
        String detalhe = "Parâmetro '%s' com valor inválido; esperado tipo %s".formatted(ex.getName(), tipoEsperado);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problema.setTitle("Parâmetro inválido");
        return problema;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail tratarParametroAusente(MissingServletRequestParameterException ex) {
        String detalhe = "Parâmetro obrigatório '%s' ausente".formatted(ex.getParameterName());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problema.setTitle("Parâmetro obrigatório ausente");
        return problema;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        String suportados = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().stream()
                .map(HttpMethod::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        String detalhe = "Método '%s' não é suportado neste recurso%s"
                .formatted(ex.getMethod(), suportados.isBlank() ? "" : "; suportados: " + suportados);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, detalhe);
        problema.setTitle("Método não permitido");
        problema.setProperty("metodosSuportados", ex.getSupportedHttpMethods());
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).body(problema);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail tratarMediaTypeNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        String recebido = ex.getContentType() != null ? ex.getContentType().toString() : "não informado";
        String detalhe = "Content-Type '%s' não é suportado".formatted(recebido);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, detalhe);
        problema.setTitle("Media type não suportado");
        return problema;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail tratarRotaInexistente(NoResourceFoundException ex) {
        String detalhe = "Recurso '%s' não encontrado".formatted(ex.getResourcePath());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detalhe);
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarErroInesperado(Exception ex) {
        log.error("Erro inesperado ao processar requisição", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado ao processar a requisição");
        problema.setTitle("Erro interno");
        return problema;
    }

    public record CampoInvalido(String campo, String mensagem) {
    }
}
