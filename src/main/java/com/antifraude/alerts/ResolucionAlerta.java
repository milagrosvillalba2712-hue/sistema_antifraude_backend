package com.antifraude.alerts;

import com.antifraude.security.tenant.TenantContext;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resolucion_alerta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolucionAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Resultado resultado;

    @Column(columnDefinition = "TEXT")
    private String conclusion;

    @Column(columnDefinition = "TEXT")
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String justificacion;

    @Column(name = "evidencia_descripcion", columnDefinition = "TEXT")
    private String evidenciaDescripcion;

    @Column(name = "contacto_cliente", columnDefinition = "TEXT")
    private String contactoCliente;

    @Builder.Default
    private Boolean fondosRetenidos = false;

    @Builder.Default
    private Boolean movimientoLiberable = false;

    @Builder.Default
    private Boolean requiereRos = false;

    @Builder.Default
    private Boolean requiereBloqueo = false;

    @Builder.Default
    private Boolean requiereEscalamientoLegal = false;

    @Column(name = "fecha_resolucion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaResolucion = LocalDateTime.now();

    @PrePersist
    protected void rellenarTenant() {
        if (empresaId == null) {
            empresaId = TenantContext.getEmpresaId();
        }
        if (empresaId == null) {
            throw new IllegalStateException("No se pudo determinar empresa_id al persistir "
                    + getClass().getSimpleName()
                    + ": falta TenantContext en el hilo actual.");
        }
    }

    public enum Resultado {
        FRAUDE_CONFIRMADO, FALSO_POSITIVO, OPERACION_JUSTIFICADA, ESCALAR, ROS_REQUERIDO
    }
}
