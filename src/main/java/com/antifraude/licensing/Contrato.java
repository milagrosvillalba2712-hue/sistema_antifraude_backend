package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "contrato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id")
    private Suscripcion suscripcion;

    @Column(name = "numero_contrato", nullable = false, unique = true, length = 50)
    private String numero;

    @Column(name = "fecha_firma")
    private LocalDate fechaFirma;

    @Column(name = "documento_referencia", length = 500)
    private String urlDocumento;

    @Transient
    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoContrato estado = EstadoContrato.VIGENTE;

    public enum EstadoContrato {
        BORRADOR, VIGENTE, VENCIDO, RESCINDIDO
    }
}
