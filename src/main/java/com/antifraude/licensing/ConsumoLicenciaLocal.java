package com.antifraude.licensing;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mapea consumo_licencia_local. Registra el consumo mensual (usuarios
 * activos, transacciones procesadas, consultas KYC, alertas, reportes) de
 * una instalacion on-premise.
 *
 * No extiende BaseEntity: esta tabla no tiene columna `activo`, asi que
 * heredar BaseEntity romperia ddl-auto=validate.
 *
 * uq_consumo_licencia_local_periodo Garantiza (instalacion_id, anio, mes)
 * unico — ya resuelto en Flyway, no se repite como constraint JPA.
 */
@Entity
@Table(name = "consumo_licencia_local")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsumoLicenciaLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instalacion_id", nullable = false)
    private UUID instalacionId;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Column(name = "usuarios_activos", nullable = false)
    @Builder.Default
    private Integer usuariosActivos = 0;

    @Column(name = "transacciones_procesadas", nullable = false)
    @Builder.Default
    private Long transaccionesProcesadas = 0L;

    @Column(name = "consultas_kyc", nullable = false)
    @Builder.Default
    private Long consultasKyc = 0L;

    @Column(name = "alertas_generadas", nullable = false)
    @Builder.Default
    private Long alertasGeneradas = 0L;

    @Column(name = "reportes_generados", nullable = false)
    @Builder.Default
    private Long reportesGenerados = 0L;

    @Column(name = "fecha_hora_modificacion", nullable = false)
    @Builder.Default
    private OffsetDateTime fechaHoraModificacion = OffsetDateTime.now();

    @PreUpdate
    void preUpdate() {
        fechaHoraModificacion = OffsetDateTime.now();
    }
}