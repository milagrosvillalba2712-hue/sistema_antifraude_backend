package com.antifraude.common.entity;

import com.antifraude.licensing.Empresa;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cliente_externo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteExterno extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(name = "entidad_financiera_id")
    private UUID entidadFinancieraId;

    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;

    @Column(name = "api_key_prefix", nullable = false, length = 16)
    private String apiKeyPrefix;

    @Column(name = "api_key_last4", length = 4)
    private String apiKeyLast4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, columnDefinition = "text[]")
    @Builder.Default
    private String[] scopes = {};

    @Column(name = "rate_limit_per_minute", nullable = false)
    @Builder.Default
    private Integer rateLimitPerMinute = 600;

    @Column(name = "ip_whitelist", columnDefinition = "cidr[]")
    private String[] ipWhitelist;

    @Column(name = "fecha_expiracion")
    private OffsetDateTime fechaExpiracion;

    @Column(name = "fecha_ultimo_uso")
    private OffsetDateTime fechaUltimoUso;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

}
