package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tipo_documento")
public class TipoDocumento extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "codigo_tecnico", length = 40)
    private String codigoTecnico;

    @Column(length = 20)
    private String sigla;

    @Column(nullable = false, length = 140)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_id")
    private Pais paisRelacion;

    @Column(name = "tipo_persona", nullable = false, length = 30)
    private String tipoPersona = "FISICA";

    @Column(name = "fuente_oficial", length = 220)
    private String fuenteOficial;

    @Column(name = "fuente_oficial_cita", columnDefinition = "text")
    private String fuenteOficialCita;

    @Column(name = "formato_regex", length = 220)
    private String formatoRegex;

    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo = true;
}
