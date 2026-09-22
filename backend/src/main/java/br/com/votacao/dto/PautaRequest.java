package br.com.votacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 150, message = "Título deve ter no máximo 150 caracteres")
        String titulo,

        @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
        String descricao
) {
}
