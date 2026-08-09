package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "cliente_pep")
public class ClientePEP extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(length = 150)
    private String cargo;

    @Column(length = 150)
    private String institucion;

    @Column(name = "tipo_pep", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private TipoPEP tipoPEP;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_riesgo_id", nullable = false)
    private NivelRiesgo nivelRiesgo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private FuentePEP fuente;

    @Column(length = 500)
    private String observacion;

    public enum TipoPEP {
        NACIONAL, EXTRANJERO, FAMILIAR, ASOCIADO
    }

    public enum FuentePEP {
        INTERNA, CARGA_MANUAL, PROVEEDOR_EXTERNO, IMPORTACION_CSV, CONSULTA_SIMULADA
    }
}
