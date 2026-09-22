package br.com.votacao.controller;

import br.com.votacao.dto.PautaRequest;
import br.com.votacao.dto.PautaResponse;
import br.com.votacao.model.Pauta;
import br.com.votacao.service.PautaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Pautas", description = "Cadastro e consulta de pautas de votação")
@RestController
@RequestMapping("/api/v1/pautas")
@RequiredArgsConstructor
public class PautaController {

    private final PautaService pautaService;

    @Operation(summary = "Cadastra uma nova pauta")
    @PostMapping
    public ResponseEntity<PautaResponse> cadastrar(@Valid @RequestBody PautaRequest request) {
        Pauta pauta = pautaService.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(pauta.getId())
                .toUri();
        return ResponseEntity.created(location).body(pautaService.detalhar(pauta.getId()));
    }

    @Operation(summary = "Lista pautas paginadas com status da sessão")
    @GetMapping
    public Page<PautaResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
        return pautaService.listar(pageable);
    }

    @Operation(summary = "Detalha uma pauta e sua sessão de votação")
    @GetMapping("/{id}")
    public PautaResponse detalhar(@PathVariable Long id) {
        return pautaService.detalhar(id);
    }
}
