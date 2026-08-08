package com.antifraude.rules;

import com.antifraude.common.entity.AuditableEntity;
import com.antifraude.common.entity.NivelRiesgo;
import com.antifraude.licensing.Empresa;
import com.antifraude.transactions.Transaccion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Mapea evaluaciones_riesgo. Resultado consolidado del motor de riesgo para
 * una transaccion (score_total, nivel_riesgo, resultado) — no un catalogo.
 *
 * Extiende AuditableEntity para heredar fecha_hora_creacion,
 * fecha_hora_modificacion, usuario_creacion_id y usuario_modificacion_id con
 * el tipo correcto (OffsetDateTime para timestamptz).
 *
 * FK compuesta hacia transacciones (id, fecha_transaccion): transacciones
 * esta particionada por RANGE sobre fecha_transaccion, asi que la referencia
 * tiene que incluirla (igual que alertas_antifraude, ejecucion_reglas, etc.).
 */
@Entity
@Table(name = "evaluaciones_riesgo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class EvaluacionRiesgo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // Relacion de solo lectura hacia la fila particionada correspondiente.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "transaccion_id", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "fecha_transaccion", referencedColumnName = "fecha_transaccion", insertable = false, updatable = false)
    })
    private Transaccion transaccion;

    @Column(name = "transaccion_id", nullable = false)
    private Long transaccionId;

    @Column(name = "fecha_transaccion", nullable = false)
    private OffsetDateTime fechaTransaccion;

    @Column(name = "score_total", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal scoreTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_riesgo_id")
    private NivelRiesgo nivelRiesgo;

    @Column(nullable = false, length = 30)
    private String resultado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private java.util.Map<String, Object> detalle = new java.util.HashMap<>();
}