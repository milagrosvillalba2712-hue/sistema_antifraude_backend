package com.antifraude.common.repository;

import com.antifraude.common.entity.NivelRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NivelRiesgoRepository extends JpaRepository<NivelRiesgo, Long> {

    Optional<NivelRiesgo> findByCodigo(String codigo);
}
