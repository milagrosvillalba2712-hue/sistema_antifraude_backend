package com.antifraude.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "configuracion_drools")
public class ConfiguracionDrools {

    @Id
    @Column(name = "clave", length = 80, nullable = false)
    private String clave;

    @Column(name = "valor", nullable = false, length = 255)
    private String valor;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private Boolean editable = true;
}
