package com.antifraude.alerts;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_origen")
    private Usuario usuarioOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_destino", nullable = false)
    private Usuario usuarioDestino;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(nullable = false, length = 30)
    private String tipo;
}
