package com.antifraude.licensing.catalog;

import java.util.List;
import java.util.Map;

/**
 * Estrategia de un catalogo del Control Plane hacia una tabla local del cliente.
 * Una implementacion por (codigo de catalogo CP -> tabla destino). La sincronizacion
 * hace upsert por codigo (inserta o actualiza) y soft-desactiva (activo=false) los
 * codigos que ya no vienen en la version vigente.
 */
public interface CatalogoDestino {

    /** Codigo del catalogo en el Control Plane (p. ej. PAISES_ISO). */
    String codigoControlPlane();

    /** Nombre de la tabla destino local (p. ej. pais). */
    String tabla();

    boolean existe(String codigo);

    /** Extrae el codigo unico del item (misma convencion que {@link #upsert}). */
    String codigoOf(Map<String, Object> item);

    void upsert(Map<String, Object> item);

    /** Desactiva los registros locales cuyo codigo no este en codigosVigentes. Devuelve el numero desactivado. */
    int desactivarAusentes(List<String> codigosVigentes);
}
