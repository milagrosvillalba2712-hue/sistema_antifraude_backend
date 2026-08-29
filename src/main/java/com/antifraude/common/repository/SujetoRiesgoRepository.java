package com.antifraude.common.repository;

import com.antifraude.common.entity.SujetoRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SujetoRiesgoRepository extends JpaRepository<SujetoRiesgo, Long> {
    List<SujetoRiesgo> findByNombreNormalizadoAndActivoTrue(String nombreNormalizado);

    List<SujetoRiesgo> findByActivoTrue();
}
