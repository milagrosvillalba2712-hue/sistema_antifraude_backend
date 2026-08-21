package com.antifraude.lists;

import com.antifraude.security.tenant.TenantContext;
import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "elemento_lista_control_cliente", uniqueConstraints = {
        @UniqueConstraint(name = "uk_elemento_lista_control_valor", columnNames = {"empresa_id", "lista_control_cliente_id", "tipo_identificador", "valor_normalizado"})
}, indexes = {
        @Index(name = "ix_elemento_lista_control_lookup", columnList = "empresa_id,tipo_identificador,valor_normalizado,estado")
})
public class ElementoListaControlCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lista_control_cliente_id", nullable = false)
    private ListaControlCliente lista;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entidad", nullable = false, length = 30)
    private TipoEntidadControl tipoEntidad = TipoEntidadControl.PERSONA;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificador", nullable = false, length = 40)
    private TipoIdentificadorControl tipoIdentificador = TipoIdentificadorControl.NOMBRE;

    @Column(name = "valor_original", nullable = false, columnDefinition = "TEXT")
    private String valorOriginal;

    @Column(name = "valor_normalizado", nullable = false, columnDefinition = "TEXT")
    private String valorNormalizado;

    @Column(name = "valor_hash")
    private byte[] valorHash;

    @Column(name = "nombre_mostrado", length = 180)
    private String nombreMostrado;

    @Column(name = "documento_mostrado", length = 80)
    private String documentoMostrado;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(nullable = false, length = 120)
    private String fuente = "CLIENTE";

    @Column(nullable = false, length = 20)
    private String severidad = "Media";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoElementoControl estado = EstadoElementoControl.ACTIVO;

    @Column(name = "fecha_vigencia_desde")
    private LocalDate fechaVigenciaDesde;

    @Column(name = "fecha_vigencia_hasta")
    private LocalDate fechaVigenciaHasta;

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
            throw new IllegalStateException("No se pudo determinar empresa_id para elemento_lista_control_cliente");
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

    public enum TipoEntidadControl { PERSONA, EMPRESA, CUENTA, DOCUMENTO, WALLET, ALIAS }
    public enum TipoIdentificadorControl { NOMBRE, DOCUMENTO, CUENTA, WALLET, ALIAS }
    public enum EstadoElementoControl { ACTIVO, INACTIVO, VENCIDO }
}

