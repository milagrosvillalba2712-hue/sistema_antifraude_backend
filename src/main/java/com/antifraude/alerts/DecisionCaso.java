package com.antifraude.alerts;

import com.antifraude.common.entity.Caso;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "decision_caso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private java.util.UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id", nullable = false)
    private Caso caso;

    @Transient
    private Alerta alerta;

    @Column(name = "resolucion_alerta_id")
    private Long resolucionAlertaId;

    @Transient
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String decision;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String justificacion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ejecutada = false;

    @Column(name = "fecha_decision")
    @Builder.Default
    private LocalDateTime fechaDecision = LocalDateTime.now();
}
