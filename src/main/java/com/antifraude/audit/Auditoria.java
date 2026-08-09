package com.antifraude.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria_sistema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "direccion_ip", length = 100)
    private String direccionIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "entidad_afectada", length = 100)
    private String entidadAfectada;

    @Column(name = "entidad_id")
    private String entidadId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_anterior_json", columnDefinition = "jsonb")
    private String valorAnteriorJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_nuevo_json", columnDefinition = "jsonb")
    private String valorNuevoJson;

    @Column(name = "fecha_evento", updatable = false)
    @Builder.Default
    private OffsetDateTime fechaEvento = OffsetDateTime.now();
}
