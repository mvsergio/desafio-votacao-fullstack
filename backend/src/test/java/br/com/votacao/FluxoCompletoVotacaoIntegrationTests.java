package br.com.votacao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.votacao.dto.PautaRequest;
import br.com.votacao.dto.VotoRequest;
import br.com.votacao.model.OpcaoVoto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "votacao.validacao-cpf.habilitada=false")
class FluxoCompletoVotacaoIntegrationTests {

    private static final String CPF = "12345678901";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveExecutarFluxoCompletoDeCadastroAbrirSessaoVotarERecuperarResultado() throws Exception {
        Long pautaId = cadastrarPauta();

        mockMvc.perform(post("/api/v1/pautas/" + pautaId + "/sessao"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aberta").value(true));

        String votoJson = objectMapper.writeValueAsString(new VotoRequest(CPF, OpcaoVoto.SIM));
        mockMvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.opcao").value("SIM"));

        mockMvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Voto duplicado"));

        mockMvc.perform(get("/api/v1/pautas/" + pautaId + "/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSim").value(1))
                .andExpect(jsonPath("$.totalNao").value(0))
                .andExpect(jsonPath("$.totalVotos").value(1))
                .andExpect(jsonPath("$.situacao").value("EM_ANDAMENTO"));
    }

    private Long cadastrarPauta() throws Exception {
        String pautaJson = objectMapper.writeValueAsString(new PautaRequest("Reforma", "Descricao"));
        MvcResult resultado = mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pautaJson))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
