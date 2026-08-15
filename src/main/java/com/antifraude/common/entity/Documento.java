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
@Table(name = "documento", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "numero_documento_hash"})
})
public class Documento extends BaseEntity {

    @Column(name = "empresa_id", nullable = false)
    private java.util.UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @Transient
    private String numeroDocumento;

    @Column(name = "numero_documento_enc")
    private byte[] numeroDocumentoEnc;

    @Column(name = "numero_documento_hash", nullable = false)
    private byte[] numeroDocumentoHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_emisor_id")
    private Pais paisEmisor;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "fecha_expiracion")
    private LocalDate fechaVencimiento;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = true;

    @Column(nullable = false, length = 30)
    private String estado = "VIGENTE";
}
