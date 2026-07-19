package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedBy
    @Column(name = "usuario_creacion", updatable = false, length = 100)
    private String usuarioCreacion;

    @CreatedDate
    @Column(name = "fecha_hora_creacion", updatable = false)
    private LocalDateTime fechaHoraCreacion;

    @LastModifiedBy
    @Column(name = "usuario_modificacion", length = 100)
    private String usuarioModificacion;

    @LastModifiedDate
    @Column(name = "fecha_hora_modificacion")
    private LocalDateTime fechaHoraModificacion;
}
