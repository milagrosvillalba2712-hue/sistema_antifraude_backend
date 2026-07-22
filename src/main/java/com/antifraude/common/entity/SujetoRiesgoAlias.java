package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sujeto_riesgo_alias", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sujeto_riesgo_id", "alias_normalizado"})
}, indexes = {
        @Index(name = "idx_sujeto_riesgo_alias_normalizado", columnList = "alias_normalizado")
})
public class SujetoRiesgoAlias extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sujeto_riesgo_id", nullable = false)
    private SujetoRiesgo sujetoRiesgo;

    @Column(name = "alias_original", nullable = false, columnDefinition = "TEXT")
    private String aliasOriginal;

    @Column(name = "alias_normalizado", nullable = false, columnDefinition = "TEXT")
    private String aliasNormalizado;

    @Column(name = "tipo_alias", length = 40)
    private String tipoAlias = "ALIAS";
}
