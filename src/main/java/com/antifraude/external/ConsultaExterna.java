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

    @Column(name = "identificador_documento", length = 30)
    private String identificadorDocumento;

    @Column(name = "tipo_consulta", length = 50)
    private String tipoConsulta;

    private Boolean resultado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_consulta", updatable = false)
    @Builder.Default
    private LocalDateTime fechaConsulta = LocalDateTime.now();
}
