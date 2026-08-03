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
@Table(name = "calendario_riesgo")
public class CalendarioRiesgo extends BaseEntity {

    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Column(name = "tipo_evento", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoDia tipoDia;

    @Column(name = "nombre", length = 150)
    private String descripcion;

    public enum TipoDia {
        FERIADO, NO_HABIL, EVENTO_ESPECIAL
    }
}
