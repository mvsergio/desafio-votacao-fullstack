package br.com.votacao.controller;

import br.com.votacao.dto.ResultadoApuracaoResponse;
import br.com.votacao.dto.VotoRegistradoResponse;
import br.com.votacao.dto.VotoRequest;
import br.com.votacao.model.Voto;
import br.com.votacao.service.VotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Votos", description = "Registro de votos e apuração de resultado")
@RestController
@RequestMapping("/api/v1/pautas/{pautaId}")
@RequiredArgsConstructor
public class VotoController {

    private final VotoService votoService;

    @Operation(summary = "Registra o voto de um associado na pauta")
    @PostMapping("/votos")
    @ResponseStatus(HttpStatus.CREATED)
    public VotoRegistradoResponse registrar(@PathVariable Long pautaId,
                                             @Valid @RequestBody VotoRequest request) {
        Voto voto = votoService.registrar(pautaId, request);
        return new VotoRegistradoResponse(voto.getId(), voto.getOpcao().name(), voto.getDataVoto());
    }

    @Operation(summary = "Apura o resultado atual da pauta")
    @GetMapping("/resultado")
    public ResultadoApuracaoResponse resultado(@PathVariable Long pautaId) {
        return votoService.apurar(pautaId);
    }
}
