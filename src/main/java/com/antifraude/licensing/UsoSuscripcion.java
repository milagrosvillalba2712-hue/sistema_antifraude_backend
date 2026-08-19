package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "uso_suscripcion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_uso_empresa_suscripcion_periodo", columnNames = {"empresa_id", "suscripcion_id", "periodo"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsoSuscripcion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @Column(name = "periodo", nullable = false)
    private java.time.LocalDate periodo;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    @Column(name = "usuarios_activos", nullable = false)
    @Builder.Default
    private Integer usuariosActivos = 0;

    @Column(name = "transacciones_procesadas", nullable = false)
    @Builder.Default
    private Integer transaccionesProcesadas = 0;

    @Column(name = "consultas_kyc", nullable = false)
    @Builder.Default
    private Integer consultasKyc = 0;

    @Column(name = "alertas_generadas", nullable = false)
    @Builder.Default
    private Integer alertasGeneradas = 0;

    @Column(name = "reportes_generados", nullable = false)
    @Builder.Default
    private Integer reportesGenerados = 0;

    @Column(name = "consumo_json", columnDefinition = "jsonb")
    @Builder.Default
    private String consumoJson = "{}";
}
