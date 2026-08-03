package com.antifraude.users;

import com.antifraude.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "intentos_fallidos")
    @Builder.Default
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    @Transient
    private UUID empresaId;

    public boolean isBlocked() {
        return bloqueadoHasta != null && OffsetDateTime.now().isBefore(bloqueadoHasta);
    }

    public void incrementFailedAttempts() {
        this.intentosFallidos++;
        if (this.intentosFallidos >= 5) {
            this.bloqueadoHasta = OffsetDateTime.now().plusMinutes(15);
        }
    }

    public void resetFailedAttempts() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }
}
