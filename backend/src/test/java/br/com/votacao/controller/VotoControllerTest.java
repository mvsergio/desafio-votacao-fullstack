package br.com.votacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.votacao.dto.ResultadoApuracaoResponse;
import br.com.votacao.dto.VotoRequest;
import br.com.votacao.exception.AssociadoNaoAptoException;
import br.com.votacao.exception.CpfInvalidoException;
import br.com.votacao.exception.RegraNegocioException;
import br.com.votacao.exception.VotoDuplicadoException;
import br.com.votacao.model.OpcaoVoto;
import br.com.votacao.model.Pauta;
import br.com.votacao.model.SituacaoResultado;
import br.com.votacao.model.Voto;
import br.com.votacao.service.VotoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VotoController.class)
class VotoControllerTest {

    private static final String CPF_VALIDO = "12345678901";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private VotoService votoService;

    @Test
    void deveRetornar201AoRegistrarVotoValido() throws Exception {
        Voto voto = votoDe(10L, OpcaoVoto.SIM);
        when(votoService.registrar(eq(1L), any(VotoRequest.class))).thenReturn(voto);
        String body = objectMapper.writeValueAsString(new VotoRequest(CPF_VALIDO, OpcaoVoto.SIM));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.opcao").value("SIM"));
    }

    @Test
    void deveRetornar400QuandoCpfNaoTiverOnzeDigitos() throws Exception {
        String body = objectMapper.writeValueAsString(new VotoRequest("123", OpcaoVoto.SIM));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.camposInvalidos[0].campo").value("cpfAssociado"));
    }

    @Test
    void deveRetornar400QuandoOpcaoForNula() throws Exception {
        String body = "{\"cpfAssociado\":\"" + CPF_VALIDO + "\"}";

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.camposInvalidos[0].campo").value("opcao"));
    }

    @Test
    void deveRetornar409QuandoAssociadoVotarDuasVezes() throws Exception {
        when(votoService.registrar(eq(1L), any(VotoRequest.class)))
                .thenThrow(new VotoDuplicadoException("Associado já votou nesta pauta"));
        String body = objectMapper.writeValueAsString(new VotoRequest(CPF_VALIDO, OpcaoVoto.SIM));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Voto duplicado"));
    }

    @Test
    void deveRetornar404QuandoCpfInvalido() throws Exception {
        when(votoService.registrar(eq(1L), any(VotoRequest.class)))
                .thenThrow(new CpfInvalidoException("CPF inválido"));
        String body = objectMapper.writeValueAsString(new VotoRequest(CPF_VALIDO, OpcaoVoto.SIM));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("CPF inválido"));
    }

    @Test
    void deveRetornar404QuandoAssociadoNaoApto() throws Exception {
        when(votoService.registrar(eq(1L), any(VotoRequest.class)))
                .thenThrow(new AssociadoNaoAptoException("Associado não está apto a votar"));
        String body = objectMapper.writeValueAsString(new VotoRequest(CPF_VALIDO, OpcaoVoto.SIM));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Associado não apto"));
    }

    @Test
    void deveRetornar422QuandoSessaoJaEstiverEncerrada() throws Exception {
        when(votoService.registrar(eq(1L), any(VotoRequest.class)))
                .thenThrow(new RegraNegocioException("Sessão de votação da pauta 1 está encerrada"));
        String body = objectMapper.writeValueAsString(new VotoRequest(CPF_VALIDO, OpcaoVoto.SIM));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    void deveRetornar400QuandoOpcaoForEnumInvalido() throws Exception {
        String body = "{\"cpfAssociado\":\"" + CPF_VALIDO + "\",\"opcao\":\"TALVEZ\"}";

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void deveRetornar200AoConsultarResultadoDaPauta() throws Exception {
        ResultadoApuracaoResponse resposta = new ResultadoApuracaoResponse(1L, 3L, 1L, 4L, SituacaoResultado.APROVADA);
        when(votoService.apurar(1L)).thenReturn(resposta);

        mockMvc.perform(get("/api/v1/pautas/1/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pautaId").value(1))
                .andExpect(jsonPath("$.totalSim").value(3))
                .andExpect(jsonPath("$.totalNao").value(1))
                .andExpect(jsonPath("$.situacao").value("APROVADA"));
    }

    private Voto votoDe(Long id, OpcaoVoto opcao) {
        Voto voto = new Voto();
        voto.setId(id);
        Pauta pauta = new Pauta();
        pauta.setId(1L);
        voto.setPauta(pauta);
        voto.setOpcao(opcao);
        voto.setCpfAssociado(CPF_VALIDO);
        voto.setDataVoto(LocalDateTime.now());
        return voto;
    }
}
