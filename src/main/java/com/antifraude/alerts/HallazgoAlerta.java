package com.antifraude.alerts;

import com.antifraude.rules.ReglaRiesgo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hallazgo_alerta", indexes = {
        @Index(name = "idx_hallazgo_alerta_alerta", columnList = "alerta_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallazgoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_id")
    private ReglaRiesgo regla;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 20)
    private String severidad;

    @Column(precision = 8, scale = 2)
    private BigDecimal score;

    @Column(length = 80)
    private String fuente;

    @Column(name = "detalle_json", columnDefinition = "TEXT")
    private String detalleJson;

    @Column(name = "fecha_registro", updatable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
