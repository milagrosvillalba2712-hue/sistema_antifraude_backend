package com.antifraude.rules;

import com.antifraude.transactions.Transaccion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ejecucion_reglas", indexes = {
        @Index(name = "idx_ejecucion_reglas_transaccion", columnList = "transaccion_id"),
        @Index(name = "idx_ejecucion_reglas_fecha", columnList = "fecha_hora_creacion")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjecucionRegla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_id", nullable = false)
    private ReglaRiesgo regla;

    @Column(name = "regla_codigo", nullable = false, length = 80)
    private String reglaCodigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "transaccion_id", referencedColumnName = "id", nullable = false),
            @JoinColumn(name = "fecha_transaccion", referencedColumnName = "fecha_transaccion", nullable = false)
    })
    private Transaccion transaccion;

    @Column(name = "fecha_transaccion", insertable = false, updatable = false)
    private OffsetDateTime fechaTransaccion;

    @Column
    @Builder.Default
    private Boolean cumplida = false;

    @Column(name = "score_generado", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal scoreGenerado = BigDecimal.ZERO;

    @Column(name = "score_regla", precision = 8, scale = 2)
    private BigDecimal scoreRegla;

    @Column(name = "condicion_evaluada", columnDefinition = "TEXT")
    private String condicionEvaluada;

    @Transient
    private String resultadoEvaluacion;

    @Column(name = "tiempo_ejecucion_ms")
    private Long tiempoEjecucionMs;

    @Column(name = "fecha_hora_creacion", updatable = false)
    @Builder.Default
    private OffsetDateTime fechaEjecucion = OffsetDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String detalle;

    public String getResultadoEvaluacion() {
        return cumplida != null && cumplida ? "CUMPLIO" : "NO_CUMPLIO";
    }

    public BigDecimal getScoreRegla() {
        return scoreRegla != null ? scoreRegla : scoreGenerado;
    }
}
