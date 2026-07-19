package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsoSuscripcionRepository extends JpaRepository<UsoSuscripcion, Long> {
    List<UsoSuscripcion> findByEmpresaIdOrderByAnioDescMesDesc(Long empresaId);
}
