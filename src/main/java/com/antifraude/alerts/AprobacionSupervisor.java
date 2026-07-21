package com.antifraude.alerts;

import com.antifraude.common.entity.Caso;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "aprobacion_supervisor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AprobacionSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id")
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id")
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolucion_alerta_id")
    private ResolucionAlerta resolucionAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Usuario supervisor;

    @Column(nullable = false, length = 40)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(columnDefinition = "TEXT")
    private String faltantes;

    @Column(name = "fecha_solicitud", nullable = false)
    @Builder.Default
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;
}
