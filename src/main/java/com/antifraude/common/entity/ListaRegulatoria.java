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
@Table(name = "lista_regulatoria")
public class ListaRegulatoria extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuente_datos_riesgo_id")
    private FuenteDatosRiesgo fuenteDatosRiesgo;

    @Column(name = "tipo_lista", nullable = false, length = 40)
    private String tipoLista = "INTERNA";

    @Column(length = 80)
    private String alcance;

    @Column(name = "url_descarga", columnDefinition = "TEXT")
    private String urlDescarga;

    @Column(name = "licencia_uso", columnDefinition = "TEXT")
    private String licenciaUso;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @Column(name = "fecha_ultima_revision")
    private LocalDate fechaUltimaRevision;

    @Transient
    private Fuente fuente;

    public enum Fuente {
        OFICIAL, INTERNA, PROVEEDOR
    }
}
