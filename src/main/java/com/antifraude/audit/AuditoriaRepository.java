package com.antifraude.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByUsuarioIdOrderByFechaEventoDesc(UUID usuarioId);

    List<Auditoria> findByAccion(String accion);

    List<Auditoria> findByFechaEventoBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Auditoria> findByEntidadAfectadaAndEntidadIdOrderByFechaEventoDesc(String entidadAfectada, String entidadId);
}
