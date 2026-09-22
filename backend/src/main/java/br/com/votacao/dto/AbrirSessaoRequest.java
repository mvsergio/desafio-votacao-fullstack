package br.com.votacao.dto;

import jakarta.validation.constraints.Min;

public record AbrirSessaoRequest(
        @Min(value = 1, message = "Duração deve ser de no mínimo 1 minuto")
        Integer duracaoEmMinutos
) {
}
