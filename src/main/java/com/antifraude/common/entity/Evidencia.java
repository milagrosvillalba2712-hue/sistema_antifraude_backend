package com.antifraude.common.entity;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "evidencia")
public class Evidencia extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id", nullable = false)
    private Caso caso;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombreArchivo;

    @Column(name = "tipo_archivo", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoArchivo tipoArchivo;

    @Column(name = "referencia_archivo", nullable = false, length = 500)
    private String rutaAlmacenamiento;

    @Column(length = 300)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargado_por_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tamanio_bytes")
    private Long tamanoBytes;

    public enum TipoArchivo {
        PDF, XLSX, IMG, EML, OTRO
    }
}
