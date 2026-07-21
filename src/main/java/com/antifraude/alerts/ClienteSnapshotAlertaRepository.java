package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteSnapshotAlertaRepository extends JpaRepository<ClienteSnapshotAlerta, Long> {
    Optional<ClienteSnapshotAlerta> findByAlertaId(Long alertaId);
}
