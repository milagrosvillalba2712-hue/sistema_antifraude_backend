package com.antifraude.alerts;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resolucion_alerta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolucionAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Resultado resultado;

    @Column(columnDefinition = "TEXT")
    private String conclusion;

    @Column(columnDefinition = "TEXT")
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String justificacion;

    @Column(name = "evidencia_descripcion", columnDefinition = "TEXT")
    private String evidenciaDescripcion;

    @Column(name = "contacto_cliente", columnDefinition = "TEXT")
    private String contactoCliente;

    @Builder.Default
    private Boolean fondosRetenidos = false;

    @Builder.Default
    private Boolean movimientoLiberable = false;

    @Builder.Default
    private Boolean requiereRos = false;

    @Builder.Default
    private Boolean requiereBloqueo = false;

    @Builder.Default
    private Boolean requiereEscalamientoLegal = false;

    @Column(name = "fecha_resolucion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaResolucion = LocalDateTime.now();

    public enum Resultado {
        FRAUDE_CONFIRMADO, FALSO_POSITIVO, OPERACION_JUSTIFICADA, ESCALAR, ROS_REQUERIDO
    }
}
