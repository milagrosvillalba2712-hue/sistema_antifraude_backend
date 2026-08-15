package com.antifraude.profile;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "disponibilidad_usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String estado = "DISPONIBLE";

    @Column(name = "carga_actual", nullable = false)
    @Builder.Default
    private Integer cargaActual = 0;

    @Column(name = "capacidad_maxima", nullable = false)
    @Builder.Default
    private Integer capacidadMaxima = 12;

    @Column(name = "ultima_actualizacion", nullable = false)
    @Builder.Default
    private OffsetDateTime ultimaActualizacion = OffsetDateTime.now();

    @Column(name = "motivo_no_disponible", columnDefinition = "TEXT")
    private String motivoNoDisponible;

    @Transient
    private String tipoEstado;

    @Column(name = "fecha_inicio")
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    @Column(name = "es_programado", nullable = false)
    @Builder.Default
    private Boolean esProgramado = false;

    @Transient
    private String motivo;

    @PrePersist
    @PreUpdate
    void syncLegacyAliases() {
        if (tipoEstado != null) {
            estado = tipoEstado;
        }
        if (fechaInicio != null) {
            ultimaActualizacion = fechaInicio;
        }
        if (motivo != null) {
            motivoNoDisponible = motivo;
        }
        if (empresaId == null && usuario != null && usuario.getEmpresaId() != null) {
            empresaId = usuario.getEmpresaId();
        }
        if (ultimaActualizacion == null) {
            ultimaActualizacion = OffsetDateTime.now();
        }
    }

    public String getTipoEstado() {
        return tipoEstado != null ? tipoEstado : estado;
    }

    public void setTipoEstado(String tipoEstado) {
        this.tipoEstado = tipoEstado;
        this.estado = tipoEstado;
    }

    public OffsetDateTime getFechaInicio() {
        return fechaInicio != null ? fechaInicio : ultimaActualizacion;
    }

    public void setFechaInicio(OffsetDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
        this.ultimaActualizacion = fechaInicio != null ? fechaInicio : OffsetDateTime.now();
    }

    public OffsetDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(OffsetDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Boolean getEsProgramado() {
        return esProgramado != null && esProgramado;
    }

    public void setEsProgramado(Boolean esProgramado) {
        this.esProgramado = esProgramado != null ? esProgramado : false;
    }

    public String getMotivo() {
        return motivo != null ? motivo : motivoNoDisponible;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
        this.motivoNoDisponible = motivo;
    }

    public Boolean getActivo() {
        return !"CANCELADA".equalsIgnoreCase(estado);
    }

    public void setActivo(Boolean activo) {
        if (Boolean.FALSE.equals(activo)) {
            this.estado = "CANCELADA";
        }
    }
}
