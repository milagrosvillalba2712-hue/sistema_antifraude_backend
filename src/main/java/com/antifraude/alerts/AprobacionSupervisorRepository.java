package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AprobacionSupervisorRepository extends JpaRepository<AprobacionSupervisor, Long> {
    Optional<AprobacionSupervisor> findFirstByAlertaIdOrderByFechaSolicitudDesc(Long alertaId);
}
