package com.antifraude.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Token de un solo uso para el ciclo de vida del usuario (INVITACION, VERIFICACION,
 * RESET). Solo se persiste el hash SHA-256 del token real (ADR-001 / OWASP).
 */
@Entity
@Table(name = "auth_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {

    public static final String TIPO_INVITACION = "INVITACION";
    public static final String TIPO_VERIFICACION = "VERIFICACION";
    public static final String TIPO_RESET = "RESET";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "rol_id")
    private Long rolId;

    @Column(length = 150)
    private String email;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "usado_en")
    private OffsetDateTime usadoEn;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revocado = false;

    public boolean esValido() {
        return Boolean.FALSE.equals(revocado)
                && usadoEn == null
                && OffsetDateTime.now().isBefore(expiraEn);
    }
}