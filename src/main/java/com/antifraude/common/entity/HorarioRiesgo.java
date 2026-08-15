package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "horario_riesgo")
public class HorarioRiesgo extends BaseEntity {

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaDesde;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaHasta;

    @Column(nullable = false, length = 30)
    private String severidad = "MEDIA";

    @Transient
    private NivelRiesgo nivelRiesgo;
}
