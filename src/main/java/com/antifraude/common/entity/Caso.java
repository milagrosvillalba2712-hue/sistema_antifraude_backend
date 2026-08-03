package com.antifraude.common.entity;

import com.antifraude.licensing.Empresa;
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
@Table(name = "caso")
public class Caso extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoCaso estado;

    @Column(name = "severidad", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PrioridadCaso prioridad;

    @Column
    private Integer score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario usuarioAnalista;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(length = 30)
    @Enumerated(EnumType.STRING)
    private ResultadoInvestigacion resultado;

    @Column(length = 1000)
    private String observaciones;

    public enum EstadoCaso {
        NUEVO, ASIGNADO, EN_INVESTIGACION, EN_REVISION, RESUELTO, ROS_GENERADO, CERRADO
    }

    public enum PrioridadCaso {
        CRITICA, ALTA, MEDIA, BAJA
    }

    public enum ResultadoInvestigacion {
        FALSO_POSITIVO, OPERACION_JUSTIFICADA, RIESGO_CONFIRMADO, ROS_GENERADO, ESCALADO
    }

    @Transient
    public UUID getEmpresaId() {
        return empresa != null ? empresa.getId() : null;
    }
}
