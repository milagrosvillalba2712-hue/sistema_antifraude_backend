package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "plan_licencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanLicencia extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "limite_usuarios")
    private Integer limiteUsuarios;

    @Column(name = "limite_transacciones_mensuales")
    private Integer limiteTransaccionesMensuales;

    @Column(name = "limite_consultas_kyc_mensuales")
    private Integer limiteConsultasKycMensuales;

    @Column(name = "limite_reportes_mensuales")
    private Integer limiteReportesMensuales;

    @Column(name = "limite_reglas")
    private Integer limiteReglas;

    @Column(name = "limite_historial_transaccional")
    private Integer limiteHistorialTransaccional;

    @Column(name = "limite_escenarios")
    private Integer limiteEscenarios;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modulos_incluidos_json", columnDefinition = "jsonb")
    private String modulosIncluidosJson;

    @Column(name = "precio_anual", precision = 18, scale = 2)
    private BigDecimal precioAnual;

    @Builder.Default
    private Boolean activo = true;
}
