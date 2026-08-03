package com.antifraude.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReporteRosRepository extends JpaRepository<ReporteRos, Long> {

    List<ReporteRos> findByGeneradoPorId(UUID usuarioId);

    List<ReporteRos> findByFechaGeneracionBetween(LocalDateTime inicio, LocalDateTime fin);
}
