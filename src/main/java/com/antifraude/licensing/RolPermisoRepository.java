package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, Long> {
    boolean existsByRolCodigoAndPermisoCodigo(String rolCodigo, String permisoCodigo);

    @Query("select rp.permiso.codigo from RolPermiso rp where rp.rol.codigo = :rolCodigo")
    List<String> findPermisosByRolCodigo(@Param("rolCodigo") String rolCodigo);
}
