package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermisoSistemaRepository extends JpaRepository<PermisoSistema, Long> {
    Optional<PermisoSistema> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
