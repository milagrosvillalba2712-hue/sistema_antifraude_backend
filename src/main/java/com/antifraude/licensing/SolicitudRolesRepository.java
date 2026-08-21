package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SolicitudRolesRepository extends JpaRepository<SolicitudRoles, Long> {

    List<SolicitudRoles> findByEmpresaIdAndEstado(UUID empresaId, String estado);

    @Query("SELECT COUNT(ue) FROM UsuarioEmpresa ue WHERE ue.empresa.id = :empresaId " +
           "AND ue.rol.codigo = :rolCodigo AND ue.activo = true")
    long contarUsuariosPorRol(@Param("empresaId") UUID empresaId, @Param("rolCodigo") String rolCodigo);

    @Query("SELECT COALESCE(SUM(ra.cantidad), 0) FROM RolesAdquiridos ra " +
           "WHERE ra.empresa.id = :empresaId AND ra.rolCodigo = :rolCodigo AND ra.activo = true")
    long contarRolesAdquiridosExtra(@Param("empresaId") UUID empresaId, @Param("rolCodigo") String rolCodigo);
}
