package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstadisticaCargaAnalistaRepository extends JpaRepository<EstadisticaCargaAnalista, Long> {

    Optional<EstadisticaCargaAnalista> findByUsuarioIdAndFecha(Long usuarioId, LocalDate fecha);

    List<EstadisticaCargaAnalista> findByFecha(LocalDate fecha);

    @Query("SELECT e FROM EstadisticaCargaAnalista e WHERE e.fecha = :fecha ORDER BY e.alertasPendientes ASC")
    List<EstadisticaCargaAnalista> findByFechaOrderedByCarga(@Param("fecha") LocalDate fecha);
}
