package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstalacionLocalRepository extends JpaRepository<InstalacionLocal, UUID> {

    Optional<InstalacionLocal> findByIdentificadorInstalacion(String identificadorInstalacion);

    Optional<InstalacionLocal> findByFingerprintHash(String fingerprintHash);

    Optional<InstalacionLocal> findTopByEmpresaIdOrderByActivadaEnDesc(UUID empresaId);
}