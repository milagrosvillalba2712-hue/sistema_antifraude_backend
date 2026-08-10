package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsumoLicenciaLocalRepository extends JpaRepository<ConsumoLicenciaLocal, Long> {
    Optional<ConsumoLicenciaLocal> findByInstalacionIdAndAnioAndMes(UUID instalacionId, int anio, int mes);
}