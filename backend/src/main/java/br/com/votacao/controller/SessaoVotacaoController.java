package br.com.votacao.controller;

import br.com.votacao.dto.AbrirSessaoRequest;
import br.com.votacao.dto.SessaoResponse;
import br.com.votacao.model.SessaoVotacao;
import br.com.votacao.service.SessaoVotacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sessões de Votação", description = "Abertura de sessões de votação")
@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/sessao")
@RequiredArgsConstructor
public class SessaoVotacaoController {

    private final SessaoVotacaoService sessaoVotacaoService;
    private final Clock clock;

    @Operation(summary = "Abre uma sessão de votação para a pauta")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoResponse abrir(@PathVariable Long pautaId,
                                @Valid @RequestBody(required = false) AbrirSessaoRequest request) {
        SessaoVotacao sessao = sessaoVotacaoService.abrir(pautaId, request);
        return new SessaoResponse(
                sessao.getId(),
                sessao.getDataAbertura(),
                sessao.getDataFechamento(),
                sessao.isAberta(LocalDateTime.now(clock)));
    }
}
