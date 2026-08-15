package com.antifraude.common.entity;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "horario_laboral_usuario", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "dia_semana"})
})
public class HorarioLaboralUsuario extends BaseEntity {

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaDesde;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaHasta;

    @PrePersist
    void prePersist() {
        if (empresaId == null && usuario != null && usuario.getEmpresaId() != null) {
            empresaId = usuario.getEmpresaId();
        }
    }

    public enum DiaSemana {
        LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO
    }
}
