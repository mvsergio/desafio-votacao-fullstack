package br.com.votacao.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        // Fixado em UTC: entidades usam LocalDateTime (sem timezone) e o frontend
        // interpreta os ISO strings como UTC, para o cronômetro não depender do fuso da máquina.
        return Clock.systemUTC();
    }
}
