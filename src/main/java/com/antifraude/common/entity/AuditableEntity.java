package com.antifraude.common.entity;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "fecha_hora_creacion", updatable = false)
    private OffsetDateTime fechaHoraCreacion;

    @Column(name = "fecha_hora_modificacion")
    private OffsetDateTime fechaHoraModificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_creacion_id", updatable = false)
    private Usuario usuarioCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_modificacion_id")
    private Usuario usuarioModificacion;

    @PrePersist
    protected void onCreate() {
        if (fechaHoraCreacion == null) fechaHoraCreacion = OffsetDateTime.now();
        fechaHoraModificacion = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaHoraModificacion = OffsetDateTime.now();
    }
}
