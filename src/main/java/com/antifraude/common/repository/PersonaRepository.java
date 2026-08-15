package com.antifraude.common.repository;

import com.antifraude.common.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {
    Optional<Persona> findByPrimerNombreAndPrimerApellido(String primerNombre, String primerApellido);
}
