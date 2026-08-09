package com.antifraude.alerts;

import com.antifraude.security.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaccion_detalle_snapshot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionDetalleSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false, unique = true)
    private Alerta alerta;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle_json", columnDefinition = "jsonb")
    private String detalleJson;

    @Column(name = "fecha_registro")
    @Builder.Default
    private OffsetDateTime fechaRegistro = OffsetDateTime.now();

    @PrePersist
    protected void rellenarTenant() {
        if (empresaId == null && alerta != null && alerta.getEmpresaId() != null) {
            empresaId = alerta.getEmpresaId();
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
