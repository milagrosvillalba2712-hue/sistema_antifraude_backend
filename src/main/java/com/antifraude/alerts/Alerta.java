package com.antifraude.alerts;

import com.antifraude.transactions.Transaccion;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.users.Usuario;
import com.antifraude.licensing.Empresa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "alertas_antifraude")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "transaccion_id", referencedColumnName = "id"),
            @JoinColumn(name = "fecha_transaccion", referencedColumnName = "fecha_transaccion")
    })
    private Transaccion transaccion;

    @Column(name = "fecha_transaccion", insertable = false, updatable = false)
    private OffsetDateTime fechaTransaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_id")
    @Transient
    private ReglaRiesgo regla;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(nullable = false, length = 30)
    private String severidad;

    @Column(nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    @Column(length = 30)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reglas_disparadas_json", columnDefinition = "jsonb")
    private String reglasDisparadasJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analista_asignado_id")
    private Usuario asignadoA;

    @Column(name = "fecha_hora_creacion", updatable = false)
    @Builder.Default
    private OffsetDateTime fechaGeneracion = OffsetDateTime.now();

    @Column(name = "fecha_hora_modificacion")
    private OffsetDateTime fechaHoraModificacion;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaResolucion;

    @Column(name = "fecha_asignacion")
    private OffsetDateTime fechaAsignacion;

    @Column(name = "resultado", length = 40)
    private String resultado;

    @Column(name = "requiere_aprobacion_supervisor")
    @Builder.Default
    private Boolean requiereAprobacionSupervisor = true;

    @Transient
    public String getPrioridad() {
        return severidad;
    }

    public void setPrioridad(String prioridad) {
        this.severidad = prioridad;
    }

    @Transient
    public String getObservacion() {
        return descripcion != null ? descripcion : motivo;
    }

    public void setObservacion(String observacion) {
        this.descripcion = observacion;
    }

    @Transient
    public UUID getEmpresaId() {
        return empresa != null ? empresa.getId() : null;
    }
}
