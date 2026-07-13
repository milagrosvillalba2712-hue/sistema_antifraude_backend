package com.antifraude.transactions;

import com.antifraude.transactions.Transaccion.EstadoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Transaccion> findByEstadoEvaluacion(EstadoEvaluacion estado);

    List<Transaccion> findByFechaTransaccionBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Transaccion> findByScoreRiesgoGreaterThan(BigDecimal score);

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.estado = :estado")
    long countByEstado(String estado);

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.estadoEvaluacion = :estado")
    long countByEstadoEvaluacion(EstadoEvaluacion estado);

    @Query("SELECT COALESCE(AVG(t.scoreRiesgo), 0) FROM Transaccion t")
    BigDecimal promedioScoreRiesgo();

    long countByProcesadaTrue();

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.identificadorDocumento = :doc " +
           "AND t.fechaTransaccion >= :desde")
    long countByDocumentoAndFechaAfter(@Param("doc") String documento, @Param("desde") LocalDateTime desde);

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.identificadorDocumento = :doc " +
           "AND t.fechaTransaccion >= :desde AND t.fechaTransaccion < :hasta")
    long countByDocumentoAndFechaBetween(@Param("doc") String documento,
                                          @Param("desde") LocalDateTime desde,
                                          @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.identificadorDocumento = :doc " +
           "AND t.canal = :canal AND t.fechaTransaccion >= :desde")
    long countByDocumentoAndCanalAndFechaAfter(@Param("doc") String documento,
                                                @Param("canal") String canal,
                                                @Param("desde") LocalDateTime desde);

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.identificadorDocumento = :doc " +
           "AND t.paisOrigen IS NOT NULL AND t.paisOrigen <> 'NACIONAL' " +
           "AND t.fechaTransaccion >= :desde")
    long countByDocumentoInternacionalesAndFechaAfter(@Param("doc") String documento,
                                                       @Param("desde") LocalDateTime desde);

    @Query("SELECT t FROM Transaccion t WHERE t.identificadorDocumento = :doc " +
           "ORDER BY t.fechaTransaccion DESC")
    List<Transaccion> findUltimasPorDocumento(@Param("doc") String documento);

    @Query("SELECT t FROM Transaccion t WHERE t.producto.id = :producto " +
           "AND t.fechaTransaccion >= :desde AND t.estadoEvaluacion = :estado")
    List<Transaccion> findByProductoAndFechaAndEstado(@Param("producto") Long productoId,
                                                       @Param("desde") LocalDateTime desde,
                                                       @Param("estado") EstadoEvaluacion estado);
}
