package com.antifraude.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DisponibilidadRepository extends JpaRepository<DisponibilidadUsuario, Long> {

    List<DisponibilidadUsuario> findByUsuarioIdAndActivoTrue(Long usuarioId);

    List<DisponibilidadUsuario> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);

    @Query("SELECT d FROM DisponibilidadUsuario d WHERE d.usuario.id = :usuarioId AND d.activo = true " +
           "AND d.fechaInicio <= :ahora AND (d.fechaFin IS NULL OR d.fechaFin > :ahora)")
    List<DisponibilidadUsuario> findActivasAhora(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);

    boolean existsByUsuarioIdAndActivoTrueAndTipoEstadoIn(Long usuarioId, List<String> tipoEstados);

    @Query("SELECT d FROM DisponibilidadUsuario d WHERE d.esProgramado = true AND d.activo = true " +
           "AND d.fechaInicio <= :ahora AND d.fechaInicio > :desde")
    List<DisponibilidadUsuario> findProgramadasRecientes(@Param("ahora") LocalDateTime ahora, @Param("desde") LocalDateTime desde);
}
