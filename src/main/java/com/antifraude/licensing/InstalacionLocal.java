package com.antifraude.licensing;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "instalacion_local")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstalacionLocal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "identificador_instalacion", nullable = false, unique = true, length = 120)
    private String identificadorInstalacion;

    @Column(name = "fingerprint_hash", nullable = false, length = 128)
    private String fingerprintHash;

    @Column(name = "clave_publica_pem", nullable = false, columnDefinition = "TEXT")
    private String clavePublicaPem;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(name = "version_producto", length = 40)
    private String versionProducto;

    @Column(name = "activada_en")
    private OffsetDateTime activadaEn;

    @Column(name = "ultimo_heartbeat_en")
    private OffsetDateTime ultimoHeartbeatEn;

    @Column(name = "clon_detectado", nullable = false)
    private boolean clonDetectado;
}
