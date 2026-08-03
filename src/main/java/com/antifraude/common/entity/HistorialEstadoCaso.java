package com.antifraude.common.entity;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "historial_estado_caso")
public class HistorialEstadoCaso extends BaseEntity {

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id", nullable = false)
    private Caso caso;

    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    void prePersist() {
        if (empresaId == null && caso != null) {
            empresaId = caso.getEmpresaId();
        }
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }
}
