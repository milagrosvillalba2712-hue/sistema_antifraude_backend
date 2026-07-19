package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {
    List<UsuarioEmpresa> findByUsuarioIdAndActivoTrue(Long usuarioId);
    Optional<UsuarioEmpresa> findFirstByUsuarioIdAndActivoTrueOrderByIdAsc(Long usuarioId);
    boolean existsByUsuarioIdAndRolCodigo(Long usuarioId, String rolCodigo);
}
