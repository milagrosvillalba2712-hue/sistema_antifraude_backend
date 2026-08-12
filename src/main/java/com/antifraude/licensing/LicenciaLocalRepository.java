package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LicenciaLocalRepository extends JpaRepository<LicenciaLocal, UUID> {

    List<LicenciaLocal> findByInstalacionId(UUID instalacionId);

    Optional<LicenciaLocal> findTopByInstalacionIdOrderByEmitidaEnDesc(UUID instalacionId);
}