package com.antifraude.common.repository;

import com.antifraude.common.entity.DocumentoLegal;
import com.antifraude.common.entity.TipoDocumentoLegal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoLegalRepository extends JpaRepository<DocumentoLegal, Long> {

    Optional<DocumentoLegal> findByTipoAndVersion(TipoDocumentoLegal tipo, Integer version);

    Optional<DocumentoLegal> findFirstByTipoAndActivoTrueOrderByVersionDesc(TipoDocumentoLegal tipo);

    List<DocumentoLegal> findByActivoTrue();

    List<DocumentoLegal> findByTipoAndActivoTrueOrderByVersionDesc(TipoDocumentoLegal tipo);
}
