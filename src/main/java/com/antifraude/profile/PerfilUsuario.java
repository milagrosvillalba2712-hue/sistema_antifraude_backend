package com.antifraude.profile;

import com.antifraude.security.tenant.TenantContext;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "perfil_usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "nombre_visible", length = 150)
    private String nombreVisible;

    @Column(name = "imagen_perfil", columnDefinition = "TEXT")
    private String imagenPerfil;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String estado = "DISPONIBLE";

    @Column(name = "estado_personalizado", length = 100)
    private String estadoPersonalizado;

    @Column(name = "ultima_actualizacion_estado")
    private LocalDateTime ultimaActualizacionEstado;

    @Column(name = "fecha_creacion", updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @PrePersist
    protected void rellenarTenant() {
        if (empresaId == null && usuario != null) {
            empresaId = usuario.getEmpresaId();
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
