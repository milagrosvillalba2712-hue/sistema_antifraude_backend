package com.antifraude.transactions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findByTransactionUuid(UUID transactionUuid);

    List<Transaccion> findByIdentificadorDocumento(String identificadorDocumento);

    List<Transaccion> findByEstado(String estado);

    List<Transaccion> findByFechaTransaccionBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Transaccion> findByScoreRiesgoGreaterThan(BigDecimal score);

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.estado = :estado")
    long countByEstado(String estado);

    @Query("SELECT COALESCE(AVG(t.scoreRiesgo), 0) FROM Transaccion t")
    BigDecimal promedioScoreRiesgo();

    long countByProcesadaTrue();
}
