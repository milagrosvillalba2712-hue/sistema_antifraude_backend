package com.antifraude.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DisponibilidadRepository extends JpaRepository<DisponibilidadUsuario, Long> {

    @Query("SELECT d FROM DisponibilidadUsuario d WHERE d.usuario.id = :usuarioId AND d.estado <> 'CANCELADA'")
    List<DisponibilidadUsuario> findByUsuarioIdAndActivoTrue(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT d FROM DisponibilidadUsuario d WHERE d.usuario.id = :usuarioId ORDER BY d.ultimaActualizacion DESC")
    List<DisponibilidadUsuario> findByUsuarioIdOrderByFechaInicioDesc(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT d FROM DisponibilidadUsuario d WHERE d.usuario.id = :usuarioId " +
            "AND d.estado <> 'CANCELADA' AND (d.fechaInicio IS NULL OR d.fechaInicio <= :ahora) " +
            "AND (d.fechaFin IS NULL OR d.fechaFin >= :ahora)")
    List<DisponibilidadUsuario> findActivasAhora(@Param("usuarioId") UUID usuarioId, @Param("ahora") OffsetDateTime ahora);

    @Query("SELECT count(d) > 0 FROM DisponibilidadUsuario d WHERE d.usuario.id = :usuarioId AND d.estado IN :tipoEstados AND d.estado <> 'CANCELADA'")
    boolean existsByUsuarioIdAndActivoTrueAndTipoEstadoIn(@Param("usuarioId") UUID usuarioId, @Param("tipoEstados") List<String> tipoEstados);

    @Query("SELECT d FROM DisponibilidadUsuario d WHERE d.esProgramado = true " +
            "AND d.fechaInicio BETWEEN :desde AND :ahora ORDER BY d.fechaInicio DESC")
    List<DisponibilidadUsuario> findProgramadasRecientes(@Param("ahora") OffsetDateTime ahora, @Param("desde") OffsetDateTime desde);
}
