package com.antifraude.alerts;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_asignacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_anterior_id")
    private Usuario usuarioOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_nuevo_id", nullable = false)
    private Usuario usuarioDestino;

    @Column(name = "fecha_asignacion", nullable = false)
    @Builder.Default
    private OffsetDateTime fecha = OffsetDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(nullable = false, length = 30)
    private String tipo;

    @PrePersist
    void prePersist() {
        if (empresaId == null && alerta != null) {
            empresaId = alerta.getEmpresaId();
        }
        if (fecha == null) {
            fecha = OffsetDateTime.now();
        }
    }
}
