package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {
    List<UsuarioEmpresa> findByUsuarioIdAndActivoTrue(UUID usuarioId);
    Optional<UsuarioEmpresa> findFirstByUsuarioIdAndActivoTrueOrderByIdAsc(UUID usuarioId);
    boolean existsByUsuarioIdAndRolCodigo(UUID usuarioId, String rolCodigo);
}
