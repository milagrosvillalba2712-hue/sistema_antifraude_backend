package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "control_frecuencia")
public class ControlFrecuencia extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "cantidad_maxima", nullable = false)
    private Integer cantidadOperaciones;

    @Column(name = "ventana_minutos", nullable = false)
    private Integer ventanaTiempo;

    @Column(name = "unidad_tiempo", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private UnidadTiempo unidadTiempo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_riesgo_id", nullable = false)
    private NivelRiesgo nivelRiesgo;

    public enum UnidadTiempo {
        MINUTOS, HORAS, DIAS
    }
}
