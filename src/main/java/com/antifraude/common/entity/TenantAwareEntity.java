package com.antifraude.common.entity;

import com.antifraude.security.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Superclase que materializa el tenant (empresa) para todas las entidades cuya tabla
 * declara {@code empresa_id uuid NOT NULL} con RLS {@code FORCE} y que además disponen
 * de la columna {@code activo} propia de {@link BaseEntity}.
 *
 * El valor se rellena de forma automatica desde {@link TenantContext} (poblado por
 * JwtAuthenticationFilter/AuthService) en {@code @PrePersist}. A diferencia de una
 * asignacion silenciosa, si al persistir no hay tenant disponible se lanza una
 * {@link IllegalStateException} (fallo temprano) para no escribir filas que el RLS
 * o el {@code NOT NULL} de la base acabarian rechazando.
 */
@Data
@NoArgsConstructor
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @PrePersist
    protected void rellenarTenant() {
        if (empresaId == null) {
            empresaId = TenantContext.getEmpresaId();
        }
        if (empresaId == null) {
            throw new IllegalStateException("No se pudo determinar empresa_id al persistir "
                    + getClass().getSimpleName()
                    + ": falta TenantContext en el hilo actual (solicitud no autenticada o flujo batch/scheduler sin tenant explicito).");
        }
    }
}