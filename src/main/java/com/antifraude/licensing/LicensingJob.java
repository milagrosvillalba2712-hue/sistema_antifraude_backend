package com.antifraude.licensing;

import java.util.UUID;

/**
 * Contrato de un job de licenciamiento on-premise (agente). Cada implementacion
 * resuelve un codigo estable y ejecuta una accion operativa real contra el
 * Control Plane y/o la instalacion local, registrando eventos auditables.
 */
public interface LicensingJob {

    String codigo();

    ResultadoJob ejecutar(ContextoJob contexto);

    record ContextoJob(UUID empresaId, InstalacionLocal instalacion) {
    }

    record ResultadoJob(String resultado, String detalle) {
    }
}