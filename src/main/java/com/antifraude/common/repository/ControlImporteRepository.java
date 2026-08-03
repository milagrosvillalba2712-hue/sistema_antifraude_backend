package com.antifraude.common.repository;

import com.antifraude.common.entity.ControlImporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ControlImporteRepository extends JpaRepository<ControlImporte, Long> {
    @Query("SELECT ci FROM ControlImporte ci WHERE ci.activo = true")
    List<ControlImporte> findByProductoCodigo(@Param("productoCodigo") String productoCodigo);
}
