package com.antifraude.common.entity;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "historial_estado_caso")
public class HistorialEstadoCaso extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id", nullable = false)
    private Caso caso;

    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}
