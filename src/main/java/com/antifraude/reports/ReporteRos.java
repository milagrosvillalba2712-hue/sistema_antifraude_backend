package com.antifraude.reports;

import com.antifraude.alerts.Alerta;
import com.antifraude.common.entity.Caso;
import com.antifraude.licensing.Empresa;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reportes_ros")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteRos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id")
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id")
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generado_por")
    private Usuario generadoPor;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String estado = "GENERADO";

    @Column(name = "descripcion_sospecha", nullable = false, columnDefinition = "TEXT")
    private String descripcionSospecha;

    @Column(name = "soporte_referencia", length = 260)
    private String soporteReferencia;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reporte_json", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String reporteJson = "{}";

    @Column(name = "fecha_generacion", updatable = false)
    @Builder.Default
    private OffsetDateTime fechaGeneracion = OffsetDateTime.now();
}
