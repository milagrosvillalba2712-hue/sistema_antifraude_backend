package com.antifraude.common.repository;

import com.antifraude.common.entity.Escenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EscenarioRepository extends JpaRepository<Escenario, Long> {
    Optional<Escenario> findByCodigo(String codigo);
}
