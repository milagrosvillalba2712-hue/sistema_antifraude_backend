package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResolucionAlertaRepository extends JpaRepository<ResolucionAlerta, Long> {
    Optional<ResolucionAlerta> findFirstByAlertaIdOrderByFechaResolucionDesc(Long alertaId);
}
