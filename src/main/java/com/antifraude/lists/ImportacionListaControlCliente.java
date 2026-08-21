package com.antifraude.lists;

import com.antifraude.security.tenant.TenantContext;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "importacion_lista_control_cliente")
public class ImportacionListaControlCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_control_cliente_id")
    private ListaControlCliente lista;

    @Column(name = "nombre_archivo", nullable = false, length = 220)
    private String nombreArchivo;

    @Column(name = "tipo_archivo", nullable = false, length = 20)
    private String tipoArchivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoImportacion estado = EstadoImportacion.RECIBIDA;

    @Column(name = "total_registros", nullable = false)
    private Integer totalRegistros = 0;

    @Column(name = "registros_validos", nullable = false)
    private Integer registrosValidos = 0;

    @Column(name = "registros_invalidos", nullable = false)
    private Integer registrosInvalidos = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "errores_json", nullable = false, columnDefinition = "jsonb")
    private String erroresJson = "[]";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_creacion_id", updatable = false)
    private Usuario usuarioCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_modificacion_id")
    private Usuario usuarioModificacion;

    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaHoraCreacion;

    @Column(name = "fecha_hora_modificacion")
    private OffsetDateTime fechaHoraModificacion;

    @PrePersist
    void onCreate() {
        if (empresaId == null) {
            empresaId = TenantContext.getEmpresaId();
        }
        if (empresaId == null) {
            throw new IllegalStateException("No se pudo determinar empresa_id para importacion_lista_control_cliente");
        }
        if (fechaHoraCreacion == null) {
            fechaHoraCreacion = OffsetDateTime.now();
        }
        fechaHoraModificacion = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        fechaHoraModificacion = OffsetDateTime.now();
    }

    public enum EstadoImportacion { RECIBIDA, PROCESADA, PROCESADA_CON_ERRORES, RECHAZADA }
}

