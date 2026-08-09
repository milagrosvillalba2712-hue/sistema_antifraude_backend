package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import com.antifraude.common.entity.Moneda;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id")
    private Suscripcion suscripcion;

    @Column(name = "codigo", nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moneda_id")
    private Moneda monedaRef;

    @Column(name = "fecha_pago")
    private OffsetDateTime fechaPago;

    @Column(name = "metodo_pago", length = 60)
    private String metodoPago;

    @Column(name = "comprobante_referencia", length = 120)
    private String comprobanteReferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoPago estado = EstadoPago.PENDIENTE;

    public enum EstadoPago {
        PENDIENTE, PAGADO, CONFIRMADO, VENCIDO, ANULADO
    }

    public String getReferencia() {
        return codigo;
    }

    public void setReferencia(String referencia) {
        this.codigo = referencia;
    }

    public String getMoneda() {
        return monedaRef != null ? monedaRef.getCodigoIso() : null;
    }
}
