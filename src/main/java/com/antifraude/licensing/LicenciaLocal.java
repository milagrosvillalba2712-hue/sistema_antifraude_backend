package com.antifraude.licensing;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "licencia_local")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenciaLocal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instalacion_id", nullable = false)
    private InstalacionLocal instalacion;

    @Column(name = "suscripcion_referencia", nullable = false, length = 120)
    private String suscripcionReferencia;

    @Column(name = "plan_codigo", nullable = false, length = 40)
    private String planCodigo;

    @Column(name = "plan_version", nullable = false)
    private Integer planVersion;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(name = "emitida_en", nullable = false)
    private OffsetDateTime emitidaEn;

    @Column(name = "vence_en", nullable = false)
    private OffsetDateTime venceEn;

    @Column(name = "dias_gracia", nullable = false)
    private Integer diasGracia;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modulos_json", nullable = false, columnDefinition = "jsonb")
    private String modulosJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "limites_json", nullable = false, columnDefinition = "jsonb")
    private String limitesJson;

    @Column(name = "lease_payload", nullable = false, columnDefinition = "TEXT")
    private String leasePayload;

    @Column(name = "lease_firma", nullable = false, columnDefinition = "TEXT")
    private String leaseFirma;

    @Column(name = "kid_firma", nullable = false, length = 120)
    private String kidFirma;

    @Column(name = "ultima_validacion_en")
    private OffsetDateTime ultimaValidacionEn;
}
