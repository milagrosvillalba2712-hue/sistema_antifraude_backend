package com.antifraude.licensing;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mapea evento_licencia_local. Bitacora de eventos de licenciamiento local
 * (activacion, verificacion, expiracion, etc.) con detalle sanitizado en JSON.
 *
 * licencia_id es nullable (ON DELETE SET NULL en la FK real) porque pueden
 * existir eventos de instalacion previos a la emision de una licencia.
 */
@Entity
@Table(name = "evento_licencia_local")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoLicenciaLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instalacion_id", nullable = false)
    private UUID instalacionId;

    @Column(name = "licencia_id")
    private UUID licenciaId;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String tipoEvento;

    @Column(nullable = false, length = 30)
    private String resultado;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle_sanitizado_json", columnDefinition = "jsonb")
    @Builder.Default
    private java.util.Map<String, Object> detalleSanitizadoJson = new java.util.HashMap<>();

    @Column(name = "fecha_evento", nullable = false)
    @Builder.Default
    private OffsetDateTime fechaEvento = OffsetDateTime.now();
}