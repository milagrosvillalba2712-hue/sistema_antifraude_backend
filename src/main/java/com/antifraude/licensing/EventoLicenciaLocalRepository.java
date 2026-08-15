package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoLicenciaLocalRepository extends JpaRepository<EventoLicenciaLocal, Long> {

    List<EventoLicenciaLocal> findTop20ByInstalacionIdOrderByFechaEventoDesc(UUID instalacionId);
}