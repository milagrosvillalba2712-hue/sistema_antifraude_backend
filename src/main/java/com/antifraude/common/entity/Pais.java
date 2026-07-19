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

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 40)
    private String continente;
}
