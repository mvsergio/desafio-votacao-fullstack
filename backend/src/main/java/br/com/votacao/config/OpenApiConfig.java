package br.com.votacao.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI votacaoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API de Votação de Pautas")
                .description("Serviço para cadastro de pautas, abertura de sessões de votação, "
                        + "registro de votos e apuração de resultados.")
                .version("v1"));
    }
}
