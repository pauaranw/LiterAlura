package com.aluracursos.desafio;

import com.aluracursos.desafio.principal.Principal;
import com.aluracursos.desafio.repository.RepositorioAutor;
import com.aluracursos.desafio.repository.RepositorioLibro;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class DesafioApplication implements CommandLineRunner {

	@Autowired
	private RepositorioLibro repoLibro;

	@Autowired
	private RepositorioAutor repoAutor;

	@Autowired
	private Principal principal;

	@Override
	public void run(String... args) {
		principal.muestraElMenu();
	}

	public static void main(String[] args) {
		SpringApplication.run(DesafioApplication.class, args);
	}


}
