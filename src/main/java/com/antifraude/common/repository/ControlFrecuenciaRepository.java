package com.antifraude.common.repository;

import com.antifraude.common.entity.ControlFrecuencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ControlFrecuenciaRepository extends JpaRepository<ControlFrecuencia, Long> {
    @Query("SELECT cf FROM ControlFrecuencia cf WHERE cf.producto.codigo = :productoCodigo AND cf.activo = true")
    List<ControlFrecuencia> findByProductoCodigo(@Param("productoCodigo") String productoCodigo);
}
