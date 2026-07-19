package com.antifraude.licensing;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol_permiso", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rol_permiso", columnNames = {"rol_id", "permiso_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolSistema rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permiso_id", nullable = false)
    private PermisoSistema permiso;
}
