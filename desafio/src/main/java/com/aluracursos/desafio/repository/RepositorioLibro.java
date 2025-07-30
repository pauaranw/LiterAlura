package com.aluracursos.desafio.repository;

import com.aluracursos.desafio.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositorioLibro extends JpaRepository<Libro, Long> {
    Optional<Libro> findByTitulo(String titulo);
    Long countByIdioma(String idioma);
}
