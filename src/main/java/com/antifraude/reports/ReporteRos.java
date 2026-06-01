package com.antifraude.reports;

import com.antifraude.alerts.Alerta;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reportes_ros")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteRos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id")
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generado_por")
    private Usuario generadoPor;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "fecha_generacion", updatable = false)
    @Builder.Default
    private LocalDateTime fechaGeneracion = LocalDateTime.now();
}
