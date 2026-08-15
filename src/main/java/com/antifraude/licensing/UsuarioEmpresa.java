package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario_empresa", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_empresa_rol", columnNames = {"usuario_id", "empresa_id", "rol_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEmpresa extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolSistema rol;

    @Builder.Default
    private Boolean activo = true;
}
