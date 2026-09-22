package br.com.votacao.dto;

import java.time.LocalDateTime;

public record VotoRegistradoResponse(
        Long id,
        String opcao,
        LocalDateTime dataVoto
) {
}
