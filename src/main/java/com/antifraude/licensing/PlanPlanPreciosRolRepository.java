package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanPlanPreciosRolRepository extends JpaRepository<PlanPlanPreciosRol, Long> {
    List<PlanPlanPreciosRol> findByPlanLicenciaCodigoOrderByPrecioAnualAsc(String planCodigo);
}