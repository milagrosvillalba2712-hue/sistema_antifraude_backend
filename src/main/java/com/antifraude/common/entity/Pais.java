package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "pais")
public class Pais extends BaseEntity {

    @Column(name = "codigo_iso", nullable = false, unique = true, length = 2)
    private String codigoIso;

    @Column(name = "codigo_iso3", length = 3)
    private String codigoIso3;

    @Column(name = "codigo_numerico", length = 3)
    private String codigoNumerico;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "nombre_oficial", length = 220)
    private String nombreOficial;

    @Column(length = 40)
    private String continente;

    @Column(length = 120)
    private String region;

    @Column(length = 120)
    private String subregion;

    @Column(name = "miembro_onu")
    private Boolean miembroOnu;

    @Column(name = "independiente")
    private Boolean independiente;

    @Column(length = 120)
    private String fuente;
}
