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
@Table(name = "lista_control_cliente", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lista_control_cliente_empresa_codigo", columnNames = {"empresa_id", "tipo_lista", "codigo"})
}, indexes = {
        @Index(name = "ix_lista_control_cliente_empresa_tipo", columnList = "empresa_id,tipo_lista,estado")
})
public class ListaControlCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lista", nullable = false, length = 20)
    private TipoListaControl tipoLista;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoListaControl estado = EstadoListaControl.ACTIVA;

    @Column(nullable = false)
    private Integer prioridad = 50;

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
            throw new IllegalStateException("No se pudo determinar empresa_id para lista_control_cliente");
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

    public enum TipoListaControl { WHITELIST, BLACKLIST }
    public enum EstadoListaControl { ACTIVA, INACTIVA, VENCIDA }
}

