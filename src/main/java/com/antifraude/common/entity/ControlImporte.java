package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "control_importe", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"producto_id", "moneda_id", "monto_minimo"})
})
public class ControlImporte extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;

    @Column(name = "monto_minimo", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoMinimo = BigDecimal.ZERO;

    @Column(name = "monto_maximo", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoMaximo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_riesgo_id", nullable = false)
    private NivelRiesgo nivelRiesgo;

    @Column(nullable = false)
    private Short prioridad;
}
