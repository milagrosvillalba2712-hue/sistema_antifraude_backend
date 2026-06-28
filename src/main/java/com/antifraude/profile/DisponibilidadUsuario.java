package com.antifraude.profile;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "disponibilidad_usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo_estado", nullable = false, length = 50)
    private String tipoEstado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "es_programado")
    @Builder.Default
    private Boolean esProgramado = false;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
