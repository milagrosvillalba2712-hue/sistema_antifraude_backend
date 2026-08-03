package com.antifraude.alerts;

import com.antifraude.common.entity.SujetoRiesgo;
import com.antifraude.transactions.Transaccion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coincidencia_lista_alerta", indexes = {
        @Index(name = "idx_coincidencia_lista_alerta_alerta", columnList = "alerta_id"),
        @Index(name = "idx_coincidencia_lista_alerta_transaccion", columnList = "transaccion_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoincidenciaListaAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id")
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "transaccion_id", referencedColumnName = "id"),
            @JoinColumn(name = "fecha_transaccion", referencedColumnName = "fecha_transaccion")
    })
    private Transaccion transaccion;

    @Column(name = "fecha_transaccion", insertable = false, updatable = false)
    private LocalDateTime fechaTransaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sujeto_riesgo_id")
    private SujetoRiesgo sujetoRiesgo;

    @Column(name = "fuente_codigo", length = 40)
    private String fuenteCodigo;

    @Column(name = "parte_transaccion", nullable = false, length = 40)
    private String parteTransaccion;

    @Column(name = "campo_evaluado", nullable = false, length = 60)
    private String campoEvaluado;

    @Column(name = "valor_evaluado", nullable = false, columnDefinition = "TEXT")
    private String valorEvaluado;

    @Column(name = "porcentaje_coincidencia", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal scoreMatch = BigDecimal.valueOf(100);

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String severidad = "Alta";

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_hora_creacion", updatable = false, insertable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
