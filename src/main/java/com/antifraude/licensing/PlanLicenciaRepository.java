package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanLicenciaRepository extends JpaRepository<PlanLicencia, Long> {
    Optional<PlanLicencia> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);

    @Query("SELECT pl FROM PlanLicencia pl JOIN Suscripcion s ON s.planLicencia = pl " +
           "WHERE s.empresa.id = :empresaId AND s.estado = 'ACTIVA' AND pl.activo = true " +
           "ORDER BY s.fechaFin DESC LIMIT 1")
    Optional<PlanLicencia> findPlanActivoByEmpresa(@Param("empresaId") UUID empresaId);

    @Query("SELECT ppr.precioAnual FROM PlanPlanPreciosRol ppr " +
           "JOIN PlanLicencia pl ON ppr.planLicencia = pl " +
           "JOIN Suscripcion s ON s.planLicencia = pl " +
           "WHERE s.empresa.id = :empresaId AND ppr.rol.codigo = :rolCodigo AND ppr.activo = true " +
           "AND s.estado = 'ACTIVA' ORDER BY s.fechaFin DESC, ppr.precioAnual ASC")
    List<BigDecimal> findPreciosRol(@Param("rolCodigo") String rolCodigo, @Param("empresaId") UUID empresaId);
}
