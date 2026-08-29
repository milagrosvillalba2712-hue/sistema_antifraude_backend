package com.antifraude.common.repository;

import com.antifraude.common.entity.CatalogoSyncEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CatalogoSyncEstadoRepository extends JpaRepository<CatalogoSyncEstado, Long> {
    Optional<CatalogoSyncEstado> findByEmpresaIdAndCatalogoCodigo(UUID empresaId, String catalogoCodigo);
}
