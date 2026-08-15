package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "perfil_cliente")
public class PerfilCliente extends TenantAwareEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;

    @Column(name = "promedio_mensual", precision = 18, scale = 2)
    private BigDecimal promedioMensual;

    @Column(name = "cantidad_operaciones_mensual")
    private Integer cantidadOperacionesMensual;

    @Column(name = "horario_habitual_desde")
    private LocalTime horarioHabitualDesde;

    @Column(name = "horario_habitual_hasta")
    private LocalTime horarioHabitualHasta;

    @Column(name = "ultima_operacion_fecha")
    private OffsetDateTime ultimaOperacionFecha;

    @Column(name = "fecha_calculo", nullable = false)
    private OffsetDateTime fechaCalculo;
}
