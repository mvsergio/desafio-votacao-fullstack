package br.com.votacao;

import org.springframework.boot.SpringApplication;

public class TestVotacaoApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(VotacaoApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
