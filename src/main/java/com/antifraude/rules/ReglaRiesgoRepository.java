package com.antifraude.rules;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReglaRiesgoRepository extends JpaRepository<ReglaRiesgo, Long> {

    List<ReglaRiesgo> findByActivaTrue();

    List<ReglaRiesgo> findBySeveridad(String severidad);

    List<ReglaRiesgo> findByTipoRegla(String tipoRegla);

    List<ReglaRiesgo> findByEscenarioId(Long escenarioId);

    List<ReglaRiesgo> findByEstado(String estado);

    Optional<ReglaRiesgo> findByNombreAndVersion(String nombre, Integer version);

    List<ReglaRiesgo> findByNombreOrderByVersionDesc(String nombre);
}
