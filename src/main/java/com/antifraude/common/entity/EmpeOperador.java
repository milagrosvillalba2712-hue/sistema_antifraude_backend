package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "empe_operador")
public class EmpeOperador extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "entidad_patrocinadora_id")
    private Long entidadPatrocinadoraId;
}
