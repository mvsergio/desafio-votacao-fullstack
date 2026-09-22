package br.com.votacao.config;

import br.com.votacao.client.CpfClient;
import br.com.votacao.client.CpfClientSempreLibera;
import br.com.votacao.client.FakeCpfClient;
import java.util.random.RandomGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CpfClientConfig {

    @Bean
    @ConditionalOnMissingBean
    public RandomGenerator randomGenerator() {
        return RandomGenerator.getDefault();
    }

    @Bean
    @ConditionalOnProperty(name = "votacao.validacao-cpf.habilitada", havingValue = "true", matchIfMissing = true)
    public CpfClient fakeCpfClient(RandomGenerator randomGenerator) {
        return new FakeCpfClient(randomGenerator);
    }

    @Bean
    @ConditionalOnProperty(name = "votacao.validacao-cpf.habilitada", havingValue = "false")
    public CpfClient cpfClientSempreLibera() {
        return new CpfClientSempreLibera();
    }
}
