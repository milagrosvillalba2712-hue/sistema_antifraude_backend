package com.antifraude.external;

import com.antifraude.licensing.Empresa;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultas_externas")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsultaExterna {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "tipo_consulta", length = 50) private String tipoConsulta;
    @Column(name = "proveedor", length = 40) private String proveedor;
    @Column(name = "documento_hash", length = 64) private String documentoHash;
    @Column(name = "correlation_id", length = 120) private String correlationId;
    @Column(name = "status_http") private Integer statusHttp;
    @Column(name = "duracion_ms") private Long duracionMs;
    @Column(name = "intentos", nullable = false) private Integer intentos;
    @Column(name = "resultado") private Boolean resultado;
    @Column(name = "resultado_funcional", length = 40) private String resultadoFuncional;
    @Column(name = "categoria_error", length = 60) private String categoriaError;
    @Column(name = "estado", nullable = false, length = 40) private String estado;
    @Column(name = "fecha_consulta", updatable = false)
    @Builder.Default private LocalDateTime fechaConsulta = LocalDateTime.now();
}
