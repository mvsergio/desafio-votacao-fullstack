package br.com.votacao.dto;

import br.com.votacao.model.OpcaoVoto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VotoRequest(
        @NotBlank(message = "CPF do associado é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "CPF do associado deve conter exatamente 11 dígitos")
        String cpfAssociado,

        @NotNull(message = "Opção de voto é obrigatória")
        OpcaoVoto opcao
) {
}
