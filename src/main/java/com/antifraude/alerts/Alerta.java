package com.antifraude.alerts;

import com.antifraude.transactions.Transaccion;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id")
    private Transaccion transaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_id")
    private ReglaRiesgo regla;

    @Column(length = 20)
    private String prioridad;

    @Column(length = 30)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_a")
    private Usuario asignadoA;

    @Column(name = "fecha_generacion", updatable = false)
    @Builder.Default
    private LocalDateTime fechaGeneracion = LocalDateTime.now();

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;
}
