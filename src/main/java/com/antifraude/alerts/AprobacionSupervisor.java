package com.antifraude.alerts;

import com.antifraude.common.entity.Caso;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "aprobacion_supervisor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AprobacionSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id")
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Usuario supervisor;

    @Column(nullable = false, length = 40)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "fecha_aprobacion")
    @Builder.Default
    private LocalDateTime fechaAprobacion = LocalDateTime.now();
}
