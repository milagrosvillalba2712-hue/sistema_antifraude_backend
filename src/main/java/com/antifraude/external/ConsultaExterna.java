package com.antifraude.external;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultas_externas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    @Column(name = "identificador_documento", length = 30)
    private String identificadorDocumento;

    @Transient
    @Column(name = "tipo_consulta", length = 50)
    private String tipoConsulta;

    @Transient
    private Boolean resultado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_creacion_id", insertable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "fecha_consulta", updatable = false)
    @Builder.Default
    private LocalDateTime fechaConsulta = LocalDateTime.now();
}
