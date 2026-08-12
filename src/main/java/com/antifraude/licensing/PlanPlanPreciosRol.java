package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "plan_plan_precios_rol", uniqueConstraints = {
        @UniqueConstraint(name = "uq_plan_rol", columnNames = {"plan_licencia_id", "rol_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanPlanPreciosRol extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_licencia_id", nullable = false)
    private PlanLicencia planLicencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolSistema rol;

    @Column(name = "precio_anual", precision = 18, scale = 2)
    private BigDecimal precioAnual;

    @Builder.Default
    private Boolean activo = true;
}