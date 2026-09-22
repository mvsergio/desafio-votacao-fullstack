package br.com.votacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.votacao.dto.AbrirSessaoRequest;
import br.com.votacao.exception.RecursoNaoEncontradoException;
import br.com.votacao.exception.RegraNegocioException;
import br.com.votacao.model.Pauta;
import br.com.votacao.model.SessaoVotacao;
import br.com.votacao.service.SessaoVotacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SessaoVotacaoController.class)
@Import(SessaoVotacaoControllerTest.ClockTestConfig.class)
class SessaoVotacaoControllerTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 1, 9, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SessaoVotacaoService sessaoVotacaoService;

    @Test
    void deveRetornar201AoAbrirSessaoSemBody() throws Exception {
        SessaoVotacao sessao = sessaoDe(1L, AGORA, AGORA.plusMinutes(1));
        when(sessaoVotacaoService.abrir(eq(1L), any())).thenReturn(sessao);

        mockMvc.perform(post("/api/v1/pautas/1/sessao"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.aberta").value(true));
    }

    @Test
    void deveRetornar201AoAbrirSessaoComDuracaoCustomizada() throws Exception {
        SessaoVotacao sessao = sessaoDe(2L, AGORA, AGORA.plusMinutes(15));
        when(sessaoVotacaoService.abrir(eq(1L), any())).thenReturn(sessao);
        String body = objectMapper.writeValueAsString(new AbrirSessaoRequest(15));

        mockMvc.perform(post("/api/v1/pautas/1/sessao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aberta").value(true));
    }

    @Test
    void deveRetornar400ComProblemDetailQuandoDuracaoInvalida() throws Exception {
        String body = objectMapper.writeValueAsString(new AbrirSessaoRequest(0));

        mockMvc.perform(post("/api/v1/pautas/1/sessao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.camposInvalidos[0].campo").value("duracaoEmMinutos"));
    }

    @Test
    void deveRetornar404QuandoPautaInexistente() throws Exception {
        when(sessaoVotacaoService.abrir(eq(9L), any()))
                .thenThrow(new RecursoNaoEncontradoException("Pauta 9 não encontrada"));

        mockMvc.perform(post("/api/v1/pautas/9/sessao"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void deveRetornar400QuandoDuracaoNaoForNumerica() throws Exception {
        String body = "{\"duracaoEmMinutos\":\"abc\"}";

        mockMvc.perform(post("/api/v1/pautas/1/sessao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void deveRetornar422QuandoPautaJaPossuiSessao() throws Exception {
        when(sessaoVotacaoService.abrir(eq(1L), any()))
                .thenThrow(new RegraNegocioException("Pauta 1 já possui sessão de votação"));

        mockMvc.perform(post("/api/v1/pautas/1/sessao"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    private SessaoVotacao sessaoDe(Long id, LocalDateTime abertura, LocalDateTime fechamento) {
        SessaoVotacao sessao = new SessaoVotacao();
        sessao.setId(id);
        Pauta pauta = new Pauta();
        pauta.setId(1L);
        sessao.setPauta(pauta);
        sessao.setDataAbertura(abertura);
        sessao.setDataFechamento(fechamento);
        return sessao;
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class ClockTestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }
}
