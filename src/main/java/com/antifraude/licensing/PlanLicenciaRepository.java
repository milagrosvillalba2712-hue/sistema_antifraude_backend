package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanLicenciaRepository extends JpaRepository<PlanLicencia, Long> {
    Optional<PlanLicencia> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
