package com.antifraude.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "direccion_ip", length = 100)
    private String direccionIp;

    @Column(name = "entidad_afectada", length = 100)
    private String entidadAfectada;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "fecha_evento", updatable = false)
    @Builder.Default
    private LocalDateTime fechaEvento = LocalDateTime.now();
}
