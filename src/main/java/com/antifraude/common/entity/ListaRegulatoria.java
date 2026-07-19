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

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Fuente fuente;

    public enum Fuente {
        OFICIAL, INTERNA, PROVEEDOR
    }
}
