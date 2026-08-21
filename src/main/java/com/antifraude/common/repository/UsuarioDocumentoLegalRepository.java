package com.antifraude.common.repository;

import com.antifraude.common.entity.UsuarioDocumentoLegal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioDocumentoLegalRepository extends JpaRepository<UsuarioDocumentoLegal, Long> {

    List<UsuarioDocumentoLegal> findByUsuarioId(UUID usuarioId);

    Optional<UsuarioDocumentoLegal> findByUsuarioIdAndDocumentoLegalId(UUID usuarioId, Long documentoLegalId);

    boolean existsByUsuarioIdAndDocumentoLegalIdAndAceptoTrue(UUID usuarioId, Long documentoLegalId);

    List<UsuarioDocumentoLegal> findByEmpresaIdAndAceptoFalse(UUID empresaId);
}
