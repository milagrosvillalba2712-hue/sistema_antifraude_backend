package com.antifraude.licensing;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "roles_adquiridos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolesAdquiridos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_roles_id", nullable = false)
    private SolicitudRoles solicitudRoles;

    @Column(name = "rol_codigo", nullable = false, length = 60)
    private String rolCodigo;

    @Column(name = "cantidad", nullable = false)
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "fecha_adquisicion", nullable = false)
    @Builder.Default
    private OffsetDateTime fechaAdquisicion = OffsetDateTime.now();

    @Builder.Default
    private Boolean activo = true;
}
