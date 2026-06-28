package com.antifraude.alerts;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "estadistica_carga_analista",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "fecha"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadisticaCargaAnalista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "alertas_asignadas")
    @Builder.Default
    private Integer alertasAsignadas = 0;

    @Column(name = "alertas_resueltas")
    @Builder.Default
    private Integer alertasResueltas = 0;

    @Column(name = "alertas_pendientes")
    @Builder.Default
    private Integer alertasPendientes = 0;

    @Column(name = "tiempo_promedio_resolucion")
    private Long tiempoPromedioResolucion;

    @Column(name = "ultima_actualizacion")
    @Builder.Default
    private LocalDateTime ultimaActualizacion = LocalDateTime.now();
}
