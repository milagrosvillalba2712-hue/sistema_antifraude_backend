package com.antifraude.common.entity;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario_documento_legal", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_documento_legal", columnNames = {"usuario_id", "documento_legal_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDocumentoLegal extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_legal_id", nullable = false, updatable = false)
    private DocumentoLegal documentoLegal;

    @Column(nullable = false)
    @Builder.Default
    private Boolean acepto = false;

    @Column(name = "fecha_aceptacion")
    private OffsetDateTime fechaAceptacion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
}
