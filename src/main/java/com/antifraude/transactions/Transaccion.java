package com.antifraude.transactions;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transacciones", indexes = {
        @Index(name = "idx_transacciones_documento", columnList = "identificador_documento"),
        @Index(name = "idx_transacciones_fecha", columnList = "fecha_transaccion"),
        @Index(name = "idx_transacciones_score", columnList = "score_riesgo")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_uuid", nullable = false, unique = true)
    private UUID transactionUuid;

    @Column(name = "identificador_documento", length = 30)
    private String identificadorDocumento;

    @Column(name = "cuenta_origen", nullable = false, columnDefinition = "TEXT")
    private String cuentaOrigen;

    @Column(name = "cuenta_destino", nullable = false, columnDefinition = "TEXT")
    private String cuentaDestino;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(length = 10)
    private String moneda;

    @Column(length = 30)
    private String canal;

    @Column(name = "tipo_transaccion", length = 50)
    private String tipoTransaccion;

    @Column(name = "ip_origen", length = 100)
    private String ipOrigen;

    @Column(name = "pais_origen", length = 100)
    private String paisOrigen;

    @Column(name = "fecha_transaccion", nullable = false)
    private LocalDateTime fechaTransaccion;

    @Column(length = 30)
    private String estado;

    @Column(name = "score_riesgo", precision = 5, scale = 2)
    private BigDecimal scoreRiesgo;

    @Builder.Default
    private Boolean procesada = false;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;
}
