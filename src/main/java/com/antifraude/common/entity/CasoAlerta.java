package com.antifraude.common.entity;

import com.antifraude.alerts.Alerta;
import com.antifraude.security.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "caso_alerta", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"caso_id", "alerta_id"})
})
public class CasoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id", nullable = false)
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

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
}
