package com.antifraude.rules;

import com.antifraude.transactions.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EjecucionReglaRepository extends JpaRepository<EjecucionRegla, Long> {

    List<EjecucionRegla> findByTransaccion(Transaccion transaccion);

    List<EjecucionRegla> findByReglaId(Long reglaId);

    List<EjecucionRegla> findByFechaEjecucionBetween(LocalDateTime inicio, LocalDateTime fin);

    long countByReglaIdAndResultadoEvaluacion(Long reglaId, String resultado);
}
