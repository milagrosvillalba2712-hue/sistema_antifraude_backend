package com.antifraude.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select distinct ue.usuario from UsuarioEmpresa ue " +
            "where ue.rol.codigo = :rolCodigo and ue.activo = true and ue.usuario.activo = true")
    List<Usuario> findActivosByRolCodigo(@Param("rolCodigo") String rolCodigo);
}
