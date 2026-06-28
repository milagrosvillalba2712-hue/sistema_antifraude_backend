package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialAsignacionRepository extends JpaRepository<HistorialAsignacion, Long> {

    List<HistorialAsignacion> findByAlertaIdOrderByFechaDesc(Long alertaId);

    List<HistorialAsignacion> findByUsuarioDestinoIdOrderByFechaDesc(Long usuarioId);
}
