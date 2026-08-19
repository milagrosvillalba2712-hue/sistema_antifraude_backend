package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "solicitud_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudRoles extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "rol_solicitado", nullable = false, length = 60)
    private String rolSolicitado;

    @Column(name = "cantidad", nullable = false)
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "precio_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private Usuario aprobadoPor;

    @Column(name = "aprobado_en")
    private OffsetDateTime aprobadoEn;

    public enum Estado {
        PENDIENTE, APROBADA, RECHAZADA, PAGADA, CANCELADA
    }
}
