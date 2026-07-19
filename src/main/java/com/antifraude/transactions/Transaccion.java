package com.antifraude.transactions;

import com.antifraude.common.entity.*;
import com.antifraude.licensing.Empresa;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transacciones", indexes = {
        @Index(name = "idx_transacciones_documento", columnList = "identificador_documento"),
        @Index(name = "idx_transacciones_fecha", columnList = "fecha_transaccion"),
        @Index(name = "idx_transacciones_score", columnList = "score_riesgo"),
        @Index(name = "idx_transacciones_uuid", columnList = "transaction_uuid"),
        @Index(name = "idx_transacciones_estado_eval", columnList = "estado_evaluacion")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "codigo", length = 30, unique = true)
    private String codigo;

    @Column(name = "identificador_documento", length = 30)
    private String identificadorDocumento;

    @Column(name = "cuenta_origen", nullable = false, columnDefinition = "TEXT")
    private String cuentaOrigen;

    @Column(name = "cuenta_destino", nullable = false, columnDefinition = "TEXT")
    private String cuentaDestino;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moneda_id")
    private Moneda monedaRef;

    @Column(name = "moneda_codigo", length = 10)
    private String moneda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canal_id")
    private Canal canalRef;

    @Column(name = "canal_codigo", length = 30)
    private String canal;

    @Column(name = "tipo_transaccion", length = 50)
    private String tipoTransaccion;

    @Column(name = "ip_origen", length = 100)
    private String ipOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_origen_id")
    private Pais paisOrigenRef;

    @Column(name = "pais_origen", length = 100)
    private String paisOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_destino_id")
    private Pais paisDestinoRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_remitente_id")
    private Persona personaRemitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_beneficiario_id")
    private Persona personaBeneficiario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "fecha_transaccion", nullable = false)
    private LocalDateTime fechaTransaccion;

    @Column(name = "score_riesgo", precision = 5, scale = 2)
    private BigDecimal scoreRiesgo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_riesgo_id")
    private NivelRiesgo nivelRiesgo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_evaluacion", length = 30)
    @Builder.Default
    private EstadoEvaluacion estadoEvaluacion = EstadoEvaluacion.PENDIENTE;

    @Column(length = 30)
    private String estado;

    @Builder.Default
    private Boolean procesada = false;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    public enum EstadoEvaluacion {
        PENDIENTE, EN_PROCESO, APROBADA, RECHAZADA, REVISION_MANUAL, SOSPECHOSA
    }
}
