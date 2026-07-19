package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "suscripcion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suscripcion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_licencia_id", nullable = false)
    private PlanLicencia planLicencia;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoSuscripcion estado = EstadoSuscripcion.ACTIVA;

    @Column(name = "renovacion_automatica")
    @Builder.Default
    private Boolean renovacionAutomatica = true;

    public enum EstadoSuscripcion {
        ACTIVA, VENCIDA, SUSPENDIDA, CANCELADA
    }
}
