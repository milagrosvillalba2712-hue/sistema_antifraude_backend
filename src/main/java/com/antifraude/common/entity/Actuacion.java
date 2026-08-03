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
@Table(name = "actuacion")
public class Actuacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id", nullable = false)
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo_actuacion", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private TipoActuacion tipo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(length = 500)
    private String resultado;

    @Column(name = "fecha_actuacion", nullable = false)
    private LocalDateTime fechaHora;

    public enum TipoActuacion {
        CONSULTA_KYC, SOLICITUD_DOCUMENTOS, ENTREVISTA, VALIDACION, OBSERVACION, ESCALAMIENTO
    }
}
