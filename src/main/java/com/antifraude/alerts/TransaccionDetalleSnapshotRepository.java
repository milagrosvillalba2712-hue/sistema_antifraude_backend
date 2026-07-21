package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransaccionDetalleSnapshotRepository extends JpaRepository<TransaccionDetalleSnapshot, Long> {
    Optional<TransaccionDetalleSnapshot> findByAlertaId(Long alertaId);
}
