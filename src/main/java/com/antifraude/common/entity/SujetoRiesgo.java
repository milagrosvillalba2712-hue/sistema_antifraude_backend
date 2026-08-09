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
@Table(name = "sujeto_riesgo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"fuente_datos_riesgo_id", "external_id"})
}, indexes = {
        @Index(name = "idx_sujeto_riesgo_nombre_normalizado", columnList = "nombre_normalizado"),
        @Index(name = "idx_sujeto_riesgo_categoria", columnList = "categoria")
})
public class SujetoRiesgo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuente_datos_riesgo_id")
    private FuenteDatosRiesgo fuenteDatosRiesgo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_regulatoria_id")
    private ListaRegulatoria listaRegulatoria;

    @Column(name = "external_id", length = 160)
    private String externalId;

    @Column(name = "tipo_sujeto", nullable = false, length = 40)
    private String tipoSujeto;

    @Column(name = "nombre_normalizado", nullable = false, columnDefinition = "TEXT")
    private String nombreNormalizado;

    @Column(name = "nombre_original", nullable = false, columnDefinition = "TEXT")
    private String nombreOriginal;

    @Column(nullable = false, length = 40)
    private String categoria;

    @Column(nullable = false, length = 20)
    private String severidad = "Alta";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_id")
    private Pais pais;

    @Column(length = 160)
    private String programa;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "licencia_uso", columnDefinition = "TEXT")
    private String licenciaUso;

    @Column(name = "fecha_listado")
    private LocalDate fechaListado;

    @Column(name = "fecha_revision")
    private LocalDate fechaRevision = LocalDate.now();
}
