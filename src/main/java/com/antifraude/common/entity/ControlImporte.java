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
    @UniqueConstraint(columnNames = {"empresa_id", "codigo"})
})
public class ControlImporte extends BaseEntity {

    @Column(name = "empresa_id", nullable = false)
    private java.util.UUID empresaId;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(name = "tipo_transaccion_id")
    private Long tipoTransaccionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moneda_id")
    private Moneda moneda;

    @Column(name = "monto_minimo", precision = 18, scale = 2)
    private BigDecimal montoMinimo = BigDecimal.ZERO;

    @Column(name = "monto_maximo", precision = 18, scale = 2)
    private BigDecimal montoMaximo;

    @Column(nullable = false, length = 30)
    private String severidad = "MEDIA";

    @Transient
    public Producto getProducto() {
        return null;
    }

    @Transient
    public NivelRiesgo getNivelRiesgo() {
        return null;
    }

    @Transient
    public Short getPrioridad() {
        return null;
    }
}
