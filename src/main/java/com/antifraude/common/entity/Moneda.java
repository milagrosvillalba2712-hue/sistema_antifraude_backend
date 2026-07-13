package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "moneda")
public class Moneda extends BaseEntity {

    @Column(name = "codigo_iso", nullable = false, unique = true, length = 3)
    private String codigoIso;

    @Column(nullable = false, length = 40)
    private String nombre;
}
