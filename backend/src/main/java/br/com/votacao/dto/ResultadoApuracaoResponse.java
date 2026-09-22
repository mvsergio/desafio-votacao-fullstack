package br.com.votacao.dto;

import br.com.votacao.model.SituacaoResultado;

public record ResultadoApuracaoResponse(
        Long pautaId,
        long totalSim,
        long totalNao,
        long totalVotos,
        SituacaoResultado situacao
) {
}
