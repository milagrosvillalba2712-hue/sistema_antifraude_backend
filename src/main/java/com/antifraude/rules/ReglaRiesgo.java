package com.antifraude.rules;

import com.antifraude.common.entity.Escenario;
import com.antifraude.licensing.Empresa;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(name = "condiciones_json", columnDefinition = "TEXT")
    private String condicionesJson;

    @Column(name = "acciones_json", columnDefinition = "TEXT")
    private String accionesJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escenario_id")
    private Escenario escenario;

    @Column(name = "score_base", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal scoreBase = BigDecimal.ZERO;

    @Column(name = "parametros", columnDefinition = "TEXT")
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
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}
