package com.antifraude.alerts;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "estadistica_carga_analista",
       uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "usuario_id", "periodo"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadisticaCargaAnalista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "periodo", nullable = false)
    private LocalDate fecha;

    @Column(name = "alertas_asignadas")
    @Builder.Default
    private Integer alertasAsignadas = 0;

    @Column(name = "alertas_cerradas")
    @Builder.Default
    private Integer alertasResueltas = 0;

    @Column(name = "alertas_pendientes")
    @Builder.Default
    private Integer alertasPendientes = 0;

    @Column(name = "tiempo_promedio_minutos", precision = 10, scale = 2)
    private BigDecimal tiempoPromedioMinutos;

    @Column(name = "fecha_hora_modificacion")
    private LocalDateTime ultimaActualizacion;

    @Transient
    private Long tiempoPromedioResolucion;

    @PrePersist
    void prePersist() {
        if (empresaId == null && usuario != null && usuario.getEmpresaId() != null) {
            empresaId = usuario.getEmpresaId();
        }
    }

    public Long getTiempoPromedioResolucion() {
        if (tiempoPromedioResolucion != null) {
            return tiempoPromedioResolucion;
        }
        return tiempoPromedioMinutos != null ? tiempoPromedioMinutos.longValue() : null;
    }

    public void setTiempoPromedioResolucion(Long tiempoPromedioResolucion) {
        this.tiempoPromedioResolucion = tiempoPromedioResolucion;
        this.tiempoPromedioMinutos = tiempoPromedioResolucion != null ? BigDecimal.valueOf(tiempoPromedioResolucion) : null;
    }
}
