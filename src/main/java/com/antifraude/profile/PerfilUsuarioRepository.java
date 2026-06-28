package com.antifraude.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilUsuarioRepository extends JpaRepository<PerfilUsuario, Long> {

    Optional<PerfilUsuario> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);
}
