package com.antifraude.common.repository;

import com.antifraude.common.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {

    Optional<TipoDocumento> findByCodigo(String codigo);

    Optional<TipoDocumento> findByCodigoTecnico(String codigoTecnico);

    @Query("""
            SELECT td
            FROM TipoDocumento td
            LEFT JOIN td.paisRelacion p
            WHERE td.estadoActivo = true
              AND td.activo = true
              AND (:paisCodigo IS NULL OR p.codigoIso = :paisCodigo OR td.paisRelacion IS NULL)
            ORDER BY CASE WHEN p.codigoIso = :paisCodigo THEN 0 ELSE 1 END, td.nombre
            """)
    List<TipoDocumento> findActivosByPaisCodigo(@Param("paisCodigo") String paisCodigo);
}
