package com.antifraude.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "catalogo_sync_estado", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "catalogo_codigo"})
})
public class CatalogoSyncEstado extends BaseEntity {

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "catalogo_codigo", nullable = false, length = 80)
    private String catalogoCodigo;

    @Column(name = "version", length = 40)
    private String version;

    @Column(name = "sha256", length = 128)
    private String sha256;

    @Column(name = "tabla_destino", length = 60)
    private String tablaDestino;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "items_recibidos", nullable = false)
    private Integer itemsRecibidos = 0;

    @Column(name = "items_upserted", nullable = false)
    private Integer itemsUpserted = 0;

    @Column(name = "items_desactivados", nullable = false)
    private Integer itemsDesactivados = 0;

    @Column(columnDefinition = "text")
    private String mensaje;

    @Column(name = "fecha_sync")
    private OffsetDateTime fechaSync;

    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaHoraCreacion = OffsetDateTime.now();

    @Column(name = "fecha_hora_modificacion", nullable = false)
    private OffsetDateTime fechaHoraModificacion = OffsetDateTime.now();
}
