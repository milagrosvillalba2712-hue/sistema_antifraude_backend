package com.antifraude.licensing;

import com.antifraude.common.entity.CatalogoSyncEstado;
import com.antifraude.common.repository.CatalogoSyncEstadoRepository;
import com.antifraude.licensing.catalog.CatalogoDestino;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sincronizacion real y no bloqueante de catalogos AML desde el Control Plane
 * hacia las tablas locales del cliente.
 *
 * Por cada catalogo del manifiesto con un mapeo configurado:
 *  - si el sha256 no cambio respecto al ultimo sync, se marca SIN_CAMBIOS;
 *  - si cambio o es la primera vez, se descarga la version, se hace upsert por
 *    codigo y se soft-desactivan los codigos ausentes de la version vigente;
 *  - el estado se persiste en catalogo_sync_estado y se audita por evento.
 *
 * Los catalogos sin mapeo se registran como SIN_MAPEO (best-effort) y un fallo
 * en uno no bloquea a los demas ni al job.
 */
@Component
public class CatalogoSyncJob implements LicensingJob {

    private final LicensingControlPlaneClient controlPlaneClient;
    private final LicensingLocalService licensingService;
    private final CatalogoSyncEstadoRepository syncEstadoRepository;
    private final Map<String, CatalogoDestino> destinos;

    public CatalogoSyncJob(LicensingControlPlaneClient controlPlaneClient,
                           LicensingLocalService licensingService,
                           CatalogoSyncEstadoRepository syncEstadoRepository,
                           List<CatalogoDestino> destinoList) {
        this.controlPlaneClient = controlPlaneClient;
        this.licensingService = licensingService;
        this.syncEstadoRepository = syncEstadoRepository;
        this.destinos = destinoList.stream()
                .collect(Collectors.toMap(CatalogoDestino::codigoControlPlane, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    public String codigo() {
        return "CATALOG_SYNC";
    }

    @Override
    @Transactional
    public ResultadoJob ejecutar(ContextoJob contexto) {
        InstalacionLocal instalacion = contexto.instalacion();
        UUID empresaId = contexto.empresaId();

        List<Map<String, Object>> catalogs = controlPlaneClient.catalogManifestCatalogs();
        if (catalogs.isEmpty()) {
            licensingService.registrarEvento(instalacion, "CATALOG_SYNC", "SIN_CONECTIVIDAD",
                    Map.of("detalle", "Control Plane no disponible; no se pudo obtener el manifesto de catalogos"));
            return new ResultadoJob("SIN_CONECTIVIDAD", "Control Plane no disponible; catalogos sin sincronizar");
        }

        int sincronizados = 0;
        int sinCambios = 0;
        int sinMapeo = 0;
        int errores = 0;
        Map<String, Object> detalle = new LinkedHashMap<>();

        for (Map<String, Object> catalog : catalogs) {
            String code = String.valueOf(catalog.getOrDefault("code", ""));
            String version = String.valueOf(catalog.getOrDefault("version", ""));
            String sha256 = String.valueOf(catalog.getOrDefault("sha256", ""));
            detalle.put(code, Map.of("version", version, "sha256", sha256, "estado", "PROCESADO"));

            CatalogoDestino destino = destinos.get(code);
            if (destino == null) {
                registrarEstado(empresaId, code, version, sha256, null, "SIN_MAPEO", 0, 0, 0,
                        "Catalogo sin mapeo hacia tabla local; se omite (best-effort)");
                sinMapeo++;
                continue;
            }

            try {
                CatalogoSyncEstado previo = syncEstadoRepository
                        .findByEmpresaIdAndCatalogoCodigo(empresaId, code).orElse(null);
                if (previo != null && sha256.equals(previo.getSha256()) && sha256 != null && !sha256.isBlank()) {
                    registrarEstado(empresaId, code, version, sha256, destino.tabla(), "SIN_CAMBIOS",
                            previo.getItemsRecibidos(), previo.getItemsUpserted(), previo.getItemsDesactivados(),
                            "Version y sha256 ya sincronizados");
                    sinCambios++;
                    continue;
                }

                Map<String, Object> descarga = controlPlaneClient.catalogVersion(code, version);
                if (!Boolean.TRUE.equals(descarga.get("online"))) {
                    registrarEstado(empresaId, code, version, sha256, destino.tabla(), "ERROR", 0, 0, 0,
                            "No se pudo descargar la version del Control Plane");
                    errores++;
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) descarga.getOrDefault("items", List.of());

                int procesados = 0;
                List<String> vigentes = new ArrayList<>();
                for (Map<String, Object> item : items) {
                    String codigoItem = destino.codigoOf(item);
                    if (codigoItem == null || codigoItem.isBlank()) {
                        continue;
                    }
                    vigentes.add(codigoItem);
                    destino.upsert(item);
                    procesados++;
                }

                int desactivados = 0;
                if (!items.isEmpty() && !vigentes.isEmpty()) {
                    desactivados = destino.desactivarAusentes(vigentes);
                }

                registrarEstado(empresaId, code, version, sha256, destino.tabla(), "OK",
                        procesados, procesados, desactivados, "Catalogo sincronizado satisfactoriamente");
                sincronizados++;
            } catch (RuntimeException exception) {
                registrarEstado(empresaId, code, version, sha256, destino.tabla(), "ERROR", 0, 0, 0,
                        exception.getClass().getSimpleName() + ": " + exception.getMessage());
                errores++;
            }
        }

        String resultado = errores > 0 ? "PARCIAL" : (sinMapeo > 0 || sinCambios > 0 ? "OK" : "OK");
        String detalleResumen = String.format(
                "Sincronizacion de catalogos: %d sincronizados, %d sin cambios, %d sin mapeo, %d errores sobre %d catalogos",
                sincronizados, sinCambios, sinMapeo, errores, catalogs.size());

        licensingService.registrarEvento(instalacion, "CATALOG_SYNC", resultado, Map.of(
                "sincronizados", sincronizados,
                "sinCambios", sinCambios,
                "sinMapeo", sinMapeo,
                "errores", errores,
                "total", catalogs.size(),
                "catalogos", detalle));
        return new ResultadoJob(resultado, detalleResumen);
    }

    private void registrarEstado(UUID empresaId, String code, String version, String sha256, String tabla,
                                 String estado, int recibidos, int upserted, int desactivados, String mensaje) {
        CatalogoSyncEstado sync = syncEstadoRepository
                .findByEmpresaIdAndCatalogoCodigo(empresaId, code)
                .orElseGet(CatalogoSyncEstado::new);
        sync.setEmpresaId(empresaId);
        sync.setCatalogoCodigo(code);
        sync.setVersion(version);
        sync.setSha256(sha256);
        sync.setTablaDestino(tabla);
        sync.setEstado(estado);
        sync.setItemsRecibidos(recibidos);
        sync.setItemsUpserted(upserted);
        sync.setItemsDesactivados(desactivados);
        sync.setMensaje(mensaje);
        sync.setFechaSync(OffsetDateTime.now());
        sync.setFechaHoraModificacion(OffsetDateTime.now());
        if (sync.getFechaHoraCreacion() == null) {
            sync.setFechaHoraCreacion(OffsetDateTime.now());
        }
        syncEstadoRepository.save(sync);
    }
}
