package com.antifraude.alerts;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evidencia_alerta", indexes = {
        @Index(name = "idx_evidencia_alerta_alerta", columnList = "alerta_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenciaAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 60)
    private String tipo;

    @Column(length = 20)
    private String extension;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "hash_archivo", length = 128)
    private String hash;

    @Column(name = "referencia_archivo", length = 500)
    private String referenciaArchivo;

    @Column(length = 30)
    @Builder.Default
    private String estado = "CARGADA";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargado_por")
    private Usuario cargadoPor;

    @Column(name = "fecha_carga", updatable = false)
    @Builder.Default
    private LocalDateTime fechaCarga = LocalDateTime.now();
}
