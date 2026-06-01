package com.antifraude.rules;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReglaRiesgoRepository extends JpaRepository<ReglaRiesgo, Long> {

    List<ReglaRiesgo> findByActivaTrue();

    List<ReglaRiesgo> findBySeveridad(String severidad);

    List<ReglaRiesgo> findByTipoRegla(String tipoRegla);
}
