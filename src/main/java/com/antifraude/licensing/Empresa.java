package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(name = "ruc", length = 40)
    private String ruc;

    @Column(name = "email_contacto", length = 180)
    private String emailContacto;

    @Column(name = "telefono_contacto", length = 60)
    private String telefonoContacto;

    @Column(length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoEmpresa estado = EstadoEmpresa.ACTIVA;

    public enum EstadoEmpresa {
        ACTIVA, SUSPENDIDA, CANCELADA
    }
}
