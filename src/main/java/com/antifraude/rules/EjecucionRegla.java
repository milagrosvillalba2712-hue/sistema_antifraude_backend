package com.antifraude.rules;

import com.antifraude.common.entity.BaseEntity;
import com.antifraude.transactions.Transaccion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ejecucion_regla", indexes = {
        @Index(name = "idx_ejecucion_regla_transaccion", columnList = "transaccion_id"),
        @Index(name = "idx_ejecucion_regla_fecha", columnList = "fecha_ejecucion")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjecucionRegla extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_id", nullable = false)
    private ReglaRiesgo regla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id", nullable = false)
    private Transaccion transaccion;

    @Column(name = "score_regla", precision = 5, scale = 2)
    private BigDecimal scoreRegla;

    @Column(name = "condicion_evaluada", columnDefinition = "TEXT")
    private String condicionEvaluada;

    @Column(name = "resultado_evaluacion", length = 20)
    private String resultadoEvaluacion; // CUMPLIO, NO_CUMPLIO

    @Column(name = "tiempo_ejecucion_ms")
    private Long tiempoEjecucionMs;

    @Column(name = "fecha_ejecucion", updatable = false)
    @Builder.Default
    private LocalDateTime fechaEjecucion = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String detalle;
}
