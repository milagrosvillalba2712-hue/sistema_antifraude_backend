package com.antifraude.rules;

import com.antifraude.transactions.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EjecucionReglaRepository extends JpaRepository<EjecucionRegla, Long> {

    List<EjecucionRegla> findByTransaccion(Transaccion transaccion);

    List<EjecucionRegla> findByReglaId(Long reglaId);

    @Query("SELECT e FROM EjecucionRegla e WHERE e.transaccion.id = :transaccionId " +
            "AND ((:resultadoEvaluacion = 'CUMPLIO' AND e.cumplida = true) " +
            "OR (:resultadoEvaluacion <> 'CUMPLIO' AND e.cumplida = false)) ORDER BY e.fechaEjecucion DESC")
    List<EjecucionRegla> findByTransaccionIdAndResultadoEvaluacionOrderByFechaEjecucionDesc(
            @Param("transaccionId") Long transaccionId,
            @Param("resultadoEvaluacion") String resultadoEvaluacion);

    List<EjecucionRegla> findByFechaEjecucionBetween(OffsetDateTime inicio, OffsetDateTime fin);

    @Query("SELECT COUNT(e) FROM EjecucionRegla e WHERE e.reglaCodigo = :reglaCodigo " +
            "AND ((:resultado = 'CUMPLIO' AND e.cumplida = true) OR (:resultado <> 'CUMPLIO' AND e.cumplida = false))")
    long countByReglaIdAndResultadoEvaluacion(@Param("reglaCodigo") String reglaCodigo, @Param("resultado") String resultado);
}
