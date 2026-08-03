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
@Table(name = "fuente_datos_riesgo")
public class FuenteDatosRiesgo extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(length = 40)
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String cobertura;

    @Column(name = "url_oficial", columnDefinition = "TEXT")
    private String url;

    @Column(name = "licencia_uso", columnDefinition = "TEXT")
    private String licenciaUso;

    @Column(name = "permite_consumo", columnDefinition = "TEXT")
    private String permiteConsumo;

    @Column(name = "permite_edicion", columnDefinition = "TEXT")
    private String permiteEdicion;

    @Column(length = 120)
    private String formatos;

    @Column(name = "frecuencia_actualizacion", length = 120)
    private String frecuenciaActualizacion;

    @Column(name = "recomendacion_bd", columnDefinition = "TEXT")
    private String recomendacionBd;

    @Column(name = "fecha_revision")
    private LocalDate fechaRevision = LocalDate.now();
}
