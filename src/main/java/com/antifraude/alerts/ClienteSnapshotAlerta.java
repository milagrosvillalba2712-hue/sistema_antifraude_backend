package com.antifraude.alerts;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente_snapshot_alerta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteSnapshotAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false, unique = true)
    private Alerta alerta;

    @Column(length = 80)
    private String fuente;

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "fecha_consulta")
    @Builder.Default
    private LocalDateTime fechaConsulta = LocalDateTime.now();
}
