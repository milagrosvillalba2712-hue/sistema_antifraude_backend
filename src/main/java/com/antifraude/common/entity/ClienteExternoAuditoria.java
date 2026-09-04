package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cliente_externo_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteExternoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_externo_id")
    private UUID clienteExternoId;

    @Column(nullable = false, length = 200)
    private String endpoint;

    @Column(name = "metodo_http", nullable = false, length = 10)
    private String metodoHttp;

    @Column(name = "ip_origen", columnDefinition = "inet")
    private String ipOrigen;

    private Integer status;

    @Column(name = "duracion_ms")
    private Integer duracionMs;

    @Column(name = "error_code", length = 60)
    private String errorCode;

    @Column(name = "request_id", length = 60)
    private String requestId;

    @Column(name = "fecha_hora_creacion", updatable = false)
    private OffsetDateTime fechaHoraCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaHoraCreacion == null) fechaHoraCreacion = OffsetDateTime.now();
    }
}
