package com.antifraude.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByUsuarioIdOrderByFechaEventoDesc(Long usuarioId);

    List<Auditoria> findByAccion(String accion);

    List<Auditoria> findByFechaEventoBetween(LocalDateTime inicio, LocalDateTime fin);
}
