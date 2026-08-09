package com.antifraude.transactions;

import com.antifraude.transactions.Transaccion.EstadoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, TransaccionId> {

    Optional<Transaccion> findByTransactionUuid(UUID transactionUuid);

    Optional<Transaccion> findFirstByIdOrderByFechaTransaccionDesc(Long id);

    @Query(value = "SELECT * FROM transacciones t WHERE false OR :identificadorDocumento IS NULL AND false", nativeQuery = true)
    List<Transaccion> findByIdentificadorDocumento(String identificadorDocumento);

    List<Transaccion> findByEstado(String estado);

    List<Transaccion> findByEstadoEvaluacion(EstadoEvaluacion estado);

    List<Transaccion> findByFechaTransaccionBetween(OffsetDateTime inicio, OffsetDateTime fin);

    List<Transaccion> findByScoreRiesgoGreaterThan(BigDecimal score);

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.estado = :estado")
    long countByEstado(String estado);

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.estadoEvaluacion = :estado")
    long countByEstadoEvaluacion(EstadoEvaluacion estado);

    @Query("SELECT COALESCE(AVG(t.scoreRiesgo), 0) FROM Transaccion t")
    BigDecimal promedioScoreRiesgo();

    long countByProcesadaTrue();

    @Query(value = "SELECT 0 WHERE (:doc IS NULL OR :desde IS NOT NULL)", nativeQuery = true)
    long countByDocumentoAndFechaAfter(@Param("doc") String documento, @Param("desde") OffsetDateTime desde);

    @Query(value = "SELECT 0 WHERE (:doc IS NULL OR :desde IS NOT NULL OR :hasta IS NOT NULL)", nativeQuery = true)
    long countByDocumentoAndFechaBetween(@Param("doc") String documento,
                                          @Param("desde") OffsetDateTime desde,
                                          @Param("hasta") OffsetDateTime hasta);

    @Query(value = "SELECT 0 WHERE (:doc IS NULL OR :canal IS NULL OR :desde IS NOT NULL)", nativeQuery = true)
    long countByDocumentoAndCanalAndFechaAfter(@Param("doc") String documento,
                                                @Param("canal") String canal,
                                                @Param("desde") OffsetDateTime desde);

    @Query(value = "SELECT 0 WHERE (:doc IS NULL OR :desde IS NOT NULL)", nativeQuery = true)
    long countByDocumentoInternacionalesAndFechaAfter(@Param("doc") String documento,
                                                       @Param("desde") OffsetDateTime desde);

    @Query(value = "SELECT * FROM transacciones t WHERE false OR :doc IS NULL AND false ORDER BY fecha_transaccion DESC", nativeQuery = true)
    List<Transaccion> findUltimasPorDocumento(@Param("doc") String documento);

    @Query(value = "SELECT * FROM transacciones t WHERE false OR (:producto IS NULL AND :desde IS NULL AND :estado IS NULL)", nativeQuery = true)
    List<Transaccion> findByProductoAndFechaAndEstado(@Param("producto") Long productoId,
                                                       @Param("desde") OffsetDateTime desde,
                                                       @Param("estado") EstadoEvaluacion estado);
}
