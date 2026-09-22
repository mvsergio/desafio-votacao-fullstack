package br.com.votacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.votacao.dto.PautaRequest;
import br.com.votacao.dto.PautaResponse;
import br.com.votacao.exception.RecursoNaoEncontradoException;
import br.com.votacao.model.Pauta;
import br.com.votacao.service.PautaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PautaController.class)
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PautaService pautaService;

    @Test
    void deveRetornar201ComLocationAoCadastrarPautaValida() throws Exception {
        Pauta salva = new Pauta();
        salva.setId(42L);
        salva.setTitulo("Titulo");
        salva.setDescricao("Descricao");
        salva.setDataCriacao(LocalDateTime.now());
        PautaResponse response = new PautaResponse(42L, "Titulo", "Descricao", salva.getDataCriacao(), null);
        when(pautaService.cadastrar(any(PautaRequest.class))).thenReturn(salva);
        when(pautaService.detalhar(42L)).thenReturn(response);

        String body = objectMapper.writeValueAsString(new PautaRequest("Titulo", "Descricao"));

        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/pautas/42"))
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void deveRetornar400ComProblemDetailQuandoTituloForBranco() throws Exception {
        String body = objectMapper.writeValueAsString(new PautaRequest("", "descricao"));

        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Requisição contém campos inválidos"))
                .andExpect(jsonPath("$.camposInvalidos[0].campo").value("titulo"));
    }

    @Test
    void deveRetornar200AoListarPautas() throws Exception {
        PautaResponse response = new PautaResponse(1L, "Titulo", "Desc", LocalDateTime.now(), null);
        when(pautaService.listar(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/pautas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void deveRetornar200AoDetalharPautaExistente() throws Exception {
        PautaResponse response = new PautaResponse(1L, "Titulo", "Desc", LocalDateTime.now(), null);
        when(pautaService.detalhar(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/pautas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Titulo"));
    }

    @Test
    void deveRetornar404ComProblemDetailQuandoPautaNaoEncontrada() throws Exception {
        when(pautaService.detalhar(9L)).thenThrow(new RecursoNaoEncontradoException("Pauta 9 não encontrada"));

        mockMvc.perform(get("/api/v1/pautas/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Pauta 9 não encontrada"));
    }

    @Test
    void deveRetornar500ComProblemDetailQuandoServicoFalhaInesperadamente() throws Exception {
        when(pautaService.detalhar(1L)).thenThrow(new RuntimeException("falha inesperada"));

        mockMvc.perform(get("/api/v1/pautas/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Erro interno"));
    }

    @Test
    void deveRetornar400QuandoBodyEstiverVazio() throws Exception {
        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void deveRetornar400QuandoJsonEstiverMalformado() throws Exception {
        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void deveRetornar415QuandoMediaTypeNaoForJson() throws Exception {
        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("qualquer coisa"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.title").value("Media type não suportado"));
    }

    @Test
    void deveRetornar405QuandoMetodoNaoForSuportado() throws Exception {
        mockMvc.perform(put("/api/v1/pautas"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.title").value("Método não permitido"));
    }

    @Test
    void deveRetornar404QuandoRotaNaoExistir() throws Exception {
        mockMvc.perform(get("/api/v1/naoexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }
}
