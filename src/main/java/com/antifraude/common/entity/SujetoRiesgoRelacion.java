package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sujeto_riesgo_relacion")
public class SujetoRiesgoRelacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sujeto_riesgo_id", nullable = false)
    private SujetoRiesgo sujetoRiesgo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relacionado_sujeto_riesgo_id")
    private SujetoRiesgo relacionadoSujetoRiesgo;

    @Column(name = "nombre_relacionado", columnDefinition = "TEXT")
    private String nombreRelacionado;

    @Column(name = "tipo_relacion", nullable = false, length = 60)
    private String tipoRelacion;

    @Column(columnDefinition = "TEXT")
    private String detalle;
}
