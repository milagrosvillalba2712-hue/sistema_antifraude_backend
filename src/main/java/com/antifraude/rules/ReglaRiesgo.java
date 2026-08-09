package com.antifraude.rules;

import com.antifraude.common.entity.Escenario;
import com.antifraude.licensing.Empresa;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reglas_riesgo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReglaRiesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(unique = true, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "tipo_regla", length = 50)
    private String tipoRegla;

    @Column(length = 20)
    private String severidad;

    @Builder.Default
    private Integer prioridad = 0;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String condicion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condiciones_json", columnDefinition = "jsonb")
    private String condicionesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "acciones_json", columnDefinition = "jsonb")
    private String accionesJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escenario_id")
    private Escenario escenario;

    @Column(name = "score_base", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal scoreBase = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parametros", columnDefinition = "jsonb")
    private String parametros;

    @Column(length = 20)
    @Builder.Default
    private String estado = "ACTIVA";

    @Builder.Default
    private Integer version = 1;

    @Column(name = "version_anterior_id")
    private Long versionAnteriorId;

    @Builder.Default
    private Boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por")
    private Usuario creadaPor;

    @Column(name = "fecha_creacion", updatable = false)
    @Builder.Default
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();

    @Column(name = "fecha_modificacion")
    private OffsetDateTime fechaModificacion;

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = OffsetDateTime.now();
    }
}
