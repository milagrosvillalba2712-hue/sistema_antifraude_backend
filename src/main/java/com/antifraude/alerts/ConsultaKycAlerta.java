package com.antifraude.alerts;

import com.antifraude.security.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consulta_kyc_alerta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaKycAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @Column(name = "proveedor", length = 120)
    private String servicio;

    @Column(name = "estado", length = 40)
    private String estado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "respuesta_json", columnDefinition = "jsonb")
    private String respuesta;

    @Column(name = "fecha_consulta")
    @Builder.Default
    private LocalDateTime fechaConsulta = LocalDateTime.now();

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
