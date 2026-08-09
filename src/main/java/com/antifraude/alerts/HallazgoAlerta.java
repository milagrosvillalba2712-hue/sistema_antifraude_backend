package com.antifraude.alerts;

import com.antifraude.rules.ReglaRiesgo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

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

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @Column(name = "transaccion_id", nullable = false)
    private Long transaccionId;

    @Column(name = "fecha_transaccion", nullable = false)
    private OffsetDateTime fechaTransaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_riesgo_id")
    private ReglaRiesgo regla;

    @Column(name = "tipo_hallazgo", nullable = false, length = 60)
    private String tipo;

    @Transient
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String severidad;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal score = BigDecimal.ZERO;

    @Transient
    private String fuente;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle_json", columnDefinition = "jsonb")
    private String detalleJson;

    @Column(name = "fecha_hora_creacion", updatable = false)
    @Builder.Default
    private OffsetDateTime fechaRegistro = OffsetDateTime.now();

    @PrePersist
    void prePersist() {
        if (empresaId == null && alerta != null) {
            empresaId = alerta.getEmpresaId();
        }
        if (transaccionId == null && alerta != null && alerta.getTransaccion() != null) {
            transaccionId = alerta.getTransaccion().getId();
            fechaTransaccion = alerta.getTransaccion().getFechaTransaccion();
        }
        if (fechaRegistro == null) {
            fechaRegistro = OffsetDateTime.now();
        }
        if (score == null) {
            score = BigDecimal.ZERO;
        }
        if (severidad == null) {
            severidad = "MEDIA";
        }
    }
}
