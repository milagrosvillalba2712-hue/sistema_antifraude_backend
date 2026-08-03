package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UsoSuscripcionRepository extends JpaRepository<UsoSuscripcion, Long> {
    List<UsoSuscripcion> findByEmpresaIdOrderByAnioDescMesDesc(UUID empresaId);
}
