package br.com.votacao.dto;

import java.time.LocalDateTime;

public record SessaoResponse(
        Long id,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        boolean aberta
) {
}
