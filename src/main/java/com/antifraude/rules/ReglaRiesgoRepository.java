package com.antifraude.rules;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReglaRiesgoRepository extends JpaRepository<ReglaRiesgo, Long> {

    List<ReglaRiesgo> findByActivaTrueAndEstado(String estado);

    List<ReglaRiesgo> findBySeveridad(String severidad);

    List<ReglaRiesgo> findByTipoRegla(String tipoRegla);

    List<ReglaRiesgo> findByEscenarioId(Long escenarioId);

    List<ReglaRiesgo> findByEstado(String estado);

    Optional<ReglaRiesgo> findByNombreAndVersion(String nombre, Integer version);

    List<ReglaRiesgo> findByNombreOrderByVersionDesc(String nombre);

    @Query("select count(r) from ReglaRiesgo r where r.empresa.id = :empresaId or r.empresa is null")
    long countParaEmpresa(@Param("empresaId") UUID empresaId);
}
