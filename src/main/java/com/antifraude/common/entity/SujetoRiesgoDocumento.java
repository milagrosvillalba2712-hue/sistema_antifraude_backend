package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sujeto_riesgo_documento", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sujeto_riesgo_id", "tipo_documento", "numero_documento"})
}, indexes = {
        @Index(name = "idx_sujeto_riesgo_documento_numero", columnList = "numero_documento")
})
public class SujetoRiesgoDocumento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sujeto_riesgo_id", nullable = false)
    private SujetoRiesgo sujetoRiesgo;

    @Column(name = "tipo_documento", length = 60)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 120)
    private String numeroDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_emision_id")
    private Pais paisEmision;
}
