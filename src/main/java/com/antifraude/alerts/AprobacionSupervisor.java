package com.antifraude.alerts;

import com.antifraude.common.entity.Caso;
import com.antifraude.security.tenant.TenantContext;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "aprobacion_supervisor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AprobacionSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id")
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id")
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolucion_alerta_id")
    private ResolucionAlerta resolucionAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Usuario supervisor;

    @Column(name = "decision", nullable = false, length = 40)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(columnDefinition = "TEXT")
    private String faltantes;

    @Column(name = "fecha_hora_creacion", nullable = false, insertable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime fechaSolicitud = OffsetDateTime.now();

    @Column(name = "fecha_decision")
    private OffsetDateTime fechaAprobacion;

    @PrePersist
    protected void rellenarTenant() {
        if (empresaId == null && alerta != null && alerta.getEmpresaId() != null) {
            empresaId = alerta.getEmpresaId();
        } else if (empresaId == null && caso != null && caso.getEmpresaId() != null) {
            empresaId = caso.getEmpresaId();
        } else if (empresaId == null && resolucionAlerta != null && resolucionAlerta.getEmpresaId() != null) {
            empresaId = resolucionAlerta.getEmpresaId();
        }
        if (empresaId == null) {
            empresaId = TenantContext.getEmpresaId();
        }
        if (empresaId == null) {
            throw new IllegalStateException("No se pudo determinar empresa_id al persistir "
                    + getClass().getSimpleName()
                    + ": falta TenantContext en el hilo actual.");
        }
    }
}
