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
@Table(name = "elemento_lista", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"lista_regulatoria_id", "valor_identificador"})
})
public class ElementoLista extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_regulatoria_id", nullable = false)
    private ListaRegulatoria listaRegulatoria;

    @Column(name = "tipo_elemento", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoElemento tipoElemento;

    @Column(name = "valor_identificador", nullable = false, length = 150)
    private String valorIdentificador;

    @Column(name = "fecha_incorporacion", nullable = false)
    private LocalDate fechaIncorporacion;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    public enum TipoElemento {
        PERSONA, EMPRESA, PAIS, ENTIDAD, CUENTA, DOCUMENTO
    }
}
